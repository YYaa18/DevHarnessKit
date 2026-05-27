package com.devharnesskit.dhk.db.migration;

import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.util.Clock;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class V10BddAcceptanceMigration extends AbstractMigrationStep {
    public V10BddAcceptanceMigration() {
        super(MigrationRunner.V10, "V0.4 BDD acceptance schema");
    }

    public void apply(Connection connection, Clock clock) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS bdd_feature ("
                    + "feature_key TEXT PRIMARY KEY,"
                    + "project_key TEXT NOT NULL,"
                    + "module_name TEXT NOT NULL DEFAULT 'global',"
                    + "title TEXT NOT NULL,"
                    + "description TEXT NOT NULL DEFAULT '',"
                    + "tags TEXT NOT NULL DEFAULT '',"
                    + "status TEXT NOT NULL DEFAULT 'draft',"
                    + "source_kind TEXT NOT NULL DEFAULT 'manual',"
                    + "created_at TEXT NOT NULL,"
                    + "updated_at TEXT NOT NULL,"
                    + "CHECK (status IN ('draft', 'active', 'deprecated', 'archived')),"
                    + "FOREIGN KEY (project_key) REFERENCES project(project_key)"
                    + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS bdd_scenario ("
                    + "scenario_key TEXT PRIMARY KEY,"
                    + "feature_key TEXT NOT NULL,"
                    + "project_key TEXT NOT NULL,"
                    + "title TEXT NOT NULL,"
                    + "description TEXT NOT NULL DEFAULT '',"
                    + "scenario_type TEXT NOT NULL DEFAULT 'acceptance',"
                    + "priority TEXT NOT NULL DEFAULT 'normal',"
                    + "status TEXT NOT NULL DEFAULT 'draft',"
                    + "tags TEXT NOT NULL DEFAULT '',"
                    + "created_at TEXT NOT NULL,"
                    + "updated_at TEXT NOT NULL,"
                    + "CHECK (scenario_type IN ('acceptance', 'edge_case', 'regression', 'manual', 'exploratory')),"
                    + "CHECK (priority IN ('low', 'normal', 'high', 'critical')),"
                    + "CHECK (status IN ('draft', 'active', 'implemented', 'verified', 'blocked', 'deprecated', 'archived')),"
                    + "FOREIGN KEY (feature_key) REFERENCES bdd_feature(feature_key),"
                    + "FOREIGN KEY (project_key) REFERENCES project(project_key)"
                    + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS bdd_step ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "scenario_key TEXT NOT NULL,"
                    + "step_order INTEGER NOT NULL,"
                    + "step_type TEXT NOT NULL,"
                    + "step_text TEXT NOT NULL,"
                    + "normalized_text TEXT NOT NULL DEFAULT '',"
                    + "created_at TEXT NOT NULL,"
                    + "CHECK (step_order > 0),"
                    + "CHECK (step_type IN ('given', 'when', 'then', 'and', 'but')),"
                    + "UNIQUE(scenario_key, step_order),"
                    + "FOREIGN KEY (scenario_key) REFERENCES bdd_scenario(scenario_key)"
                    + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS bdd_binding ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "scenario_key TEXT NOT NULL,"
                    + "binding_type TEXT NOT NULL,"
                    + "binding_key TEXT NOT NULL,"
                    + "relation TEXT NOT NULL DEFAULT '',"
                    + "metadata TEXT NOT NULL DEFAULT '',"
                    + "created_at TEXT NOT NULL,"
                    + "CHECK (binding_type IN ('spec_acceptance', 'spec_task', 'goal', 'workflow',"
                    + "'graph', 'file', 'symbol', 'sql_table', 'test')),"
                    + "UNIQUE(scenario_key, binding_type, binding_key, relation),"
                    + "FOREIGN KEY (scenario_key) REFERENCES bdd_scenario(scenario_key)"
                    + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS bdd_evidence ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "scenario_key TEXT NOT NULL,"
                    + "goal_key TEXT NOT NULL DEFAULT '',"
                    + "evidence_type TEXT NOT NULL DEFAULT 'manual',"
                    + "status TEXT NOT NULL DEFAULT 'pending',"
                    + "evidence_path TEXT NOT NULL DEFAULT '',"
                    + "summary TEXT NOT NULL DEFAULT '',"
                    + "command TEXT NOT NULL DEFAULT '',"
                    + "created_at TEXT NOT NULL,"
                    + "updated_at TEXT NOT NULL,"
                    + "CHECK (status IN ('pending', 'passed', 'failed', 'skipped', 'waived')),"
                    + "FOREIGN KEY (scenario_key) REFERENCES bdd_scenario(scenario_key)"
                    + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS bdd_quality_issue ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "project_key TEXT NOT NULL,"
                    + "feature_key TEXT NOT NULL DEFAULT '',"
                    + "scenario_key TEXT NOT NULL DEFAULT '',"
                    + "issue_type TEXT NOT NULL,"
                    + "severity TEXT NOT NULL DEFAULT 'warning',"
                    + "status TEXT NOT NULL DEFAULT 'open',"
                    + "message TEXT NOT NULL,"
                    + "created_at TEXT NOT NULL,"
                    + "CHECK (severity IN ('info', 'warning', 'error')),"
                    + "CHECK (status IN ('open', 'resolved', 'waived')),"
                    + "FOREIGN KEY (project_key) REFERENCES project(project_key)"
                    + ")");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_bdd_feature_project_status "
                    + "ON bdd_feature(project_key, status)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_bdd_feature_project_module "
                    + "ON bdd_feature(project_key, module_name)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_bdd_scenario_feature_status "
                    + "ON bdd_scenario(feature_key, status)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_bdd_scenario_project_status "
                    + "ON bdd_scenario(project_key, status)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_bdd_step_scenario_order "
                    + "ON bdd_step(scenario_key, step_order)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_bdd_binding_scenario_type "
                    + "ON bdd_binding(scenario_key, binding_type)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_bdd_binding_type_key "
                    + "ON bdd_binding(binding_type, binding_key)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_bdd_evidence_scenario_status "
                    + "ON bdd_evidence(scenario_key, status)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_bdd_evidence_goal_status "
                    + "ON bdd_evidence(goal_key, status)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_bdd_quality_issue_scenario_severity "
                    + "ON bdd_quality_issue(scenario_key, severity)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_bdd_quality_issue_project_status "
                    + "ON bdd_quality_issue(project_key, status)");
            if (!MigrationSupport.schemaVersionExists(connection, MigrationRunner.V10)) {
                statement.executeUpdate("INSERT INTO schema_version(version, description, applied_at) VALUES ("
                        + MigrationRunner.V10 + ", 'V0.6.1 BDD specification schema', '"
                        + clock.now().toString() + "')");
            }
        }
   
    }
}
