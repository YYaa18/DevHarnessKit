package com.devharnesskit.dhk.command.memory;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.TransactionTemplate;
import com.devharnesskit.dhk.guidance.CommandErrorGuidance;
import com.devharnesskit.dhk.guidance.EnumGuidance;
import com.devharnesskit.dhk.model.MemoryItem;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.repository.FtsRepository;
import com.devharnesskit.dhk.repository.MemoryRepository;
import com.devharnesskit.dhk.service.MemoryType;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.service.SensitiveDataGuard;
import com.devharnesskit.dhk.util.InputUtil;
import com.devharnesskit.dhk.util.PathUtil;
import com.devharnesskit.dhk.util.TagUtil;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public final class AddCommand implements Command {
    private final DbConnectionFactory connectionFactory;
    private final ProjectService projectService;
    private final MemoryRepository memoryRepository;
    private final FtsRepository ftsRepository;
    private final SensitiveDataGuard sensitiveDataGuard;
    private final TransactionTemplate transactionTemplate;

    public AddCommand() {
        this(new DbConnectionFactory(), new ProjectService(), new MemoryRepository(),
                new FtsRepository(), new SensitiveDataGuard(), new TransactionTemplate());
    }

    AddCommand(DbConnectionFactory connectionFactory, ProjectService projectService,
               MemoryRepository memoryRepository, FtsRepository ftsRepository,
               SensitiveDataGuard sensitiveDataGuard, TransactionTemplate transactionTemplate) {
        this.connectionFactory = connectionFactory;
        this.projectService = projectService;
        this.memoryRepository = memoryRepository;
        this.ftsRepository = ftsRepository;
        this.sensitiveDataGuard = sensitiveDataGuard;
        this.transactionTemplate = transactionTemplate;
    }

    public int run(CommandContext context, Args args) {
        String type = args.option("type").trim();
        String title = args.option("title").trim();
        String content;
        try {
            content = InputUtil.readExclusiveText(context, args, "content", "content-file", "content-stdin").trim();
        } catch (InputUtil.InputException ex) {
            return CommandErrorGuidance.missing(context, args, "MEMORY_ADD_ARGUMENTS_MISSING",
                    new String[]{"--type", "--title", "--content or --content-file or --content-stdin"},
                    "dhk memory add --type project_fact --title \"<title>\" --content \"<content>\"",
                    "README.md#core-path");
        }
        if (type.length() == 0 || title.length() == 0 || content.length() == 0) {
            return CommandErrorGuidance.missing(context, args, "MEMORY_ADD_ARGUMENTS_MISSING",
                    new String[]{"--type", "--title", "--content or --content-file or --content-stdin"},
                    "dhk memory add --type project_fact --title \"<title>\" --content \"<content>\"",
                    "README.md#core-path");
        }
        if (!MemoryType.isAllowed(type)) {
            return EnumGuidance.printInvalid(context, args, "INVALID_MEMORY_TYPE", "memory type", type,
                    EnumGuidance.MEMORY_TYPES, new String[0],
                    "dhk memory add --type project_fact --title \"<title>\" --content \"<content>\"",
                    "README.md#core-path");
        }
        if (args.hasOption("status") && !"draft".equals(args.option("status"))) {
            context.err().println("memory add only supports --status draft; use memory confirm to confirm facts");
            return ExitCodes.VALIDATION_ERROR;
        }

        int confidence = parseConfidence(context, args, args.option("confidence", "50"));
        if (confidence < 0) {
            return ExitCodes.VALIDATION_ERROR;
        }

        String module = args.option("module", "global").trim();
        if (module.length() == 0) {
            module = "global";
        }
        String tags = TagUtil.normalize(args.option("tags", ""));
        String source = args.option("source", "").trim();
        String evidence = args.option("evidence", "").trim();
        String sensitiveInput = title + "\n" + content + "\n" + tags + "\n" + source + "\n" + evidence;
        List<String> matches = sensitiveDataGuard.findMatches(sensitiveInput);
        if (!matches.isEmpty()) {
            context.err().println("Sensitive data rejected: " + matches);
            return ExitCodes.VALIDATION_ERROR;
        }

        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        try (Connection connection = connectionFactory.open(projectRoot)) {
            Project project = MemoryCommandSupport.requireInitializedProject(context, projectRoot, projectService, connection);
            if (project == null) {
                return ExitCodes.NOT_FOUND;
            }
            final String now = context.clock().now().toString();
            final Project currentProject = project;
            final MemoryItem item = new MemoryItem(0L, currentProject.projectKey(), module, type, "project",
                    title, content, tags, "draft", confidence, "manual", "",
                    "", source, evidence, "", "", now, now, "", 0);
            Long insertedId = transactionTemplate.execute(connection, new TransactionTemplate.Work<Long>() {
                public Long execute() throws Exception {
                    long id = memoryRepository.insert(connection, item);
                    MemoryItem inserted = memoryRepository.findById(connection, currentProject.projectKey(), id);
                    ftsRepository.sync(connection, inserted);
                    return Long.valueOf(id);
                }
            });
            long id = insertedId.longValue();
            context.out().println("memory_id: " + id);
            context.out().println("status: draft");
            return ExitCodes.SUCCESS;
        } catch (SQLException ex) {
            context.err().println("ERROR memory add failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        } catch (RuntimeException ex) {
            context.err().println("ERROR memory add failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        } catch (Exception ex) {
            context.err().println("ERROR memory add failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private int parseConfidence(CommandContext context, Args args, String rawValue) {
        try {
            int value = Integer.parseInt(rawValue);
            if (value < 0 || value > 100) {
                CommandErrorGuidance.invalidNumber(context, args, "INVALID_MEMORY_CONFIDENCE",
                        "memory confidence", rawValue, "confidence must be an integer from 0 to 100.",
                        "dhk memory add --confidence 70 ...", "README.md#core-path");
                return -1;
            }
            return value;
        } catch (NumberFormatException ex) {
            CommandErrorGuidance.invalidNumber(context, args, "INVALID_MEMORY_CONFIDENCE",
                    "memory confidence", rawValue, "confidence must be an integer from 0 to 100.",
                    "dhk memory add --confidence 70 ...", "README.md#core-path");
            return -1;
        }
    }
}
