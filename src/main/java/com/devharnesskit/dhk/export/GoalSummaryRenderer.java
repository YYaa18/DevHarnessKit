package com.devharnesskit.dhk.export;

import com.devharnesskit.dhk.model.goal.GoalCheck;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.model.goal.GoalStep;

import java.util.List;

public final class GoalSummaryRenderer {
    public String render(GoalRun goal, List<GoalStep> steps, List<GoalCheck> checks,
                         long checkpointId, String generatedAt) {
        StringBuilder builder = new StringBuilder();
        builder.append("# GOAL_SUMMARY\n\n");
        builder.append("<generated-at>").append(generatedAt).append("</generated-at>\n\n");
        builder.append("<goal>\n");
        builder.append("- goal_key: ").append(goal.goalKey()).append('\n');
        builder.append("- profile: ").append(goal.profileKey()).append('\n');
        builder.append("- task: ").append(goal.taskName()).append('\n');
        builder.append("- module: ").append(goal.moduleName()).append('\n');
        builder.append("- mode: ").append(goal.mode()).append('\n');
        builder.append("- status: completed\n");
        builder.append("- checkpoint_id: ").append(checkpointId).append('\n');
        builder.append("</goal>\n\n");

        builder.append("<completion-bindings>\n");
        builder.append("- workflow_run: ").append(emptyValue(goal.workflowRunKey())).append('\n');
        builder.append("- spec_change: ").append(emptyValue(goal.specChangeKey())).append('\n');
        builder.append("- checkpoint_id: ").append(checkpointId).append('\n');
        builder.append("- workflow_checkpoint_binding: created\n");
        builder.append("- workflow_artifact: GOAL_SUMMARY.md\n");
        builder.append("</completion-bindings>\n\n");

        builder.append("<steps>\n");
        for (GoalStep step : steps) {
            builder.append("- #").append(step.stepIndex()).append(' ')
                    .append(step.actionKey()).append(": ").append(step.summary()).append('\n');
            if (step.changedFiles().length() > 0) {
                builder.append("  changed_files: ").append(step.changedFiles()).append('\n');
            }
            if (step.evidence().length() > 0) {
                builder.append("  evidence: ").append(step.evidence()).append('\n');
            }
        }
        if (steps.isEmpty()) {
            builder.append("- none\n");
        }
        builder.append("</steps>\n\n");

        builder.append("<checks>\n");
        for (GoalCheck check : checks) {
            builder.append("- [").append(check.status()).append("] ")
                    .append(check.checkKey()).append(": ")
                    .append(check.resultSummary()).append('\n');
            if (check.evidencePath().length() > 0) {
                builder.append("  evidence_path: ").append(check.evidencePath()).append('\n');
            }
        }
        if (checks.isEmpty()) {
            builder.append("- none\n");
        }
        builder.append("</checks>\n\n");

        builder.append("<agent-instructions>\n");
        builder.append("- Use this summary as the completion record for the goal.\n");
        builder.append("- Do not treat generated summary text as confirmed long-term memory unless it is added as draft and confirmed separately.\n");
        builder.append("</agent-instructions>\n");
        return builder.toString();
    }

    private String emptyValue(String value) {
        return value == null || value.length() == 0 ? "none" : value;
    }
}
