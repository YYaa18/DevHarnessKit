package com.devharnesskit.dhk.db.migration;

import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.util.Clock;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class V5GoalOrchestrationMigration extends AbstractMigrationStep {
    public V5GoalOrchestrationMigration() {
        super(MigrationRunner.V5, "V0.4 goal orchestration schema");
    }

    public void apply(Connection connection, Clock clock) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS goal_run ("
                    + "goal_key TEXT PRIMARY KEY,"
                    + "project_key TEXT NOT NULL,"
                    + "workflow_run_key TEXT NOT NULL DEFAULT '',"
                    + "spec_change_key TEXT NOT NULL DEFAULT '',"
                    + "profile_key TEXT NOT NULL,"
                    + "task_name TEXT NOT NULL,"
                    + "module_name TEXT NOT NULL DEFAULT 'global',"
                    + "mode TEXT NOT NULL DEFAULT 'auto',"
                    + "condition_text TEXT NOT NULL DEFAULT '',"
                    + "status TEXT NOT NULL DEFAULT 'created',"
                    + "current_action TEXT NOT NULL DEFAULT '',"
                    + "max_steps INTEGER NOT NULL DEFAULT 30,"
                    + "step_count INTEGER NOT NULL DEFAULT 0,"
                    + "created_at TEXT NOT NULL,"
                    + "updated_at TEXT NOT NULL,"
                    + "completed_at TEXT NOT NULL DEFAULT '',"
                    + "CHECK (status IN ('created', 'initialized', 'context_exporting',"
                    + "'context_export_failed', 'context_ready', 'planning',"
                    + "'implementing', 'verifying', 'ready_to_complete', 'completed', 'blocked',"
                    + "'waiting_user', 'failed', 'abandoned')),"
                    + "FOREIGN KEY (project_key) REFERENCES project(project_key)"
                    + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS goal_step ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "goal_key TEXT NOT NULL,"
                    + "step_index INTEGER NOT NULL,"
                    + "action_key TEXT NOT NULL,"
                    + "summary TEXT NOT NULL,"
                    + "changed_files TEXT NOT NULL DEFAULT '',"
                    + "evidence TEXT NOT NULL DEFAULT '',"
                    + "status TEXT NOT NULL DEFAULT 'recorded',"
                    + "created_at TEXT NOT NULL,"
                    + "CHECK (status IN ('recorded', 'accepted', 'incomplete', 'rejected')),"
                    + "FOREIGN KEY (goal_key) REFERENCES goal_run(goal_key)"
                    + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS goal_event ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "goal_key TEXT NOT NULL,"
                    + "event_type TEXT NOT NULL,"
                    + "level TEXT NOT NULL DEFAULT 'info',"
                    + "message TEXT NOT NULL,"
                    + "data TEXT NOT NULL DEFAULT '',"
                    + "created_at TEXT NOT NULL,"
                    + "CHECK (level IN ('debug', 'info', 'warn', 'error')),"
                    + "FOREIGN KEY (goal_key) REFERENCES goal_run(goal_key)"
                    + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS goal_check ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "goal_key TEXT NOT NULL,"
                    + "check_key TEXT NOT NULL,"
                    + "check_type TEXT NOT NULL,"
                    + "required INTEGER NOT NULL DEFAULT 1,"
                    + "command TEXT NOT NULL DEFAULT '',"
                    + "status TEXT NOT NULL DEFAULT 'pending',"
                    + "result_summary TEXT NOT NULL DEFAULT '',"
                    + "evidence_path TEXT NOT NULL DEFAULT '',"
                    + "checked_at TEXT NOT NULL DEFAULT '',"
                    + "created_at TEXT NOT NULL,"
                    + "updated_at TEXT NOT NULL,"
                    + "UNIQUE(goal_key, check_key),"
                    + "CHECK (status IN ('pending', 'running', 'passed', 'failed', 'waived', 'skipped')),"
                    + "FOREIGN KEY (goal_key) REFERENCES goal_run(goal_key)"
                    + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS goal_artifact ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "goal_key TEXT NOT NULL,"
                    + "artifact_type TEXT NOT NULL,"
                    + "title TEXT NOT NULL,"
                    + "file_path TEXT NOT NULL DEFAULT '',"
                    + "content_hash TEXT NOT NULL DEFAULT '',"
                    + "summary TEXT NOT NULL DEFAULT '',"
                    + "created_at TEXT NOT NULL,"
                    + "FOREIGN KEY (goal_key) REFERENCES goal_run(goal_key)"
                    + ")");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_goal_run_project_status "
                    + "ON goal_run(project_key, status)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_goal_step_goal_index "
                    + "ON goal_step(goal_key, step_index)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_goal_event_goal_created "
                    + "ON goal_event(goal_key, created_at)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_goal_check_goal_status "
                    + "ON goal_check(goal_key, status)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_goal_artifact_goal_type "
                    + "ON goal_artifact(goal_key, artifact_type)");
            if (!MigrationSupport.schemaVersionExists(connection, MigrationRunner.V5)) {
                statement.executeUpdate("INSERT INTO schema_version(version, description, applied_at) VALUES ("
                        + MigrationRunner.V5 + ", 'V0.4 goal orchestration MVP schema', '" + clock.now().toString() + "')");
            }
        }
   
    }
}
