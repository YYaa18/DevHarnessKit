package com.devharnesskit.dhk.command.workflow;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.model.workflow.WorkflowGateRun;
import com.devharnesskit.dhk.model.workflow.WorkflowPhaseRun;
import com.devharnesskit.dhk.model.workflow.WorkflowRun;
import com.devharnesskit.dhk.repository.workflow.WorkflowGateRunRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowPhaseRunRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowRunRepository;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;

public final class StatusCommand implements Command {
    private final DbConnectionFactory connectionFactory = new DbConnectionFactory();
    private final WorkflowRunRepository runRepository = new WorkflowRunRepository();
    private final WorkflowPhaseRunRepository phaseRunRepository = new WorkflowPhaseRunRepository();
    private final WorkflowGateRunRepository gateRunRepository = new WorkflowGateRunRepository();

    public int run(CommandContext context, Args args) {
        String runKey = args.option("run").trim();
        if (runKey.length() == 0) {
            context.err().println("Missing required parameter: --run");
            return ExitCodes.USAGE_ERROR;
        }
        Path projectRoot = WorkflowCommandSupport.projectRoot(args, context);
        try (Connection connection = connectionFactory.open(projectRoot)) {
            WorkflowRun run = runRepository.findByKey(connection, runKey);
            if (run == null) {
                context.err().println("Workflow run not found: " + runKey);
                return ExitCodes.NOT_FOUND;
            }
            context.out().println("run: " + run.runKey());
            context.out().println("workflow: " + run.workflowKey());
            context.out().println("task: " + run.taskName());
            context.out().println("status: " + run.status());
            context.out().println("current_phase: " + run.currentPhaseKey());
            context.out().println();
            context.out().println("phases:");
            List<WorkflowPhaseRun> phases = phaseRunRepository.listByRun(connection, runKey);
            for (WorkflowPhaseRun phase : phases) {
                context.out().println("[" + phase.status() + "] " + phase.phaseKey());
            }
            context.out().println();
            context.out().println("hard gates:");
            List<WorkflowGateRun> gates = gateRunRepository.listByRun(connection, runKey);
            for (WorkflowGateRun gate : gates) {
                if ("hard".equals(gate.severity())) {
                    context.out().println("[" + gate.status() + "] " + gate.gateKey());
                }
            }
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR workflow status failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
