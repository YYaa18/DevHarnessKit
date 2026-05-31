package com.devharnesskit.dhk.command.goal;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.model.brief.InteractionRequest;
import com.devharnesskit.dhk.model.goal.GoalEvaluation;
import com.devharnesskit.dhk.model.goal.GoalPlan;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.service.brief.BriefLifecycleService;
import com.devharnesskit.dhk.service.goal.GoalOrchestrator;
import com.devharnesskit.dhk.util.JsonOutput;

import java.nio.file.Path;

public final class GoalNextCommand implements Command {
    private static final String[] FORBIDDEN = new String[]{
            "do_not_archive_spec",
            "do_not_waive_gates",
            "do_not_confirm_memory",
            "do_not_run_db_sql_without_user_request",
            "do_not_claim_completion_before_goal_evaluate"
    };
    private final GoalOrchestrator orchestrator = new GoalOrchestrator();
    private final BriefLifecycleService briefLifecycleService = new BriefLifecycleService();

    public int run(CommandContext context, Args args) {
        Path projectRoot = GoalCommandSupport.projectRoot(args, context);
        try {
            GoalRun goal = GoalCommandSupport.goal(orchestrator, context, args, projectRoot);
            if (!isContextExportIncomplete(goal.status())) {
                orchestrator.export(context, projectRoot, goal.goalKey());
                goal = GoalCommandSupport.goal(orchestrator, context, args, projectRoot);
            }
            InteractionRequest blocker = briefLifecycleService.findOpenBlockingInteraction(projectRoot, goal.goalKey());
            if (blocker != null) {
                GoalPlan waitPlan = waitPlan(blocker);
                briefLifecycleService.refreshAgentBriefCurrentAction(projectRoot, goal, waitPlan.currentAction());
                String[] blockers = new String[]{"blocking interaction requires user answer: "
                        + blocker.requestId()};
                if (JsonOutput.enabled(args)) {
                    GoalCommandSupport.printPlanJson(context, projectRoot, goal, waitPlan, blockers);
                } else {
                    GoalCommandSupport.printPlan(context, projectRoot, goal, waitPlan, blockers);
                }
                return ExitCodes.SUCCESS;
            }
            GoalPlan plan = orchestrator.plan(projectRoot, goal);
            briefLifecycleService.refreshAgentBriefCurrentAction(projectRoot, goal, plan.currentAction());
            GoalEvaluation evaluation = orchestrator.evaluate(context, projectRoot, goal.goalKey());
            if (JsonOutput.enabled(args)) {
                GoalCommandSupport.printPlanJson(context, projectRoot, goal, plan, evaluation.missing());
            } else {
                GoalCommandSupport.printPlan(context, projectRoot, goal, plan, evaluation.missing());
            }
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR goal next failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private boolean isContextExportIncomplete(String status) {
        return "context_export_failed".equals(status) || "context_exporting".equals(status);
    }

    private GoalPlan waitPlan(InteractionRequest request) {
        String choice = request.defaultChoice().length() == 0 ? "<choice>" : request.defaultChoice();
        return new GoalPlan("wait_for_user_answer",
                "Blocking interaction requires user answer before this goal can continue. request_id="
                        + request.requestId() + " question=" + request.question(),
                new String[]{"user_confirmation"}, FORBIDDEN,
                "dhk brief answer --request " + request.requestId()
                        + " --choice \"" + choice.replace("\"", "\\\"") + "\"");
    }
}
