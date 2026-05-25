package com.devharnesskit.dhk.model.goal;

public final class GoalActionMapping {
    private final String actionKey;
    private final String workflowPhase;
    private final String[] requiredGates;
    private final String specTask;
    private final String specAcceptanceUpdate;

    public GoalActionMapping(String actionKey, String workflowPhase, String[] requiredGates,
                             String specTask, String specAcceptanceUpdate) {
        this.actionKey = value(actionKey);
        this.workflowPhase = value(workflowPhase);
        this.requiredGates = requiredGates == null ? new String[0] : requiredGates;
        this.specTask = value(specTask);
        this.specAcceptanceUpdate = value(specAcceptanceUpdate);
    }

    public String actionKey() { return actionKey; }
    public String workflowPhase() { return workflowPhase; }
    public String[] requiredGates() { return requiredGates; }
    public String specTask() { return specTask; }
    public String specAcceptanceUpdate() { return specAcceptanceUpdate; }

    private String value(String value) {
        return value == null ? "" : value.trim();
    }
}
