package com.devharnesskit.dhk.model.workflow;

public final class WorkflowGateRun {
    private final long id;
    private final String runKey;
    private final String phaseKey;
    private final String gateKey;
    private final String gateName;
    private final String gateType;
    private final String severity;
    private final String status;
    private final String checkedAt;
    private final String resultSummary;
    private final String failureReason;
    private final String evidence;
    private final String commandOutputPath;
    private final String createdAt;
    private final String updatedAt;

    public WorkflowGateRun(long id, String runKey, String phaseKey, String gateKey, String gateName,
                           String gateType, String severity, String status, String checkedAt,
                           String resultSummary, String failureReason, String evidence,
                           String commandOutputPath, String createdAt, String updatedAt) {
        this.id = id;
        this.runKey = value(runKey);
        this.phaseKey = value(phaseKey);
        this.gateKey = value(gateKey);
        this.gateName = value(gateName);
        this.gateType = value(gateType);
        this.severity = value(severity);
        this.status = value(status);
        this.checkedAt = value(checkedAt);
        this.resultSummary = value(resultSummary);
        this.failureReason = value(failureReason);
        this.evidence = value(evidence);
        this.commandOutputPath = value(commandOutputPath);
        this.createdAt = value(createdAt);
        this.updatedAt = value(updatedAt);
    }

    public long id() { return id; }
    public String runKey() { return runKey; }
    public String phaseKey() { return phaseKey; }
    public String gateKey() { return gateKey; }
    public String gateName() { return gateName; }
    public String gateType() { return gateType; }
    public String severity() { return severity; }
    public String status() { return status; }
    public String checkedAt() { return checkedAt; }
    public String resultSummary() { return resultSummary; }
    public String failureReason() { return failureReason; }
    public String evidence() { return evidence; }
    public String commandOutputPath() { return commandOutputPath; }
    public String createdAt() { return createdAt; }
    public String updatedAt() { return updatedAt; }

    private String value(String raw) {
        return raw == null ? "" : raw;
    }
}
