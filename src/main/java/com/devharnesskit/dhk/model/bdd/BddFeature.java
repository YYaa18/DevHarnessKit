package com.devharnesskit.dhk.model.bdd;

public final class BddFeature {
    private final String featureKey;
    private final String projectKey;
    private final String moduleName;
    private final String title;
    private final String description;
    private final String tags;
    private final String status;
    private final String sourceKind;
    private final String createdAt;
    private final String updatedAt;

    public BddFeature(String featureKey, String projectKey, String moduleName, String title,
                      String description, String tags, String status, String sourceKind,
                      String createdAt, String updatedAt) {
        this.featureKey = featureKey;
        this.projectKey = projectKey;
        this.moduleName = moduleName;
        this.title = title;
        this.description = description;
        this.tags = tags;
        this.status = status;
        this.sourceKind = sourceKind;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String featureKey() {
        return featureKey;
    }

    public String projectKey() {
        return projectKey;
    }

    public String moduleName() {
        return moduleName;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public String tags() {
        return tags;
    }

    public String status() {
        return status;
    }

    public String sourceKind() {
        return sourceKind;
    }

    public String createdAt() {
        return createdAt;
    }

    public String updatedAt() {
        return updatedAt;
    }
}
