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

import static org.junit.jupiter.api.Assumptions.assumeTrue;
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

    @Test
    void dbTestRejectsNonMysqlJdbcUrlsBeforeConnecting() {
        Harness harness = new Harness(tempDir);

        int exitCode = new CommandRouter().run(new String[]{
                "db", "test", "--jdbc-url", "jdbc:sqlite:test.db", "--user", "readonly"
        }, harness.context());

        assertEquals(ExitCodes.USAGE_ERROR, exitCode);
        assertTrue(harness.stderr().contains("Only jdbc:mysql:// URLs are allowed"));
    }

    @Test
    void liveMysqlSmokeRunsWhenEnvironmentIsConfigured() {
        String url = System.getenv("DHK_TEST_MYSQL_URL");
        String user = System.getenv("DHK_TEST_MYSQL_USER");
        String password = System.getenv("DHK_TEST_MYSQL_PASSWORD");
        assumeTrue(url != null && user != null && password != null
                && url.length() > 0 && user.length() > 0 && password.length() > 0);

        Harness test = new Harness(tempDir);
        int testExit = new CommandRouter().run(new String[]{
                "db", "test",
                "--jdbc-url", url,
                "--user", user,
                "--password-env", "DHK_TEST_MYSQL_PASSWORD"
        }, test.context());
        assertEquals(ExitCodes.SUCCESS, testExit);
        assertTrue(test.stdout().contains("db_test: ok"));

        Harness sql = new Harness(tempDir);
        int sqlExit = new CommandRouter().run(new String[]{
                "db", "sql",
                "--jdbc-url", url,
                "--user", user,
                "--password-env", "DHK_TEST_MYSQL_PASSWORD",
                "--sql", "SELECT 1 AS ok"
        }, sql.context());
        assertEquals(ExitCodes.SUCCESS, sqlExit);
        assertTrue(sql.stdout().contains("rows: 1"));

        Harness rejected = new Harness(tempDir);
        int rejectedExit = new CommandRouter().run(new String[]{
                "db", "sql",
                "--jdbc-url", url,
                "--user", user,
                "--password-env", "DHK_TEST_MYSQL_PASSWORD",
                "--sql", "DELETE FROM t_order"
        }, rejected.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, rejectedExit);
        assertTrue(rejected.stderr().contains("SQL rejected"));
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
