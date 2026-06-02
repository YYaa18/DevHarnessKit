package com.devharnesskit.dhk.db.migration;

import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.util.Clock;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class V17MemoryQualityMigration extends AbstractMigrationStep {
    public V17MemoryQualityMigration() {
        super(MigrationRunner.V17, "V1.1 memory quality metadata and candidate queue");
    }

    public void apply(Connection connection, Clock clock) throws SQLException {
        if (MigrationSupport.hasTable(connection, "memory_item")) {
            MigrationSupport.addColumnIfMissing(connection, "memory_item",
                    "fingerprint", "TEXT NOT NULL DEFAULT ''");
            MigrationSupport.addColumnIfMissing(connection, "memory_item",
                    "canonical_key", "TEXT NOT NULL DEFAULT ''");
            MigrationSupport.addColumnIfMissing(connection, "memory_item",
                    "superseded_by", "INTEGER NOT NULL DEFAULT 0");
            MigrationSupport.addColumnIfMissing(connection, "memory_item",
                    "stale_reason", "TEXT NOT NULL DEFAULT ''");
            MigrationSupport.addColumnIfMissing(connection, "memory_item",
                    "last_verified_at", "TEXT NOT NULL DEFAULT ''");
            MigrationSupport.addColumnIfMissing(connection, "memory_item",
                    "source_ref", "TEXT NOT NULL DEFAULT ''");
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS memory_candidate ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "project_key TEXT NOT NULL,"
                    + "candidate_status TEXT NOT NULL DEFAULT 'pending',"
                    + "module_name TEXT NOT NULL DEFAULT 'global',"
                    + "memory_type TEXT NOT NULL,"
                    + "title TEXT NOT NULL,"
                    + "content TEXT NOT NULL,"
                    + "tags TEXT NOT NULL DEFAULT '',"
                    + "confidence INTEGER NOT NULL DEFAULT 50,"
                    + "source_kind TEXT NOT NULL DEFAULT 'manual',"
                    + "source_ref TEXT NOT NULL DEFAULT '',"
                    + "reason TEXT NOT NULL DEFAULT '',"
                    + "evidence TEXT NOT NULL DEFAULT '',"
                    + "fingerprint TEXT NOT NULL DEFAULT '',"
                    + "canonical_key TEXT NOT NULL DEFAULT '',"
                    + "created_at TEXT NOT NULL,"
                    + "updated_at TEXT NOT NULL,"
                    + "decided_at TEXT NOT NULL DEFAULT '',"
                    + "decision_reason TEXT NOT NULL DEFAULT '',"
                    + "accepted_memory_id INTEGER NOT NULL DEFAULT 0,"
                    + "CHECK (candidate_status IN ('pending', 'accepted', 'rejected')),"
                    + "CHECK (confidence >= 0 AND confidence <= 100),"
                    + "FOREIGN KEY (project_key) REFERENCES project(project_key)"
                    + ")");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_memory_candidate_project_status "
                    + "ON memory_candidate(project_key, candidate_status, updated_at)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_memory_item_project_fingerprint "
                    + "ON memory_item(project_key, fingerprint)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_memory_item_project_canonical "
                    + "ON memory_item(project_key, canonical_key)");
            if (!MigrationSupport.schemaVersionExists(connection, MigrationRunner.V17)) {
                statement.executeUpdate("INSERT INTO schema_version(version, description, applied_at) VALUES ("
                        + MigrationRunner.V17 + ", 'V1.1 memory quality metadata and candidate queue', '"
                        + clock.now().toString() + "')");
            }
        }
    }
}
