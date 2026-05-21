package com.devharnesskit.dhk.command.workflow;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.TransactionTemplate;
import com.devharnesskit.dhk.model.workflow.WorkflowRun;
import com.devharnesskit.dhk.repository.workflow.WorkflowEventRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowPhaseRunRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowRunRepository;
import com.devharnesskit.dhk.service.workflow.WorkflowPhaseService;

import java.nio.file.Path;
import java.sql.Connection;

public final class PhaseCommand implements Command {
    private final DbConnectionFactory connectionFactory = new DbConnectionFactory();
    private final WorkflowRunRepository runRepository = new WorkflowRunRepository();
    private final WorkflowPhaseService phaseService = new WorkflowPhaseService(
            runRepository, new WorkflowPhaseRunRepository(), new WorkflowEventRepository());
    private final TransactionTemplate transactionTemplate = new TransactionTemplate();

    public int run(CommandContext context, Args args) {
        String action = args.positional(2);
        if (!"pass".equals(action) && !"fail".equals(action)) {
            context.err().println("Unknown workflow phase action: " + action);
            return ExitCodes.USAGE_ERROR;
        }
        String runKey = args.option("run").trim();
        String phaseKey = args.option("phase").trim();
        if (runKey.length() == 0 || phaseKey.length() == 0) {
            context.err().println("Missing required parameters: --run, --phase");
            return ExitCodes.USAGE_ERROR;
        }
        String summary = args.option("summary", "").trim();
        String reason = args.option("reason", "").trim();
        if ("pass".equals(action) && summary.length() == 0) {
            context.err().println("Missing required parameter for phase pass: --summary");
            return ExitCodes.USAGE_ERROR;
        }
        if ("fail".equals(action) && reason.length() == 0) {
            context.err().println("Missing required parameter for phase fail: --reason");
            return ExitCodes.USAGE_ERROR;
        }
        Path projectRoot = WorkflowCommandSupport.projectRoot(args, context);
        try (Connection connection = connectionFactory.open(projectRoot)) {
            WorkflowRun run = runRepository.findByKey(connection, runKey);
            if (run == null) {
                context.err().println("Workflow run not found: " + runKey);
                return ExitCodes.NOT_FOUND;
            }
            WorkflowPhaseService.PhaseUpdateResult result = transactionTemplate.execute(connection,
                    new TransactionTemplate.Work<WorkflowPhaseService.PhaseUpdateResult>() {
                        public WorkflowPhaseService.PhaseUpdateResult execute() throws Exception {
                            if ("pass".equals(action)) {
                                return phaseService.pass(connection, run, phaseKey, summary,
                                        args.option("evidence", ""), context.clock().now().toString());
                            }
                            return phaseService.fail(connection, run, phaseKey, reason, context.clock().now().toString());
                        }
                    });
            if (!result.ok()) {
                context.err().println(result.message());
                return ExitCodes.NOT_FOUND;
            }
            context.out().println("phase: " + phaseKey);
            context.out().println("status: " + ("pass".equals(action) ? "passed" : "failed"));
            context.out().println("run_status: " + result.runStatus());
            if (result.currentPhase().length() > 0) {
                context.out().println("current_phase: " + result.currentPhase());
            }
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR workflow phase failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
