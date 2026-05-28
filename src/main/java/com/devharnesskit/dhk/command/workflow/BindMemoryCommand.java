package com.devharnesskit.dhk.command.workflow;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.guidance.CommandErrorGuidance;
import com.devharnesskit.dhk.guidance.EnumGuidance;
import com.devharnesskit.dhk.model.MemoryItem;
import com.devharnesskit.dhk.model.workflow.WorkflowRun;
import com.devharnesskit.dhk.repository.MemoryRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowArtifactRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowCheckpointBindingRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowEventRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowMemoryBindingRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowRunRepository;
import com.devharnesskit.dhk.service.SensitiveDataGuard;
import com.devharnesskit.dhk.service.workflow.WorkflowArtifactService;

import java.nio.file.Path;
import java.sql.Connection;

public final class BindMemoryCommand implements Command {
    private final DbConnectionFactory connectionFactory = new DbConnectionFactory();
    private final MigrationRunner migrationRunner = new MigrationRunner();
    private final WorkflowRunRepository runRepository = new WorkflowRunRepository();
    private final MemoryRepository memoryRepository = new MemoryRepository();
    private final SensitiveDataGuard sensitiveDataGuard = new SensitiveDataGuard();
    private final WorkflowArtifactService artifactService = new WorkflowArtifactService(
            new WorkflowArtifactRepository(), new WorkflowMemoryBindingRepository(),
            new WorkflowCheckpointBindingRepository(), new WorkflowEventRepository());

    public int run(CommandContext context, Args args) {
        String runKey = args.option("run").trim();
        String memoryRaw = args.option("memory-id").trim();
        if (runKey.length() == 0 || memoryRaw.length() == 0) {
            return CommandErrorGuidance.missing(context, args, "WORKFLOW_BIND_MEMORY_ARGUMENTS_MISSING",
                    new String[]{"--run", "--memory-id"},
                    "dhk workflow bind-memory --run <run> --memory-id <id>",
                    "docs/GOAL_CONFIGURATION.md");
        }
        long memoryId = parseLong(context, args, memoryRaw, "memory-id");
        if (memoryId <= 0) {
            return ExitCodes.VALIDATION_ERROR;
        }
        String type = args.option("type", "read").trim();
        if (!isMemoryBindingType(type)) {
            return EnumGuidance.printInvalid(context, args, "INVALID_WORKFLOW_MEMORY_BINDING_TYPE",
                    "workflow memory binding type", type, EnumGuidance.WORKFLOW_MEMORY_BINDING_TYPES,
                    new String[0], "dhk workflow bind-memory --run <run> --memory-id <id> --type read",
                    "docs/GOAL_CONFIGURATION.md");
        }
        String phase = args.option("phase", "").trim();
        String reason = args.option("reason", "").trim();
        if (WorkflowCommandSupport.rejectSensitive(context, sensitiveDataGuard,
                "workflow bind-memory", phase, reason)) {
            return ExitCodes.VALIDATION_ERROR;
        }
        Path projectRoot = WorkflowCommandSupport.projectRoot(args, context);
        try (Connection connection = connectionFactory.open(projectRoot)) {
            migrationRunner.migrate(connection, context.clock());
            WorkflowRun run = runRepository.findByKey(connection, runKey);
            if (run == null) {
                return CommandErrorGuidance.notFound(context, args, "WORKFLOW_RUN_NOT_FOUND",
                        "workflow run", runKey, "dhk workflow status --run <run>",
                        "docs/GOAL_CONFIGURATION.md");
            }
            MemoryItem memory = memoryRepository.findById(connection, run.projectKey(), memoryId);
            if (memory == null) {
                return CommandErrorGuidance.notFound(context, args, "WORKFLOW_MEMORY_NOT_FOUND",
                        "memory", Long.toString(memoryId), "dhk memory list",
                        "docs/GOAL_CONFIGURATION.md");
            }
            String phaseKey = phase.length() == 0 ? run.currentPhaseKey() : phase;
            long id = artifactService.bindMemory(connection, run, memoryId, type, phaseKey,
                    reason, context.clock().now().toString());
            context.out().println("memory_binding_id: " + id);
            context.out().println("run_key: " + run.runKey());
            context.out().println("memory_id: " + memoryId);
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR workflow bind-memory failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private long parseLong(CommandContext context, Args args, String raw, String name) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ex) {
            CommandErrorGuidance.invalidNumber(context, args, "WORKFLOW_BIND_MEMORY_INVALID_ID",
                    name, raw, name + " must be a positive integer.",
                    "dhk workflow bind-memory --run <run> --memory-id <id>",
                    "docs/GOAL_CONFIGURATION.md");
            return -1L;
        }
    }

    private boolean isMemoryBindingType(String type) {
        return "read".equals(type) || "exported".equals(type) || "suggested".equals(type)
                || "created_draft".equals(type) || "confirmed_after_run".equals(type)
                || "deprecated_after_run".equals(type);
    }
}
