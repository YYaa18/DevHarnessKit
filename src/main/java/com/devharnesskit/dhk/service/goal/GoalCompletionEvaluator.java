package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.goal.GoalCheck;
import com.devharnesskit.dhk.model.goal.GoalEvaluation;
import com.devharnesskit.dhk.model.goal.GoalRun;

import java.util.ArrayList;
import java.util.List;

public final class GoalCompletionEvaluator {
    public GoalEvaluation evaluate(GoalRun goal, List<GoalCheck> checks) {
        return evaluate(goal, checks, GoalCheckPolicy.defaults());
    }

    public GoalEvaluation evaluate(GoalRun goal, List<GoalCheck> checks, GoalCheckPolicy policy) {
        List<String> missing = new ArrayList<String>();
        if ("failed".equals(goal.status()) || "abandoned".equals(goal.status())) {
            missing.add("goal status is " + goal.status());
        }
        for (String required : policy.requiredChecks()) {
            GoalCheck check = find(checks, required);
            if (check == null) {
                missing.add("check " + required + " is pending");
                continue;
            }
            if (!isAccepted(check.status())) {
                missing.add("check " + required + " is " + check.status());
            }
        }
        if (missing.isEmpty()) {
            return new GoalEvaluation("ready_to_complete", new String[0],
                    "complete_goal", "dhk goal complete --goal " + goal.goalKey());
        }
        String first = missing.get(0);
        String nextAction = first.startsWith("check ")
                ? "run_goal_" + first.substring(0, first.indexOf(" is")).replace(' ', '_')
                : "resolve_goal_blocker";
        String nextCommand = first.startsWith("check ")
                ? "dhk goal check --goal " + goal.goalKey() + " --check " + first.substring(6, first.indexOf(" is"))
                : "dhk goal status --goal " + goal.goalKey();
        return new GoalEvaluation("not_ready", missing.toArray(new String[missing.size()]),
                nextAction, nextCommand);
    }

    private boolean isAccepted(String status) {
        return "passed".equals(status) || "skipped".equals(status) || "waived".equals(status);
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
