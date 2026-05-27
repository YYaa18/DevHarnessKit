package com.devharnesskit.dhk.db.migration;

import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.util.Clock;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class V12HumanCheckpointMigration extends AbstractMigrationStep {
    public V12HumanCheckpointMigration() {
        super(MigrationRunner.V12, "V0.4 human checkpoint schema");
    }

    public void apply(Connection connection, Clock clock) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS human_checkpoint ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "goal_key TEXT NOT NULL,"
                    + "checkpoint_type TEXT NOT NULL DEFAULT 'before_complete',"
                    + "reason TEXT NOT NULL DEFAULT '',"
                    + "status TEXT NOT NULL DEFAULT 'pending',"
                    + "requested_by TEXT NOT NULL DEFAULT 'manual',"
                    + "requested_at TEXT NOT NULL,"
                    + "approver TEXT NOT NULL DEFAULT '',"
                    + "approved_at TEXT NOT NULL DEFAULT '',"
                    + "decision_reason TEXT NOT NULL DEFAULT '',"
                    + "created_at TEXT NOT NULL,"
                    + "updated_at TEXT NOT NULL,"
                    + "CHECK (status IN ('pending', 'approved', 'rejected', 'canceled')),"
                    + "FOREIGN KEY (goal_key) REFERENCES goal_run(goal_key)"
                    + ")");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_human_checkpoint_goal_status "
                    + "ON human_checkpoint(goal_key, status)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_human_checkpoint_goal_type_status "
                    + "ON human_checkpoint(goal_key, checkpoint_type, status)");
            if (!MigrationSupport.schemaVersionExists(connection, MigrationRunner.V12)) {
                statement.executeUpdate("INSERT INTO schema_version(version, description, applied_at) VALUES ("
                        + MigrationRunner.V12 + ", 'V0.7.4 human checkpoint schema', '"
                        + clock.now().toString() + "')");
            }
        }
   
    }
}
