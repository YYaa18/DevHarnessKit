package com.devharnesskit.dhk.db;

import com.devharnesskit.dhk.db.migration.DefaultMigrationStepCatalog;
import com.devharnesskit.dhk.db.migration.MigrationStep;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.repository.ProjectRepository;
import com.devharnesskit.dhk.util.Clock;
import com.devharnesskit.dhk.util.PathUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MigrationRunnerTest {
    @TempDir
    Path tempDir;

    @Test
    void defaultMigrationCatalogIsVersionOrderedThroughCurrentSchema() {
        int expected = 1;
        for (MigrationStep step : DefaultMigrationStepCatalog.steps()) {
            assertEquals(expected, step.version());
            assertFalse(step.description().isEmpty());
            expected++;
        }
        assertEquals(MigrationRunner.V16 + 1, expected);
    }

    @Test
    void migrationIsIdempotentThroughGoalExternalRefV16() throws Exception {
        PathUtil.createMemoryDirectories(tempDir);
        DbConnectionFactory factory = new DbConnectionFactory();
        MigrationRunner runner = new MigrationRunner();

        try (Connection connection = factory.open(tempDir)) {
            MigrationResult first = runner.migrate(connection, new FixedClock());
            MigrationResult second = runner.migrate(connection, new FixedClock());

            assertEquals(MigrationRunner.V16, first.schemaVersion());
            assertEquals(MigrationRunner.V16, second.schemaVersion());
            assertTrue(MigrationRunner.hasTable(connection, "schema_version"));
            assertTrue(MigrationRunner.hasTable(connection, "project"));
            assertTrue(MigrationRunner.hasTable(connection, "memory_item"));
            assertTrue(MigrationRunner.hasTable(connection, "checkpoint"));
            assertTrue(MigrationRunner.hasTable(connection, "workflow_template"));
            assertTrue(MigrationRunner.hasTable(connection, "workflow_phase_template"));
            assertTrue(MigrationRunner.hasTable(connection, "workflow_gate_template"));
            assertTrue(MigrationRunner.hasTable(connection, "workflow_run"));
            assertTrue(MigrationRunner.hasTable(connection, "workflow_phase_run"));
            assertTrue(MigrationRunner.hasTable(connection, "workflow_gate_run"));
            assertTrue(MigrationRunner.hasTable(connection, "workflow_event"));
            assertTrue(MigrationRunner.hasTable(connection, "workflow_artifact"));
            assertTrue(MigrationRunner.hasTable(connection, "workflow_memory_binding"));
            assertTrue(MigrationRunner.hasTable(connection, "workflow_checkpoint_binding"));
            assertTrue(MigrationRunner.hasTable(connection, "spec_change"));
            assertTrue(MigrationRunner.hasTable(connection, "spec_document"));
            assertTrue(MigrationRunner.hasTable(connection, "spec_task"));
            assertTrue(MigrationRunner.hasTable(connection, "spec_acceptance"));
            assertTrue(MigrationRunner.hasTable(connection, "workflow_spec_binding"));
            assertTrue(MigrationRunner.hasTable(connection, "spec_event"));
            assertTrue(MigrationRunner.hasTable(connection, "goal_run"));
            assertTrue(MigrationRunner.hasTable(connection, "goal_step"));
            assertTrue(MigrationRunner.hasTable(connection, "goal_event"));
            assertTrue(MigrationRunner.hasTable(connection, "goal_check"));
            assertTrue(MigrationRunner.hasTable(connection, "goal_artifact"));
            assertTrue(MigrationRunner.hasTable(connection, "code_graph_snapshot"));
            assertTrue(MigrationRunner.hasTable(connection, "code_graph_file"));
            assertTrue(MigrationRunner.hasTable(connection, "code_graph_node"));
            assertTrue(MigrationRunner.hasTable(connection, "code_graph_edge"));
            assertTrue(MigrationRunner.hasTable(connection, "code_graph_query_cache"));
            assertTrue(MigrationRunner.hasTable(connection, "goal_graph_binding"));
            assertTrue(MigrationRunner.hasTable(connection, "bdd_feature"));
            assertTrue(MigrationRunner.hasTable(connection, "bdd_scenario"));
            assertTrue(MigrationRunner.hasTable(connection, "bdd_step"));
            assertTrue(MigrationRunner.hasTable(connection, "bdd_binding"));
            assertTrue(MigrationRunner.hasTable(connection, "bdd_evidence"));
            assertTrue(MigrationRunner.hasTable(connection, "bdd_quality_issue"));
            assertTrue(MigrationRunner.hasTable(connection, "skill_contract"));
            assertTrue(MigrationRunner.hasTable(connection, "human_checkpoint"));
            assertTrue(MigrationRunner.hasTable(connection, "knowledge_candidate"));
            assertTrue(MigrationRunner.hasTable(connection, "interaction_request"));
            assertTrue(MigrationRunner.hasTable(connection, "growth_lesson"));
            assertTrue(columnExists(connection, "knowledge_candidate", "source_rule_id"));
            assertTrue(columnExists(connection, "knowledge_candidate", "source_pack_key"));
            assertTrue(columnExists(connection, "knowledge_candidate", "domain"));
            assertTrue(columnExists(connection, "knowledge_candidate", "severity"));
            assertTrue(columnExists(connection, "knowledge_candidate", "metadata"));
            assertTrue(columnExists(connection, "goal_check", "step_count_at_check"));
            assertTrue(columnExists(connection, "goal_check", "workspace_fingerprint"));
            assertTrue(columnExists(connection, "goal_check", "context_fingerprint"));
            assertTrue(columnExists(connection, "goal_check", "check_fingerprint"));
            assertTrue(columnExists(connection, "goal_run", "external_ref"));
            assertTrue(columnExists(connection, "code_graph_snapshot", "snapshot_key"));
            assertTrue(columnExists(connection, "code_graph_file", "protected_file"));
            assertTrue(columnExists(connection, "code_graph_node", "qualified_name"));
            assertTrue(columnExists(connection, "code_graph_edge", "confidence"));
            assertTrue(columnExists(connection, "code_graph_query_cache", "query_type"));
            assertTrue(columnExists(connection, "goal_graph_binding", "impact_hash"));
            assertTrue(columnExists(connection, "bdd_feature", "feature_key"));
            assertTrue(columnExists(connection, "bdd_feature", "module_name"));
            assertTrue(columnExists(connection, "bdd_scenario", "scenario_type"));
            assertTrue(columnExists(connection, "bdd_step", "step_type"));
            assertTrue(columnExists(connection, "bdd_binding", "binding_type"));
            assertTrue(columnExists(connection, "bdd_evidence", "evidence_type"));
            assertTrue(columnExists(connection, "bdd_quality_issue", "severity"));
            assertTrue(columnExists(connection, "skill_contract", "skill_key"));
            assertTrue(columnExists(connection, "skill_contract", "task_type"));
            assertTrue(columnExists(connection, "skill_contract", "data_access_level"));
            assertTrue(columnExists(connection, "skill_contract", "allowed_commands"));
            assertTrue(columnExists(connection, "skill_contract", "forbidden_commands"));
            assertTrue(columnExists(connection, "skill_contract", "source_hash"));
            assertTrue(columnExists(connection, "skill_contract", "trusted_source_hash"));
            assertTrue(columnExists(connection, "skill_contract", "trust_status"));
            assertTrue(columnExists(connection, "human_checkpoint", "checkpoint_type"));
            assertTrue(columnExists(connection, "human_checkpoint", "approver"));
            assertTrue(columnExists(connection, "human_checkpoint", "approved_at"));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V1));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V2));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V3));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V4));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V5));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V6));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V7));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V8));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V9));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V10));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V11));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V12));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V13));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V14));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V15));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V16));
            assertTrue(goalRunTableSql(connection).contains("context_export_failed"));
            assertTrue(indexExists(connection, "idx_memory_item_project_status_confidence"));
            assertTrue(indexExists(connection, "idx_memory_item_project_module"));
            assertTrue(indexExists(connection, "idx_checkpoint_project_created"));
            assertTrue(indexExists(connection, "idx_workflow_template_status"));
            assertTrue(indexExists(connection, "idx_workflow_run_project_status"));
            assertTrue(indexExists(connection, "idx_workflow_event_run"));
            assertTrue(indexExists(connection, "idx_workflow_artifact_run"));
            assertTrue(indexExists(connection, "idx_workflow_memory_binding_run"));
            assertTrue(indexExists(connection, "idx_workflow_checkpoint_binding_run"));
            assertTrue(indexExists(connection, "idx_spec_change_project_status"));
            assertTrue(indexExists(connection, "idx_spec_document_change_type"));
            assertTrue(indexExists(connection, "idx_workflow_spec_binding_run"));
            assertTrue(indexExists(connection, "idx_spec_event_change"));
            assertTrue(indexExists(connection, "idx_goal_run_project_status"));
            assertTrue(indexExists(connection, "idx_goal_step_goal_index"));
            assertTrue(indexExists(connection, "idx_goal_event_goal_created"));
            assertTrue(indexExists(connection, "idx_goal_check_goal_status"));
            assertTrue(indexExists(connection, "idx_goal_artifact_goal_type"));
            assertTrue(indexExists(connection, "idx_code_graph_snapshot_project_status"));
            assertTrue(indexExists(connection, "idx_code_graph_file_snapshot_path"));
            assertTrue(indexExists(connection, "idx_code_graph_node_snapshot_kind"));
            assertTrue(indexExists(connection, "idx_code_graph_edge_snapshot_source"));
            assertTrue(indexExists(connection, "idx_code_graph_edge_snapshot_target"));
            assertTrue(indexExists(connection, "idx_code_graph_query_cache_snapshot_type"));
            assertTrue(indexExists(connection, "idx_goal_graph_binding_goal"));
            assertTrue(indexExists(connection, "idx_goal_graph_binding_snapshot"));
            assertTrue(indexExists(connection, "idx_bdd_feature_project_status"));
            assertTrue(indexExists(connection, "idx_bdd_scenario_feature_status"));
            assertTrue(indexExists(connection, "idx_bdd_step_scenario_order"));
            assertTrue(indexExists(connection, "idx_bdd_binding_scenario_type"));
            assertTrue(indexExists(connection, "idx_bdd_evidence_scenario_status"));
            assertTrue(indexExists(connection, "idx_bdd_quality_issue_project_status"));
            assertTrue(indexExists(connection, "idx_skill_contract_task_type"));
            assertTrue(indexExists(connection, "idx_skill_contract_access"));
            assertTrue(indexExists(connection, "idx_skill_contract_trust_status"));
            assertTrue(indexExists(connection, "idx_human_checkpoint_goal_status"));
            assertTrue(indexExists(connection, "idx_human_checkpoint_goal_type_status"));
            assertTrue(indexExists(connection, "idx_knowledge_candidate_project_status"));
            assertTrue(indexExists(connection, "idx_knowledge_candidate_goal"));
            assertTrue(indexExists(connection, "idx_knowledge_candidate_rule"));
            assertTrue(indexExists(connection, "idx_interaction_request_project_status"));
            assertTrue(indexExists(connection, "idx_interaction_request_goal"));
            assertTrue(indexExists(connection, "idx_growth_lesson_project_status"));
        }
    }

    @Test
    void bddPathContractCreatesExpectedDirectories() throws Exception {
        PathUtil.createBddDirectories(tempDir);

        assertTrue(Files.isDirectory(PathUtil.bddDirectory(tempDir)));
        assertTrue(Files.isDirectory(PathUtil.bddFeaturesDirectory(tempDir)));
        assertTrue(Files.isDirectory(PathUtil.bddEvidenceDirectory(tempDir)));
        assertTrue(Files.isDirectory(PathUtil.bddExportsDirectory(tempDir)));
        assertEquals(tempDir.resolve(".agents/bdd/exports/BDD_CONTEXT.md"), PathUtil.bddContext(tempDir));
        assertEquals(tempDir.resolve(".agents/bdd/exports/BDD_EVIDENCE.md"), PathUtil.bddEvidence(tempDir));
        assertEquals(tempDir.resolve(".agents/bdd/exports/BDD_COVERAGE.md"), PathUtil.bddCoverage(tempDir));
        assertEquals(tempDir.resolve(".agents/bdd/exports/SCENARIO_IMPACT_MAP.md"),
                PathUtil.scenarioImpactMap(tempDir));
    }

    @Test
    void skillPathContractCreatesExpectedDirectoryAndContractPath() throws Exception {
        PathUtil.createSkillDirectories(tempDir);

        assertTrue(Files.isDirectory(PathUtil.skillsDirectory(tempDir)));
        assertEquals(tempDir.resolve(".agents/skills/devharness-strict"),
                PathUtil.skillDirectory(tempDir, "devharness-strict"));
        assertEquals(tempDir.resolve(".agents/skills/devharness-strict/contract.json"),
                PathUtil.skillContract(tempDir, "devharness-strict"));
    }

    @Test
    void migrationBacksUpExistingOldDatabaseBeforeUpgrade() throws Exception {
        PathUtil.createMemoryDirectories(tempDir);
        loadFixture("v1-project-minimal.sql");

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PathUtil.memoryDb(tempDir))) {
            MigrationResult result = new MigrationRunner().migrate(connection, new FixedClock());
            Project project = new ProjectRepository().findByKey(connection, "demo");

            assertEquals(MigrationRunner.V16, result.schemaVersion());
            assertTrue(result.backupPath().contains("pre-migration-v1-to-v16"));
            assertTrue(Files.isRegularFile(Paths.get(result.backupPath())));
            assertTrue(MigrationRunner.hasTable(connection, "goal_check"));
            assertTrue(columnExists(connection, "goal_check", "step_count_at_check"));
            assertTrue(columnExists(connection, "goal_check", "workspace_fingerprint"));
            assertTrue(MigrationRunner.hasTable(connection, "bdd_feature"));
            assertTrue(MigrationRunner.hasTable(connection, "skill_contract"));
            assertTrue(MigrationRunner.hasTable(connection, "human_checkpoint"));
            assertTrue(MigrationRunner.hasTable(connection, "knowledge_candidate"));
            assertEquals("Demo", project.projectName());
            assertEquals("java", project.language());
            assertEquals("unknown", project.framework());
            assertTrue(columnExists(connection, "project", "root_path"));
            assertTrue(columnExists(connection, "project", "database_type"));
        }
    }

    @Test
    void migrationUpgradesV2WorkflowFixtureToSpecAndGoalSchemasAndPreservesWorkflowRows() throws Exception {
        PathUtil.createMemoryDirectories(tempDir);
        loadFixture("v2-workflow-before-artifacts.sql");

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PathUtil.memoryDb(tempDir))) {
            MigrationResult result = new MigrationRunner().migrate(connection, new FixedClock());

            assertEquals(MigrationRunner.V16, result.schemaVersion());
            assertTrue(result.backupPath().contains("pre-migration-v2-to-v16"));
            assertTrue(Files.isRegularFile(Paths.get(result.backupPath())));
            assertTrue(MigrationRunner.hasTable(connection, "workflow_artifact"));
            assertTrue(MigrationRunner.hasTable(connection, "workflow_memory_binding"));
            assertTrue(MigrationRunner.hasTable(connection, "spec_change"));
            assertTrue(MigrationRunner.hasTable(connection, "goal_run"));
            assertEquals(1, countRows(connection, "workflow_template"));
            assertEquals(1, countRows(connection, "workflow_run"));
            assertEquals(1, countRows(connection, "workflow_phase_run"));
            assertEquals(1, countRows(connection, "workflow_gate_run"));
            assertEquals(1, countRowsWhere(connection, "workflow_run",
                    "run_key = 'fixture-run' AND status = 'running' AND current_phase_key = 'export_context'"));
            assertEquals(1, countRowsWhere(connection, "workflow_gate_run",
                    "run_key = 'fixture-run' AND gate_key = 'current_context_exists' AND status = 'passed'"));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V3));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V4));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V5));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V6));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V7));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V8));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V9));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V10));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V11));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V12));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V13));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V14));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V15));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V16));
        }
    }

    @Test
    void migrationUpgradesV4FixtureToGoalSchemaAndPreservesSpecRows() throws Exception {
        PathUtil.createMemoryDirectories(tempDir);
        loadFixture("v4-before-goal.sql");

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PathUtil.memoryDb(tempDir))) {
            MigrationResult result = new MigrationRunner().migrate(connection, new FixedClock());

            assertEquals(MigrationRunner.V16, result.schemaVersion());
            assertTrue(result.backupPath().contains("pre-migration-v4-to-v16"));
            assertTrue(Files.isRegularFile(Paths.get(result.backupPath())));
            assertTrue(MigrationRunner.hasTable(connection, "goal_run"));
            assertTrue(MigrationRunner.hasTable(connection, "goal_check"));
            assertEquals(1, countRows(connection, "spec_change"));
            assertEquals(1, countRows(connection, "spec_document"));
            assertEquals(1, countRows(connection, "spec_task"));
            assertEquals(1, countRows(connection, "spec_acceptance"));
            assertEquals(1, countRows(connection, "workflow_spec_binding"));
            assertEquals(1, countRows(connection, "spec_event"));
            assertEquals(1, countRowsWhere(connection, "spec_task",
                    "change_key = 'fixture-change' AND task_key = 'T001' AND status = 'done'"));
            assertEquals(1, countRowsWhere(connection, "spec_acceptance",
                    "change_key = 'fixture-change' AND acceptance_key = 'A001' AND status = 'passed'"));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V5));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V6));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V7));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V8));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V9));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V10));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V11));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V12));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V13));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V14));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V15));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V16));
            assertTrue(columnExists(connection, "goal_check", "step_count_at_check"));
            assertTrue(columnExists(connection, "goal_check", "workspace_fingerprint"));
            assertTrue(columnExists(connection, "goal_run", "external_ref"));
        }
    }

    @Test
    void migrationUpgradesV5GoalFixtureToStepCountSchemaAndPreservesChecks() throws Exception {
        PathUtil.createMemoryDirectories(tempDir);
        loadFixture("v5-goal-before-step-count.sql");

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PathUtil.memoryDb(tempDir))) {
            MigrationResult result = new MigrationRunner().migrate(connection, new FixedClock());

            assertEquals(MigrationRunner.V16, result.schemaVersion());
            assertTrue(result.backupPath().contains("pre-migration-v5-to-v16"));
            assertTrue(Files.isRegularFile(Paths.get(result.backupPath())));
            assertTrue(columnExists(connection, "goal_check", "step_count_at_check"));
            assertTrue(columnExists(connection, "goal_check", "workspace_fingerprint"));
            assertTrue(columnExists(connection, "goal_check", "context_fingerprint"));
            assertTrue(columnExists(connection, "goal_check", "check_fingerprint"));
            assertEquals(1, countRowsWhere(connection, "goal_check",
                    "goal_key = 'fixture-goal' AND check_key = 'sensitive' AND step_count_at_check = 0"));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V5));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V6));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V7));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V8));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V9));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V10));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V11));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V12));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V13));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V14));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V15));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V16));
            assertTrue(goalRunTableSql(connection).contains("context_export_failed"));
            assertTrue(columnExists(connection, "goal_run", "external_ref"));
            assertEquals(1, countRowsWhere(connection, "goal_run",
                    "goal_key = 'fixture-goal' AND external_ref = ''"));
            assertEquals("goal_run", foreignKeyTable(connection, "goal_check"));
            assertTrue(MigrationRunner.hasTable(connection, "code_graph_snapshot"));
            assertTrue(MigrationRunner.hasTable(connection, "goal_graph_binding"));
            assertTrue(MigrationRunner.hasTable(connection, "human_checkpoint"));
        }
    }

    @Test
    void migrationUpgradesV9GraphFixtureToBddSkillAndHumanCheckpointSchemas() throws Exception {
        PathUtil.createMemoryDirectories(tempDir);
        loadFixture("v9-graph-before-bdd.sql");

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PathUtil.memoryDb(tempDir))) {
            MigrationResult result = new MigrationRunner().migrate(connection, new FixedClock());

            assertEquals(MigrationRunner.V16, result.schemaVersion());
            assertTrue(result.backupPath().contains("pre-migration-v9-to-v16"));
            assertTrue(Files.isRegularFile(Paths.get(result.backupPath())));
            assertEquals(1, countRows(connection, "code_graph_snapshot"));
            assertEquals(1, countRows(connection, "code_graph_file"));
            assertEquals(1, countRows(connection, "code_graph_node"));
            assertEquals(1, countRows(connection, "code_graph_edge"));
            assertEquals(1, countRows(connection, "code_graph_query_cache"));
            assertEquals(1, countRows(connection, "goal_graph_binding"));
            assertEquals(1, countRowsWhere(connection, "code_graph_snapshot",
                    "snapshot_key = 'fixture-snapshot' AND status = 'completed'"));
            assertEquals(1, countRowsWhere(connection, "goal_graph_binding",
                    "goal_key = 'fixture-goal' AND impact_hash = 'impact-hash'"));
            assertTrue(MigrationRunner.hasTable(connection, "bdd_feature"));
            assertTrue(MigrationRunner.hasTable(connection, "skill_contract"));
            assertTrue(MigrationRunner.hasTable(connection, "human_checkpoint"));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V10));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V11));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V12));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V13));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V14));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V15));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V16));
        }
    }

    @Test
    void migrationUpgradesV10BddFixtureToSkillAndHumanCheckpointSchemas() throws Exception {
        PathUtil.createMemoryDirectories(tempDir);
        loadFixture("v10-bdd-before-skill.sql");

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PathUtil.memoryDb(tempDir))) {
            MigrationResult result = new MigrationRunner().migrate(connection, new FixedClock());

            assertEquals(MigrationRunner.V16, result.schemaVersion());
            assertTrue(result.backupPath().contains("pre-migration-v10-to-v16"));
            assertTrue(Files.isRegularFile(Paths.get(result.backupPath())));
            assertEquals(1, countRows(connection, "bdd_feature"));
            assertEquals(1, countRows(connection, "bdd_scenario"));
            assertEquals(1, countRows(connection, "bdd_step"));
            assertEquals(1, countRows(connection, "bdd_binding"));
            assertEquals(1, countRows(connection, "bdd_evidence"));
            assertEquals(1, countRows(connection, "bdd_quality_issue"));
            assertEquals(1, countRowsWhere(connection, "bdd_scenario",
                    "scenario_key = 'fixture-scenario' AND status = 'verified'"));
            assertEquals(1, countRowsWhere(connection, "bdd_evidence",
                    "scenario_key = 'fixture-scenario' AND status = 'passed'"));
            assertTrue(MigrationRunner.hasTable(connection, "skill_contract"));
            assertTrue(MigrationRunner.hasTable(connection, "human_checkpoint"));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V11));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V12));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V13));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V14));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V15));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V16));
        }
    }

    @Test
    void migrationUpgradesV11SkillFixtureToTrustSourceHashSchema() throws Exception {
        PathUtil.createMemoryDirectories(tempDir);
        loadFixture("v11-skill-before-trust.sql");

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PathUtil.memoryDb(tempDir))) {
            MigrationResult result = new MigrationRunner().migrate(connection, new FixedClock());

            assertEquals(MigrationRunner.V16, result.schemaVersion());
            assertTrue(result.backupPath().contains("pre-migration-v11-to-v16"));
            assertTrue(Files.isRegularFile(Paths.get(result.backupPath())));
            assertEquals(1, countRows(connection, "skill_contract"));
            assertEquals(1, countRowsWhere(connection, "skill_contract",
                    "skill_key = 'fixture-skill' AND trusted = 1 AND trust_status = 'unknown'"));
            assertTrue(columnExists(connection, "skill_contract", "source_hash"));
            assertTrue(columnExists(connection, "skill_contract", "trusted_source_hash"));
            assertTrue(columnExists(connection, "skill_contract", "trust_status"));
            assertTrue(MigrationRunner.hasTable(connection, "human_checkpoint"));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V12));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V13));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V14));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V15));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V16));
        }
    }

    @Test
    void migrationUpgradesV12HumanCheckpointFixtureToSkillTrustSchema() throws Exception {
        PathUtil.createMemoryDirectories(tempDir);
        loadFixture("v12-human-checkpoint-before-skill-trust.sql");

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PathUtil.memoryDb(tempDir))) {
            MigrationResult result = new MigrationRunner().migrate(connection, new FixedClock());

            assertEquals(MigrationRunner.V16, result.schemaVersion());
            assertTrue(result.backupPath().contains("pre-migration-v12-to-v16"));
            assertTrue(Files.isRegularFile(Paths.get(result.backupPath())));
            assertEquals(1, countRows(connection, "human_checkpoint"));
            assertEquals(1, countRowsWhere(connection, "human_checkpoint",
                    "goal_key = 'fixture-goal' AND status = 'approved' AND approver = 'lead'"));
            assertEquals(1, countRowsWhere(connection, "skill_contract",
                    "skill_key = 'fixture-skill' AND trust_status = 'unknown'"));
            assertTrue(columnExists(connection, "skill_contract", "source_hash"));
            assertTrue(columnExists(connection, "skill_contract", "trusted_source_hash"));
            assertTrue(columnExists(connection, "skill_contract", "trust_status"));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V13));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V14));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V15));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V16));
        }
    }

    @Test
    void migrationUpgradesV13CurrentFixtureToBriefLifecycleSchemaThenStaysIdempotent() throws Exception {
        PathUtil.createMemoryDirectories(tempDir);
        loadFixture("v13-current-minimal.sql");

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PathUtil.memoryDb(tempDir))) {
            MigrationRunner runner = new MigrationRunner();
            MigrationResult first = runner.migrate(connection, new FixedClock());
            MigrationResult second = runner.migrate(connection, new FixedClock());

            assertEquals(MigrationRunner.V16, first.schemaVersion());
            assertEquals(MigrationRunner.V16, second.schemaVersion());
            assertTrue(first.backupPath().contains("pre-migration-v13-to-v16"));
            assertEquals("", second.backupPath());
            assertTrue(MigrationRunner.hasTable(connection, "project"));
            assertTrue(MigrationRunner.hasTable(connection, "goal_check"));
            assertTrue(MigrationRunner.hasTable(connection, "code_graph_snapshot"));
            assertTrue(MigrationRunner.hasTable(connection, "bdd_feature"));
            assertTrue(MigrationRunner.hasTable(connection, "skill_contract"));
            assertTrue(MigrationRunner.hasTable(connection, "human_checkpoint"));
            assertTrue(MigrationRunner.hasTable(connection, "knowledge_candidate"));
            assertTrue(MigrationRunner.hasTable(connection, "interaction_request"));
            assertTrue(MigrationRunner.hasTable(connection, "growth_lesson"));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V13));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V14));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V15));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V16));
        }
    }

    @Test
    void migrationImportsLegacyBriefLifecycleTsvStoresIntoSQLite() throws Exception {
        PathUtil.createMemoryDirectories(tempDir);
        Files.createDirectories(PathUtil.devharnessBriefsDirectory(tempDir));
        Files.write(PathUtil.knowledgeCandidatesStore(tempDir), (
                "kc-legacy\tgoal-legacy\tgrowth_lesson_candidate\tLegacy title\tLegacy summary\t"
                        + "goal:legacy\tgrowth\tmedium\ttrue\tpassed\tdraft\n").getBytes("UTF-8"));
        Files.write(PathUtil.interactionRequests(tempDir), (
                "interaction-legacy\tgoal-legacy\tpre_work\tclarification\tblocking\t"
                        + "Question\tWhy\tYes|No\tYes\ttrue\topen\t\n").getBytes("UTF-8"));
        Files.write(PathUtil.growthLessonsStore(tempDir), (
                "growth-legacy\tkc-legacy\tLesson title\tLesson summary\tdraft\ttrue\n").getBytes("UTF-8"));

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PathUtil.memoryDb(tempDir))) {
            MigrationResult result = new MigrationRunner().migrate(connection, new FixedClock());

            assertEquals(MigrationRunner.V16, result.schemaVersion());
            assertEquals(1, countRowsWhere(connection, "knowledge_candidate",
                    "candidate_id = 'kc-legacy' AND title = 'Legacy title' AND status = 'draft'"));
            assertEquals(1, countRowsWhere(connection, "interaction_request",
                    "request_id = 'interaction-legacy' AND interaction_type = 'clarification' AND status = 'open'"));
            assertEquals(1, countRowsWhere(connection, "growth_lesson",
                    "lesson_id = 'growth-legacy' AND source_candidate_id = 'kc-legacy' AND advisory_only = 1"));
        }
    }

    @Test
    void migrationRollsBackAppliedStepsWhenLaterStepFails() throws Exception {
        PathUtil.createMemoryDirectories(tempDir);

        MigrationStep first = new MigrationStep() {
            public int version() {
                return 1;
            }

            public String description() {
                return "rollback fixture first step";
            }

            public void apply(Connection connection, Clock clock) throws SQLException {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("CREATE TABLE rollback_marker (id INTEGER PRIMARY KEY)");
                    statement.execute("INSERT INTO rollback_marker(id) VALUES (1)");
                }
            }
        };
        MigrationStep failing = new MigrationStep() {
            public int version() {
                return 2;
            }

            public String description() {
                return "rollback fixture failing step";
            }

            public void apply(Connection connection, Clock clock) throws SQLException {
                throw new SQLException("intentional migration failure");
            }
        };

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PathUtil.memoryDb(tempDir))) {
            SQLException ex = assertThrows(SQLException.class,
                    () -> new MigrationRunner(Arrays.asList(first, failing)).migrate(connection, new FixedClock()));

            assertTrue(ex.getMessage().contains("intentional migration failure"));
            assertFalse(MigrationRunner.hasTable(connection, "rollback_marker"));
        }
    }

    @Test
    void migrationFailsBeforeSchemaChangesWhenPreMigrationBackupCannotBeCreated() throws Exception {
        PathUtil.createMemoryDirectories(tempDir);
        loadFixture("v1-project-minimal.sql");
        Files.write(PathUtil.backupsDirectory(tempDir), "not a directory".getBytes("UTF-8"));

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PathUtil.memoryDb(tempDir))) {
            SQLException ex = assertThrows(SQLException.class,
                    () -> new MigrationRunner().migrate(connection, new FixedClock()));

            assertTrue(ex.getMessage().contains("Failed to create pre-migration backup"));
            assertEquals(MigrationRunner.V1, MigrationRunner.currentSchemaVersion(connection));
            assertFalse(MigrationRunner.hasTable(connection, "goal_check"));
            assertTrue(Files.isRegularFile(PathUtil.memoryDb(tempDir)));
        }
    }

    private void loadFixture(String name) throws Exception {
        try (InputStream input = MigrationRunnerTest.class.getResourceAsStream("/fixtures/migration/" + name)) {
            assertTrue(input != null);
            String sql = new String(readAll(input), "UTF-8");
            try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PathUtil.memoryDb(tempDir));
                 Statement statement = connection.createStatement()) {
                for (String raw : sql.split(";")) {
                    String command = raw.trim();
                    if (command.length() > 0) {
                        statement.execute(command);
                    }
                }
            }
        }
    }

    private byte[] readAll(InputStream input) throws Exception {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = input.read(buffer)) >= 0) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private int countSchemaVersionRows(Connection connection, int version) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM schema_version WHERE version = " + version)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private int countRows(Connection connection, String tableName) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private int countRowsWhere(Connection connection, String tableName, String where) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + tableName + " WHERE " + where)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private boolean indexExists(Connection connection, String indexName) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT name FROM sqlite_master WHERE type = 'index' AND name = '" + indexName + "'")) {
            return resultSet.next();
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (resultSet.next()) {
                if (columnName.equals(resultSet.getString("name"))) {
                    return true;
                }
            }
            return false;
        }
    }

    private String goalRunTableSql(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = 'goal_run'")) {
            resultSet.next();
            return resultSet.getString(1);
        }
    }

    private String foreignKeyTable(Connection connection, String tableName) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA foreign_key_list(" + tableName + ")")) {
            resultSet.next();
            return resultSet.getString("table");
        }
    }

    private static final class FixedClock implements Clock {
        public Instant now() {
            return Instant.parse("2026-05-21T00:00:00Z");
        }
    }
}
