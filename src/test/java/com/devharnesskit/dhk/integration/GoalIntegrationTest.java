package com.devharnesskit.dhk.integration;

import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.CommandRouter;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.util.Clock;
import com.devharnesskit.dhk.util.PathUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GoalIntegrationTest {
    @TempDir
    Path tempDir;

    @Test
    void goalStartNextStepStatusAndExportCreateGoalContext() throws Exception {
        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "demo",
                "--profile", "java-api-change",
                "--task", "Implement order query API",
                "--module", "order",
                "--mode", "api",
                "--condition", "compile and focused tests pass"
        }, start.context());

        Path root = tempDir.resolve("demo");
        Path goalContext = PathUtil.goalContext(root);
        String goalKey = firstValue(start.stdout(), "goal_key: ");

        assertEquals(ExitCodes.SUCCESS, startExit);
        assertTrue(goalKey.length() > 0);
        assertTrue(start.stdout().contains("workflow_run:"));
        assertTrue(start.stdout().contains("spec_change:"));
        assertTrue(start.stdout().contains("current_action: inspect_existing_code"));
        assertTrue(Files.isRegularFile(goalContext));
        String initialContext = new String(Files.readAllBytes(goalContext), "UTF-8");
        assertTrue(initialContext.contains("# GOAL_CONTEXT"));
        assertTrue(initialContext.contains("<current-action>"));
        assertTrue(initialContext.contains("inspect_existing_code"));
        assertTrue(Files.isRegularFile(PathUtil.currentContext(root)));
        assertTrue(Files.isRegularFile(PathUtil.workflowContext(root)));
        assertTrue(Files.isRegularFile(PathUtil.specContext(root)));

        assertGoalRows(root, goalKey);

        Harness next = new Harness(tempDir);
        int nextExit = new CommandRouter().run(new String[]{
                "goal", "next", "--project-root", "demo", "--goal", goalKey
        }, next.context());
        assertEquals(ExitCodes.SUCCESS, nextExit);
        assertTrue(next.stdout().contains("current_action: inspect_existing_code"));
        assertTrue(next.stdout().contains("required_evidence:"));

        Harness step = new Harness(tempDir);
        int stepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", "demo",
                "--goal", goalKey,
                "--summary", "Inspected existing controller/service/mapper/tests",
                "--changed-files", "",
                "--evidence", "existing_controller,existing_service,existing_mapper,existing_tests"
        }, step.context());
        assertEquals(ExitCodes.SUCCESS, stepExit);
        assertTrue(step.stdout().contains("step_id: 1"));
        assertTrue(step.stdout().contains("current_action: create_change_plan"));

        Harness status = new Harness(tempDir);
        int statusExit = new CommandRouter().run(new String[]{
                "goal", "status", "--project-root", "demo", "--goal", goalKey
        }, status.context());
        assertEquals(ExitCodes.SUCCESS, statusExit);
        assertTrue(status.stdout().contains("step_count: 1"));
        assertTrue(status.stdout().contains("current_action: create_change_plan"));

        Harness export = new Harness(tempDir);
        int exportExit = new CommandRouter().run(new String[]{
                "goal", "export", "--project-root", "demo", "--goal", goalKey
        }, export.context());
        assertEquals(ExitCodes.SUCCESS, exportExit);
        assertTrue(export.stdout().contains("context_path: " + goalContext));

        Harness evaluateBeforeChecks = new Harness(tempDir);
        int evaluateBeforeChecksExit = new CommandRouter().run(new String[]{
                "goal", "evaluate", "--project-root", "demo", "--goal", goalKey
        }, evaluateBeforeChecks.context());
        assertEquals(ExitCodes.SUCCESS, evaluateBeforeChecksExit);
        assertTrue(evaluateBeforeChecks.stdout().contains("decision: not_ready"));
        assertTrue(evaluateBeforeChecks.stdout().contains("check compile is pending"));

        Harness check = new Harness(tempDir);
        int checkExit = new CommandRouter().run(new String[]{
                "goal", "check", "--project-root", "demo", "--goal", goalKey, "--all"
        }, check.context());
        assertEquals(ExitCodes.SUCCESS, checkExit);
        assertTrue(check.stdout().contains("check_key: compile"));
        assertTrue(check.stdout().contains("status: skipped"));
        assertTrue(check.stdout().contains("check_key: sensitive"));
        assertTrue(Files.isRegularFile(PathUtil.goalCheckArtifactsDirectory(root, goalKey).resolve("compile.log")));

        Harness evaluateReady = new Harness(tempDir);
        int evaluateReadyExit = new CommandRouter().run(new String[]{
                "goal", "evaluate", "--project-root", "demo", "--goal", goalKey
        }, evaluateReady.context());
        assertEquals(ExitCodes.SUCCESS, evaluateReadyExit);
        assertTrue(evaluateReady.stdout().contains("decision: ready_to_complete"));
        assertTrue(evaluateReady.stdout().contains("next_command: dhk goal complete --goal " + goalKey));

        Harness complete = new Harness(tempDir);
        int completeExit = new CommandRouter().run(new String[]{
                "goal", "complete", "--project-root", "demo", "--goal", goalKey
        }, complete.context());
        assertEquals(ExitCodes.SUCCESS, completeExit);
        assertTrue(complete.stdout().contains("status: completed"));
        assertTrue(complete.stdout().contains("checkpoint_id: 1"));
        assertTrue(Files.isRegularFile(PathUtil.goalSummary(root)));
        String summary = new String(Files.readAllBytes(PathUtil.goalSummary(root)), "UTF-8");
        assertTrue(summary.contains("# GOAL_SUMMARY"));
        assertTrue(summary.contains("checkpoint_id: 1"));
        assertCompletedRows(root, goalKey);
    }

    @Test
    void goalUsesConfiguredProfileCheckPolicyAndJsonOutput() throws Exception {
        Path root = tempDir.resolve("demo");
        Files.createDirectories(PathUtil.goalProfilesDirectory(root));
        Files.write(PathUtil.goalProfile(root, "custom-api"), ("{\n"
                + "  \"workflow_key\": \"api-change\",\n"
                + "  \"requires_spec\": \"false\",\n"
                + "  \"default_mode\": \"api\",\n"
                + "  \"actions\": \"custom_inspect,verify\"\n"
                + "}\n").getBytes("UTF-8"));
        Files.write(PathUtil.goalCheckPolicy(root), ("{\n"
                + "  \"required_checks\": \"sensitive\",\n"
                + "  \"fail_pending_hard_gates\": \"true\"\n"
                + "}\n").getBytes("UTF-8"));

        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "demo",
                "--profile", "custom-api",
                "--task", "Configured goal",
                "--module", "order"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");
        assertTrue(start.stdout().contains("spec_change: "));
        assertTrue(start.stdout().contains("current_action: custom_inspect"));

        Harness statusJson = new Harness(tempDir);
        int statusExit = new CommandRouter().run(new String[]{
                "goal", "status", "--project-root", "demo", "--goal", goalKey, "--json"
        }, statusJson.context());
        assertEquals(ExitCodes.SUCCESS, statusExit);
        assertTrue(statusJson.stdout().contains("\"command\": \"goal status\""));
        assertTrue(statusJson.stdout().contains("\"current_action\": \"custom_inspect\""));

        Harness checkJson = new Harness(tempDir);
        int checkExit = new CommandRouter().run(new String[]{
                "goal", "check", "--project-root", "demo", "--goal", goalKey, "--all", "--json"
        }, checkJson.context());
        assertEquals(ExitCodes.SUCCESS, checkExit);
        assertTrue(checkJson.stdout().contains("\"count\": 1"));
        assertTrue(checkJson.stdout().contains("\"check_key\": \"sensitive\""));

        Harness evaluateJson = new Harness(tempDir);
        int evaluateExit = new CommandRouter().run(new String[]{
                "goal", "evaluate", "--project-root", "demo", "--goal", goalKey, "--json"
        }, evaluateJson.context());
        assertEquals(ExitCodes.SUCCESS, evaluateExit);
        assertTrue(evaluateJson.stdout().contains("\"decision\": \"ready_to_complete\""));
    }

    private void assertGoalRows(Path root, String goalKey) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PathUtil.memoryDb(root));
             Statement statement = connection.createStatement()) {
            assertEquals(1, count(statement, "SELECT COUNT(*) FROM goal_run WHERE goal_key = '" + goalKey + "'"));
            assertEquals(1, count(statement, "SELECT COUNT(*) FROM workflow_run"));
            assertEquals(1, count(statement, "SELECT COUNT(*) FROM spec_change"));
            assertEquals(1, count(statement, "SELECT COUNT(*) FROM workflow_spec_binding"));
            assertEquals(1, count(statement, "SELECT COUNT(*) FROM goal_event WHERE goal_key = '" + goalKey + "'"));
        }
    }

    private void assertCompletedRows(Path root, String goalKey) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PathUtil.memoryDb(root));
             Statement statement = connection.createStatement()) {
            assertEquals(5, count(statement, "SELECT COUNT(*) FROM goal_check WHERE goal_key = '" + goalKey + "'"));
            assertEquals(1, count(statement, "SELECT COUNT(*) FROM checkpoint"));
            assertEquals(1, count(statement, "SELECT COUNT(*) FROM goal_run WHERE goal_key = '" + goalKey + "' AND status = 'completed'"));
            assertTrue(count(statement, "SELECT COUNT(*) FROM goal_artifact WHERE goal_key = '" + goalKey + "'") >= 3);
        }
    }

    private int count(Statement statement, String sql) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private String firstValue(String output, String prefix) {
        String[] lines = output.split("\\r?\\n");
        for (String line : lines) {
            if (line.startsWith(prefix)) {
                return line.substring(prefix.length()).trim();
            }
        }
        return "";
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
    }

    private static final class FixedClock implements Clock {
        public Instant now() {
            return Instant.parse("2026-05-21T00:00:00Z");
        }
    }
}
