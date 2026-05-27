package com.devharnesskit.dhk.model.bdd;

public final class BddScenario {
    private final String scenarioKey;
    private final String featureKey;
    private final String projectKey;
    private final String title;
    private final String description;
    private final String scenarioType;
    private final String priority;
    private final String status;
    private final String tags;
    private final String createdAt;
    private final String updatedAt;

    public BddScenario(String scenarioKey, String featureKey, String projectKey, String title,
                       String description, String scenarioType, String priority, String status,
                       String tags, String createdAt, String updatedAt) {
        this.scenarioKey = scenarioKey;
        this.featureKey = featureKey;
        this.projectKey = projectKey;
        this.title = title;
        this.description = description;
        this.scenarioType = scenarioType;
        this.priority = priority;
        this.status = status;
        this.tags = tags;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String scenarioKey() {
        return scenarioKey;
    }

    public String featureKey() {
        return featureKey;
    }

    public String projectKey() {
        return projectKey;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public String scenarioType() {
        return scenarioType;
    }

    public String priority() {
        return priority;
    }

    public String status() {
        return status;
    }

    public String tags() {
        return tags;
    }

    public String createdAt() {
        return createdAt;
    }

    public String updatedAt() {
        return updatedAt;
    }
}
