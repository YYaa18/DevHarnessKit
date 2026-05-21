package com.devharnesskit.dhk.model.workflow;

public final class WorkflowArtifact {
    private final long id;
    private final String projectKey;
    private final String runKey;
    private final String artifactType;
    private final String title;
    private final String filePath;
    private final String contentHash;
    private final String status;
    private final String producedByPhase;
    private final String summary;
    private final String tags;
    private final String createdAt;
    private final String updatedAt;

    public WorkflowArtifact(long id, String projectKey, String runKey, String artifactType, String title,
                            String filePath, String contentHash, String status, String producedByPhase,
                            String summary, String tags, String createdAt, String updatedAt) {
        this.id = id;
        this.projectKey = value(projectKey);
        this.runKey = value(runKey);
        this.artifactType = value(artifactType);
        this.title = value(title);
        this.filePath = value(filePath);
        this.contentHash = value(contentHash);
        this.status = value(status);
        this.producedByPhase = value(producedByPhase);
        this.summary = value(summary);
        this.tags = value(tags);
        this.createdAt = value(createdAt);
        this.updatedAt = value(updatedAt);
    }

    public long id() { return id; }
    public String projectKey() { return projectKey; }
    public String runKey() { return runKey; }
    public String artifactType() { return artifactType; }
    public String title() { return title; }
    public String filePath() { return filePath; }
    public String contentHash() { return contentHash; }
    public String status() { return status; }
    public String producedByPhase() { return producedByPhase; }
    public String summary() { return summary; }
    public String tags() { return tags; }
    public String createdAt() { return createdAt; }
    public String updatedAt() { return updatedAt; }

    private String value(String raw) {
        return raw == null ? "" : raw;
    }
}
