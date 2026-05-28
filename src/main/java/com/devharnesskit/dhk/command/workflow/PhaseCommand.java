package com.devharnesskit.dhk.command.workflow;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.TransactionTemplate;
import com.devharnesskit.dhk.guidance.CommandErrorGuidance;
import com.devharnesskit.dhk.guidance.EnumGuidance;
import com.devharnesskit.dhk.model.workflow.WorkflowRun;
import com.devharnesskit.dhk.repository.workflow.WorkflowEventRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowGateRunRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowPhaseRunRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowRunRepository;
import com.devharnesskit.dhk.service.SensitiveDataGuard;
import com.devharnesskit.dhk.service.workflow.WorkflowPhaseService;

import java.nio.file.Path;
import java.sql.Connection;

public final class PhaseCommand implements Command {
    private final DbConnectionFactory connectionFactory = new DbConnectionFactory();
    private final WorkflowRunRepository runRepository = new WorkflowRunRepository();
    private final SensitiveDataGuard sensitiveDataGuard = new SensitiveDataGuard();
    private final WorkflowPhaseService phaseService = new WorkflowPhaseService(
            runRepository, new WorkflowPhaseRunRepository(), new WorkflowGateRunRepository(),
            new WorkflowEventRepository());
    private final TransactionTemplate transactionTemplate = new TransactionTemplate();

    public int run(CommandContext context, Args args) {
        String action = args.positional(2);
        if (!"pass".equals(action) && !"fail".equals(action)) {
            return EnumGuidance.printInvalid(context, args, "INVALID_WORKFLOW_PHASE_ACTION",
                    "workflow phase action", action, EnumGuidance.WORKFLOW_PHASE_ACTIONS, new String[0],
                    "dhk workflow phase pass --run <run> --phase <phase> --summary \"<summary>\"",
                    "docs/GOAL_CONFIGURATION.md");
        }
        String runKey = args.option("run").trim();
        String phaseKey = args.option("phase").trim();
        if (runKey.length() == 0 || phaseKey.length() == 0) {
            return CommandErrorGuidance.missing(context, args, "WORKFLOW_PHASE_ARGUMENTS_MISSING",
                    new String[]{"--run", "--phase"},
                    "dhk workflow phase pass --run <run> --phase <phase> --summary \"<summary>\"",
                    "docs/GOAL_CONFIGURATION.md");
        }
        String summary = args.option("summary", "").trim();
        String reason = args.option("reason", "").trim();
        if ("pass".equals(action) && summary.length() == 0) {
            return CommandErrorGuidance.missing(context, args, "WORKFLOW_PHASE_SUMMARY_MISSING",
                    new String[]{"--summary"},
                    "dhk workflow phase pass --run " + runKey + " --phase " + phaseKey
                            + " --summary \"<summary>\"",
                    "docs/GOAL_CONFIGURATION.md");
        }
        if ("fail".equals(action) && reason.length() == 0) {
            return CommandErrorGuidance.missing(context, args, "WORKFLOW_PHASE_REASON_MISSING",
                    new String[]{"--reason"},
                    "dhk workflow phase fail --run " + runKey + " --phase " + phaseKey
                            + " --reason \"<reason>\"",
                    "docs/GOAL_CONFIGURATION.md");
        }
        String evidence = args.option("evidence", "");
        if (WorkflowCommandSupport.rejectSensitive(context, sensitiveDataGuard, "workflow phase",
                summary, reason, evidence)) {
            return ExitCodes.VALIDATION_ERROR;
        }
        Path projectRoot = WorkflowCommandSupport.projectRoot(args, context);
        try (Connection connection = connectionFactory.open(projectRoot)) {
            WorkflowRun run = runRepository.findByKey(connection, runKey);
            if (run == null) {
                return CommandErrorGuidance.notFound(context, args, "WORKFLOW_RUN_NOT_FOUND",
                        "workflow run", runKey, "dhk workflow status --run <run>",
                        "docs/GOAL_CONFIGURATION.md");
            }
            WorkflowPhaseService.PhaseUpdateResult result = transactionTemplate.execute(connection,
                    new TransactionTemplate.Work<WorkflowPhaseService.PhaseUpdateResult>() {
                        public WorkflowPhaseService.PhaseUpdateResult execute() throws Exception {
                            if ("pass".equals(action)) {
                                return phaseService.pass(connection, run, phaseKey, summary,
                                        evidence, context.clock().now().toString());
                            }
                            return phaseService.fail(connection, run, phaseKey, reason, context.clock().now().toString());
                        }
                    });
            if (!result.ok()) {
                context.err().println(result.message());
                return result.rejected() ? ExitCodes.VALIDATION_ERROR : ExitCodes.NOT_FOUND;
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
