package com.devharnesskit.dhk.command.goal;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.model.goal.GoalCheck;
import com.devharnesskit.dhk.service.goal.GoalOrchestrator;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class GoalAuditCommand implements Command {
    private final GoalOrchestrator orchestrator = new GoalOrchestrator();

    public int run(CommandContext context, Args args) {
        Path projectRoot = GoalCommandSupport.projectRoot(args, context);
        try {
            String goalKey = args.option("goal").trim();
            if (goalKey.length() == 0) {
                goalKey = GoalCommandSupport.goal(orchestrator, context, args, projectRoot).goalKey();
            }
            GoalOrchestrator.GoalAuditResult result = orchestrator.audit(context, projectRoot, goalKey);
            if (JsonOutput.enabled(args)) {
                printJson(context, projectRoot, result);
            } else {
                printText(context, projectRoot, result);
            }
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR goal audit failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private void printText(CommandContext context, Path projectRoot, GoalOrchestrator.GoalAuditResult result) {
        context.out().println("goal audit complete");
        context.out().println("goal_key: " + result.goal().goalKey());
        context.out().println("status: " + result.goal().status());
        context.out().println("current_action: " + result.goal().currentAction());
        context.out().println("step_count: " + result.steps().size());
        context.out().println("check_count: " + result.checks().size());
        context.out().println("artifact_count: " + result.artifacts().size());
        context.out().println("artifact_passport: " + (result.artifactPassportPresent() ? "present" : "missing"));
        context.out().println("decision: " + result.evaluation().decision());
        context.out().println("ready_to_complete: " + result.evaluation().readyToComplete());
        context.out().println("missing:");
        printArray(context, missing(result));
        context.out().println("stale:");
        printArray(context, result.evaluation().staleChecks());
        context.out().println("invalid:");
        printArray(context, invalid(result));
        context.out().println("next_command: " + result.evaluation().nextCommand());
        context.out().println("context_path: " + PathUtil.goalContext(projectRoot));
    }

    private void printJson(CommandContext context, Path projectRoot, GoalOrchestrator.GoalAuditResult result) {
        context.out().print(JsonOutput.object(
                JsonOutput.stringField("command", "goal audit"),
                JsonOutput.stringField("goal_key", result.goal().goalKey()),
                JsonOutput.stringField("status", result.goal().status()),
                JsonOutput.stringField("current_action", result.goal().currentAction()),
                JsonOutput.numberField("step_count", result.steps().size()),
                JsonOutput.numberField("check_count", result.checks().size()),
                JsonOutput.numberField("artifact_count", result.artifacts().size()),
                JsonOutput.stringField("artifact_passport", result.artifactPassportPresent() ? "present" : "missing"),
                JsonOutput.stringField("decision", result.evaluation().decision()),
                JsonOutput.booleanField("ready_to_complete", result.evaluation().readyToComplete()),
                JsonOutput.rawField("missing", JsonOutput.stringArray(missing(result))),
                JsonOutput.rawField("stale", JsonOutput.stringArray(result.evaluation().staleChecks())),
                JsonOutput.rawField("invalid", JsonOutput.stringArray(invalid(result))),
                JsonOutput.stringField("next_command", result.evaluation().nextCommand()),
                JsonOutput.stringField("context_path", PathUtil.goalContext(projectRoot).toString())
        ));
    }

    private String[] missing(GoalOrchestrator.GoalAuditResult result) {
        List<String> missing = new ArrayList<String>();
        for (String value : result.evaluation().missing()) {
            missing.add(value);
        }
        if ("completed".equals(result.goal().status()) && !result.artifactPassportPresent()) {
            missing.add("artifact passport missing");
        }
        return missing.toArray(new String[missing.size()]);
    }

    private String[] invalid(GoalOrchestrator.GoalAuditResult result) {
        List<String> invalid = new ArrayList<String>();
        for (GoalCheck check : result.checks()) {
            if ("failed".equals(check.status())) {
                invalid.add("check " + check.checkKey() + " is failed: " + check.resultSummary());
            }
        }
        return invalid.toArray(new String[invalid.size()]);
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
