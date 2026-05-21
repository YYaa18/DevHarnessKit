package com.devharnesskit.dhk.model.workflow;

public final class WorkflowRun {
    private final String runKey;
    private final String projectKey;
    private final String workflowKey;
    private final String taskName;
    private final String taskSummary;
    private final String moduleName;
    private final String mode;
    private final String status;
    private final String currentPhaseKey;
    private final String contextExportPath;
    private final String recoveryExportPath;
    private final Long checkpointId;
    private final String startedAt;
    private final String completedAt;
    private final String createdAt;
    private final String updatedAt;

    public WorkflowRun(String runKey, String projectKey, String workflowKey, String taskName,
                       String taskSummary, String moduleName, String mode, String status,
                       String currentPhaseKey, String contextExportPath, String recoveryExportPath,
                       Long checkpointId, String startedAt, String completedAt, String createdAt,
                       String updatedAt) {
        this.runKey = value(runKey);
        this.projectKey = value(projectKey);
        this.workflowKey = value(workflowKey);
        this.taskName = value(taskName);
        this.taskSummary = value(taskSummary);
        this.moduleName = value(moduleName);
        this.mode = value(mode);
        this.status = value(status);
        this.currentPhaseKey = value(currentPhaseKey);
        this.contextExportPath = value(contextExportPath);
        this.recoveryExportPath = value(recoveryExportPath);
        this.checkpointId = checkpointId;
        this.startedAt = value(startedAt);
        this.completedAt = value(completedAt);
        this.createdAt = value(createdAt);
        this.updatedAt = value(updatedAt);
    }

    public String runKey() { return runKey; }
    public String projectKey() { return projectKey; }
    public String workflowKey() { return workflowKey; }
    public String taskName() { return taskName; }
    public String taskSummary() { return taskSummary; }
    public String moduleName() { return moduleName; }
    public String mode() { return mode; }
    public String status() { return status; }
    public String currentPhaseKey() { return currentPhaseKey; }
    public String contextExportPath() { return contextExportPath; }
    public String recoveryExportPath() { return recoveryExportPath; }
    public Long checkpointId() { return checkpointId; }
    public String startedAt() { return startedAt; }
    public String completedAt() { return completedAt; }
    public String createdAt() { return createdAt; }
    public String updatedAt() { return updatedAt; }

    private String value(String raw) {
        return raw == null ? "" : raw;
    }
}
