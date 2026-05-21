package com.devharnesskit.dhk.model.workflow;

public final class WorkflowPhaseRun {
    private final long id;
    private final String runKey;
    private final String phaseKey;
    private final String phaseName;
    private final int phaseOrder;
    private final String status;
    private final String startedAt;
    private final String completedAt;
    private final String inputSummary;
    private final String outputSummary;
    private final String evidence;
    private final String notes;
    private final String createdAt;
    private final String updatedAt;

    public WorkflowPhaseRun(long id, String runKey, String phaseKey, String phaseName,
                            int phaseOrder, String status, String startedAt, String completedAt,
                            String inputSummary, String outputSummary, String evidence,
                            String notes, String createdAt, String updatedAt) {
        this.id = id;
        this.runKey = value(runKey);
        this.phaseKey = value(phaseKey);
        this.phaseName = value(phaseName);
        this.phaseOrder = phaseOrder;
        this.status = value(status);
        this.startedAt = value(startedAt);
        this.completedAt = value(completedAt);
        this.inputSummary = value(inputSummary);
        this.outputSummary = value(outputSummary);
        this.evidence = value(evidence);
        this.notes = value(notes);
        this.createdAt = value(createdAt);
        this.updatedAt = value(updatedAt);
    }

    public long id() { return id; }
    public String runKey() { return runKey; }
    public String phaseKey() { return phaseKey; }
    public String phaseName() { return phaseName; }
    public int phaseOrder() { return phaseOrder; }
    public String status() { return status; }
    public String startedAt() { return startedAt; }
    public String completedAt() { return completedAt; }
    public String inputSummary() { return inputSummary; }
    public String outputSummary() { return outputSummary; }
    public String evidence() { return evidence; }
    public String notes() { return notes; }
    public String createdAt() { return createdAt; }
    public String updatedAt() { return updatedAt; }

    private String value(String raw) {
        return raw == null ? "" : raw;
    }
}
