package com.devharnesskit.dhk.command.goal;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.service.goal.GoalOrchestrator;

import java.nio.file.Path;

public final class GoalStepCommand implements Command {
    private final GoalOrchestrator orchestrator = new GoalOrchestrator();

    public int run(CommandContext context, Args args) {
        String goalKey = args.option("goal").trim();
        String summary = args.option("summary").trim();
        if (goalKey.length() == 0 || summary.length() == 0) {
            context.err().println("Missing required parameters: --goal, --summary");
            return ExitCodes.USAGE_ERROR;
        }
        String changedFiles = args.option("changed-files", "").trim();
        String evidence = args.option("evidence", "").trim();
        Path projectRoot = GoalCommandSupport.projectRoot(args, context);
        try {
            GoalOrchestrator.GoalStepResult result = orchestrator.step(context, projectRoot, goalKey,
                    summary, changedFiles, evidence);
            context.out().println("step_id: " + result.stepId());
            context.out().println("goal_key: " + result.goal().goalKey());
            context.out().println("status: " + result.goal().status());
            context.out().println("current_action: " + result.goal().currentAction());
            context.out().println("next_command: dhk goal next --goal " + result.goal().goalKey());
            context.out().println("context_path: " + result.contextPath());
            return ExitCodes.SUCCESS;
        } catch (IllegalArgumentException ex) {
            context.err().println(ex.getMessage());
            return ExitCodes.VALIDATION_ERROR;
        } catch (Exception ex) {
            context.err().println("ERROR goal step failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
