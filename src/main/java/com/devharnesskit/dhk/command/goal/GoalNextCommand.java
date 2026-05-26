package com.devharnesskit.dhk.command.goal;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.model.goal.GoalEvaluation;
import com.devharnesskit.dhk.model.goal.GoalPlan;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.service.goal.GoalOrchestrator;

import java.nio.file.Path;

public final class GoalNextCommand implements Command {
    private final GoalOrchestrator orchestrator = new GoalOrchestrator();

    public int run(CommandContext context, Args args) {
        Path projectRoot = GoalCommandSupport.projectRoot(args, context);
        try {
            GoalRun goal = GoalCommandSupport.goal(orchestrator, context, args, projectRoot);
            if (!isContextExportIncomplete(goal.status())) {
                orchestrator.export(context, projectRoot, goal.goalKey());
                goal = GoalCommandSupport.goal(orchestrator, context, args, projectRoot);
            }
            GoalPlan plan = orchestrator.plan(projectRoot, goal);
            GoalEvaluation evaluation = orchestrator.evaluate(context, projectRoot, goal.goalKey());
            GoalCommandSupport.printPlan(context, projectRoot, goal, plan, evaluation.missing());
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR goal next failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private boolean isContextExportIncomplete(String status) {
        return "context_export_failed".equals(status) || "context_exporting".equals(status);
    }
}
