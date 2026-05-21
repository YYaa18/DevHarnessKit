package com.devharnesskit.dhk.model.workflow;

public final class WorkflowPhaseTemplate {
    private final long id;
    private final String workflowKey;
    private final String phaseKey;
    private final String phaseName;
    private final int phaseOrder;
    private final String phaseType;
    private final int required;
    private final String instruction;
    private final String expectedOutput;
    private final String defaultGatePolicy;
    private final String createdAt;
    private final String updatedAt;

    public WorkflowPhaseTemplate(long id, String workflowKey, String phaseKey, String phaseName,
                                 int phaseOrder, String phaseType, int required, String instruction,
                                 String expectedOutput, String defaultGatePolicy, String createdAt,
                                 String updatedAt) {
        this.id = id;
        this.workflowKey = value(workflowKey);
        this.phaseKey = value(phaseKey);
        this.phaseName = value(phaseName);
        this.phaseOrder = phaseOrder;
        this.phaseType = value(phaseType);
        this.required = required;
        this.instruction = value(instruction);
        this.expectedOutput = value(expectedOutput);
        this.defaultGatePolicy = value(defaultGatePolicy);
        this.createdAt = value(createdAt);
        this.updatedAt = value(updatedAt);
    }

    public long id() { return id; }
    public String workflowKey() { return workflowKey; }
    public String phaseKey() { return phaseKey; }
    public String phaseName() { return phaseName; }
    public int phaseOrder() { return phaseOrder; }
    public String phaseType() { return phaseType; }
    public int required() { return required; }
    public String instruction() { return instruction; }
    public String expectedOutput() { return expectedOutput; }
    public String defaultGatePolicy() { return defaultGatePolicy; }
    public String createdAt() { return createdAt; }
    public String updatedAt() { return updatedAt; }

    private String value(String raw) {
        return raw == null ? "" : raw;
    }
}
