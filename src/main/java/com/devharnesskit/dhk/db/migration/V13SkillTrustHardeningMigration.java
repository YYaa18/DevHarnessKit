package com.devharnesskit.dhk.db.migration;

import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.util.Clock;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class V13SkillTrustHardeningMigration extends AbstractMigrationStep {
    public V13SkillTrustHardeningMigration() {
        super(MigrationRunner.V13, "V0.4 skill trust hardening schema");
    }

    public void apply(Connection connection, Clock clock) throws SQLException {
        MigrationSupport.addColumnIfMissing(connection, "skill_contract", "source_hash", "TEXT NOT NULL DEFAULT ''");
        MigrationSupport.addColumnIfMissing(connection, "skill_contract", "trusted_source_hash", "TEXT NOT NULL DEFAULT ''");
        MigrationSupport.addColumnIfMissing(connection, "skill_contract", "trust_status", "TEXT NOT NULL DEFAULT 'unknown'");
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE INDEX IF NOT EXISTS idx_skill_contract_trust_status "
                    + "ON skill_contract(trust_status)");
            if (!MigrationSupport.schemaVersionExists(connection, MigrationRunner.V13)) {
                statement.executeUpdate("INSERT INTO schema_version(version, description, applied_at) VALUES ("
                        + MigrationRunner.V13 + ", 'V0.7.5 skill trust source hash schema', '"
                        + clock.now().toString() + "')");
            }
        }
   
    }
}
