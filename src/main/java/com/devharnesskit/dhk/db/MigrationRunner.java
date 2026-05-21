package com.devharnesskit.dhk.db;

import com.devharnesskit.dhk.util.Clock;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class MigrationRunner {
    public static final int V1 = 1;
    public static final int V2 = 2;

    public MigrationResult migrate(Connection connection, Clock clock) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS schema_version ("
                    + "version INTEGER PRIMARY KEY,"
                    + "description TEXT NOT NULL,"
                    + "applied_at TEXT NOT NULL"
                    + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS project ("
                    + "project_key TEXT PRIMARY KEY,"
                    + "project_name TEXT NOT NULL,"
                    + "root_path TEXT NOT NULL,"
                    + "project_type TEXT NOT NULL DEFAULT 'unknown',"
                    + "language TEXT NOT NULL DEFAULT 'java',"
                    + "framework TEXT NOT NULL DEFAULT 'unknown',"
                    + "database_type TEXT NOT NULL DEFAULT 'unknown',"
                    + "created_at TEXT NOT NULL,"
                    + "updated_at TEXT NOT NULL"
                    + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS memory_item ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "project_key TEXT NOT NULL,"
                    + "module_name TEXT NOT NULL DEFAULT 'global',"
                    + "memory_type TEXT NOT NULL,"
                    + "scope TEXT NOT NULL DEFAULT 'project',"
                    + "title TEXT NOT NULL,"
                    + "content TEXT NOT NULL,"
                    + "tags TEXT NOT NULL DEFAULT '',"
                    + "status TEXT NOT NULL DEFAULT 'draft',"
                    + "confidence INTEGER NOT NULL DEFAULT 50,"
                    + "source_kind TEXT NOT NULL DEFAULT 'manual',"
                    + "confirmed_at TEXT NOT NULL DEFAULT '',"
                    + "confirmed_by TEXT NOT NULL DEFAULT '',"
                    + "source_files TEXT NOT NULL DEFAULT '',"
                    + "evidence TEXT NOT NULL DEFAULT '',"
                    + "effective_from TEXT NOT NULL DEFAULT '',"
                    + "effective_to TEXT NOT NULL DEFAULT '',"
                    + "created_at TEXT NOT NULL,"
                    + "updated_at TEXT NOT NULL,"
                    + "last_used_at TEXT,"
                    + "use_count INTEGER NOT NULL DEFAULT 0,"
                    + "CHECK (status IN ('draft', 'confirmed', 'deprecated', 'archived')),"
                    + "CHECK (confidence >= 0 AND confidence <= 100),"
                    + "CHECK (memory_type IN ("
                    + "'project_fact','api_convention','mvc_convention','gateway_convention',"
                    + "'database_convention','code_pattern','module_pattern','exception_convention',"
                    + "'logging_convention','security_convention','testing_convention','decision','risk','todo'"
                    + ")),"
                    + "FOREIGN KEY (project_key) REFERENCES project(project_key)"
                    + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS checkpoint ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "project_key TEXT NOT NULL,"
                    + "task_name TEXT NOT NULL,"
                    + "module_name TEXT NOT NULL DEFAULT 'global',"
                    + "summary TEXT NOT NULL,"
                    + "changed_files TEXT NOT NULL DEFAULT '',"
                    + "pending_items TEXT NOT NULL DEFAULT '',"
                    + "verify_status TEXT NOT NULL DEFAULT '',"
                    + "next_read_files TEXT NOT NULL DEFAULT '',"
                    + "created_at TEXT NOT NULL,"
                    + "CHECK (length(task_name) > 0),"
                    + "CHECK (length(summary) > 0),"
                    + "FOREIGN KEY (project_key) REFERENCES project(project_key)"
                    + ")");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_memory_item_project_status_confidence "
                    + "ON memory_item(project_key, status, confidence)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_memory_item_project_module "
                    + "ON memory_item(project_key, module_name)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_checkpoint_project_created "
                    + "ON checkpoint(project_key, created_at)");

            if (!schemaVersionExists(connection, V1)) {
                statement.executeUpdate("INSERT INTO schema_version(version, description, applied_at) VALUES ("
                        + V1 + ", 'MVP memory schema', '" + clock.now().toString() + "')");
            }
        }
        migrateV2(connection, clock);

        boolean ftsAvailable = true;
        String ftsError = "";
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE VIRTUAL TABLE IF NOT EXISTS memory_fts USING fts5(title, content, tags)");
        } catch (SQLException ex) {
            ftsAvailable = false;
            ftsError = ex.getMessage();
        }
        return new MigrationResult(currentSchemaVersion(connection), ftsAvailable, ftsError);
    }

    private void migrateV2(Connection connection, Clock clock) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS workflow_template ("
                    + "workflow_key TEXT PRIMARY KEY,"
                    + "name TEXT NOT NULL,"
                    + "description TEXT NOT NULL DEFAULT '',"
                    + "category TEXT NOT NULL DEFAULT 'development',"
                    + "target_mode TEXT NOT NULL DEFAULT 'auto',"
                    + "status TEXT NOT NULL DEFAULT 'active',"
                    + "version INTEGER NOT NULL DEFAULT 1,"
                    + "source_kind TEXT NOT NULL DEFAULT 'seed',"
                    + "source_ref TEXT NOT NULL DEFAULT '',"
                    + "created_at TEXT NOT NULL,"
                    + "updated_at TEXT NOT NULL,"
                    + "CHECK (status IN ('active', 'deprecated', 'archived')),"
                    + "CHECK (target_mode IN ('auto', 'api', 'mvc', 'mixed', 'sql', 'debug', 'review'))"
                    + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS workflow_phase_template ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "workflow_key TEXT NOT NULL,"
                    + "phase_key TEXT NOT NULL,"
                    + "phase_name TEXT NOT NULL,"
                    + "phase_order INTEGER NOT NULL,"
                    + "phase_type TEXT NOT NULL,"
                    + "required INTEGER NOT NULL DEFAULT 1,"
                    + "instruction TEXT NOT NULL DEFAULT '',"
                    + "expected_output TEXT NOT NULL DEFAULT '',"
                    + "default_gate_policy TEXT NOT NULL DEFAULT 'none',"
                    + "created_at TEXT NOT NULL,"
                    + "updated_at TEXT NOT NULL,"
                    + "UNIQUE(workflow_key, phase_key),"
                    + "CHECK (phase_type IN ('context', 'spec', 'plan', 'approval', 'implementation',"
                    + "'verification', 'review', 'memory', 'checkpoint', 'recovery', 'db', 'custom')),"
                    + "CHECK (default_gate_policy IN ('none', 'soft', 'hard')),"
                    + "FOREIGN KEY (workflow_key) REFERENCES workflow_template(workflow_key)"
                    + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS workflow_gate_template ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "workflow_key TEXT NOT NULL,"
                    + "phase_key TEXT NOT NULL,"
                    + "gate_key TEXT NOT NULL,"
                    + "gate_name TEXT NOT NULL,"
                    + "gate_type TEXT NOT NULL,"
                    + "severity TEXT NOT NULL DEFAULT 'hard',"
                    + "rule_text TEXT NOT NULL,"
                    + "check_command TEXT NOT NULL DEFAULT '',"
                    + "expected_result TEXT NOT NULL DEFAULT '',"
                    + "created_at TEXT NOT NULL,"
                    + "updated_at TEXT NOT NULL,"
                    + "UNIQUE(workflow_key, phase_key, gate_key),"
                    + "CHECK (gate_type IN ('manual', 'command', 'file_exists', 'memory_query',"
                    + "'sql_safety', 'sensitive_guard', 'test_result', 'compile_result', 'export_check', 'custom')),"
                    + "CHECK (severity IN ('info', 'soft', 'hard')),"
                    + "FOREIGN KEY (workflow_key, phase_key) REFERENCES workflow_phase_template(workflow_key, phase_key)"
                    + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS workflow_run ("
                    + "run_key TEXT PRIMARY KEY,"
                    + "project_key TEXT NOT NULL,"
                    + "workflow_key TEXT NOT NULL,"
                    + "task_name TEXT NOT NULL,"
                    + "task_summary TEXT NOT NULL DEFAULT '',"
                    + "module_name TEXT NOT NULL DEFAULT 'global',"
                    + "mode TEXT NOT NULL DEFAULT 'auto',"
                    + "status TEXT NOT NULL DEFAULT 'created',"
                    + "current_phase_key TEXT NOT NULL DEFAULT '',"
                    + "context_export_path TEXT NOT NULL DEFAULT '',"
                    + "recovery_export_path TEXT NOT NULL DEFAULT '',"
                    + "checkpoint_id INTEGER,"
                    + "started_at TEXT,"
                    + "completed_at TEXT,"
                    + "created_at TEXT NOT NULL,"
                    + "updated_at TEXT NOT NULL,"
                    + "CHECK (status IN ('created', 'running', 'waiting_user', 'blocked',"
                    + "'completed', 'abandoned', 'failed')),"
                    + "CHECK (mode IN ('auto', 'api', 'mvc', 'mixed', 'sql', 'debug', 'review')),"
                    + "FOREIGN KEY (project_key) REFERENCES project(project_key),"
                    + "FOREIGN KEY (workflow_key) REFERENCES workflow_template(workflow_key)"
                    + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS workflow_phase_run ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "run_key TEXT NOT NULL,"
                    + "phase_key TEXT NOT NULL,"
                    + "phase_name TEXT NOT NULL,"
                    + "phase_order INTEGER NOT NULL,"
                    + "status TEXT NOT NULL DEFAULT 'pending',"
                    + "started_at TEXT,"
                    + "completed_at TEXT,"
                    + "input_summary TEXT NOT NULL DEFAULT '',"
                    + "output_summary TEXT NOT NULL DEFAULT '',"
                    + "evidence TEXT NOT NULL DEFAULT '',"
                    + "notes TEXT NOT NULL DEFAULT '',"
                    + "created_at TEXT NOT NULL,"
                    + "updated_at TEXT NOT NULL,"
                    + "UNIQUE(run_key, phase_key),"
                    + "CHECK (status IN ('pending', 'running', 'waiting_user', 'passed',"
                    + "'failed', 'skipped', 'blocked')),"
                    + "FOREIGN KEY (run_key) REFERENCES workflow_run(run_key)"
                    + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS workflow_gate_run ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "run_key TEXT NOT NULL,"
                    + "phase_key TEXT NOT NULL,"
                    + "gate_key TEXT NOT NULL,"
                    + "gate_name TEXT NOT NULL,"
                    + "gate_type TEXT NOT NULL,"
                    + "severity TEXT NOT NULL DEFAULT 'hard',"
                    + "status TEXT NOT NULL DEFAULT 'pending',"
                    + "checked_at TEXT,"
                    + "result_summary TEXT NOT NULL DEFAULT '',"
                    + "failure_reason TEXT NOT NULL DEFAULT '',"
                    + "evidence TEXT NOT NULL DEFAULT '',"
                    + "command_output_path TEXT NOT NULL DEFAULT '',"
                    + "created_at TEXT NOT NULL,"
                    + "updated_at TEXT NOT NULL,"
                    + "UNIQUE(run_key, phase_key, gate_key),"
                    + "CHECK (severity IN ('info', 'soft', 'hard')),"
                    + "CHECK (status IN ('pending', 'passed', 'failed', 'waived', 'skipped')),"
                    + "FOREIGN KEY (run_key, phase_key) REFERENCES workflow_phase_run(run_key, phase_key)"
                    + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS workflow_event ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "project_key TEXT NOT NULL,"
                    + "run_key TEXT NOT NULL,"
                    + "event_type TEXT NOT NULL,"
                    + "phase_key TEXT NOT NULL DEFAULT '',"
                    + "gate_key TEXT NOT NULL DEFAULT '',"
                    + "level TEXT NOT NULL DEFAULT 'info',"
                    + "message TEXT NOT NULL,"
                    + "data TEXT NOT NULL DEFAULT '',"
                    + "created_at TEXT NOT NULL,"
                    + "CHECK (level IN ('debug', 'info', 'warn', 'error')),"
                    + "CHECK (event_type IN ('run_created', 'run_started', 'phase_started',"
                    + "'phase_completed', 'gate_checked', 'gate_failed', 'gate_waived',"
                    + "'artifact_created', 'memory_exported', 'memory_suggested',"
                    + "'checkpoint_created', 'run_blocked', 'run_completed', 'run_failed', 'custom')),"
                    + "FOREIGN KEY (project_key) REFERENCES project(project_key),"
                    + "FOREIGN KEY (run_key) REFERENCES workflow_run(run_key)"
                    + ")");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_workflow_template_status ON workflow_template(status)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_workflow_phase_template_workflow "
                    + "ON workflow_phase_template(workflow_key, phase_order)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_workflow_gate_template_phase "
                    + "ON workflow_gate_template(workflow_key, phase_key)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_workflow_run_project_status "
                    + "ON workflow_run(project_key, status)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_workflow_run_module "
                    + "ON workflow_run(project_key, module_name)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_workflow_phase_run_status "
                    + "ON workflow_phase_run(run_key, status)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_workflow_gate_run_status "
                    + "ON workflow_gate_run(run_key, status)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_workflow_event_run "
                    + "ON workflow_event(run_key, created_at)");
            if (!schemaVersionExists(connection, V2)) {
                statement.executeUpdate("INSERT INTO schema_version(version, description, applied_at) VALUES ("
                        + V2 + ", 'V0.2 workflow persistence schema', '" + clock.now().toString() + "')");
            }
        }
    }

    private static boolean schemaVersionExists(Connection connection, int version) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT version FROM schema_version WHERE version = " + version)) {
            return resultSet.next();
        }
    }

    public static int currentSchemaVersion(Connection connection) throws SQLException {
        if (!hasTable(connection, "schema_version")) {
            return 0;
        }
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT MAX(version) FROM schema_version")) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
            return 0;
        }
    }

    public static boolean hasTable(Connection connection, String tableName) throws SQLException {
        try (java.sql.PreparedStatement statement = connection.prepareStatement(
                "SELECT name FROM sqlite_master WHERE type IN ('table', 'virtual table') AND name = ?")) {
            statement.setString(1, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }
}
