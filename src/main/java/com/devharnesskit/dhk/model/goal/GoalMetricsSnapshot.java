package com.devharnesskit.dhk.model.goal;

public final class GoalMetricsSnapshot {
    public static final String SCHEMA_VERSION = "goal-metrics/v1";

    private final String goalKey;
    private final String profileKey;
    private final String status;
    private final String currentAction;
    private final int expectedSteps;
    private final int recordedSteps;
    private final int acceptedSteps;
    private final int totalChecks;
    private final int requiredChecks;
    private final int passedChecks;
    private final int failedChecks;
    private final int skippedChecks;
    private final int waivedChecks;
    private final int staleChecks;
    private final int totalEvents;
    private final int totalArtifacts;
    private final String startedAt;
    private final String completedAt;
    private final long durationMs;
    private final String bddStatus;
    private final int bddScenarioCoveragePercent;
    private final String bddEvidenceFreshness;
    private final int bddQualityScore;
    private final String bddFailureReasons;

    public GoalMetricsSnapshot(String goalKey, String profileKey, String status, String currentAction,
                               int expectedSteps, int recordedSteps, int acceptedSteps,
                               int totalChecks, int requiredChecks, int passedChecks,
                               int failedChecks, int skippedChecks, int waivedChecks,
                               int staleChecks, int totalEvents, int totalArtifacts,
                               String startedAt, String completedAt, long durationMs) {
        this(goalKey, profileKey, status, currentAction, expectedSteps, recordedSteps, acceptedSteps,
                totalChecks, requiredChecks, passedChecks, failedChecks, skippedChecks, waivedChecks,
                staleChecks, totalEvents, totalArtifacts, startedAt, completedAt, durationMs,
                "", -1, "missing", -1, "");
    }

    public GoalMetricsSnapshot(String goalKey, String profileKey, String status, String currentAction,
                               int expectedSteps, int recordedSteps, int acceptedSteps,
                               int totalChecks, int requiredChecks, int passedChecks,
                               int failedChecks, int skippedChecks, int waivedChecks,
                               int staleChecks, int totalEvents, int totalArtifacts,
                               String startedAt, String completedAt, long durationMs,
                               String bddStatus, int bddScenarioCoveragePercent,
                               String bddEvidenceFreshness, int bddQualityScore,
                               String bddFailureReasons) {
        this.goalKey = value(goalKey);
        this.profileKey = value(profileKey);
        this.status = value(status);
        this.currentAction = value(currentAction);
        this.expectedSteps = expectedSteps;
        this.recordedSteps = recordedSteps;
        this.acceptedSteps = acceptedSteps;
        this.totalChecks = totalChecks;
        this.requiredChecks = requiredChecks;
        this.passedChecks = passedChecks;
        this.failedChecks = failedChecks;
        this.skippedChecks = skippedChecks;
        this.waivedChecks = waivedChecks;
        this.staleChecks = staleChecks;
        this.totalEvents = totalEvents;
        this.totalArtifacts = totalArtifacts;
        this.startedAt = value(startedAt);
        this.completedAt = value(completedAt);
        this.durationMs = durationMs;
        this.bddStatus = value(bddStatus);
        this.bddScenarioCoveragePercent = bddScenarioCoveragePercent;
        this.bddEvidenceFreshness = value(bddEvidenceFreshness);
        this.bddQualityScore = bddQualityScore;
        this.bddFailureReasons = value(bddFailureReasons);
    }

    public String schemaVersion() { return SCHEMA_VERSION; }
    public String goalKey() { return goalKey; }
    public String profileKey() { return profileKey; }
    public String status() { return status; }
    public String currentAction() { return currentAction; }
    public int expectedSteps() { return expectedSteps; }
    public int recordedSteps() { return recordedSteps; }
    public int acceptedSteps() { return acceptedSteps; }
    public int totalChecks() { return totalChecks; }
    public int requiredChecks() { return requiredChecks; }
    public int passedChecks() { return passedChecks; }
    public int failedChecks() { return failedChecks; }
    public int skippedChecks() { return skippedChecks; }
    public int waivedChecks() { return waivedChecks; }
    public int staleChecks() { return staleChecks; }
    public int totalEvents() { return totalEvents; }
    public int totalArtifacts() { return totalArtifacts; }
    public String startedAt() { return startedAt; }
    public String completedAt() { return completedAt; }
    public long durationMs() { return durationMs; }
    public String bddStatus() { return bddStatus; }
    public int bddScenarioCoveragePercent() { return bddScenarioCoveragePercent; }
    public String bddEvidenceFreshness() { return bddEvidenceFreshness; }
    public int bddQualityScore() { return bddQualityScore; }
    public String bddFailureReasons() { return bddFailureReasons; }

    private String value(String raw) {
        return raw == null ? "" : raw;
    }
}
