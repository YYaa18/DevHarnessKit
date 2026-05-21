package com.devharnesskit.dhk.command.memory;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.model.MemoryItem;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.repository.FtsRepository;
import com.devharnesskit.dhk.repository.MemoryRepository;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

public final class ConfirmCommand implements Command {
    private final DbConnectionFactory connectionFactory;
    private final ProjectService projectService;
    private final MemoryRepository memoryRepository;
    private final FtsRepository ftsRepository;

    public ConfirmCommand() {
        this(new DbConnectionFactory(), new ProjectService(), new MemoryRepository(), new FtsRepository());
    }

    ConfirmCommand(DbConnectionFactory connectionFactory, ProjectService projectService,
                   MemoryRepository memoryRepository, FtsRepository ftsRepository) {
        this.connectionFactory = connectionFactory;
        this.projectService = projectService;
        this.memoryRepository = memoryRepository;
        this.ftsRepository = ftsRepository;
    }

    public int run(CommandContext context, Args args) {
        if (!args.hasOption("id")) {
            context.err().println("Missing required parameter: --id");
            return ExitCodes.USAGE_ERROR;
        }
        long id = parseId(context, args.option("id"));
        if (id <= 0) {
            return ExitCodes.VALIDATION_ERROR;
        }

        Integer requestedConfidence = null;
        if (args.hasOption("confidence")) {
            requestedConfidence = parseConfidence(context, args.option("confidence"));
            if (requestedConfidence == null) {
                return ExitCodes.VALIDATION_ERROR;
            }
        }

        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        try (Connection connection = connectionFactory.open(projectRoot)) {
            Project project = MemoryCommandSupport.requireInitializedProject(context, projectRoot, projectService, connection);
            if (project == null) {
                return ExitCodes.NOT_FOUND;
            }
            MemoryItem item = memoryRepository.findById(connection, project.projectKey(), id);
            if (item == null) {
                context.err().println("Memory item not found: " + id);
                return ExitCodes.NOT_FOUND;
            }
            int confidence = requestedConfidence == null ? Math.max(item.confidence(), 70) : requestedConfidence.intValue();
            String confirmedBy = args.option("confirmed-by", "manual").trim();
            if (confirmedBy.length() == 0) {
                confirmedBy = "manual";
            }
            String now = context.clock().now().toString();
            memoryRepository.confirm(connection, project.projectKey(), id, confidence, confirmedBy, now);
            MemoryItem updated = memoryRepository.findById(connection, project.projectKey(), id);
            ftsRepository.sync(connection, updated);
            context.out().println("memory_id: " + id);
            context.out().println("status: confirmed");
            context.out().println("confidence: " + confidence);
            context.out().println("confirmed_by: " + confirmedBy);
            return ExitCodes.SUCCESS;
        } catch (SQLException ex) {
            context.err().println("ERROR memory confirm failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        } catch (RuntimeException ex) {
            context.err().println("ERROR memory confirm failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private long parseId(CommandContext context, String rawValue) {
        try {
            return Long.parseLong(rawValue);
        } catch (NumberFormatException ex) {
            context.err().println("Invalid id, expected integer: " + rawValue);
            return -1L;
        }
    }

    private Integer parseConfidence(CommandContext context, String rawValue) {
        try {
            int value = Integer.parseInt(rawValue);
            if (value < 0 || value > 100) {
                context.err().println("Invalid confidence, expected 0..100: " + rawValue);
                return null;
            }
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            context.err().println("Invalid confidence, expected integer 0..100: " + rawValue);
            return null;
        }
    }
}
