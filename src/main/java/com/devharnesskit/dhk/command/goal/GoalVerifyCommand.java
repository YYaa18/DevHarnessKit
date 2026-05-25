package com.devharnesskit.dhk.command.goal;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.model.goal.GoalCheck;
import com.devharnesskit.dhk.model.goal.GoalEvaluation;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.service.goal.GoalOrchestrator;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class GoalVerifyCommand implements Command {
    private final GoalOrchestrator orchestrator = new GoalOrchestrator();

    public int run(CommandContext context, Args args) {
        Path projectRoot = GoalCommandSupport.projectRoot(args, context);
        try {
            GoalRun goal = GoalCommandSupport.goal(orchestrator, context, args, projectRoot);
            List<GoalCheck> checks = orchestrator.runCheck(context, projectRoot, goal.goalKey(), "", true);
            GoalEvaluation evaluation = orchestrator.evaluate(context, projectRoot, goal.goalKey());
            if (JsonOutput.enabled(args)) {
                printJson(context, projectRoot, goal, checks, evaluation);
            } else {
                printText(context, projectRoot, goal, checks, evaluation);
            }
            return ExitCodes.SUCCESS;
        } catch (IllegalArgumentException ex) {
            context.err().println(ex.getMessage());
            return ExitCodes.VALIDATION_ERROR;
        } catch (Exception ex) {
            context.err().println("ERROR goal verify failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private void printText(CommandContext context, Path projectRoot, GoalRun goal, List<GoalCheck> checks,
                           GoalEvaluation evaluation) {
        context.out().println("goal_key: " + goal.goalKey());
        context.out().println("decision: " + evaluation.decision());
        context.out().println("ready_to_complete: " + evaluation.readyToComplete());
        context.out().println("checks:");
        for (GoalCheck check : checks) {
            context.out().println("  - " + check.checkKey() + ": " + check.status()
                    + " - " + check.resultSummary());
        }
        context.out().println("failed_checks:");
        printStringList(context, failedChecks(checks));
        context.out().println("missing:");
        printArray(context, evaluation.missing());
        context.out().println("stale_checks:");
        printArray(context, evaluation.staleChecks());
        context.out().println("next_action: " + evaluation.nextAction());
        context.out().println("next_command: " + evaluation.nextCommand());
        context.out().println("context_path: " + PathUtil.goalContext(projectRoot));
    }

    private void printJson(CommandContext context, Path projectRoot, GoalRun goal, List<GoalCheck> checks,
                           GoalEvaluation evaluation) {
        List<String> rawChecks = new ArrayList<String>();
        for (GoalCheck check : checks) {
            rawChecks.add(JsonOutput.object(
                    JsonOutput.stringField("check_key", check.checkKey()),
                    JsonOutput.stringField("status", check.status()),
                    JsonOutput.numberField("step_count_at_check", check.stepCountAtCheck()),
                    JsonOutput.stringField("result_summary", check.resultSummary()),
                    JsonOutput.stringField("evidence_path", check.evidencePath())
            ).trim());
        }
        context.out().print(JsonOutput.object(
                JsonOutput.stringField("command", "goal verify"),
                JsonOutput.stringField("goal_key", goal.goalKey()),
                JsonOutput.stringField("decision", evaluation.decision()),
                JsonOutput.booleanField("ready_to_complete", evaluation.readyToComplete()),
                JsonOutput.numberField("check_count", checks.size()),
                JsonOutput.rawField("checks", JsonOutput.array(rawChecks)),
                JsonOutput.numberField("failed_count", failedChecks(checks).length),
                JsonOutput.rawField("failed_checks", JsonOutput.stringArray(failedChecks(checks))),
                JsonOutput.numberField("missing_count", evaluation.missing().length),
                JsonOutput.rawField("missing", JsonOutput.stringArray(evaluation.missing())),
                JsonOutput.numberField("stale_count", evaluation.staleChecks().length),
                JsonOutput.rawField("stale_checks", JsonOutput.stringArray(evaluation.staleChecks())),
                JsonOutput.stringField("next_action", evaluation.nextAction()),
                JsonOutput.stringField("next_command", evaluation.nextCommand()),
                JsonOutput.stringField("context_path", PathUtil.goalContext(projectRoot).toString())
        ));
    }

    private void printStringList(CommandContext context, String[] values) {
        printArray(context, values);
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

    private String[] failedChecks(List<GoalCheck> checks) {
        List<String> failed = new ArrayList<String>();
        for (GoalCheck check : checks) {
            if ("failed".equals(check.status())) {
                failed.add(check.checkKey() + ": " + check.resultSummary());
            }
        }
        return failed.toArray(new String[failed.size()]);
    }
}
