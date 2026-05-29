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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MemoryExportRecoverIntegrationTest {
    @TempDir
    Path tempDir;

    @Test
    void exportUsesConfirmedMemoryOnlyAndWritesCurrentContext() throws Exception {
        initProject();
        addMemory("Confirmed gateway", "User ID is read from X-User-Id.", "api,gateway,user-id");
        confirm("1");
        addMemory("Draft only", "This draft should not be exported.", "draft-only");

        Harness export = new Harness(tempDir);
        int exportExit = new CommandRouter().run(new String[]{
                "memory", "export",
                "--project-root", "demo",
                "--task", "新增订单查询接口",
                "--module", "global",
                "--mode", "api",
                "--keywords", "gateway,mybatis"
        }, export.context());

        Path currentContext = PathUtil.currentContext(tempDir.resolve("demo"));
        String markdown = new String(Files.readAllBytes(currentContext), "UTF-8");

        assertEquals(ExitCodes.SUCCESS, exportExit);
        assertTrue(Files.isRegularFile(currentContext));
        assertTrue(markdown.contains("# CURRENT_CONTEXT"));
        assertTrue(markdown.contains("Confirmed gateway"));
        assertFalse(markdown.contains("Draft only"));
        assertTrue(export.stdout().contains("memory_exported: 1"));

        Harness exportJson = new Harness(tempDir);
        int exportJsonExit = new CommandRouter().run(new String[]{
                "memory", "export",
                "--project-root", "demo",
                "--task", "新增订单查询接口",
                "--module", "global",
                "--mode", "api",
                "--keywords", "gateway,mybatis",
                "--json"
        }, exportJson.context());
        assertEquals(ExitCodes.SUCCESS, exportJsonExit);
        assertTrue(exportJson.stdout().contains("\"command\": \"memory export\""));
        assertTrue(exportJson.stdout().contains("\"memory_exported\": 1"));
        assertTrue(markdown.contains("<truncation-report>"));

        Harness search = new Harness(tempDir);
        new CommandRouter().run(new String[]{
                "memory", "search", "--project-root", "demo", "--q", "gateway", "--status", "confirmed"
        }, search.context());
        assertTrue(search.stdout().contains("confidence>=70"));
    }

    @Test
    void exportRedactsExistingPiiWhenPolicyChangesToRedact() throws Exception {
        initProject();
        Path root = tempDir.resolve("demo");
        Files.write(PathUtil.sensitivePolicy(root), ("{\n"
                + "  \"email\": \"allow\",\n"
                + "  \"phone\": \"allow\",\n"
                + "  \"identity_number\": \"allow\"\n"
                + "}\n").getBytes("UTF-8"));
        addMemory("Customer PII sample",
                "Contact 13800138000, test@example.com, 11010519491231002X.",
                "customer,pii", "global", "project_fact", "80");
        confirm("1");
        Files.write(PathUtil.sensitivePolicy(root), ("{\n"
                + "  \"email\": \"redact\",\n"
                + "  \"phone\": \"redact\",\n"
                + "  \"identity_number\": \"redact\"\n"
                + "}\n").getBytes("UTF-8"));

        Harness export = new Harness(tempDir);
        int exportExit = new CommandRouter().run(new String[]{
                "memory", "export",
                "--project-root", "demo",
                "--task", "PII review",
                "--module", "global",
                "--keywords", "pii"
        }, export.context());

        Path currentContext = PathUtil.currentContext(root);
        String markdown = new String(Files.readAllBytes(currentContext), "UTF-8");

        assertEquals(ExitCodes.SUCCESS, exportExit);
        assertTrue(markdown.contains("[REDACTED_PHONE]"));
        assertTrue(markdown.contains("[REDACTED_EMAIL]"));
        assertTrue(markdown.contains("[REDACTED_IDENTITY_NUMBER]"));
        assertFalse(markdown.contains("13800138000"));
        assertFalse(markdown.contains("test@example.com"));
        assertFalse(markdown.contains("11010519491231002X"));
    }

    @Test
    void policyCanBlockForbiddenContextExportPath() throws Exception {
        initProject();
        Path root = tempDir.resolve("demo");
        addMemory("Confirmed policy export", "Policy hooks protect context exports.", "policy,export");
        confirm("1");
        Files.write(PathUtil.devharnessPolicy(root), ("{\n"
                + "  \"mode\": \"strict\",\n"
                + "  \"context_export_forbidden_files\": \".agents/memory/exports/CURRENT_CONTEXT.md\"\n"
                + "}\n").getBytes("UTF-8"));

        Harness export = new Harness(tempDir);
        int exportExit = new CommandRouter().run(new String[]{
                "memory", "export",
                "--project-root", "demo",
                "--task", "Policy export guard",
                "--module", "global",
                "--keywords", "policy"
        }, export.context());

        assertEquals(ExitCodes.VALIDATION_ERROR, exportExit);
        assertTrue(export.stderr().contains("Policy blocked context export"));
        assertTrue(export.stderr().contains("output path is forbidden"));
        assertFalse(Files.isRegularFile(PathUtil.currentContext(root)));
    }

    @Test
    void exportPrioritizesModuleAndKeywordRelevantMemory() throws Exception {
        initProject();
        addMemory("General high confidence", "General project convention.", "general", "global",
                "project_fact", "95");
        confirm("1");
        addMemory("Order MyBatis convention", "Order module uses mapper SQL conventions.", "mybatis,order",
                "order", "database_convention", "70");
        confirm("2");

        Harness export = new Harness(tempDir);
        int exportExit = new CommandRouter().run(new String[]{
                "memory", "export",
                "--project-root", "demo",
                "--task", "新增订单查询接口",
                "--module", "order",
                "--mode", "api",
                "--keywords", "mybatis"
        }, export.context());

        Path currentContext = PathUtil.currentContext(tempDir.resolve("demo"));
        String markdown = new String(Files.readAllBytes(currentContext), "UTF-8");

        assertEquals(ExitCodes.SUCCESS, exportExit);
        assertTrue(markdown.indexOf("Order MyBatis convention") < markdown.indexOf("General high confidence"));
    }

    @Test
    void exportUsesModuleCheckpointBeforeProjectLatestFallback() throws Exception {
        initProject();
        checkpoint("order", "Order checkpoint summary");
        checkpoint("user", "User checkpoint summary");

        Harness export = new Harness(tempDir);
        int exportExit = new CommandRouter().run(new String[]{
                "memory", "export",
                "--project-root", "demo",
                "--task", "新增订单查询接口",
                "--module", "order"
        }, export.context());

        Path currentContext = PathUtil.currentContext(tempDir.resolve("demo"));
        String markdown = new String(Files.readAllBytes(currentContext), "UTF-8");

        assertEquals(ExitCodes.SUCCESS, exportExit);
        assertTrue(markdown.contains("Order checkpoint summary"));
        assertFalse(markdown.contains("User checkpoint summary"));
    }

    @Test
    void checkpointAndRecoverWriteRecoveryContext() throws Exception {
        initProject();
        addMemory("Order gateway", "Order APIs use X-User-Id from gateway.", "order,gateway,user-id");
        confirm("1");

        Harness checkpoint = new Harness(tempDir);
        int checkpointExit = new CommandRouter().run(new String[]{
                "memory", "checkpoint",
                "--project-root", "demo",
                "--task", "新增订单查询接口",
                "--module", "order",
                "--summary", "Controller 初稿已完成，Mapper 未验证",
                "--changed", "OrderController.java",
                "--pending", "验证 SQL",
                "--verify", "compile passed",
                "--next-read", "OrderMapper.xml"
        }, checkpoint.context());

        Harness recover = new Harness(tempDir);
        int recoverExit = new CommandRouter().run(new String[]{
                "memory", "recover", "--project-root", "demo", "--latest", "--module", "order"
        }, recover.context());

        Path recoveryContext = PathUtil.recoveryContext(tempDir.resolve("demo"));
        String markdown = new String(Files.readAllBytes(recoveryContext), "UTF-8");

        assertEquals(ExitCodes.SUCCESS, checkpointExit);
        assertTrue(checkpoint.stdout().contains("checkpoint_id: 1"));
        assertEquals(ExitCodes.SUCCESS, recoverExit);
        assertTrue(Files.isRegularFile(recoveryContext));
        assertTrue(markdown.contains("# RECOVERY_CONTEXT"));
        assertTrue(markdown.contains("Controller 初稿已完成"));
        assertTrue(markdown.contains("Order gateway"));
        assertTrue(markdown.contains("OrderMapper.xml"));
    }

    @Test
    void recoverMissingModuleCheckpointReturnsNotFound() {
        initProject();

        Harness recover = new Harness(tempDir);
        int recoverExit = new CommandRouter().run(new String[]{
                "memory", "recover", "--project-root", "demo", "--latest", "--module", "order"
        }, recover.context());

        assertEquals(ExitCodes.NOT_FOUND, recoverExit);
        assertTrue(recover.stderr().contains("error_code: MEMORY_CHECKPOINT_NOT_FOUND"));
        assertTrue(recover.stderr().contains("checkpoint not found: order"));
        assertTrue(recover.stderr().contains("next_command:"));
    }

    @Test
    void backupWritesExplicitArchiveAndFailsWhenMemoryIsMissing() throws Exception {
        Harness missing = new Harness(tempDir);
        int missingExit = new CommandRouter().run(new String[]{
                "memory", "backup", "--project-root", "missing-demo"
        }, missing.context());
        assertEquals(ExitCodes.NOT_FOUND, missingExit);
        assertTrue(missing.stderr().contains("Memory is not initialized"));

        initProject();
        Path backup = tempDir.resolve("manual-backup.zip");
        Harness explicit = new Harness(tempDir);
        int explicitExit = new CommandRouter().run(new String[]{
                "memory", "backup", "--project-root", "demo", "--out", backup.toString()
        }, explicit.context());
        assertEquals(ExitCodes.SUCCESS, explicitExit);
        assertTrue(Files.isRegularFile(backup));
        assertTrue(explicit.stdout().contains("backup_path: " + backup));

        Harness defaultOut = new Harness(tempDir);
        int defaultExit = new CommandRouter().run(new String[]{
                "memory", "backup", "--project-root", "demo"
        }, defaultOut.context());
        assertEquals(ExitCodes.SUCCESS, defaultExit);
        String backupPath = firstValue(defaultOut.stdout(), "backup_path: ");
        assertTrue(backupPath.contains("memory-backup"));
        assertTrue(Files.isRegularFile(java.nio.file.Paths.get(backupPath)));
    }

    private void initProject() {
        Harness init = new Harness(tempDir);
        int exitCode = new CommandRouter().run(new String[]{"memory", "init", "--project-root", "demo"}, init.context());
        assertEquals(ExitCodes.SUCCESS, exitCode);
    }

    private String firstValue(String text, String prefix) {
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            if (line.startsWith(prefix)) {
                return line.substring(prefix.length()).trim();
            }
        }
        return "";
    }

    private void addMemory(String title, String content, String tags) {
        addMemory(title, content, tags, "global", "gateway_convention", "50");
    }

    private void addMemory(String title, String content, String tags, String module, String type, String confidence) {
        Harness add = new Harness(tempDir);
        int exitCode = new CommandRouter().run(new String[]{
                "memory", "add",
                "--project-root", "demo",
                "--type", type,
                "--module", module,
                "--title", title,
                "--content", content,
                "--tags", tags,
                "--confidence", confidence
        }, add.context());
        assertEquals(ExitCodes.SUCCESS, exitCode);
    }

    private void confirm(String id) {
        Harness confirm = new Harness(tempDir);
        int exitCode = new CommandRouter().run(new String[]{
                "memory", "confirm", "--project-root", "demo", "--id", id
        }, confirm.context());
        assertEquals(ExitCodes.SUCCESS, exitCode);
    }

    private void checkpoint(String module, String summary) {
        Harness checkpoint = new Harness(tempDir);
        int exitCode = new CommandRouter().run(new String[]{
                "memory", "checkpoint",
                "--project-root", "demo",
                "--task", "Task for " + module,
                "--module", module,
                "--summary", summary
        }, checkpoint.context());
        assertEquals(ExitCodes.SUCCESS, exitCode);
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
