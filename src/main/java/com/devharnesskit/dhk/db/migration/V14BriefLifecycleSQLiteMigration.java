package com.devharnesskit.dhk.db.migration;

import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.util.Clock;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class V14BriefLifecycleSQLiteMigration extends AbstractMigrationStep {
    public V14BriefLifecycleSQLiteMigration() {
        super(MigrationRunner.V14, "V0.8 brief lifecycle SQLite schema");
    }

    public void apply(Connection connection, Clock clock) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS knowledge_candidate ("
                    + "candidate_id TEXT PRIMARY KEY, "
                    + "project_key TEXT NOT NULL DEFAULT '', "
                    + "goal_key TEXT NOT NULL DEFAULT '', "
                    + "candidate_type TEXT NOT NULL DEFAULT '', "
                    + "title TEXT NOT NULL DEFAULT '', "
                    + "summary TEXT NOT NULL DEFAULT '', "
                    + "evidence_refs TEXT NOT NULL DEFAULT '', "
                    + "suggested_destination TEXT NOT NULL DEFAULT '', "
                    + "confidence TEXT NOT NULL DEFAULT '', "
                    + "requires_confirmation INTEGER NOT NULL DEFAULT 1, "
                    + "sensitive_scan_status TEXT NOT NULL DEFAULT '', "
                    + "status TEXT NOT NULL DEFAULT '', "
                    + "created_at TEXT NOT NULL DEFAULT '', "
                    + "updated_at TEXT NOT NULL DEFAULT ''"
                    + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS interaction_request ("
                    + "request_id TEXT PRIMARY KEY, "
                    + "project_key TEXT NOT NULL DEFAULT '', "
                    + "goal_key TEXT NOT NULL DEFAULT '', "
                    + "phase TEXT NOT NULL DEFAULT '', "
                    + "interaction_type TEXT NOT NULL DEFAULT '', "
                    + "priority TEXT NOT NULL DEFAULT '', "
                    + "question TEXT NOT NULL DEFAULT '', "
                    + "why TEXT NOT NULL DEFAULT '', "
                    + "choices TEXT NOT NULL DEFAULT '', "
                    + "default_choice TEXT NOT NULL DEFAULT '', "
                    + "blocks_progress INTEGER NOT NULL DEFAULT 0, "
                    + "status TEXT NOT NULL DEFAULT '', "
                    + "answer TEXT NOT NULL DEFAULT '', "
                    + "created_at TEXT NOT NULL DEFAULT '', "
                    + "updated_at TEXT NOT NULL DEFAULT ''"
                    + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS growth_lesson ("
                    + "lesson_id TEXT PRIMARY KEY, "
                    + "project_key TEXT NOT NULL DEFAULT '', "
                    + "source_candidate_id TEXT NOT NULL DEFAULT '', "
                    + "title TEXT NOT NULL DEFAULT '', "
                    + "summary TEXT NOT NULL DEFAULT '', "
                    + "status TEXT NOT NULL DEFAULT '', "
                    + "advisory_only INTEGER NOT NULL DEFAULT 1, "
                    + "created_at TEXT NOT NULL DEFAULT '', "
                    + "updated_at TEXT NOT NULL DEFAULT ''"
                    + ")");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_knowledge_candidate_project_status "
                    + "ON knowledge_candidate(project_key, status)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_knowledge_candidate_goal "
                    + "ON knowledge_candidate(goal_key)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_interaction_request_project_status "
                    + "ON interaction_request(project_key, status)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_interaction_request_goal "
                    + "ON interaction_request(goal_key)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_growth_lesson_project_status "
                    + "ON growth_lesson(project_key, status)");
        }
        new BriefLifecycleLegacyStoreImporter().importLegacyStores(connection, clock);
        try (Statement statement = connection.createStatement()) {
            if (!MigrationSupport.schemaVersionExists(connection, MigrationRunner.V14)) {
                statement.executeUpdate("INSERT INTO schema_version(version, description, applied_at) VALUES ("
                        + MigrationRunner.V14 + ", 'V0.8 brief lifecycle SQLite schema', '"
                        + clock.now().toString() + "')");
            }
        }
    }
}
