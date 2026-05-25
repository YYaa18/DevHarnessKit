package com.devharnesskit.dhk.model.goal;

public final class GoalStep {
    private final long id;
    private final String goalKey;
    private final int stepIndex;
    private final String actionKey;
    private final String summary;
    private final String changedFiles;
    private final String evidence;
    private final String status;
    private final String createdAt;

    public GoalStep(long id, String goalKey, int stepIndex, String actionKey, String summary,
                    String changedFiles, String evidence, String status, String createdAt) {
        this.id = id;
        this.goalKey = value(goalKey);
        this.stepIndex = stepIndex;
        this.actionKey = value(actionKey);
        this.summary = value(summary);
        this.changedFiles = value(changedFiles);
        this.evidence = value(evidence);
        this.status = value(status);
        this.createdAt = value(createdAt);
    }

    public long id() { return id; }
    public String goalKey() { return goalKey; }
    public int stepIndex() { return stepIndex; }
    public String actionKey() { return actionKey; }
    public String summary() { return summary; }
    public String changedFiles() { return changedFiles; }
    public String evidence() { return evidence; }
    public String status() { return status; }
    public String createdAt() { return createdAt; }

    private String value(String raw) {
        return raw == null ? "" : raw;
    }
}
