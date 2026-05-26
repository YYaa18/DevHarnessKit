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

        String markdown = new GoalContextRenderer().render(goal, plan,
                new String[]{"compile", "test", "sensitive"},
                new String[]{"check compile is pending"},
                new String[]{"compile"}, "stale", "now");

        assertTrue(markdown.length() <= 16 * 1024);
        assertTrue(markdown.contains("# GOAL_CONTEXT"));
        assertSectionOrder(markdown, "# GOAL_CONTEXT", "<generated-at>", "<goal>",
                "<current-action>", "<next-instruction>", "<allowed-actions>", "<allowed-commands>",
                "<forbidden-actions>", "<required-evidence>", "<evidence-contract>",
                "<structured-evidence-fields>", "<required-checks>", "<context-files>",
                "<completion-blockers>", "<freshness-status>", "<completion-condition>", "<next-command>");
        assertTrue(markdown.contains("<allowed-commands>"));
        assertTrue(markdown.contains("dhk goal verify --goal goal-1"));
        assertTrue(markdown.contains("<evidence-contract>"));
        assertTrue(markdown.contains("<structured-evidence-fields>"));
        assertTrue(markdown.contains("- --read-files"));
        assertTrue(markdown.contains("- --compile-result"));
        assertTrue(markdown.contains("<required-checks>"));
        assertTrue(markdown.contains("- compile"));
        assertTrue(markdown.contains("<completion-blockers>"));
        assertTrue(markdown.contains("- check compile is pending"));
        assertTrue(markdown.contains("<freshness-status>"));
        assertTrue(markdown.contains("- status: stale"));
        assertTrue(markdown.contains("- compile"));
        assertTrue(markdown.contains("<completion-condition>"));
        assertTrue(markdown.contains("<next-command>"));
        assertTrue(markdown.contains("<!-- truncated: goal context exceeded budget -->"));
    }

    private void assertSectionOrder(String text, String... markers) {
        int previous = -1;
        for (String marker : markers) {
            int current = text.indexOf(marker);
            assertTrue(current > previous, "Expected marker in order: " + marker);
            previous = current;
        }
    }

    private String repeat(String text, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(text);
        }
        return builder.toString();
    }
}
