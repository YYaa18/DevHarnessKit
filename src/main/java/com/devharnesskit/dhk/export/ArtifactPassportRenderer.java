package com.devharnesskit.dhk.export;

import com.devharnesskit.dhk.model.goal.GoalCheck;
import com.devharnesskit.dhk.model.goal.GoalGraphArtifacts;
import com.devharnesskit.dhk.model.goal.GoalProfile;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.model.goal.GoalStep;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ArtifactPassportRenderer {
    public String render(Path projectRoot, GoalRun goal, GoalProfile profile,
                         List<GoalStep> steps, List<GoalCheck> checks,
                         long checkpointId, String generatedAt,
                         GoalGraphArtifacts graphArtifacts, Path summaryPath,
                         Path passportPath) {
        return JsonOutput.object(
                JsonOutput.stringField("schema_version", "artifact-passport/v1-alpha"),
                JsonOutput.stringField("generated_at", generatedAt),
                JsonOutput.rawField("goal", goalObject(goal, checkpointId)),
                JsonOutput.rawField("completion", completionObject(goal, checkpointId, summaryPath, passportPath)),
                JsonOutput.rawField("checks", checksArray(checks)),
                JsonOutput.rawField("steps", stepsArray(steps)),
                JsonOutput.rawField("evidence", evidenceObject(steps, checks)),
                JsonOutput.rawField("graph", graphObject(graphArtifacts)),
                JsonOutput.rawField("bdd", bddObject(projectRoot, profile)),
                JsonOutput.rawField("rollback", rollbackObject(steps)),
                JsonOutput.rawField("artifacts", artifactsObject(summaryPath, passportPath))
        );
    }

    private String goalObject(GoalRun goal, long checkpointId) {
        return JsonOutput.object(
                JsonOutput.stringField("goal_key", goal.goalKey()),
                JsonOutput.stringField("profile", goal.profileKey()),
                JsonOutput.stringField("task", goal.taskName()),
                JsonOutput.stringField("module", goal.moduleName()),
                JsonOutput.stringField("mode", goal.mode()),
                JsonOutput.stringField("workflow_run", goal.workflowRunKey()),
                JsonOutput.stringField("spec_change", goal.specChangeKey()),
                JsonOutput.numberField("checkpoint_id", checkpointId)
        ).trim();
    }

    private String completionObject(GoalRun goal, long checkpointId, Path summaryPath, Path passportPath) {
        return JsonOutput.object(
                JsonOutput.stringField("status", "completed"),
                JsonOutput.stringField("workflow_run", goal.workflowRunKey()),
                JsonOutput.stringField("spec_change", goal.specChangeKey()),
                JsonOutput.numberField("checkpoint_id", checkpointId),
                JsonOutput.stringField("goal_summary", path(summaryPath)),
                JsonOutput.stringField("artifact_passport", path(passportPath))
        ).trim();
    }

    private String checksArray(List<GoalCheck> checks) {
        List<String> raw = new ArrayList<String>();
        for (GoalCheck check : checks == null ? new ArrayList<GoalCheck>() : checks) {
            raw.add(JsonOutput.object(
                    JsonOutput.stringField("check_key", check.checkKey()),
                    JsonOutput.stringField("check_type", check.checkType()),
                    JsonOutput.stringField("status", check.status()),
                    JsonOutput.booleanField("required", check.required()),
                    JsonOutput.numberField("step_count_at_check", check.stepCountAtCheck()),
                    JsonOutput.stringField("summary", check.resultSummary()),
                    JsonOutput.stringField("evidence_path", check.evidencePath()),
                    JsonOutput.stringField("check_fingerprint", check.checkFingerprint())
            ).trim());
        }
        return JsonOutput.array(raw);
    }

    private String stepsArray(List<GoalStep> steps) {
        List<String> raw = new ArrayList<String>();
        for (GoalStep step : steps == null ? new ArrayList<GoalStep>() : steps) {
            raw.add(JsonOutput.object(
                    JsonOutput.numberField("step_index", step.stepIndex()),
                    JsonOutput.stringField("action", step.actionKey()),
                    JsonOutput.stringField("summary", step.summary()),
                    JsonOutput.stringField("changed_files", step.changedFiles()),
                    JsonOutput.stringField("evidence", step.evidence()),
                    JsonOutput.stringField("status", step.status()),
                    JsonOutput.stringField("created_at", step.createdAt())
            ).trim());
        }
        return JsonOutput.array(raw);
    }

    private String evidenceObject(List<GoalStep> steps, List<GoalCheck> checks) {
        return JsonOutput.object(
                JsonOutput.rawField("changed_files", JsonOutput.stringArray(splitChangedFiles(steps))),
                JsonOutput.rawField("check_logs", JsonOutput.stringArray(checkLogs(checks))),
                JsonOutput.stringField("scope_justification", evidenceValue(steps, "scope_justification")),
                JsonOutput.stringField("impact_evidence", firstNonEmpty(evidenceValue(steps, "impact_evidence"),
                        evidenceValue(steps, "impact_map"))),
                JsonOutput.stringField("manual_evidence_status", firstNonEmpty(
                        evidenceValue(steps, "manual_evidence_status"),
                        evidenceValue(steps, "protected_file_confirmation")))
        ).trim();
    }

    private String graphObject(GoalGraphArtifacts artifacts) {
        GoalGraphArtifacts graph = artifacts == null ? GoalGraphArtifacts.none() : artifacts;
        return JsonOutput.object(
                JsonOutput.booleanField("enabled", graph.enabled()),
                JsonOutput.numberField("snapshot_id", graph.snapshotId()),
                JsonOutput.stringField("snapshot_key", graph.snapshotKey()),
                JsonOutput.stringField("provider", graph.provider()),
                JsonOutput.numberField("file_count", graph.fileCount()),
                JsonOutput.numberField("node_count", graph.nodeCount()),
                JsonOutput.numberField("edge_count", graph.edgeCount()),
                JsonOutput.stringField("graph_snapshot", graph.graphSnapshotPath()),
                JsonOutput.stringField("graph_context", graph.graphContextPath()),
                JsonOutput.stringField("impact_map", graph.impactMapPath()),
                JsonOutput.stringField("scenario_impact_map", graph.scenarioImpactMapPath()),
                JsonOutput.stringField("graph_snapshot_hash", graph.graphSnapshotHash()),
                JsonOutput.stringField("graph_context_hash", graph.graphContextHash()),
                JsonOutput.stringField("impact_map_hash", graph.impactMapHash()),
                JsonOutput.stringField("scenario_impact_map_hash", graph.scenarioImpactMapHash())
        ).trim();
    }

    private String bddObject(Path projectRoot, GoalProfile profile) {
        boolean enabled = profile != null && profile.bddRequired();
        return JsonOutput.object(
                JsonOutput.booleanField("enabled", enabled),
                JsonOutput.stringField("bdd_context", enabled ? path(PathUtil.bddContext(projectRoot)) : ""),
                JsonOutput.stringField("bdd_evidence", enabled ? path(PathUtil.bddEvidence(projectRoot)) : ""),
                JsonOutput.stringField("bdd_coverage", enabled ? path(PathUtil.bddCoverage(projectRoot)) : ""),
                JsonOutput.stringField("scenario_impact_map",
                        enabled ? path(PathUtil.scenarioImpactMap(projectRoot)) : "")
        ).trim();
    }

    private String rollbackObject(List<GoalStep> steps) {
        return JsonOutput.object(
                JsonOutput.stringField("rollback_plan", evidenceValue(steps, "rollback_plan")),
                JsonOutput.stringField("rollback_status", evidenceValue(steps, "rollback_status"))
        ).trim();
    }

    private String artifactsObject(Path summaryPath, Path passportPath) {
        List<String> raw = new ArrayList<String>();
        raw.add(JsonOutput.object(
                JsonOutput.stringField("type", "goal_summary"),
                JsonOutput.stringField("path", path(summaryPath))
        ).trim());
        raw.add(JsonOutput.object(
                JsonOutput.stringField("type", "artifact_passport"),
                JsonOutput.stringField("path", path(passportPath))
        ).trim());
        return JsonOutput.array(raw);
    }

    private String[] splitChangedFiles(List<GoalStep> steps) {
        java.util.LinkedHashSet<String> files = new java.util.LinkedHashSet<String>();
        for (GoalStep step : steps == null ? new ArrayList<GoalStep>() : steps) {
            addFiles(files, step.changedFiles());
            addFiles(files, evidenceValue(step.evidence(), "changed_files"));
        }
        return files.toArray(new String[files.size()]);
    }

    private void addFiles(java.util.Set<String> files, String raw) {
        if (raw == null || raw.trim().length() == 0 || "none".equalsIgnoreCase(raw.trim())) {
            return;
        }
        String[] parts = raw.replace('\n', ',').replace(';', ',').split(",");
        for (String part : parts) {
            String file = part.trim();
            if (file.length() > 0) {
                files.add(file);
            }
        }
    }

    private String[] checkLogs(List<GoalCheck> checks) {
        List<String> result = new ArrayList<String>();
        for (GoalCheck check : checks == null ? new ArrayList<GoalCheck>() : checks) {
            if (check.evidencePath().length() > 0) {
                result.add(check.evidencePath());
            }
        }
        return result.toArray(new String[result.size()]);
    }

    private String evidenceValue(List<GoalStep> steps, String key) {
        String value = "";
        for (GoalStep step : steps == null ? new ArrayList<GoalStep>() : steps) {
            String candidate = evidenceValue(step.evidence(), key);
            if (candidate.length() > 0) {
                value = candidate;
            }
        }
        return value;
    }

    private String evidenceValue(String evidence, String key) {
        Pattern pattern = Pattern.compile("(?i)(?:^|[;\\n\\r])\\s*" + Pattern.quote(key)
                + "\\s*=\\s*([^;\\n\\r]+)");
        Matcher matcher = pattern.matcher(evidence == null ? "" : evidence);
        String value = "";
        while (matcher.find()) {
            value = matcher.group(1).trim();
        }
        return value;
    }

    private String firstNonEmpty(String first, String second) {
        if (first != null && first.length() > 0) {
            return first;
        }
        return second == null ? "" : second;
    }

    private String path(Path path) {
        return path == null ? "" : path.toString();
    }
}
