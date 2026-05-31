package com.devharnesskit.dhk.db.migration;

import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.util.Clock;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class V16GoalExternalRefMigration extends AbstractMigrationStep {
    public V16GoalExternalRefMigration() {
        super(MigrationRunner.V16, "V1.0 goal external reference metadata");
    }

    public void apply(Connection connection, Clock clock) throws SQLException {
        if (MigrationSupport.hasTable(connection, "goal_run")) {
            MigrationSupport.addColumnIfMissing(connection, "goal_run",
                    "external_ref", "TEXT NOT NULL DEFAULT ''");
        }
        try (Statement statement = connection.createStatement()) {
            if (!MigrationSupport.schemaVersionExists(connection, MigrationRunner.V16)) {
                statement.executeUpdate("INSERT INTO schema_version(version, description, applied_at) VALUES ("
                        + MigrationRunner.V16 + ", 'V1.0 goal external reference metadata', '"
                        + clock.now().toString() + "')");
            }
        }
    }
}
