package com.devharnesskit.dhk.db.migration;

import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.util.Clock;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class V15KnowledgeCandidateMetadataMigration extends AbstractMigrationStep {
    public V15KnowledgeCandidateMetadataMigration() {
        super(MigrationRunner.V15, "V0.8 professional knowledge candidate metadata");
    }

    public void apply(Connection connection, Clock clock) throws SQLException {
        if (MigrationSupport.hasTable(connection, "knowledge_candidate")) {
            MigrationSupport.addColumnIfMissing(connection, "knowledge_candidate",
                    "source_rule_id", "TEXT NOT NULL DEFAULT ''");
            MigrationSupport.addColumnIfMissing(connection, "knowledge_candidate",
                    "source_pack_key", "TEXT NOT NULL DEFAULT ''");
            MigrationSupport.addColumnIfMissing(connection, "knowledge_candidate",
                    "domain", "TEXT NOT NULL DEFAULT ''");
            MigrationSupport.addColumnIfMissing(connection, "knowledge_candidate",
                    "severity", "TEXT NOT NULL DEFAULT ''");
            MigrationSupport.addColumnIfMissing(connection, "knowledge_candidate",
                    "applicable_when", "TEXT NOT NULL DEFAULT ''");
            MigrationSupport.addColumnIfMissing(connection, "knowledge_candidate",
                    "not_applicable_when", "TEXT NOT NULL DEFAULT ''");
            MigrationSupport.addColumnIfMissing(connection, "knowledge_candidate",
                    "destination_reason", "TEXT NOT NULL DEFAULT ''");
            MigrationSupport.addColumnIfMissing(connection, "knowledge_candidate",
                    "metadata", "TEXT NOT NULL DEFAULT ''");
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE INDEX IF NOT EXISTS idx_knowledge_candidate_rule "
                    + "ON knowledge_candidate(source_pack_key, source_rule_id)");
            if (!MigrationSupport.schemaVersionExists(connection, MigrationRunner.V15)) {
                statement.executeUpdate("INSERT INTO schema_version(version, description, applied_at) VALUES ("
                        + MigrationRunner.V15 + ", 'V0.8 professional knowledge candidate metadata', '"
                        + clock.now().toString() + "')");
            }
        }
    }
}
