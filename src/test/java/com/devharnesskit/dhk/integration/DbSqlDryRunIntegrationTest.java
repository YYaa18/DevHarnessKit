package com.devharnesskit.dhk.integration;

import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.CommandRouter;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.util.Clock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DbSqlDryRunIntegrationTest {
    @TempDir
    Path tempDir;

    @Test
    void dryRunAllowsReadonlySqlWithoutDbCredentials() {
        Harness harness = new Harness(tempDir);

        int exitCode = new CommandRouter().run(new String[]{
                "db", "sql", "--dry-run", "--sql", "SELECT 1"
        }, harness.context());

        assertEquals(ExitCodes.SUCCESS, exitCode);
        assertTrue(harness.stdout().contains("sql_safety: ok"));
        assertTrue(harness.stdout().contains("SELECT 1"));
    }

    @Test
    void dryRunRejectsWriteSql() {
        Harness harness = new Harness(tempDir);

        int exitCode = new CommandRouter().run(new String[]{
                "db", "sql", "--dry-run", "--sql", "UPDATE t SET a = 1"
        }, harness.context());

        assertEquals(ExitCodes.VALIDATION_ERROR, exitCode);
        assertTrue(harness.stderr().contains("SQL rejected"));
    }

    private static final class Harness {
        private final Path workingDirectory;
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();
        private final ByteArrayOutputStream err = new ByteArrayOutputStream();

        private Harness(Path workingDirectory) {
            this.workingDirectory = workingDirectory;
        }

        private CommandContext context() {
            return new CommandContext(
                    workingDirectory.toAbsolutePath().normalize(),
                    new PrintStream(out),
                    new PrintStream(err),
                    new FixedClock()
            );
        }

        private String stdout() {
            return out.toString();
        }

        private String stderr() {
            return err.toString();
        }
    }

    private static final class FixedClock implements Clock {
        public Instant now() {
            return Instant.parse("2026-05-21T00:00:00Z");
        }
    }
}
