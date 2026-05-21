package com.devharnesskit.dhk.model;

public final class Checkpoint {
    private final long id;
    private final String projectKey;
    private final String taskName;
    private final String moduleName;
    private final String summary;
    private final String changedFiles;
    private final String pendingItems;
    private final String verifyStatus;
    private final String nextReadFiles;
    private final String createdAt;

    public Checkpoint(long id, String projectKey, String taskName, String moduleName, String summary,
                      String changedFiles, String pendingItems, String verifyStatus,
                      String nextReadFiles, String createdAt) {
        this.id = id;
        this.projectKey = value(projectKey);
        this.taskName = value(taskName);
        this.moduleName = value(moduleName);
        this.summary = value(summary);
        this.changedFiles = value(changedFiles);
        this.pendingItems = value(pendingItems);
        this.verifyStatus = value(verifyStatus);
        this.nextReadFiles = value(nextReadFiles);
        this.createdAt = value(createdAt);
    }

    public long id() {
        return id;
    }

    public String projectKey() {
        return projectKey;
    }

    public String taskName() {
        return taskName;
    }

    public String moduleName() {
        return moduleName;
    }

    public String summary() {
        return summary;
    }

    public String changedFiles() {
        return changedFiles;
    }

    public String pendingItems() {
        return pendingItems;
    }

    public String verifyStatus() {
        return verifyStatus;
    }

    public String nextReadFiles() {
        return nextReadFiles;
    }

    public String createdAt() {
        return createdAt;
    }

    private String value(String raw) {
        return raw == null ? "" : raw;
    }
}
