package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.goal.GoalCheck;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class GraphGoalCheckRunner extends AbstractGoalCheckRunner {
    private final GoalCheckRecorder recorder;
    private final WorkspaceFingerprintService fingerprintService;

    GraphGoalCheckRunner(GoalCheckRecorder recorder, WorkspaceFingerprintService fingerprintService) {
        super("graph");
        this.recorder = recorder;
        this.fingerprintService = fingerprintService;
    }

    public GoalCheck run(GoalCheckContext context) throws Exception {
        Path log = recorder.logPath(context.projectRoot(), context.goal(), key());
        String command = GoalCheckSupport.graphRefreshCommand(context.projectRoot());
        if (context.profile() == null || !context.profile().graphRequired()) {
            String summary = "graph not required by goal profile";
            recorder.writeLog(log, summary + "\n");
            return recorder.save(context.connection(), context.projectRoot(), context.goal(), key(), "graph",
                    command, "skipped", summary, log, context.now());
        }

        Path snapshot = PathUtil.graphSnapshotJson(context.projectRoot());
        Path graphContext = PathUtil.graphContext(context.projectRoot());
        List<String> failures = new ArrayList<String>();
        StringBuilder output = new StringBuilder();
        output.append("graph_snapshot: ").append(snapshot).append('\n');
        output.append("graph_context: ").append(graphContext).append('\n');
        output.append("next_command: ").append(command).append('\n');

        String snapshotText = "";
        if (!Files.isRegularFile(snapshot)) {
            failures.add("GRAPH_SNAPSHOT.json missing; next_command=" + command);
        } else {
            snapshotText = new String(Files.readAllBytes(snapshot), "UTF-8");
            String snapshotKey = GoalCheckSupport.jsonString(snapshotText, "snapshot_key");
            String snapshotFingerprint = GoalCheckSupport.jsonString(snapshotText, "workspace_fingerprint");
            String generatedAt = GoalCheckSupport.jsonString(snapshotText, "generated_at");
            output.append("snapshot_key: ").append(snapshotKey).append('\n');
            output.append("generated_at: ").append(generatedAt).append('\n');
            output.append("snapshot_workspace_fingerprint: ").append(snapshotFingerprint).append('\n');
            if (context.profile().graphRequireFreshSnapshot()) {
                String currentFingerprint = fingerprintService.workspaceFingerprint(context.projectRoot());
                output.append("current_workspace_fingerprint: ").append(currentFingerprint).append('\n');
                if (snapshotFingerprint.length() == 0) {
                    failures.add("graph snapshot stale: workspace fingerprint missing; next_command=" + command);
                } else if (!snapshotFingerprint.equals(currentFingerprint)) {
                    failures.add("graph snapshot stale: workspace fingerprint changed; next_command=" + command);
                }
                GoalCheckSupport.addAgeFailure("graph snapshot", generatedAt,
                        context.profile().graphMaxStalenessMinutes(), context.now(), command, failures);
            }
        }
        if (!Files.isRegularFile(graphContext)) {
            failures.add("GRAPH_CONTEXT.md missing; next_command=" + command);
        }

        recorder.writeLog(log, output.toString());
        String status = failures.isEmpty() ? "passed" : "failed";
        String summary = failures.isEmpty()
                ? "graph snapshot fresh; snapshot_key=" + GoalCheckSupport.jsonString(snapshotText, "snapshot_key")
                : "graph freshness failed: " + failures;
        return recorder.save(context.connection(), context.projectRoot(), context.goal(), key(), "graph",
                command, status, summary, log, context.now());
    }
}
