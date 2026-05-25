package com.devharnesskit.dhk.model.goal;

public final class GoalProfile {
    private final String profileKey;
    private final String workflowKey;
    private final boolean specRequired;
    private final String defaultMode;
    private final String[] actions;

    public GoalProfile(String profileKey, String workflowKey, boolean specRequired,
                       String defaultMode, String[] actions) {
        this.profileKey = profileKey;
        this.workflowKey = workflowKey;
        this.specRequired = specRequired;
        this.defaultMode = defaultMode;
        this.actions = actions == null ? new String[0] : actions;
    }

    public String profileKey() { return profileKey; }
    public String workflowKey() { return workflowKey; }
    public boolean specRequired() { return specRequired; }
    public String defaultMode() { return defaultMode; }
    public String[] actions() { return actions; }
}
