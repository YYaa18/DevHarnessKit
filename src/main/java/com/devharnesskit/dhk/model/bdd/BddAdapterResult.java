package com.devharnesskit.dhk.model.bdd;

public final class BddAdapterResult {
    private final String adapterKey;
    private final long evidenceId;
    private final String evidenceType;
    private final String sourceStatus;
    private final String normalizedStatus;
    private final boolean covered;
    private final String summary;
    private final String evidencePath;
    private final String command;
    private final String message;

    public BddAdapterResult(String adapterKey, long evidenceId, String evidenceType,
                            String sourceStatus, String normalizedStatus, boolean covered,
                            String summary, String evidencePath, String command, String message) {
        this.adapterKey = value(adapterKey);
        this.evidenceId = evidenceId;
        this.evidenceType = value(evidenceType);
        this.sourceStatus = value(sourceStatus);
        this.normalizedStatus = value(normalizedStatus);
        this.covered = covered;
        this.summary = value(summary);
        this.evidencePath = value(evidencePath);
        this.command = value(command);
        this.message = value(message);
    }

    public String adapterKey() {
        return adapterKey;
    }

    public long evidenceId() {
        return evidenceId;
    }

    public String evidenceType() {
        return evidenceType;
    }

    public String sourceStatus() {
        return sourceStatus;
    }

    public String normalizedStatus() {
        return normalizedStatus;
    }

    public boolean covered() {
        return covered;
    }

    public String summary() {
        return summary;
    }

    public String evidencePath() {
        return evidencePath;
    }

    public String command() {
        return command;
    }

    public String message() {
        return message;
    }

    private String value(String raw) {
        return raw == null ? "" : raw;
    }
}
