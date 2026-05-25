package com.devharnesskit.dhk.command.goal;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
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
            GoalPlan plan = orchestrator.plan(projectRoot, goal);
            GoalCommandSupport.printPlan(context, projectRoot, goal, plan);
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR goal next failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
