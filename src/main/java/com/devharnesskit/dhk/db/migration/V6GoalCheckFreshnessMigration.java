package com.devharnesskit.dhk.db.migration;

import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.util.Clock;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class V6GoalCheckFreshnessMigration extends AbstractMigrationStep {
    public V6GoalCheckFreshnessMigration() {
        super(MigrationRunner.V6, "V0.4 goal check freshness schema");
    }

    public void apply(Connection connection, Clock clock) throws SQLException {
        MigrationSupport.addColumnIfMissing(connection, "goal_check", "step_count_at_check", "INTEGER NOT NULL DEFAULT 0");
        try (Statement statement = connection.createStatement()) {
            if (!MigrationSupport.schemaVersionExists(connection, MigrationRunner.V6)) {
                statement.executeUpdate("INSERT INTO schema_version(version, description, applied_at) VALUES ("
                        + MigrationRunner.V6 + ", 'V0.4.1 goal check freshness metadata schema', '" + clock.now().toString() + "')");
            }
        }
   
    }
}
