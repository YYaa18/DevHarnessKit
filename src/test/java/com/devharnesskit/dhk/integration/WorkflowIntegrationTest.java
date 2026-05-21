package com.devharnesskit.dhk.integration;

import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.CommandRouter;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.MigrationRunner;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WorkflowIntegrationTest {
    @TempDir
    Path tempDir;

    @Test
    void templateSeedListAndShowAreIdempotent() throws Exception {
        Harness firstSeed = new Harness(tempDir);
        int firstExit = new CommandRouter().run(new String[]{
                "workflow", "template", "seed", "--project-root", "demo"
        }, firstSeed.context());

        Harness secondSeed = new Harness(tempDir);
        int secondExit = new CommandRouter().run(new String[]{
                "workflow", "template", "seed", "--project-root", "demo"
        }, secondSeed.context());

        assertEquals(ExitCodes.SUCCESS, firstExit);
        assertEquals(ExitCodes.SUCCESS, secondExit);
        assertTrue(firstSeed.stdout().contains("workflow_templates_seeded: 6"));
        assertTrue(secondSeed.stdout().contains("workflow_templates_inserted: 0"));
        assertEquals(1, countSchemaVersion(MigrationRunner.V1));
        assertEquals(1, countSchemaVersion(MigrationRunner.V2));

        Harness list = new Harness(tempDir);
        int listExit = new CommandRouter().run(new String[]{
                "workflow", "template", "list", "--project-root", "demo"
        }, list.context());

        assertEquals(ExitCodes.SUCCESS, listExit);
        assertTrue(list.stdout().contains("api-change"));
        assertTrue(list.stdout().contains("code-review"));

        Harness show = new Harness(tempDir);
        int showExit = new CommandRouter().run(new String[]{
                "workflow", "template", "show", "--project-root", "demo", "--key", "api-change"
        }, show.context());

        assertEquals(ExitCodes.SUCCESS, showExit);
        assertTrue(show.stdout().contains("1. export_context"));
        assertTrue(show.stdout().contains("create_change_plan/impacted_files_listed hard"));
    }

    @Test
    void workflowRunStatusPhaseGateExportAndMemoryExportIntegration() throws Exception {
        seed();

        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "workflow", "start",
                "--project-root", "demo",
                "--workflow", "api-change",
                "--task", "新增订单查询接口",
                "--module", "order",
                "--mode", "api"
        }, start.context());
        String runKey = valueAfter(start.stdout(), "run_key: ");

        assertEquals(ExitCodes.SUCCESS, startExit);
        assertEquals("20260521000000-api-change-order", runKey);
        assertTrue(start.stdout().contains("current_phase: export_context"));

        Harness status = new Harness(tempDir);
        int statusExit = new CommandRouter().run(new String[]{
                "workflow", "status", "--project-root", "demo", "--run", runKey
        }, status.context());

        assertEquals(ExitCodes.SUCCESS, statusExit);
        assertTrue(status.stdout().contains("[pending] export_context"));
        assertTrue(status.stdout().contains("[pending] current_context_exists"));

        Harness waiveWithoutReason = new Harness(tempDir);
        int waiveWithoutReasonExit = new CommandRouter().run(new String[]{
                "workflow", "gate", "waive", "--project-root", "demo", "--run", runKey,
                "--gate", "tests_recorded"
        }, waiveWithoutReason.context());
        assertEquals(ExitCodes.USAGE_ERROR, waiveWithoutReasonExit);

        Harness gatePass = new Harness(tempDir);
        int gatePassExit = new CommandRouter().run(new String[]{
                "workflow", "gate", "pass", "--project-root", "demo", "--run", runKey,
                "--gate", "current_context_exists", "--summary", "CURRENT_CONTEXT.md exists"
        }, gatePass.context());
        assertEquals(ExitCodes.SUCCESS, gatePassExit);
        assertTrue(gatePass.stdout().contains("status: passed"));

        Harness secondGatePass = new Harness(tempDir);
        int secondGatePassExit = new CommandRouter().run(new String[]{
                "workflow", "gate", "pass", "--project-root", "demo", "--run", runKey,
                "--gate", "confirmed_memory_only", "--summary", "Only confirmed memory used"
        }, secondGatePass.context());
        assertEquals(ExitCodes.SUCCESS, secondGatePassExit);

        Harness phasePass = new Harness(tempDir);
        int phasePassExit = new CommandRouter().run(new String[]{
                "workflow", "phase", "pass", "--project-root", "demo", "--run", runKey,
                "--phase", "export_context", "--summary", "Context exported"
        }, phasePass.context());
        assertEquals(ExitCodes.SUCCESS, phasePassExit);
        assertTrue(phasePass.stdout().contains("current_phase: inspect_existing_code"));

        Harness export = new Harness(tempDir);
        int exportExit = new CommandRouter().run(new String[]{
                "workflow", "export", "--project-root", "demo", "--run", runKey
        }, export.context());
        Path workflowContext = PathUtil.workflowContext(tempDir.resolve("demo"));
        String workflowMarkdown = new String(Files.readAllBytes(workflowContext), "UTF-8");
        assertEquals(ExitCodes.SUCCESS, exportExit);
        assertTrue(workflowMarkdown.contains("# WORKFLOW_CONTEXT"));
        assertTrue(workflowMarkdown.contains("current_phase: inspect_existing_code"));
        assertTrue(workflowMarkdown.length() < 12 * 1024);

        Harness memoryExport = new Harness(tempDir);
        int memoryExportExit = new CommandRouter().run(new String[]{
                "memory", "export", "--project-root", "demo", "--task", "新增订单查询接口",
                "--module", "order", "--include-workflow", runKey
        }, memoryExport.context());
        String currentContext = new String(Files.readAllBytes(PathUtil.currentContext(tempDir.resolve("demo"))), "UTF-8");
        assertEquals(ExitCodes.SUCCESS, memoryExportExit);
        assertTrue(currentContext.contains("<workflow-context>"));
        assertTrue(currentContext.contains("run_key: " + runKey));
        assertTrue(currentContext.contains("Current phase:"));
        assertTrue(currentContext.contains("Read existing endpoint/service/data-access code before editing."));

        Harness hardFail = new Harness(tempDir);
        int hardFailExit = new CommandRouter().run(new String[]{
                "workflow", "gate", "fail", "--project-root", "demo", "--run", runKey,
                "--gate", "impacted_files_listed", "--reason", "Plan missing impacted files"
        }, hardFail.context());
        assertEquals(ExitCodes.SUCCESS, hardFailExit);
        assertTrue(hardFail.stdout().contains("run_status: blocked"));

        Harness softStart = new Harness(tempDir);
        new CommandRouter().run(new String[]{
                "workflow", "start", "--project-root", "demo", "--workflow", "api-change",
                "--task", "第二个任务", "--module", "order", "--mode", "api"
        }, softStart.context());
        String secondRun = valueAfter(softStart.stdout(), "run_key: ");
        assertTrue(secondRun.endsWith("-2"));

        Harness softFail = new Harness(tempDir);
        int softFailExit = new CommandRouter().run(new String[]{
                "workflow", "gate", "fail", "--project-root", "demo", "--run", secondRun,
                "--gate", "tests_recorded", "--reason", "Tests not present"
        }, softFail.context());
        assertEquals(ExitCodes.SUCCESS, softFailExit);
        assertTrue(softFail.stdout().contains("run_status: running"));
    }

    @Test
    void workflowSensitiveDataIsRejectedAtWriteAndExportBoundaries() throws Exception {
        seed();

        Harness sensitiveStart = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "workflow", "start", "--project-root", "demo", "--workflow", "api-change",
                "--task", "Authorization: Bearer eyJabcdefghij", "--module", "order", "--mode", "api"
        }, sensitiveStart.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, startExit);
        assertTrue(sensitiveStart.stderr().contains("Sensitive data rejected"));

        String runKey = startApiChangeRun();

        Harness sensitivePhase = new Harness(tempDir);
        int phaseExit = new CommandRouter().run(new String[]{
                "workflow", "phase", "pass", "--project-root", "demo", "--run", runKey,
                "--phase", "export_context", "--summary", "password=abc"
        }, sensitivePhase.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, phaseExit);
        assertTrue(sensitivePhase.stderr().contains("Sensitive data rejected"));

        Harness sensitiveGate = new Harness(tempDir);
        int gateExit = new CommandRouter().run(new String[]{
                "workflow", "gate", "waive", "--project-root", "demo", "--run", runKey,
                "--gate", "current_context_exists", "--reason", "jdbc:mysql://127.0.0.1/app"
        }, sensitiveGate.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, gateExit);
        assertTrue(sensitiveGate.stderr().contains("Sensitive data rejected"));

        executeSql("UPDATE workflow_run SET task_name = 'password=abc' WHERE run_key = '" + runKey + "'");

        Harness workflowExport = new Harness(tempDir);
        int workflowExportExit = new CommandRouter().run(new String[]{
                "workflow", "export", "--project-root", "demo", "--run", runKey
        }, workflowExport.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, workflowExportExit);
        assertTrue(workflowExport.stderr().contains("Sensitive data rejected"));

        executeSql("UPDATE workflow_phase_template SET instruction = 'password=abc' "
                + "WHERE workflow_key = 'api-change' AND phase_key = 'export_context'");

        Harness memoryExport = new Harness(tempDir);
        int memoryExportExit = new CommandRouter().run(new String[]{
                "memory", "export", "--project-root", "demo", "--task", "safe task",
                "--include-workflow", runKey
        }, memoryExport.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, memoryExportExit);
        assertTrue(memoryExport.stderr().contains("Sensitive data rejected"));
    }

    @Test
    void hardGatesBlockPhasePassAndWaiveCanResume() {
        seed();
        String runKey = startApiChangeRun();

        Harness blockedByPendingGate = new Harness(tempDir);
        int blockedByPendingGateExit = new CommandRouter().run(new String[]{
                "workflow", "phase", "pass", "--project-root", "demo", "--run", runKey,
                "--phase", "export_context", "--summary", "Context exported"
        }, blockedByPendingGate.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, blockedByPendingGateExit);
        assertTrue(blockedByPendingGate.stderr().contains("Blocking hard gates exist"));

        Harness hardFail = new Harness(tempDir);
        int hardFailExit = new CommandRouter().run(new String[]{
                "workflow", "gate", "fail", "--project-root", "demo", "--run", runKey,
                "--gate", "current_context_exists", "--reason", "Context missing"
        }, hardFail.context());
        assertEquals(ExitCodes.SUCCESS, hardFailExit);
        assertTrue(hardFail.stdout().contains("run_status: blocked"));

        Harness blockedRunPhasePass = new Harness(tempDir);
        int blockedRunPhasePassExit = new CommandRouter().run(new String[]{
                "workflow", "phase", "pass", "--project-root", "demo", "--run", runKey,
                "--phase", "export_context", "--summary", "Context exported"
        }, blockedRunPhasePass.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, blockedRunPhasePassExit);
        assertTrue(blockedRunPhasePass.stderr().contains("status is: blocked"));

        Harness waiveFailedGate = new Harness(tempDir);
        int waiveFailedGateExit = new CommandRouter().run(new String[]{
                "workflow", "gate", "waive", "--project-root", "demo", "--run", runKey,
                "--gate", "current_context_exists", "--reason", "Context was produced outside dhk"
        }, waiveFailedGate.context());
        assertEquals(ExitCodes.SUCCESS, waiveFailedGateExit);
        assertTrue(waiveFailedGate.stdout().contains("run_status: blocked"));

        Harness passRemainingGate = new Harness(tempDir);
        int passRemainingGateExit = new CommandRouter().run(new String[]{
                "workflow", "gate", "pass", "--project-root", "demo", "--run", runKey,
                "--gate", "confirmed_memory_only", "--summary", "Confirmed memory only"
        }, passRemainingGate.context());
        assertEquals(ExitCodes.SUCCESS, passRemainingGateExit);
        assertTrue(passRemainingGate.stdout().contains("run_status: running"));

        Harness phasePass = new Harness(tempDir);
        int phasePassExit = new CommandRouter().run(new String[]{
                "workflow", "phase", "pass", "--project-root", "demo", "--run", runKey,
                "--phase", "export_context", "--summary", "Context exported"
        }, phasePass.context());
        assertEquals(ExitCodes.SUCCESS, phasePassExit);
        assertTrue(phasePass.stdout().contains("current_phase: inspect_existing_code"));
    }

    @Test
    void duplicateGateKeysRequirePhase() throws Exception {
        seed();
        String runKey = startApiChangeRun();
        executeSql("UPDATE workflow_gate_run SET gate_key = 'same_gate' "
                + "WHERE gate_key IN ('current_context_exists', 'impacted_files_listed')");

        Harness ambiguous = new Harness(tempDir);
        int ambiguousExit = new CommandRouter().run(new String[]{
                "workflow", "gate", "pass", "--project-root", "demo", "--run", runKey,
                "--gate", "same_gate", "--summary", "ok"
        }, ambiguous.context());
        assertEquals(ExitCodes.USAGE_ERROR, ambiguousExit);
        assertTrue(ambiguous.stderr().contains("Ambiguous gate"));

        Harness phaseSpecific = new Harness(tempDir);
        int phaseSpecificExit = new CommandRouter().run(new String[]{
                "workflow", "gate", "pass", "--project-root", "demo", "--run", runKey,
                "--phase", "export_context", "--gate", "same_gate", "--summary", "ok"
        }, phaseSpecific.context());
        assertEquals(ExitCodes.SUCCESS, phaseSpecificExit);
    }

    private void seed() {
        Harness seed = new Harness(tempDir);
        int exitCode = new CommandRouter().run(new String[]{
                "workflow", "template", "seed", "--project-root", "demo"
        }, seed.context());
        assertEquals(ExitCodes.SUCCESS, exitCode);
    }

    private String startApiChangeRun() {
        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "workflow", "start", "--project-root", "demo", "--workflow", "api-change",
                "--task", "safe task", "--module", "order", "--mode", "api"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        return valueAfter(start.stdout(), "run_key: ");
    }

    private int countSchemaVersion(int version) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:sqlite:" + PathUtil.memoryDb(tempDir.resolve("demo")).toString());
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT COUNT(*) FROM schema_version WHERE version = " + version)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private String valueAfter(String text, String prefix) {
        String[] lines = text.split("\\R");
        for (String line : lines) {
            if (line.startsWith(prefix)) {
                return line.substring(prefix.length()).trim();
            }
        }
        return "";
    }

    private void executeSql(String sql) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:sqlite:" + PathUtil.memoryDb(tempDir.resolve("demo")).toString());
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
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
