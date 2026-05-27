package com.devharnesskit.dhk.db.migration;

import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.util.Clock;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class V9GraphLiteMigration extends AbstractMigrationStep {
    public V9GraphLiteMigration() {
        super(MigrationRunner.V9, "V0.4 graph lite schema");
    }

    public void apply(Connection connection, Clock clock) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS code_graph_snapshot ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "project_key TEXT NOT NULL,"
                    + "snapshot_key TEXT NOT NULL UNIQUE,"
                    + "provider TEXT NOT NULL DEFAULT 'lite',"
                    + "config_hash TEXT NOT NULL DEFAULT '',"
                    + "workspace_fingerprint TEXT NOT NULL DEFAULT '',"
                    + "status TEXT NOT NULL DEFAULT 'created',"
                    + "file_count INTEGER NOT NULL DEFAULT 0,"
                    + "node_count INTEGER NOT NULL DEFAULT 0,"
                    + "edge_count INTEGER NOT NULL DEFAULT 0,"
                    + "skipped_file_count INTEGER NOT NULL DEFAULT 0,"
                    + "max_file_bytes INTEGER NOT NULL DEFAULT 1048576,"
                    + "max_indexed_files INTEGER NOT NULL DEFAULT 5000,"
                    + "summary TEXT NOT NULL DEFAULT '',"
                    + "created_at TEXT NOT NULL,"
                    + "completed_at TEXT NOT NULL DEFAULT '',"
                    + "CHECK (status IN ('created', 'indexing', 'completed', 'failed', 'stale')),"
                    + "FOREIGN KEY (project_key) REFERENCES project(project_key)"
                    + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS code_graph_file ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "snapshot_id INTEGER NOT NULL,"
                    + "relative_path TEXT NOT NULL,"
                    + "file_kind TEXT NOT NULL DEFAULT '',"
                    + "language TEXT NOT NULL DEFAULT '',"
                    + "content_hash TEXT NOT NULL DEFAULT '',"
                    + "size_bytes INTEGER NOT NULL DEFAULT 0,"
                    + "indexed INTEGER NOT NULL DEFAULT 1,"
                    + "skip_reason TEXT NOT NULL DEFAULT '',"
                    + "protected_file INTEGER NOT NULL DEFAULT 0,"
                    + "created_at TEXT NOT NULL,"
                    + "UNIQUE(snapshot_id, relative_path),"
                    + "FOREIGN KEY (snapshot_id) REFERENCES code_graph_snapshot(id)"
                    + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS code_graph_node ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "snapshot_id INTEGER NOT NULL,"
                    + "file_id INTEGER NOT NULL,"
                    + "node_key TEXT NOT NULL,"
                    + "node_kind TEXT NOT NULL,"
                    + "name TEXT NOT NULL,"
                    + "qualified_name TEXT NOT NULL DEFAULT '',"
                    + "start_line INTEGER NOT NULL DEFAULT 0,"
                    + "end_line INTEGER NOT NULL DEFAULT 0,"
                    + "signature TEXT NOT NULL DEFAULT '',"
                    + "metadata TEXT NOT NULL DEFAULT '',"
                    + "UNIQUE(snapshot_id, node_key),"
                    + "FOREIGN KEY (snapshot_id) REFERENCES code_graph_snapshot(id),"
                    + "FOREIGN KEY (file_id) REFERENCES code_graph_file(id)"
                    + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS code_graph_edge ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "snapshot_id INTEGER NOT NULL,"
                    + "source_node_key TEXT NOT NULL,"
                    + "target_node_key TEXT NOT NULL,"
                    + "edge_kind TEXT NOT NULL,"
                    + "file_id INTEGER,"
                    + "confidence INTEGER NOT NULL DEFAULT 100,"
                    + "metadata TEXT NOT NULL DEFAULT '',"
                    + "CHECK (confidence >= 0 AND confidence <= 100),"
                    + "UNIQUE(snapshot_id, source_node_key, target_node_key, edge_kind),"
                    + "FOREIGN KEY (snapshot_id) REFERENCES code_graph_snapshot(id),"
                    + "FOREIGN KEY (file_id) REFERENCES code_graph_file(id)"
                    + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS code_graph_query_cache ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "snapshot_id INTEGER NOT NULL,"
                    + "query_key TEXT NOT NULL,"
                    + "query_type TEXT NOT NULL,"
                    + "input_hash TEXT NOT NULL DEFAULT '',"
                    + "result_path TEXT NOT NULL DEFAULT '',"
                    + "result_summary TEXT NOT NULL DEFAULT '',"
                    + "created_at TEXT NOT NULL,"
                    + "expires_at TEXT NOT NULL DEFAULT '',"
                    + "UNIQUE(snapshot_id, query_key),"
                    + "FOREIGN KEY (snapshot_id) REFERENCES code_graph_snapshot(id)"
                    + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS goal_graph_binding ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "goal_key TEXT NOT NULL,"
                    + "snapshot_id INTEGER NOT NULL,"
                    + "binding_type TEXT NOT NULL DEFAULT 'used',"
                    + "artifact_path TEXT NOT NULL DEFAULT '',"
                    + "impact_hash TEXT NOT NULL DEFAULT '',"
                    + "created_at TEXT NOT NULL,"
                    + "CHECK (binding_type IN ('used', 'pre_change', 'post_change', 'impact_map', 'summary')),"
                    + "UNIQUE(goal_key, snapshot_id, binding_type),"
                    + "FOREIGN KEY (goal_key) REFERENCES goal_run(goal_key),"
                    + "FOREIGN KEY (snapshot_id) REFERENCES code_graph_snapshot(id)"
                    + ")");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_code_graph_snapshot_project_status "
                    + "ON code_graph_snapshot(project_key, status)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_code_graph_file_snapshot_path "
                    + "ON code_graph_file(snapshot_id, relative_path)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_code_graph_node_snapshot_kind "
                    + "ON code_graph_node(snapshot_id, node_kind)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_code_graph_node_snapshot_qname "
                    + "ON code_graph_node(snapshot_id, qualified_name)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_code_graph_edge_snapshot_source "
                    + "ON code_graph_edge(snapshot_id, source_node_key)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_code_graph_edge_snapshot_target "
                    + "ON code_graph_edge(snapshot_id, target_node_key)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_code_graph_edge_snapshot_kind "
                    + "ON code_graph_edge(snapshot_id, edge_kind)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_code_graph_query_cache_snapshot_type "
                    + "ON code_graph_query_cache(snapshot_id, query_type)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_goal_graph_binding_goal "
                    + "ON goal_graph_binding(goal_key)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_goal_graph_binding_snapshot "
                    + "ON goal_graph_binding(snapshot_id)");
            if (!MigrationSupport.schemaVersionExists(connection, MigrationRunner.V9)) {
                statement.executeUpdate("INSERT INTO schema_version(version, description, applied_at) VALUES ("
                        + MigrationRunner.V9 + ", 'V0.5.0 Graph Lite snapshot schema', '"
                        + clock.now().toString() + "')");
            }
        }
   
    }
}
