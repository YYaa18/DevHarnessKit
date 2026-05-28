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
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SensitivePolicyIntegrationTest {
    private static final String PII_SAMPLE =
            "客户邮箱 finance.case@example.com，手机号 13800138000，证件号 11010519491231002X";
    private static final String[] RAW_PII = new String[]{
            "finance.case@example.com",
            "13800138000",
            "11010519491231002X"
    };

    @TempDir
    Path tempDir;

    @Test
    void financialRedactionPolicyAppliesAcrossMemorySpecWorkflowGoalAndContextExports() throws Exception {
        initProject();
        Path root = tempDir.resolve("demo");
        writeFinancialRedactPolicy(root);

        assertSuccess(run("memory", "add", "--project-root", "demo",
                "--type", "project_fact",
                "--module", "global",
                "--title", "金融 PII 规则",
                "--content", PII_SAMPLE,
                "--tags", "financial,pii",
                "--confidence", "90"));
        assertSuccess(run("memory", "confirm", "--project-root", "demo", "--id", "1"));
        assertSuccess(run("memory", "export", "--project-root", "demo",
                "--task", "验证金融 PII 导出",
                "--module", "global",
                "--keywords", "financial,pii"));
        assertRedactedExport(PathUtil.currentContext(root));

        assertSuccess(run("spec", "create", "--project-root", "demo",
                "--change", "financial-pii-policy",
                "--title", "金融 PII 策略",
                "--summary", PII_SAMPLE,
                "--module", "risk",
                "--mode", "api"));
        assertSuccess(run("spec", "document", "set", "--project-root", "demo",
                "--change", "financial-pii-policy",
                "--type", "design",
                "--title", "PII handling",
                "--content", PII_SAMPLE,
                "--status", "confirmed"));
        assertSuccess(run("spec", "export", "--project-root", "demo",
                "--change", "financial-pii-policy"));
        assertRedactedExport(PathUtil.specContext(root));

        assertSuccess(run("workflow", "template", "seed", "--project-root", "demo"));
        Harness workflowStart = run("workflow", "start", "--project-root", "demo",
                "--workflow", "api-change",
                "--task", PII_SAMPLE,
                "--module", "risk",
                "--mode", "api",
                "--summary", PII_SAMPLE);
        assertSuccess(workflowStart);
        String runKey = valueAfter(workflowStart.stdout(), "run_key: ");
        assertSuccess(run("workflow", "export", "--project-root", "demo", "--run", runKey));
        assertRedactedExport(PathUtil.workflowContext(root));

        assertSuccess(run("goal", "start", "--project-root", "demo",
                "--profile", "bugfix",
                "--task", PII_SAMPLE,
                "--module", "risk"));
        assertRedactedExport(PathUtil.goalContext(root));
    }

    @Test
    void financialRedactionPolicyStillRejectsCredentialShapesAcrossCommands() throws Exception {
        initProject();
        Path root = tempDir.resolve("demo");
        writeFinancialRedactPolicy(root);
        assertSuccess(run("workflow", "template", "seed", "--project-root", "demo"));

        assertValidationError(run("memory", "add", "--project-root", "demo",
                "--type", "project_fact",
                "--title", "Credential",
                "--content", "api_key=abc123"));
        assertValidationError(run("spec", "create", "--project-root", "demo",
                "--change", "credential-policy",
                "--title", "Credential policy",
                "--summary", "github token ghp_abcdefghijklmnopqrstuvwxyz"));
        assertValidationError(run("workflow", "start", "--project-root", "demo",
                "--workflow", "api-change",
                "--task", "https://user:secret@example.com/path",
                "--module", "risk",
                "--mode", "api"));
        assertValidationError(run("goal", "start", "--project-root", "demo",
                "--profile", "java-api-patch",
                "--task", "jdbc:mysql://127.0.0.1/demo",
                "--module", "risk"));
    }

    private void initProject() {
        assertSuccess(run("memory", "init", "--project-root", "demo"));
    }

    private void writeFinancialRedactPolicy(Path root) throws Exception {
        Files.createDirectories(PathUtil.devharnessDirectory(root));
        Files.write(PathUtil.sensitivePolicy(root), ("{\n"
                + "  \"email\": \"redact\",\n"
                + "  \"phone\": \"redact\",\n"
                + "  \"identity_number\": \"redact\",\n"
                + "  \"jwt\": \"reject\",\n"
                + "  \"api_key\": \"reject\",\n"
                + "  \"private_key\": \"reject\",\n"
                + "  \"jdbc:mysql://\": \"reject\",\n"
                + "  \"private key block\": \"reject\",\n"
                + "  \"authorization:\": \"reject\",\n"
                + "  \"github token\": \"reject\",\n"
                + "  \"github pat\": \"reject\",\n"
                + "  \"url credential\": \"reject\"\n"
                + "}\n").getBytes("UTF-8"));
    }

    private Harness run(String... args) {
        Harness harness = new Harness(tempDir);
        int exitCode = new CommandRouter().run(args, harness.context());
        harness.exitCode = exitCode;
        return harness;
    }

    private void assertSuccess(Harness harness) {
        assertEquals(ExitCodes.SUCCESS, harness.exitCode, harness.stderr());
    }

    private void assertValidationError(Harness harness) {
        assertEquals(ExitCodes.VALIDATION_ERROR, harness.exitCode, harness.stdout());
        assertTrue(harness.stderr().contains("Sensitive data rejected"), harness.stderr());
    }

    private void assertRedactedExport(Path path) throws Exception {
        assertTrue(Files.isRegularFile(path), path.toString());
        String text = new String(Files.readAllBytes(path), "UTF-8");
        assertTrue(text.contains("[REDACTED_EMAIL]"), text);
        assertTrue(text.contains("[REDACTED_PHONE]"), text);
        assertTrue(text.contains("[REDACTED_IDENTITY_NUMBER]"), text);
        for (String raw : RAW_PII) {
            assertFalse(text.contains(raw), "raw PII leaked: " + raw + "\n" + text);
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
        private int exitCode;

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
