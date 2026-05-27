package com.devharnesskit.dhk.model.bdd;

public final class BddEvidence {
    private final long id;
    private final String scenarioKey;
    private final String goalKey;
    private final String evidenceType;
    private final String status;
    private final String evidencePath;
    private final String summary;
    private final String command;
    private final String createdAt;
    private final String updatedAt;

    public BddEvidence(long id, String scenarioKey, String goalKey, String evidenceType,
                       String status, String evidencePath, String summary, String command,
                       String createdAt, String updatedAt) {
        this.id = id;
        this.scenarioKey = value(scenarioKey);
        this.goalKey = value(goalKey);
        this.evidenceType = value(evidenceType);
        this.status = value(status);
        this.evidencePath = value(evidencePath);
        this.summary = value(summary);
        this.command = value(command);
        this.createdAt = value(createdAt);
        this.updatedAt = value(updatedAt);
    }

    public long id() {
        return id;
    }

    public String scenarioKey() {
        return scenarioKey;
    }

    public String goalKey() {
        return goalKey;
    }

    public String evidenceType() {
        return evidenceType;
    }

    public String status() {
        return status;
    }

    public String evidencePath() {
        return evidencePath;
    }

    public String summary() {
        return summary;
    }

    public String command() {
        return command;
    }

    public String createdAt() {
        return createdAt;
    }

    public String updatedAt() {
        return updatedAt;
    }

    private String value(String raw) {
        return raw == null ? "" : raw;
    }
}
