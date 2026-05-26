package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.goal.GoalProfile;

public final class GoalCheckPolicy {
    public static final String[] DEFAULT_REQUIRED_CHECKS =
            new String[]{"compile", "test", "sensitive", "spec", "workflow"};
    private static final String[] DEFAULT_ACCEPTED_STATUSES =
            new String[]{"passed", "skipped", "waived"};
    private static final String[] PASSED_ONLY = new String[]{"passed"};

    private final String[] requiredChecks;
    private final boolean requiredChecksConfigured;
    private final String[] compileCommand;
    private final String[] testCommand;
    private final boolean failPendingHardGates;
    private final boolean failPendingHardGatesConfigured;
    private final String[] acceptedCompileStatuses;
    private final String[] acceptedTestStatuses;
    private final String[] acceptedSensitiveStatuses;
    private final String[] acceptedSpecStatuses;
    private final String[] acceptedWorkflowStatuses;
    private final String[] acceptedGraphStatuses;
    private final String[] acceptedImpactStatuses;
    private final String[] acceptedArchitectureStatuses;

    public GoalCheckPolicy(String[] requiredChecks, String[] compileCommand,
                           String[] testCommand, boolean failPendingHardGates) {
        this(requiredChecks, false, compileCommand, testCommand, failPendingHardGates, true,
                new String[0], new String[0], new String[0], new String[0], new String[0],
                new String[0], new String[0], new String[0]);
    }

    public GoalCheckPolicy(String[] requiredChecks, String[] compileCommand,
                           String[] testCommand, boolean failPendingHardGates,
                           String[] acceptedCompileStatuses, String[] acceptedTestStatuses,
                           String[] acceptedSensitiveStatuses, String[] acceptedSpecStatuses,
                           String[] acceptedWorkflowStatuses) {
        this(requiredChecks, false, compileCommand, testCommand, failPendingHardGates, true,
                acceptedCompileStatuses, acceptedTestStatuses, acceptedSensitiveStatuses,
                acceptedSpecStatuses, acceptedWorkflowStatuses, new String[0], new String[0],
                new String[0]);
    }

    public GoalCheckPolicy(String[] requiredChecks, boolean requiredChecksConfigured,
                           String[] compileCommand, String[] testCommand, boolean failPendingHardGates,
                           String[] acceptedCompileStatuses, String[] acceptedTestStatuses,
                           String[] acceptedSensitiveStatuses, String[] acceptedSpecStatuses,
                           String[] acceptedWorkflowStatuses) {
        this(requiredChecks, requiredChecksConfigured, compileCommand, testCommand, failPendingHardGates, true,
                acceptedCompileStatuses, acceptedTestStatuses, acceptedSensitiveStatuses,
                acceptedSpecStatuses, acceptedWorkflowStatuses, new String[0], new String[0], new String[0]);
    }

    public GoalCheckPolicy(String[] requiredChecks, boolean requiredChecksConfigured,
                           String[] compileCommand, String[] testCommand, boolean failPendingHardGates,
                           boolean failPendingHardGatesConfigured,
                           String[] acceptedCompileStatuses, String[] acceptedTestStatuses,
                           String[] acceptedSensitiveStatuses, String[] acceptedSpecStatuses,
                           String[] acceptedWorkflowStatuses, String[] acceptedGraphStatuses,
                           String[] acceptedImpactStatuses, String[] acceptedArchitectureStatuses) {
        this.requiredChecks = requiredChecks == null || requiredChecks.length == 0
                ? DEFAULT_REQUIRED_CHECKS
                : requiredChecks;
        this.requiredChecksConfigured = requiredChecksConfigured && requiredChecks != null && requiredChecks.length > 0;
        this.compileCommand = compileCommand == null || compileCommand.length == 0
                ? new String[]{"mvn", "-q", "-DskipTests", "compile"}
                : compileCommand;
        this.testCommand = testCommand == null || testCommand.length == 0
                ? new String[]{"mvn", "-q", "test"}
                : testCommand;
        this.failPendingHardGates = failPendingHardGates;
        this.failPendingHardGatesConfigured = failPendingHardGatesConfigured;
        this.acceptedCompileStatuses = acceptedCompileStatuses == null ? new String[0] : acceptedCompileStatuses;
        this.acceptedTestStatuses = acceptedTestStatuses == null ? new String[0] : acceptedTestStatuses;
        this.acceptedSensitiveStatuses = acceptedSensitiveStatuses == null ? new String[0] : acceptedSensitiveStatuses;
        this.acceptedSpecStatuses = acceptedSpecStatuses == null ? new String[0] : acceptedSpecStatuses;
        this.acceptedWorkflowStatuses = acceptedWorkflowStatuses == null ? new String[0] : acceptedWorkflowStatuses;
        this.acceptedGraphStatuses = acceptedGraphStatuses == null ? new String[0] : acceptedGraphStatuses;
        this.acceptedImpactStatuses = acceptedImpactStatuses == null ? new String[0] : acceptedImpactStatuses;
        this.acceptedArchitectureStatuses = acceptedArchitectureStatuses == null
                ? new String[0] : acceptedArchitectureStatuses;
    }

    public GoalCheckPolicy(String[] requiredChecks, boolean requiredChecksConfigured,
                           String[] compileCommand, String[] testCommand, boolean failPendingHardGates,
                           boolean failPendingHardGatesConfigured,
                           String[] acceptedCompileStatuses, String[] acceptedTestStatuses,
                           String[] acceptedSensitiveStatuses, String[] acceptedSpecStatuses,
                           String[] acceptedWorkflowStatuses) {
        this(requiredChecks, requiredChecksConfigured, compileCommand, testCommand, failPendingHardGates,
                failPendingHardGatesConfigured, acceptedCompileStatuses, acceptedTestStatuses,
                acceptedSensitiveStatuses, acceptedSpecStatuses, acceptedWorkflowStatuses,
                new String[0], new String[0], new String[0]);
    }

    public static GoalCheckPolicy defaults() {
        return new GoalCheckPolicy(DEFAULT_REQUIRED_CHECKS, false,
                new String[]{"mvn", "-q", "-DskipTests", "compile"},
                new String[]{"mvn", "-q", "test"}, false, false,
                new String[0], new String[0], new String[0], new String[0], new String[0],
                new String[0], new String[0], new String[0]);
    }

    public String[] requiredChecks() {
        return requiredChecks;
    }

    public String[] requiredChecks(GoalProfile profile) {
        String[] selected;
        if (requiredChecksConfigured) {
            selected = requiredChecks;
        } else if (profile != null && profile.requiredChecks().length > 0) {
            selected = profile.requiredChecks();
        } else {
            selected = requiredChecks;
        }
        return withGraphChecks(selected, profile);
    }

    public String[] compileCommand() {
        return compileCommand;
    }

    public String[] testCommand() {
        return testCommand;
    }

    public boolean failPendingHardGates() {
        return failPendingHardGates;
    }

    public boolean failPendingHardGates(GoalProfile profile) {
        if (failPendingHardGatesConfigured) {
            return failPendingHardGates;
        }
        if (isBuiltInJavaProfile(profile)) {
            return true;
        }
        return profile != null && !profile.completionAllowSkippedChecks();
    }

    public boolean accepts(String checkKey, String status, GoalProfile profile) {
        String[] accepted = acceptedStatuses(checkKey, profile);
        for (String value : accepted) {
            if (value.equals(status)) {
                return true;
            }
        }
        return false;
    }

    public String acceptedStatusesText(String checkKey, GoalProfile profile) {
        return join(acceptedStatuses(checkKey, profile));
    }

    public String[] acceptedStatuses(String checkKey, GoalProfile profile) {
        String[] configured = configuredAcceptedStatuses(checkKey);
        if (configured.length > 0) {
            return configured;
        }
        if (profile != null && !profile.completionAllowSkippedChecks()) {
            return PASSED_ONLY;
        }
        if ("graph".equals(checkKey) || "impact".equals(checkKey)) {
            return PASSED_ONLY;
        }
        if ("legacy".equals(checkKey)) {
            return PASSED_ONLY;
        }
        if (isBuiltInJavaProfile(profile)) {
            if ("compile".equals(checkKey) || "test".equals(checkKey) || "sensitive".equals(checkKey)) {
                return PASSED_ONLY;
            }
            if ("spec".equals(checkKey) && profile.specRequired()) {
                return PASSED_ONLY;
            }
        }
        return DEFAULT_ACCEPTED_STATUSES;
    }

    private String[] configuredAcceptedStatuses(String checkKey) {
        if ("compile".equals(checkKey)) {
            return acceptedCompileStatuses;
        }
        if ("test".equals(checkKey)) {
            return acceptedTestStatuses;
        }
        if ("sensitive".equals(checkKey)) {
            return acceptedSensitiveStatuses;
        }
        if ("spec".equals(checkKey)) {
            return acceptedSpecStatuses;
        }
        if ("workflow".equals(checkKey)) {
            return acceptedWorkflowStatuses;
        }
        if ("graph".equals(checkKey)) {
            return acceptedGraphStatuses;
        }
        if ("impact".equals(checkKey)) {
            return acceptedImpactStatuses;
        }
        if ("architecture".equals(checkKey)) {
            return acceptedArchitectureStatuses;
        }
        return new String[0];
    }

    private boolean isBuiltInJavaProfile(GoalProfile profile) {
        if (profile == null) {
            return false;
        }
        return "java-api-change".equals(profile.profileKey())
                || "java-mvc-change".equals(profile.profileKey())
                || "java-api-change-with-graph".equals(profile.profileKey())
                || "java-mvc-change-with-graph".equals(profile.profileKey())
                || "safe-refactor-with-graph".equals(profile.profileKey())
                || profile.legacyGraphProfile();
    }

    private String[] withGraphChecks(String[] checks, GoalProfile profile) {
        if (profile == null || !profile.graphRequired()) {
            return checks;
        }
        java.util.LinkedHashSet<String> merged = new java.util.LinkedHashSet<String>();
        for (String check : checks) {
            if (check != null && check.length() > 0) {
                merged.add(check);
            }
        }
        if (profile.graphRequireFreshSnapshot()) {
            merged.add("graph");
        }
        if (profile.graphRequireImpactMap()) {
            merged.add("impact");
        }
        return merged.toArray(new String[merged.size()]);
    }

    private String join(String[] values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(value);
        }
        return builder.toString();
    }
}
