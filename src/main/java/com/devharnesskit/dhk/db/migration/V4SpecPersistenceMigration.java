package com.devharnesskit.dhk.db.migration;

import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.util.Clock;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class V4SpecPersistenceMigration extends AbstractMigrationStep {
    public V4SpecPersistenceMigration() {
        super(MigrationRunner.V4, "V0.3 spec persistence schema");
    }

    public void apply(Connection connection, Clock clock) throws SQLException {
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
            if (!MigrationSupport.schemaVersionExists(connection, MigrationRunner.V4)) {
                statement.executeUpdate("INSERT INTO schema_version(version, description, applied_at) VALUES ("
                        + MigrationRunner.V4 + ", 'V0.3 spec persistence schema', '" + clock.now().toString() + "')");
            }
        }
   
    }
}
