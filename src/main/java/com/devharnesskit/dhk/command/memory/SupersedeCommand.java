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
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;
import java.sql.Connection;

public final class SupersedeCommand implements Command {
    private final DbConnectionFactory connectionFactory;
    private final ProjectService projectService;
    private final MemoryRepository memoryRepository;

    public SupersedeCommand() {
        this(new DbConnectionFactory(), new ProjectService(), new MemoryRepository());
    }

    SupersedeCommand(DbConnectionFactory connectionFactory, ProjectService projectService,
                     MemoryRepository memoryRepository) {
        this.connectionFactory = connectionFactory;
        this.projectService = projectService;
        this.memoryRepository = memoryRepository;
    }

    public int run(CommandContext context, Args args) {
        if (!args.hasOption("old") || !args.hasOption("new")) {
            return CommandErrorGuidance.missing(context, args, "MEMORY_SUPERSEDE_ARGUMENTS_MISSING",
                    new String[]{"--old", "--new"},
                    "dhk memory supersede --old <id> --new <id> --reason \"<reason>\"",
                    "README.md#core-path");
        }
        String reason = args.option("reason", "").trim();
        if (reason.length() == 0) {
            return CommandErrorGuidance.missing(context, args, "MEMORY_SUPERSEDE_REASON_MISSING",
                    new String[]{"--reason"}, "dhk memory supersede --old <id> --new <id> --reason \"<reason>\"",
                    "README.md#core-path");
        }
        if (MemoryCommandSupport.rejectSensitive(context, reason)) {
            return ExitCodes.VALIDATION_ERROR;
        }
        long oldId = MemoryCommandSupport.parseId(context, args, "old memory id", args.option("old"),
                "MEMORY_SUPERSEDE_INVALID_ID", "dhk memory supersede --old <id> --new <id>");
        long newId = MemoryCommandSupport.parseId(context, args, "new memory id", args.option("new"),
                "MEMORY_SUPERSEDE_INVALID_ID", "dhk memory supersede --old <id> --new <id>");
        if (oldId <= 0 || newId <= 0) {
            return ExitCodes.VALIDATION_ERROR;
        }
        if (oldId == newId) {
            context.err().println("old and new memory ids must be different");
            return ExitCodes.VALIDATION_ERROR;
        }
        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        try (Connection connection = connectionFactory.open(projectRoot)) {
            Project project = MemoryCommandSupport.requireInitializedProject(context, projectRoot, projectService, connection);
            if (project == null) {
                return ExitCodes.NOT_FOUND;
            }
            MemoryItem oldItem = memoryRepository.findById(connection, project.projectKey(), oldId);
            MemoryItem newItem = memoryRepository.findById(connection, project.projectKey(), newId);
            if (oldItem == null || newItem == null) {
                return CommandErrorGuidance.notFound(context, args, "MEMORY_ITEM_NOT_FOUND",
                        "memory item", oldItem == null ? Long.toString(oldId) : Long.toString(newId),
                        "dhk memory list", "README.md#core-path");
            }
            if ("archived".equals(oldItem.status()) || "archived".equals(newItem.status())) {
                context.err().println("archived memory cannot participate in supersede");
                return ExitCodes.VALIDATION_ERROR;
            }
            memoryRepository.supersede(connection, project.projectKey(), oldId, newId, reason,
                    context.clock().now().toString());
            if (JsonOutput.enabled(args)) {
                context.out().print(JsonOutput.object(
                        JsonOutput.stringField("command", "memory supersede"),
                        JsonOutput.numberField("old_id", oldId),
                        JsonOutput.numberField("new_id", newId),
                        JsonOutput.stringField("status", "deprecated")
                ));
            } else {
                context.out().println("old_id: " + oldId);
                context.out().println("new_id: " + newId);
                context.out().println("status: deprecated");
            }
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR memory supersede failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
