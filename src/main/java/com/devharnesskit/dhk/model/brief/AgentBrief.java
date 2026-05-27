package com.devharnesskit.dhk.model.brief;

public final class AgentBrief {
    public static final String SCHEMA_VERSION = "devharness-agent-brief/v1-alpha";

    private final String briefId;
    private final String recommendationId;
    private final String taskKey;
    private final String goalKey;
    private final String mode;
    private final String profileKey;
    private final String currentAction;
    private final String[] allowedActions;
    private final String[] forbiddenActions;
    private final String[] requiredEvidence;
    private final String[] softEscalationRules;
    private final String[] hardEscalationRules;
    private final String compileMode;
    private final String testMode;
    private final String graphMode;
    private final boolean manualEvidenceRequired;
    private final String userBriefPath;
    private final boolean requireUserConfirmation;
    private final HarnessCommand[] harnessCommands;

    public AgentBrief(String briefId, String recommendationId, String taskKey, String goalKey,
                      String mode, String profileKey, String currentAction,
                      String[] allowedActions, String[] forbiddenActions, String[] requiredEvidence,
                      String[] softEscalationRules, String[] hardEscalationRules,
                      String compileMode, String testMode, String graphMode,
                      boolean manualEvidenceRequired, String userBriefPath,
                      boolean requireUserConfirmation, HarnessCommand[] harnessCommands) {
        this.briefId = value(briefId);
        this.recommendationId = value(recommendationId);
        this.taskKey = value(taskKey);
        this.goalKey = value(goalKey);
        this.mode = value(mode);
        this.profileKey = value(profileKey);
        this.currentAction = value(currentAction);
        this.allowedActions = array(allowedActions);
        this.forbiddenActions = array(forbiddenActions);
        this.requiredEvidence = array(requiredEvidence);
        this.softEscalationRules = array(softEscalationRules);
        this.hardEscalationRules = array(hardEscalationRules);
        this.compileMode = value(compileMode);
        this.testMode = value(testMode);
        this.graphMode = value(graphMode);
        this.manualEvidenceRequired = manualEvidenceRequired;
        this.userBriefPath = value(userBriefPath);
        this.requireUserConfirmation = requireUserConfirmation;
        this.harnessCommands = harnessCommands == null ? new HarnessCommand[0] : harnessCommands;
    }

    public String briefId() { return briefId; }
    public String recommendationId() { return recommendationId; }
    public String taskKey() { return taskKey; }
    public String goalKey() { return goalKey; }
    public String mode() { return mode; }
    public String profileKey() { return profileKey; }
    public String currentAction() { return currentAction; }
    public String[] allowedActions() { return allowedActions; }
    public String[] forbiddenActions() { return forbiddenActions; }
    public String[] requiredEvidence() { return requiredEvidence; }
    public String[] softEscalationRules() { return softEscalationRules; }
    public String[] hardEscalationRules() { return hardEscalationRules; }
    public String compileMode() { return compileMode; }
    public String testMode() { return testMode; }
    public String graphMode() { return graphMode; }
    public boolean manualEvidenceRequired() { return manualEvidenceRequired; }
    public String userBriefPath() { return userBriefPath; }
    public boolean requireUserConfirmation() { return requireUserConfirmation; }
    public HarnessCommand[] harnessCommands() { return harnessCommands; }

    private static String value(String value) {
        return value == null ? "" : value.trim();
    }

    private static String[] array(String[] values) {
        return values == null ? new String[0] : values;
    }
}
