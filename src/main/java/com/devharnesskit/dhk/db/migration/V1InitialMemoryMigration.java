package com.devharnesskit.dhk.db.migration;

import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.util.Clock;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class V1InitialMemoryMigration extends AbstractMigrationStep {
    public V1InitialMemoryMigration() {
        super(MigrationRunner.V1, "MVP memory schema");
    }

    public void apply(Connection connection, Clock clock) throws SQLException {
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

            if (!MigrationSupport.schemaVersionExists(connection, MigrationRunner.V1)) {
                statement.executeUpdate("INSERT INTO schema_version(version, description, applied_at) VALUES ("
                        + MigrationRunner.V1 + ", 'MVP memory schema', '" + clock.now().toString() + "')");
            }
        }
        MigrationSupport.repairProjectColumns(connection);
   
    }
}
