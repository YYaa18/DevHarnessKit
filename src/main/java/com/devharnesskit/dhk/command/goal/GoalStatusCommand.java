package com.devharnesskit.dhk.command.goal;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.model.goal.GoalPlan;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.model.goal.GoalStep;
import com.devharnesskit.dhk.service.goal.GoalOrchestrator;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;
import java.util.List;

public final class GoalStatusCommand implements Command {
    private final GoalOrchestrator orchestrator = new GoalOrchestrator();

    public int run(CommandContext context, Args args) {
        Path projectRoot = GoalCommandSupport.projectRoot(args, context);
        try {
            GoalRun goal = GoalCommandSupport.goal(orchestrator, context, args, projectRoot);
            GoalPlan plan = orchestrator.plan(projectRoot, goal);
            List<GoalStep> steps = orchestrator.steps(context, projectRoot, goal.goalKey());
            if (JsonOutput.enabled(args)) {
                context.out().print(JsonOutput.object(
                        JsonOutput.stringField("command", "goal status"),
                        JsonOutput.stringField("goal_key", goal.goalKey()),
                        JsonOutput.stringField("profile", goal.profileKey()),
                        JsonOutput.stringField("status", goal.status()),
                        JsonOutput.stringField("workflow_run", goal.workflowRunKey()),
                        JsonOutput.stringField("spec_change", goal.specChangeKey()),
                        JsonOutput.stringField("current_action", plan.currentAction()),
                        JsonOutput.numberField("step_count", steps.size()),
                        JsonOutput.stringField("context_path", PathUtil.goalContext(projectRoot).toString())
                ));
            } else {
                context.out().println("goal_key: " + goal.goalKey());
                context.out().println("profile: " + goal.profileKey());
                context.out().println("status: " + goal.status());
                context.out().println("workflow_run: " + goal.workflowRunKey());
                context.out().println("spec_change: " + goal.specChangeKey());
                context.out().println("current_action: " + plan.currentAction());
                context.out().println("step_count: " + steps.size());
                context.out().println("context_path: " + PathUtil.goalContext(projectRoot));
            }
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR goal status failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
