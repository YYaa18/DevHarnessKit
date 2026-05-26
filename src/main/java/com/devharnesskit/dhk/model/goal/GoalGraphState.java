package com.devharnesskit.dhk.model.goal;

public final class GoalGraphState {
    private final boolean enabled;
    private final String provider;
    private final boolean requireFreshSnapshot;
    private final boolean requireImpactMap;
    private final int maxStalenessMinutes;
    private final String snapshotPath;
    private final boolean snapshotExists;
    private final String snapshotKey;
    private final String snapshotWorkspaceFingerprint;
    private final String currentWorkspaceFingerprint;
    private final boolean snapshotStale;
    private final String freshnessStatus;
    private final String graphContextPath;
    private final boolean graphContextExists;
    private final String impactMapPath;
    private final boolean impactMapExists;
    private final String requiredGraphAction;
    private final String graphNextCommand;
    private final String[] protectedImpactFiles;

    public GoalGraphState(boolean enabled, String provider, boolean requireFreshSnapshot,
                          boolean requireImpactMap, int maxStalenessMinutes,
                          String snapshotPath, boolean snapshotExists, String snapshotKey,
                          String graphContextPath, boolean graphContextExists,
                          String impactMapPath, boolean impactMapExists,
                          String requiredGraphAction, String graphNextCommand) {
        this(enabled, provider, requireFreshSnapshot, requireImpactMap, maxStalenessMinutes,
                snapshotPath, snapshotExists, snapshotKey, graphContextPath, graphContextExists,
                impactMapPath, impactMapExists, requiredGraphAction, graphNextCommand, new String[0]);
    }

    public GoalGraphState(boolean enabled, String provider, boolean requireFreshSnapshot,
                          boolean requireImpactMap, int maxStalenessMinutes,
                          String snapshotPath, boolean snapshotExists, String snapshotKey,
                          String graphContextPath, boolean graphContextExists,
                          String impactMapPath, boolean impactMapExists,
                          String requiredGraphAction, String graphNextCommand,
                          String[] protectedImpactFiles) {
        this(enabled, provider, requireFreshSnapshot, requireImpactMap, maxStalenessMinutes,
                snapshotPath, snapshotExists, snapshotKey, "", "", false, "",
                graphContextPath, graphContextExists, impactMapPath, impactMapExists,
                requiredGraphAction, graphNextCommand, protectedImpactFiles);
    }

    public GoalGraphState(boolean enabled, String provider, boolean requireFreshSnapshot,
                          boolean requireImpactMap, int maxStalenessMinutes,
                          String snapshotPath, boolean snapshotExists, String snapshotKey,
                          String snapshotWorkspaceFingerprint, String currentWorkspaceFingerprint,
                          boolean snapshotStale, String freshnessStatus,
                          String graphContextPath, boolean graphContextExists,
                          String impactMapPath, boolean impactMapExists,
                          String requiredGraphAction, String graphNextCommand,
                          String[] protectedImpactFiles) {
        this.enabled = enabled;
        this.provider = provider == null ? "" : provider;
        this.requireFreshSnapshot = requireFreshSnapshot;
        this.requireImpactMap = requireImpactMap;
        this.maxStalenessMinutes = maxStalenessMinutes;
        this.snapshotPath = snapshotPath == null ? "" : snapshotPath;
        this.snapshotExists = snapshotExists;
        this.snapshotKey = snapshotKey == null ? "" : snapshotKey;
        this.snapshotWorkspaceFingerprint = snapshotWorkspaceFingerprint == null ? "" : snapshotWorkspaceFingerprint;
        this.currentWorkspaceFingerprint = currentWorkspaceFingerprint == null ? "" : currentWorkspaceFingerprint;
        this.snapshotStale = snapshotStale;
        this.freshnessStatus = freshnessStatus == null ? "" : freshnessStatus;
        this.graphContextPath = graphContextPath == null ? "" : graphContextPath;
        this.graphContextExists = graphContextExists;
        this.impactMapPath = impactMapPath == null ? "" : impactMapPath;
        this.impactMapExists = impactMapExists;
        this.requiredGraphAction = requiredGraphAction == null ? "" : requiredGraphAction;
        this.graphNextCommand = graphNextCommand == null ? "" : graphNextCommand;
        this.protectedImpactFiles = protectedImpactFiles == null ? new String[0] : protectedImpactFiles;
    }

    public static GoalGraphState disabled() {
        return new GoalGraphState(false, "", false, false, 0,
                "", false, "", "", false, "", false, "", "");
    }

    public boolean enabled() { return enabled; }
    public String provider() { return provider; }
    public boolean requireFreshSnapshot() { return requireFreshSnapshot; }
    public boolean requireImpactMap() { return requireImpactMap; }
    public int maxStalenessMinutes() { return maxStalenessMinutes; }
    public String snapshotPath() { return snapshotPath; }
    public boolean snapshotExists() { return snapshotExists; }
    public String snapshotKey() { return snapshotKey; }
    public String snapshotWorkspaceFingerprint() { return snapshotWorkspaceFingerprint; }
    public String currentWorkspaceFingerprint() { return currentWorkspaceFingerprint; }
    public boolean snapshotStale() { return snapshotStale; }
    public String freshnessStatus() { return freshnessStatus; }
    public String graphContextPath() { return graphContextPath; }
    public boolean graphContextExists() { return graphContextExists; }
    public String impactMapPath() { return impactMapPath; }
    public boolean impactMapExists() { return impactMapExists; }
    public String requiredGraphAction() { return requiredGraphAction; }
    public String graphNextCommand() { return graphNextCommand; }
    public String[] protectedImpactFiles() { return protectedImpactFiles; }
}
