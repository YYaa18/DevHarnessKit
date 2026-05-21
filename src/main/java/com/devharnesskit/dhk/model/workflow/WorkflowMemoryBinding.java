package com.devharnesskit.dhk.model.workflow;

public final class WorkflowMemoryBinding {
    private final long id;
    private final String runKey;
    private final long memoryId;
    private final String bindingType;
    private final String phaseKey;
    private final String reason;
    private final String createdAt;

    public WorkflowMemoryBinding(long id, String runKey, long memoryId, String bindingType,
                                 String phaseKey, String reason, String createdAt) {
        this.id = id;
        this.runKey = value(runKey);
        this.memoryId = memoryId;
        this.bindingType = value(bindingType);
        this.phaseKey = value(phaseKey);
        this.reason = value(reason);
        this.createdAt = value(createdAt);
    }

    public long id() { return id; }
    public String runKey() { return runKey; }
    public long memoryId() { return memoryId; }
    public String bindingType() { return bindingType; }
    public String phaseKey() { return phaseKey; }
    public String reason() { return reason; }
    public String createdAt() { return createdAt; }

    private String value(String raw) {
        return raw == null ? "" : raw;
    }
}
