package com.devharnesskit.dhk.command.workflow;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.db.TransactionTemplate;
import com.devharnesskit.dhk.model.Checkpoint;
import com.devharnesskit.dhk.model.workflow.WorkflowRun;
import com.devharnesskit.dhk.repository.CheckpointRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowArtifactRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowCheckpointBindingRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowEventRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowMemoryBindingRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowRunRepository;
import com.devharnesskit.dhk.service.workflow.WorkflowArtifactService;

import java.nio.file.Path;
import java.sql.Connection;

public final class BindCheckpointCommand implements Command {
    private final DbConnectionFactory connectionFactory = new DbConnectionFactory();
    private final MigrationRunner migrationRunner = new MigrationRunner();
    private final WorkflowRunRepository runRepository = new WorkflowRunRepository();
    private final CheckpointRepository checkpointRepository = new CheckpointRepository();
    private final WorkflowArtifactService artifactService = new WorkflowArtifactService(
            new WorkflowArtifactRepository(), new WorkflowMemoryBindingRepository(),
            new WorkflowCheckpointBindingRepository(), new WorkflowEventRepository());
    private final TransactionTemplate transactionTemplate = new TransactionTemplate();

    public int run(CommandContext context, Args args) {
        String runKey = args.option("run").trim();
        String checkpointRaw = args.option("checkpoint").trim();
        if (runKey.length() == 0 || checkpointRaw.length() == 0) {
            context.err().println("Missing required parameters: --run, --checkpoint");
            return ExitCodes.USAGE_ERROR;
        }
        long checkpointId = parseLong(context, checkpointRaw, "checkpoint");
        if (checkpointId <= 0) {
            return ExitCodes.USAGE_ERROR;
        }
        String type = args.option("type", "created").trim();
        if (!isCheckpointBindingType(type)) {
            context.err().println("Invalid checkpoint binding type: " + type);
            return ExitCodes.VALIDATION_ERROR;
        }
        Path projectRoot = WorkflowCommandSupport.projectRoot(args, context);
        try (Connection connection = connectionFactory.open(projectRoot)) {
            migrationRunner.migrate(connection, context.clock());
            final WorkflowRun run = runRepository.findByKey(connection, runKey);
            if (run == null) {
                context.err().println("Workflow run not found: " + runKey);
                return ExitCodes.NOT_FOUND;
            }
            final Checkpoint checkpoint = checkpointRepository.findById(connection, run.projectKey(), checkpointId);
            if (checkpoint == null) {
                context.err().println("Checkpoint not found: " + checkpointId);
                return ExitCodes.NOT_FOUND;
            }
            final String bindingType = type;
            Long id = transactionTemplate.execute(connection, new TransactionTemplate.Work<Long>() {
                public Long execute() throws Exception {
                    return Long.valueOf(artifactService.bindCheckpoint(connection, run, checkpoint,
                            bindingType, context.clock().now().toString()));
                }
            });
            context.out().println("checkpoint_binding_id: " + id);
            context.out().println("run_key: " + run.runKey());
            context.out().println("checkpoint_id: " + checkpoint.id());
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR workflow bind-checkpoint failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private long parseLong(CommandContext context, String raw, String name) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ex) {
            context.err().println("Invalid " + name + ": " + raw);
            return -1L;
        }
    }

    private boolean isCheckpointBindingType(String type) {
        return "created".equals(type) || "recovered_from".equals(type) || "updated".equals(type);
    }
}
