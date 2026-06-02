package com.devharnesskit.dhk.command.memory;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.guidance.CommandErrorGuidance;
import com.devharnesskit.dhk.model.MemoryItem;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.repository.MemoryRepository;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.util.InputUtil;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;
import java.sql.Connection;

public final class RefreshCommand implements Command {
    private final DbConnectionFactory connectionFactory;
    private final ProjectService projectService;
    private final MemoryRepository memoryRepository;

    public RefreshCommand() {
        this(new DbConnectionFactory(), new ProjectService(), new MemoryRepository());
    }

    RefreshCommand(DbConnectionFactory connectionFactory, ProjectService projectService,
                   MemoryRepository memoryRepository) {
        this.connectionFactory = connectionFactory;
        this.projectService = projectService;
        this.memoryRepository = memoryRepository;
    }

    public int run(CommandContext context, Args args) {
        if (!args.hasOption("id")) {
            return CommandErrorGuidance.missing(context, args, "MEMORY_REFRESH_ID_MISSING",
                    new String[]{"--id"}, "dhk memory refresh --id <id> --evidence \"<evidence>\"",
                    "README.md#core-path");
        }
        String evidence;
        try {
            evidence = InputUtil.readExclusiveText(context, args, "evidence", "evidence-file", "evidence-stdin").trim();
        } catch (InputUtil.InputException ex) {
            return CommandErrorGuidance.missing(context, args, "MEMORY_REFRESH_EVIDENCE_MISSING",
                    new String[]{"--evidence, --evidence-file, or --evidence-stdin"},
                    "dhk memory refresh --id <id> --evidence \"<evidence>\"", "README.md#core-path");
        }
        if (evidence.length() == 0) {
            return CommandErrorGuidance.missing(context, args, "MEMORY_REFRESH_EVIDENCE_MISSING",
                    new String[]{"non-empty evidence"}, "dhk memory refresh --id <id> --evidence \"<evidence>\"",
                    "README.md#core-path");
        }
        if (MemoryCommandSupport.rejectSensitive(context, evidence)) {
            return ExitCodes.VALIDATION_ERROR;
        }
        long id = MemoryCommandSupport.parseId(context, args, "memory id", args.option("id"),
                "MEMORY_REFRESH_INVALID_ID", "dhk memory refresh --id <id> --evidence \"<evidence>\"");
        if (id <= 0) {
            return ExitCodes.VALIDATION_ERROR;
        }
        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        try (Connection connection = connectionFactory.open(projectRoot)) {
            Project project = MemoryCommandSupport.requireInitializedProject(context, projectRoot, projectService, connection);
            if (project == null) {
                return ExitCodes.NOT_FOUND;
            }
            MemoryItem item = memoryRepository.findById(connection, project.projectKey(), id);
            if (item == null) {
                return CommandErrorGuidance.notFound(context, args, "MEMORY_ITEM_NOT_FOUND",
                        "memory item", Long.toString(id), "dhk memory list", "README.md#core-path");
            }
            if ("archived".equals(item.status())) {
                context.err().println("archived memory cannot be refreshed");
                return ExitCodes.VALIDATION_ERROR;
            }
            memoryRepository.refresh(connection, project.projectKey(), id, evidence, context.clock().now().toString());
            MemoryItem refreshed = memoryRepository.findById(connection, project.projectKey(), id);
            if (JsonOutput.enabled(args)) {
                context.out().print(JsonOutput.object(
                        JsonOutput.stringField("command", "memory refresh"),
                        JsonOutput.numberField("memory_id", id),
                        JsonOutput.stringField("status", refreshed.status()),
                        JsonOutput.stringField("stale", "false")
                ));
            } else {
                context.out().println("memory_id: " + id);
                context.out().println("status: " + refreshed.status());
                context.out().println("stale: false");
            }
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR memory refresh failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
