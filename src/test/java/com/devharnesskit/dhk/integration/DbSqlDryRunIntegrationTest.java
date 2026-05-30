package com.devharnesskit.dhk.integration;

import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.CommandRouter;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.util.Clock;
import com.devharnesskit.dhk.util.PathUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
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
    void dryRunReadsSqlFile() throws Exception {
        Path sqlFile = tempDir.resolve("query.sql");
        Files.write(sqlFile, "SELECT 1 AS ok".getBytes("UTF-8"));
        Harness harness = new Harness(tempDir);

        int exitCode = new CommandRouter().run(new String[]{
                "db", "sql", "--dry-run", "--sql-file", sqlFile.toString()
        }, harness.context());

        assertEquals(ExitCodes.SUCCESS, exitCode);
        assertTrue(harness.stdout().contains("sql_safety: ok"));
        assertTrue(harness.stdout().contains("SELECT 1 AS ok"));
    }

    @Test
    void dryRunCanReturnJson() {
        Harness harness = new Harness(tempDir);

        int exitCode = new CommandRouter().run(new String[]{
                "db", "sql", "--dry-run", "--json", "--sql", "SELECT 1"
        }, harness.context());

        assertEquals(ExitCodes.SUCCESS, exitCode);
        assertTrue(harness.stdout().contains("\"command\": \"db sql\""));
        assertTrue(harness.stdout().contains("\"status\": \"ok\""));
        assertTrue(harness.stdout().contains("\"dry_run\": true"));
        assertTrue(harness.stdout().contains("\"sql\": \"SELECT 1\""));
    }

    @Test
    void dryRunFormatJsonReturnsMachineReadableSuccess() {
        Harness harness = new Harness(tempDir);

        int exitCode = new CommandRouter().run(new String[]{
                "db", "sql", "--dry-run", "--format", "json", "--sql", "SELECT 1"
        }, harness.context());

        assertEquals(ExitCodes.SUCCESS, exitCode);
        assertTrue(harness.stdout().contains("\"command\": \"db sql\""));
        assertTrue(harness.stdout().contains("\"status\": \"ok\""));
        assertTrue(harness.stdout().contains("\"dry_run\": true"));
        assertTrue(harness.stdout().contains("\"sql\": \"SELECT 1\""));
        assertEquals("", harness.stderr());
    }

    @Test
    void policyCanBlockDbSqlEvenForDryRun() throws Exception {
        Path projectRoot = tempDir.resolve("demo");
        Files.createDirectories(PathUtil.devharnessDirectory(projectRoot));
        Files.write(PathUtil.devharnessPolicy(projectRoot), ("{\n"
                + "  \"mode\": \"strict\",\n"
                + "  \"forbidden_dhk_commands\": \"db sql\"\n"
                + "}\n").getBytes("UTF-8"));
        Harness harness = new Harness(tempDir);

        int exitCode = new CommandRouter().run(new String[]{
                "db", "sql",
                "--project-root", "demo",
                "--dry-run",
                "--sql", "SELECT 1"
        }, harness.context());

        assertEquals(ExitCodes.VALIDATION_ERROR, exitCode);
        assertTrue(harness.stderr().contains("Policy blocked db sql"));
        assertTrue(harness.stderr().contains("command is forbidden by policy"));
        assertTrue(harness.stderr().contains("next_command: dhk doctor --project-root"));
    }

    @Test
    void policyAllowsDbSqlWhenCommandIsAllowed() throws Exception {
        Path projectRoot = tempDir.resolve("demo");
        Files.createDirectories(PathUtil.devharnessDirectory(projectRoot));
        Files.write(PathUtil.devharnessPolicy(projectRoot), ("{\n"
                + "  \"mode\": \"strict\",\n"
                + "  \"allowed_dhk_commands\": \"db sql\",\n"
                + "  \"db_sql_requires_explicit_request\": \"false\"\n"
                + "}\n").getBytes("UTF-8"));
        Harness harness = new Harness(tempDir);

        int exitCode = new CommandRouter().run(new String[]{
                "db", "sql",
                "--project-root", "demo",
                "--dry-run",
                "--sql", "SELECT 1"
        }, harness.context());

        assertEquals(ExitCodes.SUCCESS, exitCode);
        assertTrue(harness.stdout().contains("sql_safety: ok"));
    }

    @Test
    void policyRequiresExplicitRiskAcknowledgementBeforeNonDryRunDbSql() throws Exception {
        Path projectRoot = tempDir.resolve("demo");
        Files.createDirectories(PathUtil.devharnessDirectory(projectRoot));
        Files.write(PathUtil.devharnessPolicy(projectRoot), ("{\n"
                + "  \"mode\": \"strict\",\n"
                + "  \"allowed_dhk_commands\": \"db sql\",\n"
                + "  \"db_sql_requires_explicit_request\": \"true\"\n"
                + "}\n").getBytes("UTF-8"));
        Harness harness = new Harness(tempDir);

        int exitCode = new CommandRouter().run(new String[]{
                "db", "sql",
                "--project-root", "demo",
                "--sql", "SELECT 1"
        }, harness.context());

        assertEquals(ExitCodes.VALIDATION_ERROR, exitCode);
        assertTrue(harness.stderr().contains("Policy blocked db sql"));
        assertTrue(harness.stderr().contains("--i-understand-db-readonly-risk"));
        assertTrue(harness.stderr().contains("next_command: dhk db sql --project-root"));
    }

    @Test
    void dryRunJsonReportsRejectedSql() {
        Harness harness = new Harness(tempDir);

        int exitCode = new CommandRouter().run(new String[]{
                "db", "sql", "--dry-run", "--json", "--sql", "DELETE FROM t_order"
        }, harness.context());

        assertEquals(ExitCodes.VALIDATION_ERROR, exitCode);
        assertTrue(harness.stdout().contains("\"command\": \"db sql\""));
        assertTrue(harness.stdout().contains("\"status\": \"rejected\""));
        assertTrue(harness.stdout().contains("\"reason\": \"high risk SQL pattern is not allowed\""));
        assertEquals("", harness.stderr());
    }

    @Test
    void dbSqlFormatJsonReportsSafetyRejection() {
        Harness harness = new Harness(tempDir);

        int exitCode = new CommandRouter().run(new String[]{
                "db", "sql", "--dry-run", "--format", "json", "--sql", "DELETE FROM t_order"
        }, harness.context());

        assertEquals(ExitCodes.VALIDATION_ERROR, exitCode);
        assertTrue(harness.stdout().contains("\"command\": \"db sql\""));
        assertTrue(harness.stdout().contains("\"status\": \"rejected\""));
        assertTrue(harness.stdout().contains("\"dry_run\": true"));
        assertTrue(harness.stdout().contains("\"reason\": \"high risk SQL pattern is not allowed\""));
        assertTrue(harness.stdout().contains("\"risk_warning\": \"SQL guard and JDBC read-only mode"));
        assertEquals("", harness.stderr());
    }

    @Test
    void dbSqlFormatJsonReportsConnectionUsageError() {
        Harness harness = new Harness(tempDir);

        int exitCode = new CommandRouter().run(new String[]{
                "db", "sql",
                "--format", "json",
                "--jdbc-url", "jdbc:mysql://localhost/demo",
                "--user", "readonly",
                "--sql", "SELECT 1"
        }, harness.context());

        assertEquals(ExitCodes.USAGE_ERROR, exitCode);
        assertTrue(harness.stdout().contains("\"command\": \"db sql\""));
        assertTrue(harness.stdout().contains("\"status\": \"error\""));
        assertTrue(harness.stdout().contains("\"dry_run\": false"));
        assertTrue(harness.stdout().contains("\"error\": \"Missing password source: use --password-env or --password-stdin\""));
        assertTrue(harness.stdout().contains("\"compatibility_hint\": \"\""));
        assertTrue(harness.stdout().contains("\"risk_warning\": \"SQL guard and JDBC read-only mode"));
        assertEquals("", harness.stderr());
    }

    @Test
    void dryRunJsonReportsVersionedCommentRejection() {
        Harness harness = new Harness(tempDir);

        int exitCode = new CommandRouter().run(new String[]{
                "db", "sql", "--dry-run", "--json",
                "--sql", "SELECT 1 /*!50000 INTO OUTFILE '/tmp/orders.txt' */"
        }, harness.context());

        assertEquals(ExitCodes.VALIDATION_ERROR, exitCode);
        assertTrue(harness.stdout().contains("\"command\": \"db sql\""));
        assertTrue(harness.stdout().contains("\"status\": \"rejected\""));
        assertTrue(harness.stdout().contains("\"reason\": \"versioned comments are not allowed\""));
        assertEquals("", harness.stderr());
    }

    @Test
    void dryRunKeepsSqlExecutableButRedactsPolicyOutput() throws Exception {
        Path projectRoot = tempDir.resolve("demo");
        Files.createDirectories(PathUtil.devharnessDirectory(projectRoot));
        Files.write(PathUtil.sensitivePolicy(projectRoot), ("{\n"
                + "  \"phone\": \"redact\"\n"
                + "}\n").getBytes("UTF-8"));
        Harness harness = new Harness(tempDir);

        int exitCode = new CommandRouter().run(new String[]{
                "db", "sql",
                "--project-root", "demo",
                "--dry-run",
                "--sql", "SELECT * FROM customer WHERE phone = '13800138000'"
        }, harness.context());

        assertEquals(ExitCodes.SUCCESS, exitCode);
        assertTrue(harness.stdout().contains("sql_safety: ok"));
        assertTrue(harness.stdout().contains("[REDACTED_PHONE]"));
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
    void dbTestJsonReportsUsageError() {
        Harness harness = new Harness(tempDir);

        int exitCode = new CommandRouter().run(new String[]{
                "db", "test", "--json", "--jdbc-url", "jdbc:sqlite:test.db", "--user", "readonly"
        }, harness.context());

        assertEquals(ExitCodes.USAGE_ERROR, exitCode);
        assertTrue(harness.stdout().contains("\"command\": \"db test\""));
        assertTrue(harness.stdout().contains("\"status\": \"error\""));
        assertTrue(harness.stdout().contains("\"error\": \"Only jdbc:mysql:// URLs are allowed\""));
        assertTrue(harness.stdout().contains("\"compatibility_hint\": \"\""));
        assertTrue(harness.stdout().contains("\"risk_warning\": \"SQL guard and JDBC read-only mode"));
        assertEquals("", harness.stderr());
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
                    com.devharnesskit.dhk.testsupport.Utf8HarnessSupport.printStream(out),
                    com.devharnesskit.dhk.testsupport.Utf8HarnessSupport.printStream(err),
                    new FixedClock()
            );
        }

        private String stdout() {
            return com.devharnesskit.dhk.testsupport.Utf8HarnessSupport.text(out);
        }

        private String stderr() {
            return com.devharnesskit.dhk.testsupport.Utf8HarnessSupport.text(err);
        }
    }

    private static final class FixedClock implements Clock {
        public Instant now() {
            return Instant.parse("2026-05-21T00:00:00Z");
        }
    }
}
