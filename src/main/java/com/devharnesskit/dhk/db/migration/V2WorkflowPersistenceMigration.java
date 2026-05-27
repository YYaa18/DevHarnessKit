package com.devharnesskit.dhk.db.migration;

import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.util.Clock;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class V2WorkflowPersistenceMigration extends AbstractMigrationStep {
    public V2WorkflowPersistenceMigration() {
        super(MigrationRunner.V2, "V0.2 workflow persistence schema");
    }

    public void apply(Connection connection, Clock clock) throws SQLException {
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
            if (!MigrationSupport.schemaVersionExists(connection, MigrationRunner.V2)) {
                statement.executeUpdate("INSERT INTO schema_version(version, description, applied_at) VALUES ("
                        + MigrationRunner.V2 + ", 'V0.2 workflow persistence schema', '" + clock.now().toString() + "')");
            }
        }
   
    }
}
