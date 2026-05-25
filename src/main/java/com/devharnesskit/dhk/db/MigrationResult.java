package com.devharnesskit.dhk.db;

public final class MigrationResult {
    private final int schemaVersion;
    private final boolean ftsAvailable;
    private final String ftsError;
    private final String backupPath;

    public MigrationResult(int schemaVersion, boolean ftsAvailable, String ftsError) {
        this(schemaVersion, ftsAvailable, ftsError, "");
    }

    public MigrationResult(int schemaVersion, boolean ftsAvailable, String ftsError, String backupPath) {
        this.schemaVersion = schemaVersion;
        this.ftsAvailable = ftsAvailable;
        this.ftsError = ftsError == null ? "" : ftsError;
        this.backupPath = backupPath == null ? "" : backupPath;
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

    public String backupPath() {
        return backupPath;
    }
}
