package com.devharnesskit.dhk.model.workflow;

public final class WorkflowGateTemplate {
    private final long id;
    private final String workflowKey;
    private final String phaseKey;
    private final String gateKey;
    private final String gateName;
    private final String gateType;
    private final String severity;
    private final String ruleText;
    private final String checkCommand;
    private final String expectedResult;
    private final String createdAt;
    private final String updatedAt;

    public WorkflowGateTemplate(long id, String workflowKey, String phaseKey, String gateKey,
                                String gateName, String gateType, String severity, String ruleText,
                                String checkCommand, String expectedResult, String createdAt,
                                String updatedAt) {
        this.id = id;
        this.workflowKey = value(workflowKey);
        this.phaseKey = value(phaseKey);
        this.gateKey = value(gateKey);
        this.gateName = value(gateName);
        this.gateType = value(gateType);
        this.severity = value(severity);
        this.ruleText = value(ruleText);
        this.checkCommand = value(checkCommand);
        this.expectedResult = value(expectedResult);
        this.createdAt = value(createdAt);
        this.updatedAt = value(updatedAt);
    }

    public long id() { return id; }
    public String workflowKey() { return workflowKey; }
    public String phaseKey() { return phaseKey; }
    public String gateKey() { return gateKey; }
    public String gateName() { return gateName; }
    public String gateType() { return gateType; }
    public String severity() { return severity; }
    public String ruleText() { return ruleText; }
    public String checkCommand() { return checkCommand; }
    public String expectedResult() { return expectedResult; }
    public String createdAt() { return createdAt; }
    public String updatedAt() { return updatedAt; }

    private String value(String raw) {
        return raw == null ? "" : raw;
    }
}
