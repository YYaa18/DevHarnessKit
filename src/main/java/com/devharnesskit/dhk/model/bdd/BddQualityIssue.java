package com.devharnesskit.dhk.model.bdd;

public final class BddQualityIssue {
    private final long id;
    private final String projectKey;
    private final String featureKey;
    private final String scenarioKey;
    private final String issueType;
    private final String severity;
    private final String status;
    private final String message;
    private final String createdAt;

    public BddQualityIssue(long id, String projectKey, String featureKey, String scenarioKey,
                           String issueType, String severity, String status, String message,
                           String createdAt) {
        this.id = id;
        this.projectKey = projectKey;
        this.featureKey = featureKey;
        this.scenarioKey = scenarioKey;
        this.issueType = issueType;
        this.severity = severity;
        this.status = status;
        this.message = message;
        this.createdAt = createdAt;
    }

    public long id() {
        return id;
    }

    public String projectKey() {
        return projectKey;
    }

    public String featureKey() {
        return featureKey;
    }

    public String scenarioKey() {
        return scenarioKey;
    }

    public String issueType() {
        return issueType;
    }

    public String severity() {
        return severity;
    }

    public String status() {
        return status;
    }

    public String message() {
        return message;
    }

    public String createdAt() {
        return createdAt;
    }
}
