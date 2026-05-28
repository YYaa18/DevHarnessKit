package com.devharnesskit.dhk.command.goal;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.service.brief.BriefLifecycleService;
import com.devharnesskit.dhk.service.goal.GoalOrchestrator;
import com.devharnesskit.dhk.service.policy.PolicyViolationException;
import com.devharnesskit.dhk.util.JsonOutput;

import java.nio.file.Path;

public final class GoalCompleteCommand implements Command {
    private final GoalOrchestrator orchestrator = new GoalOrchestrator();
    private final BriefLifecycleService briefLifecycleService = new BriefLifecycleService();

    public int run(CommandContext context, Args args) {
        Path projectRoot = GoalCommandSupport.projectRoot(args, context);
        GoalRun goal = null;
        try {
            goal = GoalCommandSupport.goal(orchestrator, context, args, projectRoot);
            GoalOrchestrator.GoalCompleteResult result = orchestrator.complete(context, projectRoot, goal.goalKey());
            Path completionBriefPath = briefLifecycleService.writeCompletionBrief(projectRoot, result.goal(),
                    result.checkpointId(), result.summaryPath());
            if (JsonOutput.enabled(args)) {
                context.out().print(JsonOutput.object(
                        JsonOutput.stringField("command", "goal complete"),
                        JsonOutput.stringField("goal_key", result.goal().goalKey()),
                        JsonOutput.stringField("status", result.goal().status()),
                        JsonOutput.numberField("checkpoint_id", result.checkpointId()),
                        JsonOutput.stringField("summary_path", result.summaryPath().toString()),
                        JsonOutput.stringField("completion_brief_path", completionBriefPath.toString())
                ));
            } else {
                context.out().println("goal_key: " + result.goal().goalKey());
                context.out().println("status: " + result.goal().status());
                context.out().println("checkpoint_id: " + result.checkpointId());
                context.out().println("summary_path: " + result.summaryPath());
                context.out().println("completion_brief_path: " + completionBriefPath);
            }
            return ExitCodes.SUCCESS;
        } catch (GoalOrchestrator.GoalNotReadyException ex) {
            if (goal == null) {
                context.err().println("ERROR goal complete failed: unable to reload goal for blocker report");
                return ExitCodes.RUNTIME_ERROR;
            }
            if (JsonOutput.enabled(args)) {
                context.out().print(GoalBlockerReport.renderJson(goal, ex.evaluation()));
            } else {
                context.out().print(GoalBlockerReport.renderText(goal, ex.evaluation()));
            }
            return ExitCodes.VALIDATION_ERROR;
        } catch (PolicyViolationException ex) {
            if (JsonOutput.enabled(args)) {
                context.out().print(JsonOutput.object(
                        JsonOutput.stringField("command", "goal complete"),
                        JsonOutput.stringField("status", "rejected"),
                        JsonOutput.stringField("reason", ex.getMessage())
                ));
            } else {
                context.err().println(ex.getMessage());
            }
            return ExitCodes.VALIDATION_ERROR;
        } catch (Exception ex) {
            context.err().println("ERROR goal complete failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
