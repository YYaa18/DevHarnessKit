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

final class QuickstartCommandIntegrationTest {
    @TempDir
    Path tempDir;

    @Test
    void quickstartDryRunDoesNotWriteConfigOrDatabase() {
        Harness dryRun = new Harness(tempDir);
        int exit = new CommandRouter().run(new String[]{
                "quickstart",
                "--project-root", "dry-run-project",
                "--preset", "springboot-manual-ide-test",
                "--task", "Implement order query endpoint",
                "--module", "order",
                "--target", "all",
                "--graph", "advisory",
                "--dry-run",
                "--json"
        }, dryRun.context());

        Path root = tempDir.resolve("dry-run-project");
        assertEquals(ExitCodes.SUCCESS, exit);
        assertTrue(dryRun.stdout().contains("\"quickstart\": \"dry_run\""));
        assertTrue(dryRun.stdout().contains("\"goal_key\": \"pending\""));
        assertTrue(dryRun.stdout().contains("\"next_command\": \"dhk quickstart"));
        assertFalse(Files.exists(PathUtil.devharnessConfig(root)));
        assertFalse(Files.exists(PathUtil.memoryDb(root)));
    }

    @Test
    void quickstartCreatesConfigAndFirstGoalWithoutCompletingIt() throws Exception {
        Harness quickstart = new Harness(tempDir);
        int exit = new CommandRouter().run(new String[]{
                "quickstart",
                "--project-root", "first-project",
                "--preset", "springboot-manual-ide-test",
                "--task", "Implement order query endpoint",
                "--module", "order",
                "--mode", "api"
        }, quickstart.context());

        Path root = tempDir.resolve("first-project");
        assertEquals(ExitCodes.SUCCESS, exit);
        assertTrue(quickstart.stdout().contains("quickstart: ready"));
        assertTrue(quickstart.stdout().contains("readiness: ready_with_warnings"));
        assertTrue(quickstart.stdout().contains("config: created"));
        assertTrue(quickstart.stdout().contains("install_state: missing"));
        assertTrue(quickstart.stdout().contains("next_command: dhk goal next"));
        assertTrue(Files.isRegularFile(PathUtil.devharnessConfig(root)));
        assertTrue(Files.isRegularFile(PathUtil.memoryDb(root)));
        assertTrue(Files.isRegularFile(PathUtil.goalContext(root)));

        String goalKey = firstValue(quickstart.stdout(), "goal_key: ");
        assertTrue(goalKey.length() > 0);

        Harness duplicateStart = new Harness(tempDir);
        int duplicateStartExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", root.toString(),
                "--profile", "java-api-change",
                "--task", "Implement order query endpoint",
                "--module", "order",
                "--mode", "api"
        }, duplicateStart.context());
        assertEquals(ExitCodes.SUCCESS, duplicateStartExit);
        assertTrue(duplicateStart.stdout().contains("status: context_ready"));
        assertTrue(duplicateStart.stdout().contains("start_result: existing_goal"));
        assertEquals(goalKey, firstValue(duplicateStart.stdout(), "goal_key: "));
        assertTrue(duplicateStart.stdout().contains("--force-new"));

        Harness status = new Harness(tempDir);
        int statusExit = new CommandRouter().run(new String[]{
                "goal", "status",
                "--project-root", root.toString(),
                "--goal", goalKey
        }, status.context());
        assertEquals(ExitCodes.SUCCESS, statusExit);
        assertTrue(status.stdout().contains("status: context_ready"));
        assertTrue(status.stdout().contains("current_action: inspect_existing_code"));
    }

    @Test
    void quickstartReusesExistingConfigAndOpenGoalWithoutOverwrite() throws Exception {
        Harness configure = new Harness(tempDir);
        int configureExit = new CommandRouter().run(new String[]{
                "configure", "init",
                "--project-root", "reuse-project",
                "--preset", "springboot-auto-test"
        }, configure.context());
        assertEquals(ExitCodes.SUCCESS, configureExit);

        Harness first = new Harness(tempDir);
        int firstExit = new CommandRouter().run(new String[]{
                "quickstart",
                "--project-root", "reuse-project",
                "--preset", "springboot-manual-ide-test",
                "--task", "First goal",
                "--module", "order"
        }, first.context());
        assertEquals(ExitCodes.SUCCESS, firstExit);
        String firstGoal = firstValue(first.stdout(), "goal_key: ");
        String configText = read(PathUtil.devharnessConfig(tempDir.resolve("reuse-project")));
        assertTrue(configText.contains("\"preset\": \"springboot-auto-test\""));

        Harness second = new Harness(tempDir);
        int secondExit = new CommandRouter().run(new String[]{
                "quickstart",
                "--project-root", "reuse-project",
                "--preset", "springboot-manual-ide-test",
                "--task", "First goal",
                "--module", "order"
        }, second.context());
        assertEquals(ExitCodes.SUCCESS, secondExit);
        assertTrue(second.stdout().contains("quickstart: existing_goal"));
        assertTrue(second.stdout().contains("config: existing"));
        assertEquals(firstGoal, firstValue(second.stdout(), "goal_key: "));

        Harness forced = new Harness(tempDir);
        int forcedExit = new CommandRouter().run(new String[]{
                "quickstart",
                "--project-root", "reuse-project",
                "--preset", "springboot-manual-ide-test",
                "--task", "First goal",
                "--module", "order",
                "--force"
        }, forced.context());
        assertEquals(ExitCodes.SUCCESS, forcedExit);
        String forcedConfigText = read(PathUtil.devharnessConfig(tempDir.resolve("reuse-project")));
        assertTrue(forcedConfigText.contains("\"preset\": \"springboot-manual-ide-test\""));
        assertEquals(firstGoal, firstValue(forced.stdout(), "goal_key: "));
    }

    @Test
    void quickstartDoesNotReuseOpenGoalFromDifferentModuleOrProfile() {
        Harness prior = new Harness(tempDir);
        int priorExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "cross-module-project",
                "--profile", "java-api-change-with-graph",
                "--task", "Payment graph-aware change",
                "--module", "payment",
                "--mode", "api"
        }, prior.context());
        assertEquals(ExitCodes.SUCCESS, priorExit);
        String priorGoal = firstValue(prior.stdout(), "goal_key: ");

        Harness patch = new Harness(tempDir);
        int patchExit = new CommandRouter().run(new String[]{
                "quickstart",
                "--project-root", "cross-module-project",
                "--preset", "springboot-manual-ide-test",
                "--task", "Fix user NPE bug",
                "--module", "user",
                "--mode", "patch"
        }, patch.context());
        assertEquals(ExitCodes.SUCCESS, patchExit);

        String patchGoal = firstValue(patch.stdout(), "goal_key: ");
        assertTrue(patch.stdout().contains("quickstart: ready"));
        assertTrue(patch.stdout().contains("profile: java-api-patch"));
        assertTrue(patch.stdout().contains("module: user"));
        assertTrue(patchGoal.length() > 0);
        assertFalse(priorGoal.equals(patchGoal));
    }

    @Test
    void quickstartMissingTaskReturnsActionableNextCommand() {
        Harness quickstart = new Harness(tempDir);
        int exit = new CommandRouter().run(new String[]{
                "quickstart",
                "--project-root", "missing-task-project"
        }, quickstart.context());

        assertEquals(ExitCodes.USAGE_ERROR, exit);
        assertTrue(quickstart.stdout().contains("quickstart: missing_task"));
        assertTrue(quickstart.stdout().contains("readiness: not_ready"));
        assertTrue(quickstart.stdout().contains("next_command: dhk quickstart"));
    }

    private String read(Path path) throws Exception {
        return new String(Files.readAllBytes(path), "UTF-8");
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
