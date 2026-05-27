package com.devharnesskit.dhk.model.checkpoint;

public final class HumanCheckpoint {
    private final long id;
    private final String goalKey;
    private final String checkpointType;
    private final String reason;
    private final String status;
    private final String requestedBy;
    private final String requestedAt;
    private final String approver;
    private final String approvedAt;
    private final String decisionReason;
    private final String createdAt;
    private final String updatedAt;

    public HumanCheckpoint(long id, String goalKey, String checkpointType, String reason,
                           String status, String requestedBy, String requestedAt, String approver,
                           String approvedAt, String decisionReason, String createdAt,
                           String updatedAt) {
        this.id = id;
        this.goalKey = value(goalKey);
        this.checkpointType = value(checkpointType);
        this.reason = value(reason);
        this.status = value(status);
        this.requestedBy = value(requestedBy);
        this.requestedAt = value(requestedAt);
        this.approver = value(approver);
        this.approvedAt = value(approvedAt);
        this.decisionReason = value(decisionReason);
        this.createdAt = value(createdAt);
        this.updatedAt = value(updatedAt);
    }

    public long id() { return id; }
    public String goalKey() { return goalKey; }
    public String checkpointType() { return checkpointType; }
    public String reason() { return reason; }
    public String status() { return status; }
    public String requestedBy() { return requestedBy; }
    public String requestedAt() { return requestedAt; }
    public String approver() { return approver; }
    public String approvedAt() { return approvedAt; }
    public String decisionReason() { return decisionReason; }
    public String createdAt() { return createdAt; }
    public String updatedAt() { return updatedAt; }

    private String value(String raw) {
        return raw == null ? "" : raw;
    }
}
