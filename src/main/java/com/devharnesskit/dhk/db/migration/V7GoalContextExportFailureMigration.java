package com.devharnesskit.dhk.db.migration;

import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.util.Clock;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class V7GoalContextExportFailureMigration extends AbstractMigrationStep {
    public V7GoalContextExportFailureMigration() {
        super(MigrationRunner.V7, "V0.4 goal context export failure schema");
    }

    public void apply(Connection connection, Clock clock) throws SQLException {
        if (MigrationSupport.hasTable(connection, "goal_run") && !MigrationSupport.goalRunAllowsContextExportFailure(connection)) {
            MigrationSupport.rebuildGoalRunForV7(connection);
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE INDEX IF NOT EXISTS idx_goal_run_project_status "
                    + "ON goal_run(project_key, status)");
            if (!MigrationSupport.schemaVersionExists(connection, MigrationRunner.V7)) {
                statement.executeUpdate("INSERT INTO schema_version(version, description, applied_at) VALUES ("
                        + MigrationRunner.V7 + ", 'V0.4.1 goal context export recovery schema', '" + clock.now().toString() + "')");
            }
        }
   
    }
}
