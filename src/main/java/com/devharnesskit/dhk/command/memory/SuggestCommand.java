package com.devharnesskit.dhk.command.memory;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.guidance.CommandErrorGuidance;
import com.devharnesskit.dhk.guidance.EnumGuidance;
import com.devharnesskit.dhk.model.MemoryCandidate;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.repository.MemoryRepository;
import com.devharnesskit.dhk.service.MemoryIdentity;
import com.devharnesskit.dhk.service.MemoryType;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.util.InputUtil;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;
import com.devharnesskit.dhk.util.TagUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;

public final class SuggestCommand implements Command {
    private final DbConnectionFactory connectionFactory;
    private final ProjectService projectService;
    private final MemoryRepository memoryRepository;

    public SuggestCommand() {
        this(new DbConnectionFactory(), new ProjectService(), new MemoryRepository());
    }

    SuggestCommand(DbConnectionFactory connectionFactory, ProjectService projectService,
                   MemoryRepository memoryRepository) {
        this.connectionFactory = connectionFactory;
        this.projectService = projectService;
        this.memoryRepository = memoryRepository;
    }

    public int run(CommandContext context, Args args) {
        String title = args.option("title").trim();
        String type = args.option("type", "project_fact").trim();
        if (title.length() == 0) {
            return CommandErrorGuidance.missing(context, args, "MEMORY_SUGGEST_ARGUMENTS_MISSING",
                    new String[]{"--title"}, "dhk memory suggest --title \"<title>\" --content \"<content>\"",
                    "README.md#core-path");
        }
        if (!MemoryType.isAllowed(type)) {
            return EnumGuidance.printInvalid(context, args, "INVALID_MEMORY_TYPE", "memory type", type,
                    EnumGuidance.MEMORY_TYPES, new String[0],
                    "dhk memory suggest --type project_fact --title \"<title>\" --content \"<content>\"",
                    "README.md#core-path");
        }
        int confidence = MemoryCommandSupport.parseConfidence(context, args,
                args.option("confidence", "50"), "dhk memory suggest --confidence 70 ...");
        if (confidence < 0) {
            return ExitCodes.VALIDATION_ERROR;
        }
        String content;
        String sourceKind = args.option("from", "text").trim();
        if (!"text".equals(sourceKind) && !"file".equals(sourceKind)) {
            context.err().println("Invalid memory suggestion source: " + sourceKind);
            context.err().println("valid_values: text, file");
            return ExitCodes.VALIDATION_ERROR;
        }
        try {
            content = readContent(context, args, sourceKind).trim();
        } catch (InputUtil.InputException ex) {
            return CommandErrorGuidance.missing(context, args, "MEMORY_SUGGEST_ARGUMENTS_MISSING",
                    new String[]{"--content, --content-file, --content-stdin, or --from file --path <path>"},
                    "dhk memory suggest --title \"<title>\" --content \"<content>\"", "README.md#core-path");
        }
        if (content.length() == 0) {
            return CommandErrorGuidance.missing(context, args, "MEMORY_SUGGEST_ARGUMENTS_MISSING",
                    new String[]{"non-empty content"}, "dhk memory suggest --title \"<title>\" --content \"<content>\"",
                    "README.md#core-path");
        }
        String module = args.option("module", "global").trim();
        if (module.length() == 0) {
            module = "global";
        }
        String tags = TagUtil.normalize(args.option("tags", ""));
        String sourceRef = args.option("source-ref", args.option("path", "")).trim();
        String reason = args.option("reason", "").trim();
        String evidence = args.option("evidence", "").trim();
        String sensitiveInput = title + "\n" + content + "\n" + tags + "\n" + sourceRef + "\n" + reason + "\n" + evidence;
        if (MemoryCommandSupport.rejectSensitive(context, sensitiveInput)) {
            return ExitCodes.VALIDATION_ERROR;
        }

        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        try (Connection connection = connectionFactory.open(projectRoot)) {
            Project project = MemoryCommandSupport.requireInitializedProject(context, projectRoot, projectService, connection);
            if (project == null) {
                return ExitCodes.NOT_FOUND;
            }
            String fingerprint = MemoryIdentity.fingerprint(title, content);
            String canonicalKey = MemoryIdentity.canonicalKey(module, title);
            String now = context.clock().now().toString();
            MemoryCandidate candidate = new MemoryCandidate(0L, project.projectKey(), "pending", module, type,
                    title, content, tags, confidence, sourceKind, sourceRef, reason, evidence,
                    fingerprint, canonicalKey, now, now, "", "", 0L);
            long id = memoryRepository.insertCandidate(connection, candidate);
            if (JsonOutput.enabled(args)) {
                context.out().print(JsonOutput.object(
                        JsonOutput.stringField("command", "memory suggest"),
                        JsonOutput.numberField("candidate_id", id),
                        JsonOutput.stringField("status", "pending"),
                        JsonOutput.stringField("fingerprint", fingerprint),
                        JsonOutput.stringField("canonical_key", canonicalKey)
                ));
            } else {
                context.out().println("candidate_id: " + id);
                context.out().println("status: pending");
                context.out().println("fingerprint: " + fingerprint);
                context.out().println("canonical_key: " + canonicalKey);
            }
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR memory suggest failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private String readContent(CommandContext context, Args args, String sourceKind) throws InputUtil.InputException {
        if ("file".equals(sourceKind) && args.hasOption("path")) {
            Path path = PathUtil.resolvePath(args.option("path"), context.workingDirectory());
            try {
                if (!Files.isRegularFile(path)) {
                    throw new InputUtil.InputException("--path is not a regular file: " + path);
                }
                return new String(Files.readAllBytes(path), "UTF-8");
            } catch (InputUtil.InputException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new InputUtil.InputException("Failed to read --path: " + ex.getMessage());
            }
        }
        return InputUtil.readExclusiveText(context, args, "content", "content-file", "content-stdin");
    }
}
