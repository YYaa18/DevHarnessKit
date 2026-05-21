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

final class MemoryAddConfirmSearchIntegrationTest {
    @TempDir
    Path tempDir;

    @Test
    void addConfirmAndSearchMemory() {
        initProject();

        Harness add = new Harness(tempDir);
        int addExit = new CommandRouter().run(new String[]{
                "memory", "add",
                "--project-root", "demo",
                "--type", "gateway_convention",
                "--module", "global",
                "--title", "User identity from gateway",
                "--content", "User ID is read from X-User-Id.",
                "--tags", "api,gateway,user-id,X-User-Id"
        }, add.context());

        assertEquals(ExitCodes.SUCCESS, addExit);
        assertTrue(add.stdout().contains("memory_id: 1"));
        assertTrue(add.stdout().contains("status: draft"));

        Harness searchDraft = new Harness(tempDir);
        int searchDraftExit = new CommandRouter().run(new String[]{
                "memory", "search", "--project-root", "demo", "--q", "gateway user-id"
        }, searchDraft.context());

        assertEquals(ExitCodes.SUCCESS, searchDraftExit);
        assertTrue(searchDraft.stdout().contains("[1] User identity from gateway"));
        assertTrue(searchDraft.stdout().contains("status: draft"));
        assertTrue(searchDraft.stdout().contains("match:"));

        Harness searchConfirmedBefore = new Harness(tempDir);
        int searchConfirmedBeforeExit = new CommandRouter().run(new String[]{
                "memory", "search", "--project-root", "demo", "--q", "gateway user-id", "--status", "confirmed"
        }, searchConfirmedBefore.context());

        assertEquals(ExitCodes.SUCCESS, searchConfirmedBeforeExit);
        assertTrue(searchConfirmedBefore.stdout().contains("No memory found."));

        Harness confirm = new Harness(tempDir);
        int confirmExit = new CommandRouter().run(new String[]{
                "memory", "confirm", "--project-root", "demo", "--id", "1"
        }, confirm.context());

        assertEquals(ExitCodes.SUCCESS, confirmExit);
        assertTrue(confirm.stdout().contains("status: confirmed"));
        assertTrue(confirm.stdout().contains("confidence: 70"));

        Harness searchConfirmedAfter = new Harness(tempDir);
        int searchConfirmedAfterExit = new CommandRouter().run(new String[]{
                "memory", "search", "--project-root", "demo", "--q", "gateway user-id", "--status", "confirmed"
        }, searchConfirmedAfter.context());

        assertEquals(ExitCodes.SUCCESS, searchConfirmedAfterExit);
        assertTrue(searchConfirmedAfter.stdout().contains("[1] User identity from gateway"));
        assertTrue(searchConfirmedAfter.stdout().contains("status: confirmed"));
    }

    @Test
    void addRejectsConfirmedStatusAndSensitiveContent() {
        initProject();

        Harness confirmedStatus = new Harness(tempDir);
        int confirmedStatusExit = new CommandRouter().run(new String[]{
                "memory", "add",
                "--project-root", "demo",
                "--type", "project_fact",
                "--title", "Bad status",
                "--content", "Ordinary content",
                "--status", "confirmed"
        }, confirmedStatus.context());

        assertEquals(ExitCodes.VALIDATION_ERROR, confirmedStatusExit);
        assertTrue(confirmedStatus.stderr().contains("memory add only supports --status draft"));

        Harness sensitive = new Harness(tempDir);
        int sensitiveExit = new CommandRouter().run(new String[]{
                "memory", "add",
                "--project-root", "demo",
                "--type", "project_fact",
                "--title", "Secret",
                "--content", "password=abc"
        }, sensitive.context());

        assertEquals(ExitCodes.VALIDATION_ERROR, sensitiveExit);
        assertTrue(sensitive.stderr().contains("Sensitive data rejected"));
    }

    @Test
    void confirmMissingMemoryReturnsNotFound() {
        initProject();

        Harness confirm = new Harness(tempDir);
        int exitCode = new CommandRouter().run(new String[]{
                "memory", "confirm", "--project-root", "demo", "--id", "99"
        }, confirm.context());

        assertEquals(ExitCodes.NOT_FOUND, exitCode);
        assertTrue(confirm.stderr().contains("Memory item not found: 99"));
    }

    private void initProject() {
        Harness init = new Harness(tempDir);
        int exitCode = new CommandRouter().run(new String[]{"memory", "init", "--project-root", "demo"}, init.context());
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
