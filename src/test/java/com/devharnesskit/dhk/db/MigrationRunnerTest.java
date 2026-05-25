package com.devharnesskit.dhk.db;

import com.devharnesskit.dhk.util.Clock;
import com.devharnesskit.dhk.util.PathUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MigrationRunnerTest {
    @TempDir
    Path tempDir;

    @Test
    void migrationIsIdempotentThroughGoalV5() throws Exception {
        PathUtil.createMemoryDirectories(tempDir);
        DbConnectionFactory factory = new DbConnectionFactory();
        MigrationRunner runner = new MigrationRunner();

        try (Connection connection = factory.open(tempDir)) {
            MigrationResult first = runner.migrate(connection, new FixedClock());
            MigrationResult second = runner.migrate(connection, new FixedClock());

            assertEquals(MigrationRunner.V5, first.schemaVersion());
            assertEquals(MigrationRunner.V5, second.schemaVersion());
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
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V1));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V2));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V3));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V4));
            assertEquals(1, countSchemaVersionRows(connection, MigrationRunner.V5));
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
        }
    }

    private int countSchemaVersionRows(Connection connection, int version) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM schema_version WHERE version = " + version)) {
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

    private static final class FixedClock implements Clock {
        public Instant now() {
            return Instant.parse("2026-05-21T00:00:00Z");
        }
    }
}
