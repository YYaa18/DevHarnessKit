package com.devharnesskit.dhk.db;

import com.devharnesskit.dhk.service.MemoryBackupService;
import com.devharnesskit.dhk.util.Clock;
import com.devharnesskit.dhk.util.PathUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class MigrationRunner {
    public static final int V1 = 1;
    public static final int V2 = 2;
    public static final int V3 = 3;
    public static final int V4 = 4;
    public static final int V5 = 5;
    public static final int V6 = 6;
    public static final int V7 = 7;

    public MigrationResult migrate(Connection connection, Clock clock) throws SQLException {
        String backupPath = backupBeforeUpgrade(connection, clock);
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
        repairProjectColumns(connection);
        migrateV2(connection, clock);
        migrateV3(connection, clock);
        migrateV4(connection, clock);
        migrateV5(connection, clock);
        migrateV6(connection, clock);
        migrateV7(connection, clock);

        boolean ftsAvailable = true;
        String ftsError = "";
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE VIRTUAL TABLE IF NOT EXISTS memory_fts USING fts5(title, content, tags)");
        } catch (SQLException ex) {
            ftsAvailable = false;
            ftsError = ex.getMessage();
        }
        return new MigrationResult(currentSchemaVersion(connection), ftsAvailable, ftsError, backupPath);
    }

    private String backupBeforeUpgrade(Connection connection, Clock clock) throws SQLException {
        Path dbPath = databasePath(connection);
        if (dbPath == null || !Files.isRegularFile(dbPath)) {
            return "";
        }
        int currentVersion = currentSchemaVersion(connection);
        if (currentVersion >= V7) {
            return "";
        }
        try {
            if (Files.size(dbPath) <= 0L) {
                return "";
            }
            Path memoryDir = dbPath.getParent();
            if (memoryDir == null) {
                return "";
            }
            Path projectRoot = memoryDir.getParent() == null ? null : memoryDir.getParent().getParent();
            if (projectRoot == null) {
                return "";
            }
            MemoryBackupService backupService = new MemoryBackupService();
            Path out = backupService.defaultBackupPath(projectRoot, clock.now().toString(),
                    "pre-migration-v" + currentVersion + "-to-v" + V7);
            backupService.writeBackup(PathUtil.memoryDirectory(projectRoot), out);
            return out.toString();
        } catch (IOException ex) {
            throw new SQLException("Failed to create pre-migration backup: " + ex.getMessage(), ex);
        }
    }

    private Path databasePath(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA database_list")) {
            while (resultSet.next()) {
                String name = resultSet.getString("name");
                if ("main".equals(name)) {
                    String file = resultSet.getString("file");
                    if (file != null && file.length() > 0) {
                        return Paths.get(file);
                    }
                }
            }
            return null;
        }
    }

    private void repairProjectColumns(Connection connection) throws SQLException {
        addColumnIfMissing(connection, "project", "root_path", "TEXT NOT NULL DEFAULT ''");
        addColumnIfMissing(connection, "project", "project_type", "TEXT NOT NULL DEFAULT 'unknown'");
        addColumnIfMissing(connection, "project", "language", "TEXT NOT NULL DEFAULT 'java'");
        addColumnIfMissing(connection, "project", "framework", "TEXT NOT NULL DEFAULT 'unknown'");
        addColumnIfMissing(connection, "project", "database_type", "TEXT NOT NULL DEFAULT 'unknown'");
        addColumnIfMissing(connection, "project", "created_at", "TEXT NOT NULL DEFAULT ''");
        addColumnIfMissing(connection, "project", "updated_at", "TEXT NOT NULL DEFAULT ''");
    }

    private void addColumnIfMissing(Connection connection, String tableName, String columnName,
                                    String definition) throws SQLException {
        if (columnExists(connection, tableName, columnName)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (resultSet.next()) {
                if (columnName.equals(resultSet.getString("name"))) {
                    return true;
                }
            }
            return false;
        }
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

    private void migrateV3(Connection connection, Clock clock) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS workflow_artifact ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "project_key TEXT NOT NULL,"
                    + "run_key TEXT NOT NULL,"
                    + "artifact_type TEXT NOT NULL,"
                    + "title TEXT NOT NULL,"
                    + "file_path TEXT NOT NULL DEFAULT '',"
                    + "content_hash TEXT NOT NULL DEFAULT '',"
                    + "status TEXT NOT NULL DEFAULT 'draft',"
                    + "produced_by_phase TEXT NOT NULL DEFAULT '',"
                    + "summary TEXT NOT NULL DEFAULT '',"
                    + "tags TEXT NOT NULL DEFAULT '',"
                    + "created_at TEXT NOT NULL,"
                    + "updated_at TEXT NOT NULL,"
                    + "CHECK (artifact_type IN ('current_context', 'workflow_context', 'recovery_context',"
                    + "'sql_result', 'checkpoint', 'command_output', 'memory_suggestion', 'review',"
                    + "'verification', 'custom')),"
                    + "CHECK (status IN ('draft', 'confirmed', 'deprecated', 'archived')),"
                    + "FOREIGN KEY (project_key) REFERENCES project(project_key),"
                    + "FOREIGN KEY (run_key) REFERENCES workflow_run(run_key)"
                    + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS workflow_memory_binding ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "run_key TEXT NOT NULL,"
                    + "memory_id INTEGER NOT NULL,"
                    + "binding_type TEXT NOT NULL,"
                    + "phase_key TEXT NOT NULL DEFAULT '',"
                    + "reason TEXT NOT NULL DEFAULT '',"
                    + "created_at TEXT NOT NULL,"
                    + "CHECK (binding_type IN ('read', 'exported', 'suggested',"
                    + "'created_draft', 'confirmed_after_run', 'deprecated_after_run')),"
                    + "FOREIGN KEY (run_key) REFERENCES workflow_run(run_key),"
                    + "FOREIGN KEY (memory_id) REFERENCES memory_item(id)"
                    + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS workflow_checkpoint_binding ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "run_key TEXT NOT NULL,"
                    + "checkpoint_id INTEGER NOT NULL,"
                    + "binding_type TEXT NOT NULL DEFAULT 'created',"
                    + "created_at TEXT NOT NULL,"
                    + "CHECK (binding_type IN ('created', 'recovered_from', 'updated')),"
                    + "FOREIGN KEY (run_key) REFERENCES workflow_run(run_key),"
                    + "FOREIGN KEY (checkpoint_id) REFERENCES checkpoint(id)"
                    + ")");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_workflow_artifact_run "
                    + "ON workflow_artifact(run_key, artifact_type)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_workflow_memory_binding_run "
                    + "ON workflow_memory_binding(run_key, binding_type)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_workflow_memory_binding_memory "
                    + "ON workflow_memory_binding(memory_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_workflow_checkpoint_binding_run "
                    + "ON workflow_checkpoint_binding(run_key)");
            if (!schemaVersionExists(connection, V3)) {
                statement.executeUpdate("INSERT INTO schema_version(version, description, applied_at) VALUES ("
                        + V3 + ", 'V0.2-B workflow artifact binding schema', '" + clock.now().toString() + "')");
            }
        }
    }

    private void migrateV4(Connection connection, Clock clock) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS spec_change ("
                    + "change_key TEXT PRIMARY KEY,"
                    + "project_key TEXT NOT NULL,"
                    + "title TEXT NOT NULL,"
                    + "summary TEXT NOT NULL DEFAULT '',"
                    + "module_name TEXT NOT NULL DEFAULT 'global',"
                    + "mode TEXT NOT NULL DEFAULT 'auto',"
                    + "status TEXT NOT NULL DEFAULT 'draft',"
                    + "priority TEXT NOT NULL DEFAULT 'normal',"
                    + "source_kind TEXT NOT NULL DEFAULT 'manual',"
                    + "created_by TEXT NOT NULL DEFAULT 'manual',"
                    + "created_at TEXT NOT NULL,"
                    + "updated_at TEXT NOT NULL,"
                    + "archived_at TEXT NOT NULL DEFAULT '',"
                    + "CHECK (status IN ('draft', 'proposed', 'approved', 'in_progress',"
                    + "'verified', 'archived', 'abandoned')),"
                    + "CHECK (priority IN ('low', 'normal', 'high', 'critical')),"
                    + "CHECK (mode IN ('auto', 'api', 'mvc', 'mixed', 'sql', 'debug', 'review')),"
                    + "FOREIGN KEY (project_key) REFERENCES project(project_key)"
                    + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS spec_document ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "change_key TEXT NOT NULL,"
                    + "document_type TEXT NOT NULL,"
                    + "title TEXT NOT NULL,"
                    + "content TEXT NOT NULL,"
                    + "content_hash TEXT NOT NULL DEFAULT '',"
                    + "status TEXT NOT NULL DEFAULT 'draft',"
                    + "version INTEGER NOT NULL DEFAULT 1,"
                    + "created_at TEXT NOT NULL,"
                    + "updated_at TEXT NOT NULL,"
                    + "CHECK (document_type IN ('proposal', 'design', 'requirements', 'tasks',"
                    + "'acceptance', 'verification', 'notes')),"
                    + "CHECK (status IN ('draft', 'confirmed', 'deprecated', 'archived')),"
                    + "FOREIGN KEY (change_key) REFERENCES spec_change(change_key)"
                    + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS spec_task ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "change_key TEXT NOT NULL,"
                    + "task_key TEXT NOT NULL,"
                    + "task_order INTEGER NOT NULL,"
                    + "title TEXT NOT NULL,"
                    + "description TEXT NOT NULL DEFAULT '',"
                    + "status TEXT NOT NULL DEFAULT 'pending',"
                    + "phase_key TEXT NOT NULL DEFAULT '',"
                    + "evidence TEXT NOT NULL DEFAULT '',"
                    + "created_at TEXT NOT NULL,"
                    + "updated_at TEXT NOT NULL,"
                    + "completed_at TEXT NOT NULL DEFAULT '',"
                    + "UNIQUE(change_key, task_key),"
                    + "CHECK (status IN ('pending', 'in_progress', 'done', 'blocked', 'skipped')),"
                    + "FOREIGN KEY (change_key) REFERENCES spec_change(change_key)"
                    + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS spec_acceptance ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "change_key TEXT NOT NULL,"
                    + "acceptance_key TEXT NOT NULL,"
                    + "acceptance_order INTEGER NOT NULL,"
                    + "description TEXT NOT NULL,"
                    + "expected_result TEXT NOT NULL DEFAULT '',"
                    + "status TEXT NOT NULL DEFAULT 'pending',"
                    + "evidence TEXT NOT NULL DEFAULT '',"
                    + "created_at TEXT NOT NULL,"
                    + "updated_at TEXT NOT NULL,"
                    + "verified_at TEXT NOT NULL DEFAULT '',"
                    + "UNIQUE(change_key, acceptance_key),"
                    + "CHECK (status IN ('pending', 'passed', 'failed', 'waived')),"
                    + "FOREIGN KEY (change_key) REFERENCES spec_change(change_key)"
                    + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS workflow_spec_binding ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "run_key TEXT NOT NULL,"
                    + "change_key TEXT NOT NULL,"
                    + "binding_type TEXT NOT NULL,"
                    + "created_at TEXT NOT NULL,"
                    + "CHECK (binding_type IN ('implements', 'reviews', 'verifies', 'archives')),"
                    + "FOREIGN KEY (run_key) REFERENCES workflow_run(run_key),"
                    + "FOREIGN KEY (change_key) REFERENCES spec_change(change_key)"
                    + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS spec_event ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "project_key TEXT NOT NULL,"
                    + "change_key TEXT NOT NULL,"
                    + "event_type TEXT NOT NULL,"
                    + "level TEXT NOT NULL DEFAULT 'info',"
                    + "message TEXT NOT NULL,"
                    + "data TEXT NOT NULL DEFAULT '',"
                    + "created_at TEXT NOT NULL,"
                    + "CHECK (level IN ('debug', 'info', 'warn', 'error')),"
                    + "CHECK (event_type IN ('change_created', 'document_upserted', 'task_added',"
                    + "'task_updated', 'acceptance_added', 'acceptance_updated', 'change_approved',"
                    + "'change_verified', 'change_archived', 'workflow_bound', 'custom')),"
                    + "FOREIGN KEY (project_key) REFERENCES project(project_key),"
                    + "FOREIGN KEY (change_key) REFERENCES spec_change(change_key)"
                    + ")");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_spec_change_project_status "
                    + "ON spec_change(project_key, status)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_spec_change_module "
                    + "ON spec_change(project_key, module_name)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_spec_document_change_type "
                    + "ON spec_document(change_key, document_type)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_spec_task_change_status "
                    + "ON spec_task(change_key, status)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_spec_acceptance_change_status "
                    + "ON spec_acceptance(change_key, status)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_workflow_spec_binding_run "
                    + "ON workflow_spec_binding(run_key)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_workflow_spec_binding_change "
                    + "ON workflow_spec_binding(change_key)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_spec_event_change "
                    + "ON spec_event(change_key, created_at)");
            if (!schemaVersionExists(connection, V4)) {
                statement.executeUpdate("INSERT INTO schema_version(version, description, applied_at) VALUES ("
                        + V4 + ", 'V0.3 spec persistence schema', '" + clock.now().toString() + "')");
            }
        }
    }

    private void migrateV5(Connection connection, Clock clock) throws SQLException {
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
            if (!schemaVersionExists(connection, V5)) {
                statement.executeUpdate("INSERT INTO schema_version(version, description, applied_at) VALUES ("
                        + V5 + ", 'V0.4 goal orchestration MVP schema', '" + clock.now().toString() + "')");
            }
        }
    }

    private void migrateV6(Connection connection, Clock clock) throws SQLException {
        addColumnIfMissing(connection, "goal_check", "step_count_at_check", "INTEGER NOT NULL DEFAULT 0");
        try (Statement statement = connection.createStatement()) {
            if (!schemaVersionExists(connection, V6)) {
                statement.executeUpdate("INSERT INTO schema_version(version, description, applied_at) VALUES ("
                        + V6 + ", 'V0.4.1 goal check freshness metadata schema', '" + clock.now().toString() + "')");
            }
        }
    }

    private void migrateV7(Connection connection, Clock clock) throws SQLException {
        if (hasTable(connection, "goal_run") && !goalRunAllowsContextExportFailure(connection)) {
            rebuildGoalRunForV7(connection);
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE INDEX IF NOT EXISTS idx_goal_run_project_status "
                    + "ON goal_run(project_key, status)");
            if (!schemaVersionExists(connection, V7)) {
                statement.executeUpdate("INSERT INTO schema_version(version, description, applied_at) VALUES ("
                        + V7 + ", 'V0.4.1 goal context export recovery schema', '" + clock.now().toString() + "')");
            }
        }
    }

    private boolean goalRunAllowsContextExportFailure(Connection connection) throws SQLException {
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

    private void rebuildGoalRunForV7(Connection connection) throws SQLException {
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
