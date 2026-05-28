package com.devharnesskit.dhk.command.workflow;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.guidance.CommandErrorGuidance;
import com.devharnesskit.dhk.model.workflow.WorkflowGateRun;
import com.devharnesskit.dhk.model.workflow.WorkflowRun;
import com.devharnesskit.dhk.repository.workflow.WorkflowArtifactRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowCheckpointBindingRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowGateRunRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowMemoryBindingRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowRunRepository;
import com.devharnesskit.dhk.repository.spec.WorkflowSpecBindingRepository;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;

public final class SummaryCommand implements Command {
    private final DbConnectionFactory connectionFactory = new DbConnectionFactory();
    private final MigrationRunner migrationRunner = new MigrationRunner();
    private final WorkflowRunRepository runRepository = new WorkflowRunRepository();
    private final WorkflowArtifactRepository artifactRepository = new WorkflowArtifactRepository();
    private final WorkflowMemoryBindingRepository memoryBindingRepository = new WorkflowMemoryBindingRepository();
    private final WorkflowCheckpointBindingRepository checkpointBindingRepository = new WorkflowCheckpointBindingRepository();
    private final WorkflowGateRunRepository gateRunRepository = new WorkflowGateRunRepository();
    private final WorkflowSpecBindingRepository specBindingRepository = new WorkflowSpecBindingRepository();

    public int run(CommandContext context, Args args) {
        String runKey = args.option("run").trim();
        if (runKey.length() == 0) {
            return CommandErrorGuidance.missing(context, args, "WORKFLOW_SUMMARY_RUN_MISSING",
                    new String[]{"--run"}, "dhk workflow summary --run <run>",
                    "docs/GOAL_CONFIGURATION.md");
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
            context.out().println("run: " + run.runKey());
            context.out().println("workflow: " + run.workflowKey());
            context.out().println("task: " + run.taskName());
            context.out().println("status: " + run.status());
            context.out().println("current_phase: " + run.currentPhaseKey());
            context.out().println("exported_memory_count: "
                    + memoryBindingRepository.countByRunAndType(connection, run.runKey(), "exported"));
            context.out().println("artifact_count: " + artifactRepository.countByRun(connection, run.runKey()));
            context.out().println("checkpoint_count: " + checkpointBindingRepository.countByRun(connection, run.runKey()));
            context.out().println("bound_spec_count: " + specBindingRepository.countByRun(connection, run.runKey()));
            context.out().println("pending_hard_gate_count: " + pendingHardGateCount(connection, run.runKey()));
            context.out().println("blocking_hard_gate_count: " + blockingHardGateCount(connection, run.runKey()));
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR workflow summary failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private int pendingHardGateCount(Connection connection, String runKey) throws Exception {
        List<WorkflowGateRun> gates = gateRunRepository.listByRun(connection, runKey);
        int count = 0;
        for (WorkflowGateRun gate : gates) {
            if ("hard".equals(gate.severity()) && "pending".equals(gate.status())) {
                count++;
            }
        }
        return count;
    }

    private int blockingHardGateCount(Connection connection, String runKey) throws Exception {
        List<WorkflowGateRun> gates = gateRunRepository.listByRun(connection, runKey);
        int count = 0;
        for (WorkflowGateRun gate : gates) {
            if ("hard".equals(gate.severity())
                    && ("pending".equals(gate.status()) || "failed".equals(gate.status()))) {
                count++;
            }
        }
        return count;
    }
}
