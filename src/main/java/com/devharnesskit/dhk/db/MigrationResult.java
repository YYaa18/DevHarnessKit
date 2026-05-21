package com.devharnesskit.dhk.db;

public final class MigrationResult {
    private final int schemaVersion;
    private final boolean ftsAvailable;
    private final String ftsError;

    public MigrationResult(int schemaVersion, boolean ftsAvailable, String ftsError) {
        this.schemaVersion = schemaVersion;
        this.ftsAvailable = ftsAvailable;
        this.ftsError = ftsError == null ? "" : ftsError;
    }

    public int schemaVersion() {
        return schemaVersion;
    }

    public boolean ftsAvailable() {
        return ftsAvailable;
    }

    public String ftsError() {
        return ftsError;
    }
}
