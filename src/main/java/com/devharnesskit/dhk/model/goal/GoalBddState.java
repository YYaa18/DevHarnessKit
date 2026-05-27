package com.devharnesskit.dhk.model.goal;

public final class GoalBddState {
    private static final GoalBddState DISABLED = new GoalBddState(false, false,
            0, 0, 0, 0, 0, "", "", "", false, "");

    private final boolean enabled;
    private final boolean required;
    private final int scenarioCount;
    private final int coveredCount;
    private final int missingCount;
    private final int pendingCount;
    private final int failedCount;
    private final String evidencePath;
    private final String coveragePath;
    private final String scenarioImpactMapPath;
    private final boolean scenarioImpactMapExists;
    private final String nextCommand;

    public GoalBddState(boolean enabled, boolean required, int scenarioCount, int coveredCount,
                        int missingCount, int pendingCount, int failedCount,
                        String evidencePath, String coveragePath, String nextCommand) {
        this(enabled, required, scenarioCount, coveredCount, missingCount, pendingCount, failedCount,
                evidencePath, coveragePath, "", false, nextCommand);
    }

    public GoalBddState(boolean enabled, boolean required, int scenarioCount, int coveredCount,
                        int missingCount, int pendingCount, int failedCount,
                        String evidencePath, String coveragePath, String scenarioImpactMapPath,
                        boolean scenarioImpactMapExists, String nextCommand) {
        this.enabled = enabled;
        this.required = required;
        this.scenarioCount = scenarioCount;
        this.coveredCount = coveredCount;
        this.missingCount = missingCount;
        this.pendingCount = pendingCount;
        this.failedCount = failedCount;
        this.evidencePath = value(evidencePath);
        this.coveragePath = value(coveragePath);
        this.scenarioImpactMapPath = value(scenarioImpactMapPath);
        this.scenarioImpactMapExists = scenarioImpactMapExists;
        this.nextCommand = value(nextCommand);
    }

    public static GoalBddState disabled() {
        return DISABLED;
    }

    public boolean enabled() { return enabled; }
    public boolean required() { return required; }
    public int scenarioCount() { return scenarioCount; }
    public int coveredCount() { return coveredCount; }
    public int missingCount() { return missingCount; }
    public int pendingCount() { return pendingCount; }
    public int failedCount() { return failedCount; }
    public String evidencePath() { return evidencePath; }
    public String coveragePath() { return coveragePath; }
    public String scenarioImpactMapPath() { return scenarioImpactMapPath; }
    public boolean scenarioImpactMapExists() { return scenarioImpactMapExists; }
    public String nextCommand() { return nextCommand; }

    private String value(String raw) {
        return raw == null ? "" : raw;
    }
}
