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

public final class GoalCheckCommand implements Command {
    private final GoalOrchestrator orchestrator = new GoalOrchestrator();
    private final PolicyHookService policyHookService = new PolicyHookService();

    public int run(CommandContext context, Args args) {
        boolean all = args.hasFlag("all");
        String check = args.option("check").trim();
        if (!all && check.length() == 0) {
            context.err().println("Missing required parameter: --check <check-key> or --all");
            return ExitCodes.USAGE_ERROR;
        }
        Path projectRoot = GoalCommandSupport.projectRoot(args, context);
        try {
            policyHookService.requireGoalCheckAllowed(projectRoot);
            GoalRun goal = GoalCommandSupport.goal(orchestrator, context, args, projectRoot);
            List<GoalCheck> checks = orchestrator.runCheck(context, projectRoot, goal.goalKey(), check, all);
            if (JsonOutput.enabled(args)) {
                printJson(context, goal, checks);
                return ExitCodes.SUCCESS;
            }
            for (GoalCheck result : checks) {
                context.out().println("check_key: " + result.checkKey());
                context.out().println("status: " + result.status());
                context.out().println("step_count_at_check: " + result.stepCountAtCheck());
                context.out().println("workspace_fingerprint: " + result.workspaceFingerprint());
                context.out().println("context_fingerprint: " + result.contextFingerprint());
                context.out().println("check_fingerprint: " + result.checkFingerprint());
                context.out().println("result_summary: " + result.resultSummary());
                if (result.evidencePath().length() > 0) {
                    context.out().println("evidence_path: " + result.evidencePath());
                }
            }
            return ExitCodes.SUCCESS;
        } catch (IllegalArgumentException ex) {
            context.err().println(ex.getMessage());
            return ExitCodes.VALIDATION_ERROR;
        } catch (PolicyViolationException ex) {
            context.err().println(ex.getMessage());
            return ExitCodes.VALIDATION_ERROR;
        } catch (Exception ex) {
            context.err().println("ERROR goal check failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private void printJson(CommandContext context, GoalRun goal, List<GoalCheck> checks) {
        List<String> rawChecks = new ArrayList<String>();
        for (GoalCheck check : checks) {
            rawChecks.add(JsonOutput.object(
                    JsonOutput.stringField("check_key", check.checkKey()),
                    JsonOutput.stringField("status", check.status()),
                    JsonOutput.numberField("step_count_at_check", check.stepCountAtCheck()),
                    JsonOutput.stringField("workspace_fingerprint", check.workspaceFingerprint()),
                    JsonOutput.stringField("context_fingerprint", check.contextFingerprint()),
                    JsonOutput.stringField("check_fingerprint", check.checkFingerprint()),
                    JsonOutput.stringField("result_summary", check.resultSummary()),
                    JsonOutput.stringField("evidence_path", check.evidencePath())
            ).trim());
        }
        context.out().print(JsonOutput.object(
                JsonOutput.stringField("command", "goal check"),
                JsonOutput.stringField("goal_key", goal.goalKey()),
                JsonOutput.numberField("count", checks.size()),
                JsonOutput.rawField("checks", JsonOutput.array(rawChecks))
        ));
    }
}
