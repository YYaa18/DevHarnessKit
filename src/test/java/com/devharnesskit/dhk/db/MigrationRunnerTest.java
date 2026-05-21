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
    void migrationV1IsIdempotent() throws Exception {
        PathUtil.createMemoryDirectories(tempDir);
        DbConnectionFactory factory = new DbConnectionFactory();
        MigrationRunner runner = new MigrationRunner();

        try (Connection connection = factory.open(tempDir)) {
            MigrationResult first = runner.migrate(connection, new FixedClock());
            MigrationResult second = runner.migrate(connection, new FixedClock());

            assertEquals(1, first.schemaVersion());
            assertEquals(1, second.schemaVersion());
            assertTrue(MigrationRunner.hasTable(connection, "schema_version"));
            assertTrue(MigrationRunner.hasTable(connection, "project"));
            assertTrue(MigrationRunner.hasTable(connection, "memory_item"));
            assertTrue(MigrationRunner.hasTable(connection, "checkpoint"));
            assertEquals(1, countSchemaVersionRows(connection));
            assertTrue(indexExists(connection, "idx_memory_item_project_status_confidence"));
            assertTrue(indexExists(connection, "idx_memory_item_project_module"));
            assertTrue(indexExists(connection, "idx_checkpoint_project_created"));
        }
    }

    private int countSchemaVersionRows(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM schema_version WHERE version = 1")) {
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
