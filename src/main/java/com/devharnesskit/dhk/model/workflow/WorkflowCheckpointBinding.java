package com.devharnesskit.dhk.model.workflow;

public final class WorkflowCheckpointBinding {
    private final long id;
    private final String runKey;
    private final long checkpointId;
    private final String bindingType;
    private final String createdAt;

    public WorkflowCheckpointBinding(long id, String runKey, long checkpointId,
                                     String bindingType, String createdAt) {
        this.id = id;
        this.runKey = value(runKey);
        this.checkpointId = checkpointId;
        this.bindingType = value(bindingType);
        this.createdAt = value(createdAt);
    }

    public long id() { return id; }
    public String runKey() { return runKey; }
    public long checkpointId() { return checkpointId; }
    public String bindingType() { return bindingType; }
    public String createdAt() { return createdAt; }

    private String value(String raw) {
        return raw == null ? "" : raw;
    }
}
