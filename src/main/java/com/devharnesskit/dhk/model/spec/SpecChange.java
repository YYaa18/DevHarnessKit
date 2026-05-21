package com.devharnesskit.dhk.model.spec;

public final class SpecChange {
    private final String changeKey;
    private final String projectKey;
    private final String title;
    private final String summary;
    private final String moduleName;
    private final String mode;
    private final String status;
    private final String priority;
    private final String sourceKind;
    private final String createdBy;
    private final String createdAt;
    private final String updatedAt;
    private final String archivedAt;

    public SpecChange(String changeKey, String projectKey, String title, String summary,
                      String moduleName, String mode, String status, String priority,
                      String sourceKind, String createdBy, String createdAt,
                      String updatedAt, String archivedAt) {
        this.changeKey = changeKey;
        this.projectKey = projectKey;
        this.title = title;
        this.summary = summary;
        this.moduleName = moduleName;
        this.mode = mode;
        this.status = status;
        this.priority = priority;
        this.sourceKind = sourceKind;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.archivedAt = archivedAt;
    }

    public String changeKey() {
        return changeKey;
    }

    public String projectKey() {
        return projectKey;
    }

    public String title() {
        return title;
    }

    public String summary() {
        return summary;
    }

    public String moduleName() {
        return moduleName;
    }

    public String mode() {
        return mode;
    }

    public String status() {
        return status;
    }

    public String priority() {
        return priority;
    }

    public String sourceKind() {
        return sourceKind;
    }

    public String createdBy() {
        return createdBy;
    }

    public String createdAt() {
        return createdAt;
    }

    public String updatedAt() {
        return updatedAt;
    }

    public String archivedAt() {
        return archivedAt;
    }
}
