package com.devharnesskit.dhk.db.migration;

import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.util.Clock;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class V3WorkflowArtifactBindingMigration extends AbstractMigrationStep {
    public V3WorkflowArtifactBindingMigration() {
        super(MigrationRunner.V3, "V0.2-B workflow artifact binding schema");
    }

    public void apply(Connection connection, Clock clock) throws SQLException {
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
            if (!MigrationSupport.schemaVersionExists(connection, MigrationRunner.V3)) {
                statement.executeUpdate("INSERT INTO schema_version(version, description, applied_at) VALUES ("
                        + MigrationRunner.V3 + ", 'V0.2-B workflow artifact binding schema', '" + clock.now().toString() + "')");
            }
        }
   
    }
}
