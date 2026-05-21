package com.devharnesskit.dhk.model;

public final class Project {
    private final String projectKey;
    private final String projectName;
    private final String rootPath;
    private final String projectType;
    private final String language;
    private final String framework;
    private final String databaseType;
    private final String createdAt;
    private final String updatedAt;

    public Project(String projectKey, String projectName, String rootPath, String projectType,
                   String language, String framework, String databaseType, String createdAt, String updatedAt) {
        this.projectKey = projectKey;
        this.projectName = projectName;
        this.rootPath = rootPath;
        this.projectType = projectType;
        this.language = language;
        this.framework = framework;
        this.databaseType = databaseType;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String projectKey() {
        return projectKey;
    }

    public String projectName() {
        return projectName;
    }

    public String rootPath() {
        return rootPath;
    }

    public String projectType() {
        return projectType;
    }

    public String language() {
        return language;
    }

    public String framework() {
        return framework;
    }

    public String databaseType() {
        return databaseType;
    }

    public String createdAt() {
        return createdAt;
    }

    public String updatedAt() {
        return updatedAt;
    }
}
