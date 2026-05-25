package com.devharnesskit.dhk.export;

import com.devharnesskit.dhk.model.goal.GoalPlan;
import com.devharnesskit.dhk.model.goal.GoalRun;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class GoalContextRendererTest {
    @Test
    void keepsGoalContextWithinBudgetAndReportsTruncation() {
        GoalRun goal = new GoalRun("goal-1", "project-1", "workflow-1", "spec-1",
                "java-api-change", "Implement goal export contracts", "goal", "api",
                "compile and tests pass", "verifying", "verify", 30, 3,
                "now", "now", "");
        GoalPlan plan = new GoalPlan("verify", "Run checks and record evidence.",
                new String[]{"compile_result", "test_result", "sensitive_result"},
                new String[]{"do_not_claim_completion_before_goal_evaluate"},
                "dhk goal step --goal goal-1 --summary \"done\" --evidence \"" + repeat("evidence ", 3000));

        String markdown = new GoalContextRenderer().render(goal, plan, "now");

        assertTrue(markdown.length() <= 16 * 1024);
        assertTrue(markdown.contains("# GOAL_CONTEXT"));
        assertTrue(markdown.contains("<completion-condition>"));
        assertTrue(markdown.contains("<next-command>"));
        assertTrue(markdown.contains("<!-- truncated: goal context exceeded budget -->"));
    }

    private String repeat(String text, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(text);
        }
        return builder.toString();
    }
}
