package com.devharnesskit.dhk.model.graph;

public final class GraphSnapshot {
    private final long id;
    private final String projectKey;
    private final String snapshotKey;
    private final String provider;
    private final String configHash;
    private final String workspaceFingerprint;
    private final String status;
    private final int fileCount;
    private final int nodeCount;
    private final int edgeCount;
    private final int skippedFileCount;
    private final int maxFileBytes;
    private final int maxIndexedFiles;
    private final String summary;
    private final String createdAt;
    private final String completedAt;

    public GraphSnapshot(long id, String projectKey, String snapshotKey, String provider,
                         String configHash, String workspaceFingerprint, String status,
                         int fileCount, int nodeCount, int edgeCount, int skippedFileCount,
                         int maxFileBytes, int maxIndexedFiles, String summary,
                         String createdAt, String completedAt) {
        this.id = id;
        this.projectKey = value(projectKey);
        this.snapshotKey = value(snapshotKey);
        this.provider = value(provider);
        this.configHash = value(configHash);
        this.workspaceFingerprint = value(workspaceFingerprint);
        this.status = value(status);
        this.fileCount = fileCount;
        this.nodeCount = nodeCount;
        this.edgeCount = edgeCount;
        this.skippedFileCount = skippedFileCount;
        this.maxFileBytes = maxFileBytes;
        this.maxIndexedFiles = maxIndexedFiles;
        this.summary = value(summary);
        this.createdAt = value(createdAt);
        this.completedAt = value(completedAt);
    }

    public long id() {
        return id;
    }

    public String projectKey() {
        return projectKey;
    }

    public String snapshotKey() {
        return snapshotKey;
    }

    public String provider() {
        return provider;
    }

    public String configHash() {
        return configHash;
    }

    public String workspaceFingerprint() {
        return workspaceFingerprint;
    }

    public String status() {
        return status;
    }

    public int fileCount() {
        return fileCount;
    }

    public int nodeCount() {
        return nodeCount;
    }

    public int edgeCount() {
        return edgeCount;
    }

    public int skippedFileCount() {
        return skippedFileCount;
    }

    public int maxFileBytes() {
        return maxFileBytes;
    }

    public int maxIndexedFiles() {
        return maxIndexedFiles;
    }

    public String summary() {
        return summary;
    }

    public String createdAt() {
        return createdAt;
    }

    public String completedAt() {
        return completedAt;
    }

    private static String value(String input) {
        return input == null ? "" : input;
    }
}
