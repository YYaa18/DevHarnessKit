package com.devharnesskit.dhk.export;

import com.devharnesskit.dhk.model.goal.GoalCheck;
import com.devharnesskit.dhk.model.goal.GoalGraphArtifacts;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.model.goal.GoalStep;

import java.util.List;

public final class GoalSummaryRenderer {
    public String render(GoalRun goal, List<GoalStep> steps, List<GoalCheck> checks,
                         long checkpointId, String generatedAt) {
        return render(goal, steps, checks, checkpointId, generatedAt, GoalGraphArtifacts.none());
    }

    public String render(GoalRun goal, List<GoalStep> steps, List<GoalCheck> checks,
                         long checkpointId, String generatedAt, GoalGraphArtifacts graphArtifacts) {
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

        appendGraphArtifacts(builder, graphArtifacts);

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

    private void appendGraphArtifacts(StringBuilder builder, GoalGraphArtifacts artifacts) {
        if (artifacts == null || !artifacts.enabled()) {
            return;
        }
        builder.append("<graph-artifacts>\n");
        builder.append("- snapshot_id: ").append(artifacts.snapshotId()).append('\n');
        builder.append("- snapshot_key: ").append(artifacts.snapshotKey()).append('\n');
        builder.append("- provider: ").append(artifacts.provider()).append('\n');
        builder.append("- file_count: ").append(artifacts.fileCount()).append('\n');
        builder.append("- node_count: ").append(artifacts.nodeCount()).append('\n');
        builder.append("- edge_count: ").append(artifacts.edgeCount()).append('\n');
        builder.append("- graph_snapshot: ").append(emptyValue(artifacts.graphSnapshotPath())).append('\n');
        builder.append("- graph_context: ").append(emptyValue(artifacts.graphContextPath())).append('\n');
        builder.append("- impact_map: ").append(emptyValue(artifacts.impactMapPath())).append('\n');
        builder.append("- graph_snapshot_hash: ").append(emptyValue(artifacts.graphSnapshotHash())).append('\n');
        builder.append("- graph_context_hash: ").append(emptyValue(artifacts.graphContextHash())).append('\n');
        builder.append("- impact_map_hash: ").append(emptyValue(artifacts.impactMapHash())).append('\n');
        builder.append("- goal_graph_binding: used,summary");
        if (artifacts.impactMapPath().length() > 0) {
            builder.append(",impact_map");
        }
        builder.append('\n');
        builder.append("- limitation: graph facts are generated snapshot facts, not confirmed memory\n");
        builder.append("- limitation: impact map is query-scoped and must be regenerated after code changes\n");
        builder.append("</graph-artifacts>\n\n");
    }

    private String emptyValue(String value) {
        return value == null || value.length() == 0 ? "none" : value;
    }
}
