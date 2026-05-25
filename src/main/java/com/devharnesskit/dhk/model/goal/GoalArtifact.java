package com.devharnesskit.dhk.model.goal;

public final class GoalArtifact {
    private final long id;
    private final String goalKey;
    private final String artifactType;
    private final String title;
    private final String filePath;
    private final String contentHash;
    private final String summary;
    private final String createdAt;

    public GoalArtifact(long id, String goalKey, String artifactType, String title,
                        String filePath, String contentHash, String summary, String createdAt) {
        this.id = id;
        this.goalKey = value(goalKey);
        this.artifactType = value(artifactType);
        this.title = value(title);
        this.filePath = value(filePath);
        this.contentHash = value(contentHash);
        this.summary = value(summary);
        this.createdAt = value(createdAt);
    }

    public long id() { return id; }
    public String goalKey() { return goalKey; }
    public String artifactType() { return artifactType; }
    public String title() { return title; }
    public String filePath() { return filePath; }
    public String contentHash() { return contentHash; }
    public String summary() { return summary; }
    public String createdAt() { return createdAt; }

    private String value(String raw) {
        return raw == null ? "" : raw;
    }
}
