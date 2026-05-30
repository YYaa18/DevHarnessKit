package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.export.ArtifactPassportRenderer;
import com.devharnesskit.dhk.export.GoalSummaryRenderer;
import com.devharnesskit.dhk.model.Checkpoint;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.goal.GoalArtifact;
import com.devharnesskit.dhk.model.goal.GoalCheck;
import com.devharnesskit.dhk.model.goal.GoalGraphArtifacts;
import com.devharnesskit.dhk.model.goal.GoalProfile;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.model.goal.GoalStep;
import com.devharnesskit.dhk.model.graph.GraphSnapshot;
import com.devharnesskit.dhk.model.workflow.WorkflowArtifact;
import com.devharnesskit.dhk.model.workflow.WorkflowCheckpointBinding;
import com.devharnesskit.dhk.repository.CheckpointRepository;
import com.devharnesskit.dhk.repository.goal.GoalArtifactRepository;
import com.devharnesskit.dhk.repository.graph.GoalGraphBindingRepository;
import com.devharnesskit.dhk.repository.graph.GraphRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowArtifactRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowCheckpointBindingRepository;
import com.devharnesskit.dhk.service.SensitiveDataGuard;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.util.List;

final class GoalCompletionArtifactService {
    private final GoalArtifactRepository goalArtifactRepository;
    private final GraphRepository graphRepository;
    private final GoalGraphBindingRepository goalGraphBindingRepository;
    private final CheckpointRepository checkpointRepository;
    private final WorkflowArtifactRepository workflowArtifactRepository;
    private final WorkflowCheckpointBindingRepository workflowCheckpointBindingRepository;
    private final GoalSummaryRenderer summaryRenderer;
    private final ArtifactPassportRenderer artifactPassportRenderer;
    private final SensitiveDataGuard sensitiveDataGuard;

    GoalCompletionArtifactService(GoalArtifactRepository goalArtifactRepository,
                                  GraphRepository graphRepository,
                                  GoalGraphBindingRepository goalGraphBindingRepository,
                                  CheckpointRepository checkpointRepository,
                                  WorkflowArtifactRepository workflowArtifactRepository,
                                  WorkflowCheckpointBindingRepository workflowCheckpointBindingRepository,
                                  GoalSummaryRenderer summaryRenderer,
                                  ArtifactPassportRenderer artifactPassportRenderer,
                                  SensitiveDataGuard sensitiveDataGuard) {
        this.goalArtifactRepository = goalArtifactRepository;
        this.graphRepository = graphRepository;
        this.goalGraphBindingRepository = goalGraphBindingRepository;
        this.checkpointRepository = checkpointRepository;
        this.workflowArtifactRepository = workflowArtifactRepository;
        this.workflowCheckpointBindingRepository = workflowCheckpointBindingRepository;
        this.summaryRenderer = summaryRenderer;
        this.artifactPassportRenderer = artifactPassportRenderer;
        this.sensitiveDataGuard = sensitiveDataGuard;
    }

    GoalCompletionArtifacts create(Connection connection, Path projectRoot, Project project, GoalRun goal,
                                   GoalProfile profile, List<GoalStep> steps, List<GoalCheck> checks,
                                   String changedFiles, String now) throws Exception {
        GoalGraphArtifacts graphArtifacts = bindGraphArtifacts(connection, projectRoot, project, goal,
                profile, now);
        long checkpointId = checkpointRepository.insert(connection, new Checkpoint(0L,
                project.projectKey(), goal.taskName(), goal.moduleName(),
                "Goal completed: " + goal.taskName(), changedFiles,
                "none", checkpointSummary(profile, graphArtifacts), PathUtil.GOAL_SUMMARY, now));
        Path summaryPath = writeSummary(connection, projectRoot, goal, profile, steps, checks,
                checkpointId, graphArtifacts, now);
        writeArtifactPassport(connection, projectRoot, goal, profile, steps, checks,
                checkpointId, graphArtifacts, summaryPath, now);
        goalArtifactRepository.insert(connection, new GoalArtifact(0L, goal.goalKey(),
                "checkpoint", "Completion checkpoint", "", "",
                "checkpoint_id=" + checkpointId, now));
        return new GoalCompletionArtifacts(checkpointId, summaryPath, graphArtifacts);
    }

    void bindWorkflowCompletion(Connection connection, Project project, GoalRun goal,
                                long checkpointId, Path summaryPath, String now) throws Exception {
        if (goal.workflowRunKey().length() == 0) {
            return;
        }
        workflowCheckpointBindingRepository.insert(connection, new WorkflowCheckpointBinding(0L,
                goal.workflowRunKey(), checkpointId, "created", now));
        workflowArtifactRepository.insert(connection, new WorkflowArtifact(0L, project.projectKey(),
                goal.workflowRunKey(), "custom", "GOAL_SUMMARY.md", summaryPath.toString(), "",
                "confirmed", "goal_complete", "Goal summary exported for goal " + goal.goalKey(),
                "goal,completion", now, now));
    }

    private Path writeSummary(Connection connection, Path projectRoot, GoalRun goal, GoalProfile profile,
                              List<GoalStep> steps, List<GoalCheck> checks, long checkpointId,
                              GoalGraphArtifacts graphArtifacts, String now) throws Exception {
        String summary = summaryRenderer.render(goal, steps, checks, checkpointId, now, graphArtifacts);
        summary = sensitiveDataGuard.redact(summary);
        if (sensitiveDataGuard.containsSensitiveData(GeneratedHashMasker.mask(summary))) {
            throw new IllegalStateException("Sensitive data rejected during goal summary export: "
                    + sensitiveDataGuard.findMatches(GeneratedHashMasker.mask(summary)));
        }
        Path summaryPath = PathUtil.goalSummary(projectRoot);
        Files.createDirectories(summaryPath.getParent());
        Files.write(summaryPath, summary.getBytes("UTF-8"));
        goalArtifactRepository.insert(connection, new GoalArtifact(0L, goal.goalKey(),
                "goal_summary", "GOAL_SUMMARY.md", summaryPath.toString(), "",
                "Goal summary exported", now));
        return summaryPath;
    }

    private void writeArtifactPassport(Connection connection, Path projectRoot, GoalRun goal, GoalProfile profile,
                                       List<GoalStep> steps, List<GoalCheck> checks, long checkpointId,
                                       GoalGraphArtifacts graphArtifacts, Path summaryPath, String now)
            throws Exception {
        Path passportPath = PathUtil.artifactPassport(projectRoot);
        String passport = artifactPassportRenderer.render(projectRoot, goal, profile, steps,
                checks, checkpointId, now, graphArtifacts, summaryPath, passportPath);
        passport = sensitiveDataGuard.redact(passport);
        if (sensitiveDataGuard.containsSensitiveData(GeneratedHashMasker.mask(passport))) {
            throw new IllegalStateException("Sensitive data rejected during artifact passport export: "
                    + sensitiveDataGuard.findMatches(GeneratedHashMasker.mask(passport)));
        }
        Files.write(passportPath, passport.getBytes("UTF-8"));
        goalArtifactRepository.insert(connection, new GoalArtifact(0L, goal.goalKey(),
                "artifact_passport", "ARTIFACT_PASSPORT.json", passportPath.toString(),
                fileHash(passportPath), "Artifact passport exported", now));
    }

    private GoalGraphArtifacts bindGraphArtifacts(Connection connection, Path projectRoot, Project project,
                                                  GoalRun goal, GoalProfile profile, String now) throws Exception {
        if (profile == null || !profile.graphRequired()) {
            return GoalGraphArtifacts.none();
        }
        GraphSnapshot snapshot = graphRepository.latestCompletedSnapshot(connection, project.projectKey());
        if (snapshot == null) {
            throw new IllegalStateException("Graph-required goal completed without a completed graph snapshot");
        }

        Path snapshotPath = PathUtil.graphSnapshotJson(projectRoot);
        Path contextPath = PathUtil.graphContext(projectRoot);
        Path impactPath = PathUtil.graphImpactMap(projectRoot);
        Path scenarioImpactPath = PathUtil.scenarioImpactMap(projectRoot);
        String snapshotHash = fileHash(snapshotPath);
        String contextHash = fileHash(contextPath);
        String impactHash = fileHash(impactPath);
        String scenarioImpactHash = fileHash(scenarioImpactPath);

        if (Files.isRegularFile(snapshotPath)) {
            bindGraphArtifact(connection, project, goal, snapshot, "used", "graph_snapshot",
                    "GRAPH_SNAPSHOT.json", snapshotPath, snapshotHash,
                    "Graph snapshot used for goal completion", now);
        }
        if (Files.isRegularFile(contextPath)) {
            bindGraphArtifact(connection, project, goal, snapshot, "summary", "graph_context",
                    "GRAPH_CONTEXT.md", contextPath, contextHash,
                    "Graph context export used for goal completion", now);
        }
        if (Files.isRegularFile(impactPath)) {
            bindGraphArtifact(connection, project, goal, snapshot, "impact_map", "graph_impact_map",
                    "IMPACT_MAP.md", impactPath, impactHash,
                    "Impact map used for goal completion", now);
        }
        if (Files.isRegularFile(scenarioImpactPath)) {
            bindScenarioImpactArtifact(connection, project, goal, scenarioImpactPath,
                    scenarioImpactHash, now);
        }

        return new GoalGraphArtifacts(true, snapshot.id(), snapshot.snapshotKey(), snapshot.provider(),
                snapshot.fileCount(), snapshot.nodeCount(), snapshot.edgeCount(),
                Files.isRegularFile(snapshotPath) ? snapshotPath.toString() : "",
                Files.isRegularFile(contextPath) ? contextPath.toString() : "",
                Files.isRegularFile(impactPath) ? impactPath.toString() : "",
                Files.isRegularFile(scenarioImpactPath) ? scenarioImpactPath.toString() : "",
                snapshotHash, contextHash, impactHash, scenarioImpactHash);
    }

    private void bindGraphArtifact(Connection connection, Project project, GoalRun goal, GraphSnapshot snapshot,
                                   String bindingType, String artifactType, String title, Path path,
                                   String contentHash, String summary, String now) throws Exception {
        goalGraphBindingRepository.upsert(connection, goal.goalKey(), snapshot.id(), bindingType,
                path.toString(), contentHash, now);
        goalArtifactRepository.insert(connection, new GoalArtifact(0L, goal.goalKey(), artifactType,
                title, path.toString(), contentHash,
                summary + "; snapshot_key=" + snapshot.snapshotKey(), now));
        if (goal.workflowRunKey().length() > 0) {
            workflowArtifactRepository.insert(connection, new WorkflowArtifact(0L, project.projectKey(),
                    goal.workflowRunKey(), "custom", title, path.toString(), contentHash,
                    "confirmed", "goal_complete", summary + " for goal " + goal.goalKey(),
                    "goal,completion,graph", now, now));
        }
    }

    private void bindScenarioImpactArtifact(Connection connection, Project project, GoalRun goal,
                                            Path path, String contentHash, String now) throws Exception {
        goalArtifactRepository.insert(connection, new GoalArtifact(0L, goal.goalKey(), "scenario_impact_map",
                "SCENARIO_IMPACT_MAP.md", path.toString(), contentHash,
                "Scenario impact map used for goal completion", now));
        if (goal.workflowRunKey().length() > 0) {
            workflowArtifactRepository.insert(connection, new WorkflowArtifact(0L, project.projectKey(),
                    goal.workflowRunKey(), "custom", "SCENARIO_IMPACT_MAP.md", path.toString(), contentHash,
                    "confirmed", "goal_complete",
                    "Scenario impact map exported for goal " + goal.goalKey(),
                    "goal,completion,graph,bdd", now, now));
        }
    }

    private String checkpointSummary(GoalProfile profile, GoalGraphArtifacts graphArtifacts) {
        if (profile != null && profile.graphRequired() && graphArtifacts != null && graphArtifacts.enabled()) {
            return "goal checks accepted; graph evidence bound snapshot_key=" + graphArtifacts.snapshotKey();
        }
        return "goal checks accepted";
    }

    private String fileHash(Path path) throws Exception {
        if (path == null || !Files.isRegularFile(path)) {
            return "";
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(Files.readAllBytes(path));
        StringBuilder builder = new StringBuilder("sha256:");
        for (byte b : hash) {
            builder.append(String.format("%02x", b & 0xff));
        }
        return builder.toString();
    }
}
