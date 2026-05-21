package com.devharnesskit.dhk.integration;

import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.CommandRouter;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.service.ProjectService;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MemoryInitDoctorIntegrationTest {
    @TempDir
    Path tempDir;

    @Test
    void memoryInitCreatesProjectFilesAndIsIdempotent() {
        Path root = tempDir.resolve("demo");
        Harness firstHarness = new Harness(tempDir);
        int firstExit = new CommandRouter().run(new String[]{"memory", "init", "--project-root", "demo"}, firstHarness.context());

        Project firstProject = new ProjectService().readProject(PathUtil.projectJson(root));
        Harness secondHarness = new Harness(tempDir);
        int secondExit = new CommandRouter().run(new String[]{"memory", "init", "--project-root", "demo"}, secondHarness.context());
        Project secondProject = new ProjectService().readProject(PathUtil.projectJson(root));

        assertEquals(ExitCodes.SUCCESS, firstExit);
        assertEquals(ExitCodes.SUCCESS, secondExit);
        assertTrue(Files.isRegularFile(PathUtil.memoryDb(root)));
        assertTrue(Files.isRegularFile(PathUtil.projectJson(root)));
        assertTrue(Files.isDirectory(PathUtil.exportsDirectory(root)));
        assertTrue(Files.isRegularFile(PathUtil.projectIndex(root)));
        assertEquals(firstProject.projectKey(), secondProject.projectKey());
        assertTrue(firstHarness.stdout().contains("memory init complete"));
        assertTrue(secondHarness.stdout().contains("fts:"));
    }

    @Test
    void doctorReturnsNotFoundBeforeInit() {
        Harness harness = new Harness(tempDir);

        int exitCode = new CommandRouter().run(new String[]{"doctor", "--project-root", "missing"}, harness.context());

        assertEquals(ExitCodes.NOT_FOUND, exitCode);
        assertTrue(harness.stdout().contains("memory_db: missing"));
    }

    @Test
    void doctorReturnsSuccessAfterInit() {
        Path root = tempDir.resolve("demo");
        Harness initHarness = new Harness(tempDir);
        new CommandRouter().run(new String[]{"memory", "init", "--project-root", "demo"}, initHarness.context());

        Harness doctorHarness = new Harness(tempDir);
        int exitCode = new CommandRouter().run(new String[]{"doctor", "--project-root", root.toString()}, doctorHarness.context());

        assertEquals(ExitCodes.SUCCESS, exitCode);
        assertTrue(doctorHarness.stdout().contains("schema_version: ok (3)"));
        assertTrue(doctorHarness.stdout().contains("mysql_driver: ok"));
        assertTrue(doctorHarness.stdout().contains("memory_total: 0"));
        assertTrue(doctorHarness.stdout().contains("checkpoint_total: 0"));
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
