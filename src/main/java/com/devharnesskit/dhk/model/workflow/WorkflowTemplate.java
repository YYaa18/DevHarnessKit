package com.devharnesskit.dhk.model.workflow;

public final class WorkflowTemplate {
    private final String workflowKey;
    private final String name;
    private final String description;
    private final String category;
    private final String targetMode;
    private final String status;
    private final int version;
    private final String sourceKind;
    private final String sourceRef;
    private final String createdAt;
    private final String updatedAt;

    public WorkflowTemplate(String workflowKey, String name, String description, String category,
                            String targetMode, String status, int version, String sourceKind,
                            String sourceRef, String createdAt, String updatedAt) {
        this.workflowKey = value(workflowKey);
        this.name = value(name);
        this.description = value(description);
        this.category = value(category);
        this.targetMode = value(targetMode);
        this.status = value(status);
        this.version = version;
        this.sourceKind = value(sourceKind);
        this.sourceRef = value(sourceRef);
        this.createdAt = value(createdAt);
        this.updatedAt = value(updatedAt);
    }

    public String workflowKey() { return workflowKey; }
    public String name() { return name; }
    public String description() { return description; }
    public String category() { return category; }
    public String targetMode() { return targetMode; }
    public String status() { return status; }
    public int version() { return version; }
    public String sourceKind() { return sourceKind; }
    public String sourceRef() { return sourceRef; }
    public String createdAt() { return createdAt; }
    public String updatedAt() { return updatedAt; }

    private String value(String raw) {
        return raw == null ? "" : raw;
    }
}
