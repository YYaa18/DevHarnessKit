package com.devharnesskit.dhk.db.migration;

import com.devharnesskit.dhk.db.MigrationRunner;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class MigrationSupport {
    private MigrationSupport() {
    }

    public static void repairProjectColumns(Connection connection) throws SQLException {
        addColumnIfMissing(connection, "project", "root_path", "TEXT NOT NULL DEFAULT ''");
        addColumnIfMissing(connection, "project", "project_type", "TEXT NOT NULL DEFAULT 'unknown'");
        addColumnIfMissing(connection, "project", "language", "TEXT NOT NULL DEFAULT 'java'");
        addColumnIfMissing(connection, "project", "framework", "TEXT NOT NULL DEFAULT 'unknown'");
        addColumnIfMissing(connection, "project", "database_type", "TEXT NOT NULL DEFAULT 'unknown'");
        addColumnIfMissing(connection, "project", "created_at", "TEXT NOT NULL DEFAULT ''");
        addColumnIfMissing(connection, "project", "updated_at", "TEXT NOT NULL DEFAULT ''");
    }

    public static void addColumnIfMissing(Connection connection, String tableName, String columnName,
                                          String columnDefinition) throws SQLException {
        if (columnExists(connection, tableName, columnName)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + tableName + " ADD COLUMN "
                    + columnName + " " + columnDefinition);
        }
    }

    public static boolean columnExists(Connection connection, String tableName, String columnName)
            throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (resultSet.next()) {
                if (columnName.equals(resultSet.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean goalRunAllowsContextExportFailure(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = 'goal_run'")) {
            if (!resultSet.next()) {
                return false;
            }
            String sql = resultSet.getString(1);
            return sql != null && sql.indexOf("context_export_failed") >= 0;
        }
    }

    public static void rebuildGoalRunForV7(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = OFF");
            try {
                statement.execute("DROP TABLE IF EXISTS goal_run_v7");
                statement.execute("CREATE TABLE goal_run_v7 ("
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
                statement.execute("INSERT INTO goal_run_v7(goal_key, project_key, workflow_run_key, spec_change_key, "
                        + "profile_key, task_name, module_name, mode, condition_text, status, "
                        + "current_action, max_steps, step_count, created_at, updated_at, completed_at) "
                        + "SELECT goal_key, project_key, workflow_run_key, spec_change_key, "
                        + "profile_key, task_name, module_name, mode, condition_text, status, "
                        + "current_action, max_steps, step_count, created_at, updated_at, completed_at "
                        + "FROM goal_run");
                statement.execute("DROP TABLE goal_run");
                statement.execute("ALTER TABLE goal_run_v7 RENAME TO goal_run");
            } finally {
                statement.execute("PRAGMA foreign_keys = ON");
            }
        }
    }

    public static boolean schemaVersionExists(Connection connection, int version) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT version FROM schema_version WHERE version = " + version)) {
            return resultSet.next();
        }
    }

    public static boolean hasTable(Connection connection, String tableName) throws SQLException {
        return MigrationRunner.hasTable(connection, tableName);
    }
}
