package com.devharnesskit.dhk.db.migration;

import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.util.Clock;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class V8GoalWorkflowSpecSyncMigration extends AbstractMigrationStep {
    public V8GoalWorkflowSpecSyncMigration() {
        super(MigrationRunner.V8, "V0.4 goal workflow/spec sync schema");
    }

    public void apply(Connection connection, Clock clock) throws SQLException {
        MigrationSupport.addColumnIfMissing(connection, "goal_check", "workspace_fingerprint", "TEXT NOT NULL DEFAULT ''");
        MigrationSupport.addColumnIfMissing(connection, "goal_check", "context_fingerprint", "TEXT NOT NULL DEFAULT ''");
        MigrationSupport.addColumnIfMissing(connection, "goal_check", "check_fingerprint", "TEXT NOT NULL DEFAULT ''");
        try (Statement statement = connection.createStatement()) {
            if (!MigrationSupport.schemaVersionExists(connection, MigrationRunner.V8)) {
                statement.executeUpdate("INSERT INTO schema_version(version, description, applied_at) VALUES ("
                        + MigrationRunner.V8 + ", 'V0.4.3 goal workspace fingerprint freshness schema', '"
                        + clock.now().toString() + "')");
            }
        }
   
    }
}
