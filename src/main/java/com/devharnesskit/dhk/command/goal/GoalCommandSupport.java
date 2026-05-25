package com.devharnesskit.dhk.command.goal;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.model.goal.GoalPlan;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.service.goal.GoalOrchestrator;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;

final class GoalCommandSupport {
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
        context.out().println("goal_key: " + goal.goalKey());
        context.out().println("status: " + goal.status());
        context.out().println("current_action: " + plan.currentAction());
        context.out().println("instruction: " + plan.instruction());
        context.out().println("required_evidence:");
        for (String evidence : plan.requiredEvidence()) {
            context.out().println("  - " + evidence);
        }
        context.out().println("forbidden_actions:");
        for (String action : plan.forbiddenActions()) {
            context.out().println("  - " + action);
        }
        context.out().println("next_command: " + plan.nextCommand());
        context.out().println("context_path: " + PathUtil.goalContext(projectRoot));
    }
}
