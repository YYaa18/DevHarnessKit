package com.devharnesskit.dhk.command.goal;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.model.goal.GoalCheck;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.service.goal.GoalOrchestrator;
import com.devharnesskit.dhk.service.policy.PolicyHookService;
import com.devharnesskit.dhk.service.policy.PolicyViolationException;
import com.devharnesskit.dhk.util.JsonOutput;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class GoalRecheckCommand implements Command {
    private final GoalOrchestrator orchestrator = new GoalOrchestrator();
    private final PolicyHookService policyHookService = new PolicyHookService();

    public int run(CommandContext context, Args args) {
        Path projectRoot = GoalCommandSupport.projectRoot(args, context);
        try {
            policyHookService.requireGoalCheckAllowed(projectRoot);
            GoalRun before = GoalCommandSupport.goal(orchestrator, context, args, projectRoot);
            List<GoalCheck> checks = orchestrator.runCheck(context, projectRoot, before.goalKey(), "", true);
            GoalOrchestrator.GoalAuditResult audit = orchestrator.audit(context, projectRoot, before.goalKey());
            if (JsonOutput.enabled(args)) {
                printJson(context, before, audit, checks);
            } else {
                printText(context, before, audit, checks);
            }
            return ExitCodes.SUCCESS;
        } catch (PolicyViolationException ex) {
            context.err().println(ex.getMessage());
            return ExitCodes.VALIDATION_ERROR;
        } catch (Exception ex) {
            context.err().println("ERROR goal recheck failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private void printText(CommandContext context, GoalRun before,
                           GoalOrchestrator.GoalAuditResult audit, List<GoalCheck> checks) {
        context.out().println("goal recheck complete");
        context.out().println("goal_key: " + audit.goal().goalKey());
        context.out().println("status_before: " + before.status());
        context.out().println("status_after: " + audit.goal().status());
        context.out().println("step_count_before: " + before.stepCount());
        context.out().println("step_count_after: " + audit.goal().stepCount());
        context.out().println("check_count: " + checks.size());
        context.out().println("checks:");
        for (GoalCheck check : checks) {
            context.out().println("  - " + check.checkKey() + ": " + check.status()
                    + " - " + check.resultSummary());
        }
        context.out().println("decision: " + audit.evaluation().decision());
        context.out().println("ready_to_complete: " + audit.evaluation().readyToComplete());
        context.out().println("stale_checks:");
        printArray(context, audit.evaluation().staleChecks());
        context.out().println("missing:");
        printArray(context, audit.evaluation().missing());
        context.out().println("next_command: " + audit.evaluation().nextCommand());
    }

    private void printJson(CommandContext context, GoalRun before,
                           GoalOrchestrator.GoalAuditResult audit, List<GoalCheck> checks) {
        List<String> rawChecks = new ArrayList<String>();
        for (GoalCheck check : checks) {
            rawChecks.add(JsonOutput.object(
                    JsonOutput.stringField("check_key", check.checkKey()),
                    JsonOutput.stringField("status", check.status()),
                    JsonOutput.stringField("result_summary", check.resultSummary())
            ).trim());
        }
        context.out().print(JsonOutput.object(
                JsonOutput.stringField("command", "goal recheck"),
                JsonOutput.stringField("goal_key", audit.goal().goalKey()),
                JsonOutput.stringField("status_before", before.status()),
                JsonOutput.stringField("status_after", audit.goal().status()),
                JsonOutput.numberField("step_count_before", before.stepCount()),
                JsonOutput.numberField("step_count_after", audit.goal().stepCount()),
                JsonOutput.numberField("check_count", checks.size()),
                JsonOutput.rawField("checks", JsonOutput.array(rawChecks)),
                JsonOutput.stringField("decision", audit.evaluation().decision()),
                JsonOutput.booleanField("ready_to_complete", audit.evaluation().readyToComplete()),
                JsonOutput.rawField("stale_checks", JsonOutput.stringArray(audit.evaluation().staleChecks())),
                JsonOutput.rawField("missing", JsonOutput.stringArray(audit.evaluation().missing())),
                JsonOutput.stringField("next_command", audit.evaluation().nextCommand())
        ));
    }

    private void printArray(CommandContext context, String[] values) {
        if (values.length == 0) {
            context.out().println("  - none");
            return;
        }
        for (String value : values) {
            context.out().println("  - " + value);
        }
    }
}
