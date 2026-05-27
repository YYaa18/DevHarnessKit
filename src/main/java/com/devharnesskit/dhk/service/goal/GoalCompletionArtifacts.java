package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.goal.GoalGraphArtifacts;

import java.nio.file.Path;

final class GoalCompletionArtifacts {
    private final long checkpointId;
    private final Path summaryPath;
    private final GoalGraphArtifacts graphArtifacts;

    GoalCompletionArtifacts(long checkpointId, Path summaryPath, GoalGraphArtifacts graphArtifacts) {
        this.checkpointId = checkpointId;
        this.summaryPath = summaryPath;
        this.graphArtifacts = graphArtifacts;
    }

    long checkpointId() { return checkpointId; }
    Path summaryPath() { return summaryPath; }
    GoalGraphArtifacts graphArtifacts() { return graphArtifacts; }
}
