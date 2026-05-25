package com.devharnesskit.dhk.model.goal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class GoalProfile {
    private final String profileKey;
    private final String workflowKey;
    private final boolean specRequired;
    private final String defaultMode;
    private final String[] actions;
    private final Map<String, String[]> requiredEvidenceByAction;
    private final String[] requiredChecks;
    private final boolean completionRequireFreshChecks;
    private final boolean completionAllowSkippedChecks;
    private final boolean completionRequireCheckpoint;
    private final boolean specRequireNonEmptyTasks;
    private final boolean specRequireNonEmptyAcceptance;
    private final Map<String, GoalActionMapping> actionMappings;

    public GoalProfile(String profileKey, String workflowKey, boolean specRequired,
                       String defaultMode, String[] actions) {
        this(profileKey, workflowKey, specRequired, defaultMode, actions,
                new LinkedHashMap<String, String[]>(), new String[0],
                true, true, true, specRequired, specRequired,
                new LinkedHashMap<String, GoalActionMapping>());
    }

    public GoalProfile(String profileKey, String workflowKey, boolean specRequired,
                       String defaultMode, String[] actions,
                       Map<String, String[]> requiredEvidenceByAction,
                       String[] requiredChecks,
                       boolean completionRequireFreshChecks,
                       boolean completionAllowSkippedChecks,
                       boolean completionRequireCheckpoint,
                       Map<String, GoalActionMapping> actionMappings) {
        this(profileKey, workflowKey, specRequired, defaultMode, actions,
                requiredEvidenceByAction, requiredChecks, completionRequireFreshChecks,
                completionAllowSkippedChecks, completionRequireCheckpoint,
                specRequired, specRequired, actionMappings);
    }

    public GoalProfile(String profileKey, String workflowKey, boolean specRequired,
                       String defaultMode, String[] actions,
                       Map<String, String[]> requiredEvidenceByAction,
                       String[] requiredChecks,
                       boolean completionRequireFreshChecks,
                       boolean completionAllowSkippedChecks,
                       boolean completionRequireCheckpoint,
                       boolean specRequireNonEmptyTasks,
                       boolean specRequireNonEmptyAcceptance,
                       Map<String, GoalActionMapping> actionMappings) {
        this.profileKey = profileKey;
        this.workflowKey = workflowKey;
        this.specRequired = specRequired;
        this.defaultMode = defaultMode;
        this.actions = actions == null ? new String[0] : actions;
        this.requiredEvidenceByAction = copyEvidence(requiredEvidenceByAction);
        this.requiredChecks = requiredChecks == null ? new String[0] : requiredChecks;
        this.completionRequireFreshChecks = completionRequireFreshChecks;
        this.completionAllowSkippedChecks = completionAllowSkippedChecks;
        this.completionRequireCheckpoint = completionRequireCheckpoint;
        this.specRequireNonEmptyTasks = specRequireNonEmptyTasks;
        this.specRequireNonEmptyAcceptance = specRequireNonEmptyAcceptance;
        this.actionMappings = copyMappings(actionMappings);
    }

    public String profileKey() { return profileKey; }
    public String workflowKey() { return workflowKey; }
    public boolean specRequired() { return specRequired; }
    public String defaultMode() { return defaultMode; }
    public String[] actions() { return actions; }
    public String[] requiredChecks() { return requiredChecks; }
    public boolean completionRequireFreshChecks() { return completionRequireFreshChecks; }
    public boolean completionAllowSkippedChecks() { return completionAllowSkippedChecks; }
    public boolean completionRequireCheckpoint() { return completionRequireCheckpoint; }
    public boolean specRequireNonEmptyTasks() { return specRequireNonEmptyTasks; }
    public boolean specRequireNonEmptyAcceptance() { return specRequireNonEmptyAcceptance; }

    public String[] requiredEvidence(String actionKey) {
        String[] evidence = requiredEvidenceByAction.get(actionKey);
        return evidence == null ? new String[0] : evidence;
    }

    public Map<String, String[]> requiredEvidenceByAction() {
        return requiredEvidenceByAction;
    }

    public GoalActionMapping actionMapping(String actionKey) {
        return actionMappings.get(actionKey);
    }

    public Map<String, GoalActionMapping> actionMappings() {
        return actionMappings;
    }

    private Map<String, String[]> copyEvidence(Map<String, String[]> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String[]> result = new LinkedHashMap<String, String[]>();
        for (Map.Entry<String, String[]> entry : source.entrySet()) {
            result.put(entry.getKey(), entry.getValue() == null ? new String[0] : entry.getValue());
        }
        return Collections.unmodifiableMap(result);
    }

    private Map<String, GoalActionMapping> copyMappings(Map<String, GoalActionMapping> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<String, GoalActionMapping>(source));
    }
}
