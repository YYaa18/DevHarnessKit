package com.devharnesskit.dhk.command.goal;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.service.goal.GoalOrchestrator;

import java.nio.file.Path;

public final class GoalCompleteCommand implements Command {
    private final GoalOrchestrator orchestrator = new GoalOrchestrator();

    public int run(CommandContext context, Args args) {
        Path projectRoot = GoalCommandSupport.projectRoot(args, context);
        try {
            GoalRun goal = GoalCommandSupport.goal(orchestrator, context, args, projectRoot);
            GoalOrchestrator.GoalCompleteResult result = orchestrator.complete(context, projectRoot, goal.goalKey());
            context.out().println("goal_key: " + result.goal().goalKey());
            context.out().println("status: " + result.goal().status());
            context.out().println("checkpoint_id: " + result.checkpointId());
            context.out().println("summary_path: " + result.summaryPath());
            return ExitCodes.SUCCESS;
        } catch (GoalOrchestrator.GoalNotReadyException ex) {
            GoalEvaluateCommand.print(context, ex.evaluation());
            return ExitCodes.VALIDATION_ERROR;
        } catch (Exception ex) {
            context.err().println("ERROR goal complete failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
