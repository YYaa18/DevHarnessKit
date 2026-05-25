package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.goal.GoalCheck;
import com.devharnesskit.dhk.model.goal.GoalEvaluation;
import com.devharnesskit.dhk.model.goal.GoalProfile;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.model.goal.GoalStep;

import java.util.ArrayList;
import java.util.List;

public final class GoalCompletionEvaluator {
    public GoalEvaluation evaluate(GoalRun goal, List<GoalCheck> checks) {
        return evaluate(goal, checks, GoalCheckPolicy.defaults());
    }

    public GoalEvaluation evaluate(GoalRun goal, List<GoalCheck> checks, GoalCheckPolicy policy) {
        return evaluate(goal, checks, policy, null, new ArrayList<GoalStep>());
    }

    public GoalEvaluation evaluate(GoalRun goal, List<GoalCheck> checks, GoalCheckPolicy policy,
                                   GoalProfile profile, List<GoalStep> steps) {
        List<String> missing = new ArrayList<String>();
        List<String> staleChecks = new ArrayList<String>();
        if ("failed".equals(goal.status()) || "abandoned".equals(goal.status())) {
            missing.add("goal status is " + goal.status());
        }
        if ("context_export_failed".equals(goal.status()) || "context_exporting".equals(goal.status())) {
            missing.add("context export is not ready: status is " + goal.status());
        }
        int requiredSteps = profile == null ? 0 : profile.actions().length;
        int recordedSteps = steps == null ? goal.stepCount() : steps.size();
        if (requiredSteps > 0 && recordedSteps < requiredSteps) {
            missing.add("goal steps incomplete: expected " + requiredSteps + " actions, recorded " + recordedSteps);
        }
        for (String required : policy.requiredChecks()) {
            GoalCheck check = find(checks, required);
            if (check == null) {
                missing.add("check " + required + " is pending");
                continue;
            }
            if (!policy.accepts(required, check.status(), profile)) {
                missing.add("check " + required + " is " + check.status()
                        + "; accepted_statuses=" + policy.acceptedStatusesText(required, profile));
                continue;
            }
            if (check.stepCountAtCheck() < recordedSteps) {
                missing.add("check " + required + " is stale: checked_at_step="
                        + check.stepCountAtCheck() + " current_step=" + recordedSteps);
                staleChecks.add(required);
            }
        }
        if (missing.isEmpty()) {
            return new GoalEvaluation("ready_to_complete", new String[0], new String[0],
                    "complete_goal", "dhk goal complete --goal " + goal.goalKey());
        }
        String first = missing.get(0);
        String nextAction;
        String nextCommand;
        if (first.startsWith("goal steps incomplete")) {
            nextAction = "record_goal_step";
            nextCommand = "dhk goal next --goal " + goal.goalKey();
        } else if (first.startsWith("check ")) {
            nextAction = "run_goal_" + first.substring(0, first.indexOf(" is")).replace(' ', '_');
            nextCommand = "dhk goal check --goal " + goal.goalKey() + " --check "
                    + first.substring(6, first.indexOf(" is"));
        } else {
            nextAction = "resolve_goal_blocker";
            nextCommand = "dhk goal status --goal " + goal.goalKey();
        }
        return new GoalEvaluation("not_ready", missing.toArray(new String[missing.size()]),
                staleChecks.toArray(new String[staleChecks.size()]),
                nextAction, nextCommand);
    }

    private GoalCheck find(List<GoalCheck> checks, String key) {
        for (GoalCheck check : checks) {
            if (key.equals(check.checkKey())) {
                return check;
            }
        }
        return null;
    }
}
