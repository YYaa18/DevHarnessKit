package com.devharnesskit.dhk.model.goal;

public final class GoalActionMapping {
    public static final String MODE_NONE = "none";
    public static final String MODE_STEP = "step";
    public static final String MODE_CHECK = "check";
    public static final String ACCEPTANCE_NONE = "none";
    public static final String ACCEPTANCE_MANUAL = "manual";
    public static final String ACCEPTANCE_CHECKS = "checks";

    private final String actionKey;
    private final String workflowPhase;
    private final String[] requiredGates;
    private final String specTask;
    private final String specAcceptanceUpdate;
    private final String phasePassMode;
    private final String gatePassMode;
    private final String acceptanceSource;
    private final String[] requiredChecks;

    public GoalActionMapping(String actionKey, String workflowPhase, String[] requiredGates,
                             String specTask, String specAcceptanceUpdate) {
        this(actionKey, workflowPhase, requiredGates, specTask, specAcceptanceUpdate,
                "", "", "", new String[0]);
    }

    public GoalActionMapping(String actionKey, String workflowPhase, String[] requiredGates,
                             String specTask, String specAcceptanceUpdate,
                             String phasePassMode, String gatePassMode,
                             String acceptanceSource, String[] requiredChecks) {
        this.actionKey = value(actionKey);
        this.workflowPhase = value(workflowPhase);
        this.requiredGates = requiredGates == null ? new String[0] : requiredGates;
        this.specTask = value(specTask);
        this.specAcceptanceUpdate = value(specAcceptanceUpdate);
        this.phasePassMode = normalizeMode(phasePassMode, defaultPhasePassMode());
        this.gatePassMode = normalizeMode(gatePassMode, defaultGatePassMode());
        this.acceptanceSource = normalizeAcceptanceSource(acceptanceSource, defaultAcceptanceSource());
        this.requiredChecks = requiredChecks == null ? new String[0] : requiredChecks;
    }

    public String actionKey() { return actionKey; }
    public String workflowPhase() { return workflowPhase; }
    public String[] requiredGates() { return requiredGates; }
    public String specTask() { return specTask; }
    public String specAcceptanceUpdate() { return specAcceptanceUpdate; }
    public String phasePassMode() { return phasePassMode; }
    public String gatePassMode() { return gatePassMode; }
    public String acceptanceSource() { return acceptanceSource; }
    public String[] requiredChecks() { return requiredChecks; }

    private String value(String value) {
        return value == null ? "" : value.trim();
    }

    private String defaultPhasePassMode() {
        if (workflowPhase.length() == 0) {
            return MODE_NONE;
        }
        if (workflowPhase.startsWith("verify_") || actionKey.indexOf("verify") >= 0) {
            return MODE_CHECK;
        }
        return MODE_STEP;
    }

    private String defaultGatePassMode() {
        return requiredGates.length == 0 ? MODE_NONE : MODE_STEP;
    }

    private String defaultAcceptanceSource() {
        if ("auto_pass".equals(specAcceptanceUpdate)) {
            return ACCEPTANCE_CHECKS;
        }
        if ("manual".equals(specAcceptanceUpdate)) {
            return ACCEPTANCE_MANUAL;
        }
        return ACCEPTANCE_NONE;
    }

    private String normalizeMode(String value, String defaultValue) {
        String normalized = value(value);
        if (MODE_NONE.equals(normalized) || MODE_STEP.equals(normalized) || MODE_CHECK.equals(normalized)) {
            return normalized;
        }
        return defaultValue;
    }

    private String normalizeAcceptanceSource(String value, String defaultValue) {
        String normalized = value(value);
        if (ACCEPTANCE_NONE.equals(normalized) || ACCEPTANCE_MANUAL.equals(normalized)
                || ACCEPTANCE_CHECKS.equals(normalized)) {
            return normalized;
        }
        return defaultValue;
    }
}
