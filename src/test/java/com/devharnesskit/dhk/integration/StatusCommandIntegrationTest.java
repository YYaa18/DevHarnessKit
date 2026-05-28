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
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StatusCommandIntegrationTest {
    @TempDir
    Path tempDir;

    @Test
    void statusReportsNotReadyForUninitializedProjectAndExitCodeCanFail() {
        Harness status = new Harness(tempDir);
        int statusExit = new CommandRouter().run(new String[]{
                "status",
                "--project-root", "missing-project"
        }, status.context());
        assertEquals(ExitCodes.SUCCESS, statusExit);
        assertTrue(status.stdout().contains("readiness: not_ready"));
        assertTrue(status.stdout().contains("config: missing"));
        assertTrue(status.stdout().contains("memory_db: missing"));
        assertTrue(status.stdout().contains("next_command: dhk configure init"));

        Harness readiness = new Harness(tempDir);
        int readinessExit = new CommandRouter().run(new String[]{
                "readiness",
                "--project-root", "missing-project",
                "--exit-code"
        }, readiness.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, readinessExit);
        assertTrue(readiness.stdout().contains("dhk readiness"));
        assertTrue(readiness.stdout().contains("readiness: not_ready"));
    }

    @Test
    void statusReportsConfiguredReadinessAsJsonAndCanWriteMarkdown() throws Exception {
        Harness configure = new Harness(tempDir);
        int configureExit = new CommandRouter().run(new String[]{
                "configure", "init",
                "--project-root", "ready-project",
                "--preset", "springboot-manual-ide-test",
                "--force"
        }, configure.context());
        assertEquals(ExitCodes.SUCCESS, configureExit);

        Harness init = new Harness(tempDir);
        int initExit = new CommandRouter().run(new String[]{
                "memory", "init",
                "--project-root", "ready-project"
        }, init.context());
        assertEquals(ExitCodes.SUCCESS, initExit);

        Path root = tempDir.resolve("ready-project");
        Path installState = PathUtil.devharnessDirectory(root).resolve("install-state.json");
        Files.write(installState, ("{\n"
                + "  \"schema_version\": \"devharness-install-state/v1-alpha\",\n"
                + "  \"managed_files\": []\n"
                + "}\n").getBytes("UTF-8"));

        Harness json = new Harness(tempDir);
        int jsonExit = new CommandRouter().run(new String[]{
                "status",
                "--project-root", root.toString(),
                "--json"
        }, json.context());
        assertEquals(ExitCodes.SUCCESS, jsonExit);
        assertTrue(json.stdout().contains("\"command\": \"status\""));
        assertTrue(json.stdout().contains("\"readiness\": \"ready\""));
        assertTrue(json.stdout().contains("\"preset\": \"springboot-manual-ide-test\""));
        assertTrue(json.stdout().contains("\"compile_mode\": \"manual\""));
        assertTrue(json.stdout().contains("\"install_state\": \"ok\""));
        assertTrue(json.stdout().contains("\"schema_version\": \"14\""));

        Path markdown = tempDir.resolve("status.md");
        Harness markdownHarness = new Harness(tempDir);
        int markdownExit = new CommandRouter().run(new String[]{
                "readiness",
                "--project-root", root.toString(),
                "--markdown",
                "--write", markdown.toString()
        }, markdownHarness.context());
        assertEquals(ExitCodes.SUCCESS, markdownExit);
        assertTrue(Files.isRegularFile(markdown));
        String markdownText = new String(Files.readAllBytes(markdown), "UTF-8");
        assertTrue(markdownText.contains("# DevHarnessKit readiness"));
        assertTrue(markdownText.contains("- readiness: ready"));
        assertTrue(markdownText.contains("- next_command: dhk goal start"));
    }

    @Test
    void statusReportsActiveGoalAndCreatesWriteParentDirectory() throws Exception {
        Harness configure = new Harness(tempDir);
        int configureExit = new CommandRouter().run(new String[]{
                "configure", "init",
                "--project-root", "active-goal-project",
                "--preset", "springboot-auto-test",
                "--force"
        }, configure.context());
        assertEquals(ExitCodes.SUCCESS, configureExit);

        Path root = tempDir.resolve("active-goal-project");
        Files.createDirectories(PathUtil.devharnessDirectory(root));
        Files.write(PathUtil.devharnessDirectory(root).resolve("install-state.json"), ("{\n"
                + "  \"schema_version\": \"devharness-install-state/v1-alpha\",\n"
                + "  \"managed_files\": []\n"
                + "}\n").getBytes("UTF-8"));

        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", root.toString(),
                "--profile", "bugfix",
                "--task", "Fix status active goal reporting",
                "--module", "status"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");

        Path nestedMarkdown = tempDir.resolve("nested/status/readiness.md");
        Harness status = new Harness(tempDir);
        int statusExit = new CommandRouter().run(new String[]{
                "status",
                "--project-root", root.toString(),
                "--markdown",
                "--write", nestedMarkdown.toString()
        }, status.context());

        assertEquals(ExitCodes.SUCCESS, statusExit);
        assertTrue(Files.isRegularFile(nestedMarkdown));
        String rendered = new String(Files.readAllBytes(nestedMarkdown), "UTF-8");
        assertTrue(rendered.contains("- readiness: ready"));
        assertTrue(rendered.contains("- active_goal: " + goalKey));
        assertTrue(rendered.contains("- current_action: collect_error"));
        assertTrue(rendered.contains("- next_command: dhk goal next"));
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

    private static final class Harness {
        private final Path workingDirectory;
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();
        private final ByteArrayOutputStream err = new ByteArrayOutputStream();

        private Harness(Path workingDirectory) {
            this.workingDirectory = workingDirectory;
        }

        CommandContext context() {
            return new CommandContext(
                    workingDirectory,
                    new PrintStream(out),
                    new PrintStream(err),
                    new FixedClock()
            );
        }

        String stdout() {
            return out.toString();
        }
    }

    private static final class FixedClock implements Clock {
        public Instant now() {
            return Instant.parse("2026-05-27T00:00:00Z");
        }
    }
}
