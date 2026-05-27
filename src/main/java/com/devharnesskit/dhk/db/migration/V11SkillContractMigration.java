package com.devharnesskit.dhk.db.migration;

import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.util.Clock;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class V11SkillContractMigration extends AbstractMigrationStep {
    public V11SkillContractMigration() {
        super(MigrationRunner.V11, "V0.4 skill contract schema");
    }

    public void apply(Connection connection, Clock clock) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS skill_contract ("
                    + "skill_key TEXT PRIMARY KEY,"
                    + "version TEXT NOT NULL,"
                    + "task_type TEXT NOT NULL,"
                    + "risk_level TEXT NOT NULL DEFAULT 'medium',"
                    + "mode TEXT NOT NULL DEFAULT 'strict',"
                    + "data_access_level TEXT NOT NULL,"
                    + "allowed_commands TEXT NOT NULL DEFAULT '',"
                    + "forbidden_commands TEXT NOT NULL DEFAULT '',"
                    + "contract_json TEXT NOT NULL,"
                    + "source_path TEXT NOT NULL DEFAULT '',"
                    + "trusted INTEGER NOT NULL DEFAULT 0,"
                    + "created_at TEXT NOT NULL,"
                    + "updated_at TEXT NOT NULL,"
                    + "CHECK (length(skill_key) > 0),"
                    + "CHECK (length(task_type) > 0),"
                    + "CHECK (data_access_level IN ('none', 'metadata', 'context', 'raw')),"
                    + "CHECK (trusted IN (0, 1))"
                    + ")");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_skill_contract_task_type "
                    + "ON skill_contract(task_type)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_skill_contract_access "
                    + "ON skill_contract(data_access_level)");
            if (!MigrationSupport.schemaVersionExists(connection, MigrationRunner.V11)) {
                statement.executeUpdate("INSERT INTO schema_version(version, description, applied_at) VALUES ("
                        + MigrationRunner.V11 + ", 'V0.7.1 skill contract schema', '"
                        + clock.now().toString() + "')");
            }
        }
   
    }
}
