package com.devharnesskit.dhk.command.goal;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.model.goal.GoalPlan;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.service.goal.GoalCheckPolicyService;
import com.devharnesskit.dhk.service.goal.GoalOrchestrator;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;

final class GoalCommandSupport {
    private static final GoalCheckPolicyService CHECK_POLICY_SERVICE = new GoalCheckPolicyService();
    private static final String[] ALLOWED_ACTIONS = new String[]{
            "perform_current_action_only",
            "record_goal_step_after_work",
            "run_goal_next_before_continuing"
    };

    private GoalCommandSupport() {
    }

    static Path projectRoot(Args args, CommandContext context) {
        return PathUtil.resolveProjectRoot(args, context.workingDirectory());
    }

    static GoalRun goal(GoalOrchestrator orchestrator, CommandContext context,
                        Args args, Path projectRoot) throws Exception {
        String goalKey = args.option("goal").trim();
        if (goalKey.length() == 0) {
            return orchestrator.latestOpen(context, projectRoot);
        }
        return orchestrator.find(context, projectRoot, goalKey);
    }

    static void printPlan(CommandContext context, Path projectRoot, GoalRun goal, GoalPlan plan) {
        printPlan(context, projectRoot, goal, plan, new String[0]);
    }

    static void printPlan(CommandContext context, Path projectRoot, GoalRun goal, GoalPlan plan,
                          String[] completionBlockers) {
        context.out().println("goal_key: " + goal.goalKey());
        context.out().println("status: " + goal.status());
        context.out().println("current_action: " + plan.currentAction());
        context.out().println("instruction: " + plan.instruction());
        context.out().println("allowed_actions:");
        for (String action : ALLOWED_ACTIONS) {
            context.out().println("  - " + action);
        }
        context.out().println("required_evidence:");
        for (String evidence : plan.requiredEvidence()) {
            context.out().println("  - " + evidence);
        }
        context.out().println("forbidden_actions:");
        for (String action : plan.forbiddenActions()) {
            context.out().println("  - " + action);
        }
        context.out().println("required_checks:");
        for (String check : CHECK_POLICY_SERVICE.load(projectRoot).requiredChecks()) {
            context.out().println("  - " + check);
        }
        context.out().println("context_files:");
        context.out().println("  - .agents/memory/exports/CURRENT_CONTEXT.md");
        context.out().println("  - .agents/memory/exports/WORKFLOW_CONTEXT.md");
        if (goal.specChangeKey().length() > 0) {
            context.out().println("  - .agents/memory/exports/SPEC_CONTEXT.md");
        }
        context.out().println("completion_blockers:");
        if (completionBlockers == null || completionBlockers.length == 0) {
            context.out().println("  - none");
        } else {
            for (String blocker : completionBlockers) {
                context.out().println("  - " + blocker);
            }
        }
        context.out().println("next_command: " + plan.nextCommand());
        context.out().println("context_path: " + PathUtil.goalContext(projectRoot));
    }
}
