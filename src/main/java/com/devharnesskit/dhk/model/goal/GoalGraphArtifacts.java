package com.devharnesskit.dhk.model.goal;

public final class GoalGraphArtifacts {
    private final boolean enabled;
    private final long snapshotId;
    private final String snapshotKey;
    private final String provider;
    private final int fileCount;
    private final int nodeCount;
    private final int edgeCount;
    private final String graphSnapshotPath;
    private final String graphContextPath;
    private final String impactMapPath;
    private final String graphSnapshotHash;
    private final String graphContextHash;
    private final String impactMapHash;

    public GoalGraphArtifacts(boolean enabled, long snapshotId, String snapshotKey, String provider,
                              int fileCount, int nodeCount, int edgeCount,
                              String graphSnapshotPath, String graphContextPath, String impactMapPath,
                              String graphSnapshotHash, String graphContextHash, String impactMapHash) {
        this.enabled = enabled;
        this.snapshotId = snapshotId;
        this.snapshotKey = value(snapshotKey);
        this.provider = value(provider);
        this.fileCount = fileCount;
        this.nodeCount = nodeCount;
        this.edgeCount = edgeCount;
        this.graphSnapshotPath = value(graphSnapshotPath);
        this.graphContextPath = value(graphContextPath);
        this.impactMapPath = value(impactMapPath);
        this.graphSnapshotHash = value(graphSnapshotHash);
        this.graphContextHash = value(graphContextHash);
        this.impactMapHash = value(impactMapHash);
    }

    public static GoalGraphArtifacts none() {
        return new GoalGraphArtifacts(false, 0L, "", "", 0, 0, 0,
                "", "", "", "", "", "");
    }

    public boolean enabled() { return enabled; }
    public long snapshotId() { return snapshotId; }
    public String snapshotKey() { return snapshotKey; }
    public String provider() { return provider; }
    public int fileCount() { return fileCount; }
    public int nodeCount() { return nodeCount; }
    public int edgeCount() { return edgeCount; }
    public String graphSnapshotPath() { return graphSnapshotPath; }
    public String graphContextPath() { return graphContextPath; }
    public String impactMapPath() { return impactMapPath; }
    public String graphSnapshotHash() { return graphSnapshotHash; }
    public String graphContextHash() { return graphContextHash; }
    public String impactMapHash() { return impactMapHash; }

    private static String value(String raw) {
        return raw == null ? "" : raw;
    }
}
