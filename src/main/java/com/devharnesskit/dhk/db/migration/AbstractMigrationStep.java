package com.devharnesskit.dhk.db.migration;

abstract class AbstractMigrationStep implements MigrationStep {
    private final int version;
    private final String description;

    AbstractMigrationStep(int version, String description) {
        this.version = version;
        this.description = description;
    }

    public int version() {
        return version;
    }

    public String description() {
        return description;
    }
}
