package com.devharnesskit.dhk.model.goal;

public final class GoalRun {
    private final String goalKey;
    private final String projectKey;
    private final String workflowRunKey;
    private final String specChangeKey;
    private final String externalRef;
    private final String profileKey;
    private final String taskName;
    private final String moduleName;
    private final String mode;
    private final String conditionText;
    private final String status;
    private final String currentAction;
    private final int maxSteps;
    private final int stepCount;
    private final String createdAt;
    private final String updatedAt;
    private final String completedAt;

    public GoalRun(String goalKey, String projectKey, String workflowRunKey, String specChangeKey,
                   String profileKey, String taskName, String moduleName, String mode,
                   String conditionText, String status, String currentAction, int maxSteps,
                   int stepCount, String createdAt, String updatedAt, String completedAt) {
        this(goalKey, projectKey, workflowRunKey, specChangeKey, "", profileKey, taskName,
                moduleName, mode, conditionText, status, currentAction, maxSteps, stepCount,
                createdAt, updatedAt, completedAt);
    }

    public GoalRun(String goalKey, String projectKey, String workflowRunKey, String specChangeKey,
                   String externalRef, String profileKey, String taskName, String moduleName, String mode,
                   String conditionText, String status, String currentAction, int maxSteps,
                   int stepCount, String createdAt, String updatedAt, String completedAt) {
        this.goalKey = value(goalKey);
        this.projectKey = value(projectKey);
        this.workflowRunKey = value(workflowRunKey);
        this.specChangeKey = value(specChangeKey);
        this.externalRef = value(externalRef);
        this.profileKey = value(profileKey);
        this.taskName = value(taskName);
        this.moduleName = value(moduleName);
        this.mode = value(mode);
        this.conditionText = value(conditionText);
        this.status = value(status);
        this.currentAction = value(currentAction);
        this.maxSteps = maxSteps;
        this.stepCount = stepCount;
        this.createdAt = value(createdAt);
        this.updatedAt = value(updatedAt);
        this.completedAt = value(completedAt);
    }

    public String goalKey() { return goalKey; }
    public String projectKey() { return projectKey; }
    public String workflowRunKey() { return workflowRunKey; }
    public String specChangeKey() { return specChangeKey; }
    public String externalRef() { return externalRef; }
    public String profileKey() { return profileKey; }
    public String taskName() { return taskName; }
    public String moduleName() { return moduleName; }
    public String mode() { return mode; }
    public String conditionText() { return conditionText; }
    public String status() { return status; }
    public String currentAction() { return currentAction; }
    public int maxSteps() { return maxSteps; }
    public int stepCount() { return stepCount; }
    public String createdAt() { return createdAt; }
    public String updatedAt() { return updatedAt; }
    public String completedAt() { return completedAt; }

    private String value(String raw) {
        return raw == null ? "" : raw;
    }
}
