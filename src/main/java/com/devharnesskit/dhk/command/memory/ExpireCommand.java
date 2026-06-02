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

public final class ExpireCommand implements Command {
    private final DbConnectionFactory connectionFactory;
    private final ProjectService projectService;
    private final MemoryRepository memoryRepository;

    public ExpireCommand() {
        this(new DbConnectionFactory(), new ProjectService(), new MemoryRepository());
    }

    ExpireCommand(DbConnectionFactory connectionFactory, ProjectService projectService,
                  MemoryRepository memoryRepository) {
        this.connectionFactory = connectionFactory;
        this.projectService = projectService;
        this.memoryRepository = memoryRepository;
    }

    public int run(CommandContext context, Args args) {
        if (!args.hasOption("id")) {
            return CommandErrorGuidance.missing(context, args, "MEMORY_EXPIRE_ID_MISSING",
                    new String[]{"--id"}, "dhk memory expire --id <id> --reason \"<reason>\"",
                    "README.md#core-path");
        }
        String reason = args.option("reason", "").trim();
        if (reason.length() == 0) {
            return CommandErrorGuidance.missing(context, args, "MEMORY_EXPIRE_REASON_MISSING",
                    new String[]{"--reason"}, "dhk memory expire --id <id> --reason \"<reason>\"",
                    "README.md#core-path");
        }
        if (MemoryCommandSupport.rejectSensitive(context, reason)) {
            return ExitCodes.VALIDATION_ERROR;
        }
        long id = MemoryCommandSupport.parseId(context, args, "memory id", args.option("id"),
                "MEMORY_EXPIRE_INVALID_ID", "dhk memory expire --id <id> --reason \"<reason>\"");
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
            memoryRepository.expire(connection, project.projectKey(), id, reason, context.clock().now().toString());
            if (JsonOutput.enabled(args)) {
                context.out().print(JsonOutput.object(
                        JsonOutput.stringField("command", "memory expire"),
                        JsonOutput.numberField("memory_id", id),
                        JsonOutput.stringField("status", "deprecated")
                ));
            } else {
                context.out().println("memory_id: " + id);
                context.out().println("status: deprecated");
            }
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR memory expire failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
