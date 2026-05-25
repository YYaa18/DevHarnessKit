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

final class SpecIntegrationTest {
    @TempDir
    Path tempDir;

    @Test
    void specCreateDocumentTaskAcceptanceWorkflowBindingExportAndArchive() throws Exception {
        seedWorkflow();

        Harness create = new Harness(tempDir);
        int createExit = new CommandRouter().run(new String[]{
                "spec", "create", "--project-root", "demo",
                "--change", "order-query-api",
                "--title", "新增订单查询接口",
                "--summary", "为前端提供订单分页查询接口",
                "--module", "order",
                "--mode", "api"
        }, create.context());
        assertEquals(ExitCodes.SUCCESS, createExit);
        assertTrue(create.stdout().contains("change_key: order-query-api"));
        assertEquals(1, countRows("spec_change", "change_key = 'order-query-api'"));
        assertEquals(1, countRows("spec_document", "change_key = 'order-query-api' AND document_type = 'proposal'"));
        assertEquals(1, countRows("spec_event", "change_key = 'order-query-api' AND event_type = 'change_created'"));
        assertEquals(1, countSchemaVersion(MigrationRunner.V4));

        Harness document = new Harness(tempDir);
        int documentExit = new CommandRouter().run(new String[]{
                "spec", "document", "set", "--project-root", "demo",
                "--change", "order-query-api",
                "--type", "design",
                "--title", "订单查询接口 Design",
                "--content", "使用 Controller -> Service -> Mapper 实现分页查询",
                "--status", "confirmed"
        }, document.context());
        assertEquals(ExitCodes.SUCCESS, documentExit);
        assertTrue(document.stdout().contains("version: 1"));

        Harness documentUpdate = new Harness(tempDir);
        int documentUpdateExit = new CommandRouter().run(new String[]{
                "spec", "document", "set", "--project-root", "demo",
                "--change", "order-query-api",
                "--type", "design",
                "--title", "订单查询接口 Design",
                "--content", "补充统一响应包装",
                "--status", "confirmed"
        }, documentUpdate.context());
        assertEquals(ExitCodes.SUCCESS, documentUpdateExit);
        assertTrue(documentUpdate.stdout().contains("version: 2"));

        Path notesFile = tempDir.resolve("notes.md");
        Files.write(notesFile, "从文件读取规格备注".getBytes("UTF-8"));
        Harness fileDocument = new Harness(tempDir);
        int fileDocumentExit = new CommandRouter().run(new String[]{
                "spec", "document", "set", "--project-root", "demo",
                "--change", "order-query-api",
                "--type", "notes",
                "--content-file", notesFile.toString()
        }, fileDocument.context());
        assertEquals(ExitCodes.SUCCESS, fileDocumentExit);
        assertTrue(fileDocument.stdout().contains("document_type: notes"));

        Harness sensitiveDocument = new Harness(tempDir);
        int sensitiveDocumentExit = new CommandRouter().run(new String[]{
                "spec", "document", "set", "--project-root", "demo",
                "--change", "order-query-api", "--type", "notes",
                "--content", "password=abc"
        }, sensitiveDocument.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, sensitiveDocumentExit);

        Harness task = new Harness(tempDir);
        int taskExit = new CommandRouter().run(new String[]{
                "spec", "task", "add", "--project-root", "demo",
                "--change", "order-query-api",
                "--task", "T001",
                "--title", "新增请求 DTO",
                "--description", "包含分页参数和筛选条件",
                "--phase", "implement_minimal_change"
        }, task.context());
        assertEquals(ExitCodes.SUCCESS, taskExit);

        Harness acceptance = new Harness(tempDir);
        int acceptanceExit = new CommandRouter().run(new String[]{
                "spec", "acceptance", "add", "--project-root", "demo",
                "--change", "order-query-api",
                "--acceptance", "A001",
                "--description", "分页查询返回统一结果",
                "--expected", "接口返回 ApiResult<PageResult<OrderVO>>"
        }, acceptance.context());
        assertEquals(ExitCodes.SUCCESS, acceptanceExit);

        Harness run = new Harness(tempDir);
        int runExit = new CommandRouter().run(new String[]{
                "workflow", "start", "--project-root", "demo",
                "--workflow", "api-change",
                "--task", "新增订单查询接口",
                "--module", "order",
                "--mode", "api"
        }, run.context());
        assertEquals(ExitCodes.SUCCESS, runExit);
        String runKey = valueAfter(run.stdout(), "run_key: ");

        Harness bind = new Harness(tempDir);
        int bindExit = new CommandRouter().run(new String[]{
                "spec", "bind-workflow", "--project-root", "demo",
                "--change", "order-query-api",
                "--run", runKey,
                "--type", "implements"
        }, bind.context());
        assertEquals(ExitCodes.SUCCESS, bindExit);
        assertEquals(1, countRows("workflow_spec_binding", "change_key = 'order-query-api' AND run_key = '" + runKey + "'"));

        Harness status = new Harness(tempDir);
        int statusExit = new CommandRouter().run(new String[]{
                "spec", "status", "--project-root", "demo", "--change", "order-query-api"
        }, status.context());
        assertEquals(ExitCodes.SUCCESS, statusExit);
        assertTrue(status.stdout().contains("[pending] T001 新增请求 DTO"));
        assertTrue(status.stdout().contains("[pending] A001 分页查询返回统一结果"));
        assertTrue(status.stdout().contains(runKey + " implements"));

        Harness specExport = new Harness(tempDir);
        int specExportExit = new CommandRouter().run(new String[]{
                "spec", "export", "--project-root", "demo", "--change", "order-query-api"
        }, specExport.context());
        String specMarkdown = new String(Files.readAllBytes(PathUtil.specContext(tempDir.resolve("demo"))), "UTF-8");
        assertEquals(ExitCodes.SUCCESS, specExportExit);
        assertTrue(specMarkdown.contains("# SPEC_CONTEXT"));
        assertTrue(specMarkdown.contains("<design>"));
        assertTrue(specMarkdown.contains("T001 新增请求 DTO"));
        assertTrue(specMarkdown.length() < 20 * 1024);

        Harness memoryExport = new Harness(tempDir);
        int memoryExportExit = new CommandRouter().run(new String[]{
                "memory", "export", "--project-root", "demo",
                "--task", "新增订单查询接口",
                "--module", "order",
                "--include-workflow", runKey,
                "--include-spec", "order-query-api"
        }, memoryExport.context());
        String currentContext = new String(Files.readAllBytes(PathUtil.currentContext(tempDir.resolve("demo"))), "UTF-8");
        assertEquals(ExitCodes.SUCCESS, memoryExportExit);
        assertTrue(currentContext.contains("<spec-context>"));
        assertTrue(currentContext.contains("change_key: order-query-api"));
        assertTrue(currentContext.contains("Active tasks:"));
        assertFalse(currentContext.contains("<design>"));

        Harness archiveOpen = new Harness(tempDir);
        int archiveOpenExit = new CommandRouter().run(new String[]{
                "spec", "archive", "--project-root", "demo",
                "--change", "order-query-api", "--reason", "验收完成"
        }, archiveOpen.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, archiveOpenExit);

        Harness taskDone = new Harness(tempDir);
        int taskDoneExit = new CommandRouter().run(new String[]{
                "spec", "task", "update", "--project-root", "demo",
                "--change", "order-query-api",
                "--task", "T001",
                "--status", "done",
                "--evidence", "DTO added"
        }, taskDone.context());
        assertEquals(ExitCodes.SUCCESS, taskDoneExit);

        Harness acceptancePassed = new Harness(tempDir);
        int acceptancePassedExit = new CommandRouter().run(new String[]{
                "spec", "acceptance", "update", "--project-root", "demo",
                "--change", "order-query-api",
                "--acceptance", "A001",
                "--status", "passed",
                "--evidence", "接口测试通过"
        }, acceptancePassed.context());
        assertEquals(ExitCodes.SUCCESS, acceptancePassedExit);

        Harness archive = new Harness(tempDir);
        int archiveExit = new CommandRouter().run(new String[]{
                "spec", "archive", "--project-root", "demo",
                "--change", "order-query-api", "--reason", "验收完成"
        }, archive.context());
        assertEquals(ExitCodes.SUCCESS, archiveExit);
        assertTrue(archive.stdout().contains("status: archived"));
    }

    @Test
    void waivedAcceptanceRequiresEvidence() {
        createSpec();
        Harness add = new Harness(tempDir);
        new CommandRouter().run(new String[]{
                "spec", "acceptance", "add", "--project-root", "demo",
                "--change", "order-query-api",
                "--acceptance", "A001",
                "--description", "分页查询返回统一结果"
        }, add.context());

        Harness waive = new Harness(tempDir);
        int waiveExit = new CommandRouter().run(new String[]{
                "spec", "acceptance", "update", "--project-root", "demo",
                "--change", "order-query-api",
                "--acceptance", "A001",
                "--status", "waived"
        }, waive.context());
        assertEquals(ExitCodes.USAGE_ERROR, waiveExit);
        assertTrue(waive.stderr().contains("requires --evidence or --reason"));
    }

    private void seedWorkflow() {
        Harness seed = new Harness(tempDir);
        int exitCode = new CommandRouter().run(new String[]{
                "workflow", "template", "seed", "--project-root", "demo"
        }, seed.context());
        assertEquals(ExitCodes.SUCCESS, exitCode);
    }

    private void createSpec() {
        Harness create = new Harness(tempDir);
        int createExit = new CommandRouter().run(new String[]{
                "spec", "create", "--project-root", "demo",
                "--change", "order-query-api",
                "--title", "新增订单查询接口",
                "--summary", "为前端提供订单分页查询接口",
                "--module", "order",
                "--mode", "api"
        }, create.context());
        assertEquals(ExitCodes.SUCCESS, createExit);
    }

    private int countSchemaVersion(int version) throws Exception {
        return countRows("schema_version", "version = " + version);
    }

    private int countRows(String tableName, String where) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:sqlite:" + PathUtil.memoryDb(tempDir.resolve("demo")).toString());
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT COUNT(*) FROM " + tableName + " WHERE " + where)) {
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
