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
import com.devharnesskit.dhk.service.workflow.WorkflowGateService;

import java.nio.file.Path;
import java.sql.Connection;

public final class GateCommand implements Command {
    private final DbConnectionFactory connectionFactory = new DbConnectionFactory();
    private final WorkflowRunRepository runRepository = new WorkflowRunRepository();
    private final SensitiveDataGuard sensitiveDataGuard = new SensitiveDataGuard();
    private final WorkflowGateService gateService = new WorkflowGateService(
            runRepository, new WorkflowGateRunRepository(), new WorkflowPhaseRunRepository(),
            new WorkflowEventRepository());
    private final TransactionTemplate transactionTemplate = new TransactionTemplate();

    public int run(CommandContext context, Args args) {
        String action = args.positional(2);
        if (!"pass".equals(action) && !"fail".equals(action) && !"waive".equals(action)) {
            return EnumGuidance.printInvalid(context, args, "INVALID_WORKFLOW_GATE_ACTION",
                    "workflow gate action", action, EnumGuidance.WORKFLOW_GATE_ACTIONS, new String[0],
                    "dhk workflow gate pass --run <run> --gate <gate> --summary \"<summary>\"",
                    "docs/GOAL_CONFIGURATION.md");
        }
        String runKey = args.option("run").trim();
        String gateKey = args.option("gate").trim();
        if (runKey.length() == 0 || gateKey.length() == 0) {
            return CommandErrorGuidance.missing(context, args, "WORKFLOW_GATE_ARGUMENTS_MISSING",
                    new String[]{"--run", "--gate"},
                    "dhk workflow gate pass --run <run> --gate <gate> --summary \"<summary>\"",
                    "docs/GOAL_CONFIGURATION.md");
        }
        String summary = args.option("summary", "").trim();
        String reason = args.option("reason", "").trim();
        if ("pass".equals(action) && summary.length() == 0) {
            return CommandErrorGuidance.missing(context, args, "WORKFLOW_GATE_SUMMARY_MISSING",
                    new String[]{"--summary"},
                    "dhk workflow gate pass --run " + runKey + " --gate " + gateKey + " --summary \"<summary>\"",
                    "docs/GOAL_CONFIGURATION.md");
        }
        if (("fail".equals(action) || "waive".equals(action)) && reason.length() == 0) {
            return CommandErrorGuidance.missing(context, args, "WORKFLOW_GATE_REASON_MISSING",
                    new String[]{"--reason"},
                    "dhk workflow gate " + action + " --run " + runKey + " --gate " + gateKey
                            + " --reason \"<reason>\"",
                    "docs/GOAL_CONFIGURATION.md");
        }
        String evidence = args.option("evidence", "");
        if (WorkflowCommandSupport.rejectSensitive(context, sensitiveDataGuard, "workflow gate",
                summary, reason, evidence)) {
            return ExitCodes.VALIDATION_ERROR;
        }
        String phaseKey = args.option("phase", "").trim();
        Path projectRoot = WorkflowCommandSupport.projectRoot(args, context);
        try (Connection connection = connectionFactory.open(projectRoot)) {
            WorkflowRun run = runRepository.findByKey(connection, runKey);
            if (run == null) {
                return CommandErrorGuidance.notFound(context, args, "WORKFLOW_RUN_NOT_FOUND",
                        "workflow run", runKey, "dhk workflow status --run <run>",
                        "docs/GOAL_CONFIGURATION.md");
            }
            WorkflowGateService.GateUpdateResult result = transactionTemplate.execute(connection,
                    new TransactionTemplate.Work<WorkflowGateService.GateUpdateResult>() {
                        public WorkflowGateService.GateUpdateResult execute() throws Exception {
                            return gateService.update(connection, run, phaseKey, gateKey, action, summary, reason,
                                    evidence, context.clock().now().toString());
                        }
                    });
            if (!result.ok()) {
                context.err().println(result.message());
                if (result.usageError()) {
                    return ExitCodes.USAGE_ERROR;
                }
                return result.rejected() ? ExitCodes.VALIDATION_ERROR : ExitCodes.NOT_FOUND;
            }
            context.out().println("gate: " + gateKey);
            context.out().println("status: " + result.gateStatus());
            context.out().println("run_status: " + result.runStatus());
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR workflow gate failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
