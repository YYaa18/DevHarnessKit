package com.devharnesskit.dhk.model.workflow;

public final class WorkflowEvent {
    private final long id;
    private final String projectKey;
    private final String runKey;
    private final String eventType;
    private final String phaseKey;
    private final String gateKey;
    private final String level;
    private final String message;
    private final String data;
    private final String createdAt;

    public WorkflowEvent(long id, String projectKey, String runKey, String eventType, String phaseKey,
                         String gateKey, String level, String message, String data, String createdAt) {
        this.id = id;
        this.projectKey = value(projectKey);
        this.runKey = value(runKey);
        this.eventType = value(eventType);
        this.phaseKey = value(phaseKey);
        this.gateKey = value(gateKey);
        this.level = value(level);
        this.message = value(message);
        this.data = value(data);
        this.createdAt = value(createdAt);
    }

    public long id() { return id; }
    public String projectKey() { return projectKey; }
    public String runKey() { return runKey; }
    public String eventType() { return eventType; }
    public String phaseKey() { return phaseKey; }
    public String gateKey() { return gateKey; }
    public String level() { return level; }
    public String message() { return message; }
    public String data() { return data; }
    public String createdAt() { return createdAt; }

    private String value(String raw) {
        return raw == null ? "" : raw;
    }
}
