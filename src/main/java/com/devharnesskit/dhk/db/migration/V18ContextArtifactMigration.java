package com.devharnesskit.dhk.db.migration;

import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.util.Clock;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class V18ContextArtifactMigration extends AbstractMigrationStep {
    public V18ContextArtifactMigration() {
        super(MigrationRunner.V18, "Context Governor artifact storage");
    }

    public void apply(Connection connection, Clock clock) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS context_artifact ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "project_key TEXT NOT NULL,"
                    + "goal_key TEXT NOT NULL DEFAULT '',"
                    + "artifact_key TEXT NOT NULL,"
                    + "source_type TEXT NOT NULL,"
                    + "source_path TEXT NOT NULL DEFAULT '',"
                    + "original_sha256 TEXT NOT NULL DEFAULT '',"
                    + "original_text TEXT NOT NULL DEFAULT '',"
                    + "compressed_text TEXT NOT NULL DEFAULT '',"
                    + "retained_spans_json TEXT NOT NULL DEFAULT '',"
                    + "omitted_lines INTEGER NOT NULL DEFAULT 0,"
                    + "token_before INTEGER NOT NULL DEFAULT 0,"
                    + "token_after INTEGER NOT NULL DEFAULT 0,"
                    + "created_at TEXT NOT NULL,"
                    + "UNIQUE(project_key, artifact_key),"
                    + "FOREIGN KEY (project_key) REFERENCES project(project_key)"
                    + ")");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_context_artifact_project_goal_created "
                    + "ON context_artifact(project_key, goal_key, created_at)");
            if (!MigrationSupport.schemaVersionExists(connection, MigrationRunner.V18)) {
                statement.executeUpdate("INSERT INTO schema_version(version, description, applied_at) VALUES ("
                        + MigrationRunner.V18 + ", 'Context Governor artifact storage', '"
                        + clock.now().toString() + "')");
            }
        }
    }
}
