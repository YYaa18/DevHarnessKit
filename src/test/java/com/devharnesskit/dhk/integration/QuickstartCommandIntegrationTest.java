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
    void quickstartEnumErrorsIncludeValidValuesBeforeWritingFiles() {
        Harness invalidMode = new Harness(tempDir);
        int invalidModeExit = new CommandRouter().run(new String[]{
                "quickstart",
                "--project-root", "quick-invalid-mode",
                "--task", "Implement order query endpoint",
                "--mode", "robot"
        }, invalidMode.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, invalidModeExit);
        assertTrue(invalidMode.stderr().contains("error_code: INVALID_RECOMMENDATION_MODE"));
        assertTrue(invalidMode.stderr().contains("valid_values:"));
        assertFalse(Files.exists(PathUtil.workBrief(tempDir.resolve("quick-invalid-mode"))));

        Harness invalidGraph = new Harness(tempDir);
        int invalidGraphExit = new CommandRouter().run(new String[]{
                "quickstart",
                "--project-root", "quick-invalid-graph",
                "--task", "Implement order query endpoint",
                "--graph", "maybe"
        }, invalidGraph.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, invalidGraphExit);
        assertTrue(invalidGraph.stderr().contains("error_code: INVALID_GRAPH_MODE"));
        assertTrue(invalidGraph.stderr().contains("valid_values:"));
        assertFalse(Files.exists(PathUtil.workBrief(tempDir.resolve("quick-invalid-graph"))));
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
        assertTrue(quickstart.stdout().contains("next_command: .agents/skills/devharness-goal-development/scripts/goal-next.sh"));
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
    void quickstartCreatesNewGoalByDefaultAndResumeExistingIsExplicit() throws Exception {
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
        assertTrue(second.stdout().contains("quickstart: ready"));
        assertTrue(second.stdout().contains("config: existing"));
        String secondGoal = firstValue(second.stdout(), "goal_key: ");
        assertTrue(secondGoal.length() > 0);
        assertFalse(firstGoal.equals(secondGoal));

        Harness resumed = new Harness(tempDir);
        int resumedExit = new CommandRouter().run(new String[]{
                "quickstart",
                "--project-root", "reuse-project",
                "--preset", "springboot-manual-ide-test",
                "--task", "First goal",
                "--module", "order",
                "--resume-existing"
        }, resumed.context());
        assertEquals(ExitCodes.SUCCESS, resumedExit);
        assertTrue(resumed.stdout().contains("quickstart: existing_goal"));
        assertEquals(secondGoal, firstValue(resumed.stdout(), "goal_key: "));

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
        assertFalse(secondGoal.equals(firstValue(forced.stdout(), "goal_key: ")));
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
    void initialNewProjectPresetStartsPatchGoalAndWritesInitialProjectWarning() throws Exception {
        Harness quickstart = new Harness(tempDir);
        int exit = new CommandRouter().run(new String[]{
                "quickstart",
                "--project-root", "initial-new-project",
                "--preset", "initial-new-project",
                "--task", "Create initial project skeleton",
                "--module", "app"
        }, quickstart.context());

        Path root = tempDir.resolve("initial-new-project");
        assertEquals(ExitCodes.SUCCESS, exit);
        assertTrue(quickstart.stdout().contains("quickstart: ready"));
        assertTrue(quickstart.stdout().contains("preset: initial-new-project"));
        assertTrue(quickstart.stdout().contains("profile: java-api-patch"));
        assertTrue(quickstart.stdout().contains("confirmation_required: true"));
        assertTrue(quickstart.stdout().contains("confirmation_reason: 项目配置要求实施前确认任务边界和推进顺序。"));

        String configText = read(PathUtil.devharnessConfig(root));
        assertTrue(configText.contains("\"verification.compile.mode\": \"disabled\""));
        assertTrue(configText.contains("\"verification.test.mode\": \"disabled\""));
        assertTrue(configText.contains("\"verification.initial_project.enabled\": \"true\""));
        assertTrue(configText.contains("\"workflow.pre_work.confirmation.required\": \"true\""));
        assertFalse(configText.contains("\"verification.demo.enabled\""));

        String workBrief = read(PathUtil.workBrief(root));
        assertTrue(workBrief.contains("confirmation_required: true"));
        assertTrue(workBrief.contains("requires_user_confirmation_reason: 项目配置要求实施前确认任务边界和推进顺序。"));
        assertTrue(workBrief.contains("同意当前任务边界、实施范围和推进顺序"));
        assertTrue(workBrief.contains("如任务包含分步交付，同意每一步完成后先停下等待确认"));
        assertTrue(workBrief.contains("了解验证证据可能需要人工、IDE 或替代风险说明"));

        String agentBrief = read(PathUtil.agentBrief(root));
        assertTrue(agentBrief.contains("\"require_user_confirmation\": true"));

        String goalContext = read(PathUtil.goalContext(root));
        assertTrue(goalContext.contains("<initial-project-warning>"));
        assertTrue(goalContext.contains("initial project mode does not prove production correctness"));
    }

    @Test
    void quickstartRequiresConfirmationForExplicitStepwiseUserIntent() throws Exception {
        Harness quickstart = new Harness(tempDir);
        int exit = new CommandRouter().run(new String[]{
                "quickstart",
                "--project-root", "stepwise-confirmation-project",
                "--preset", "springboot-auto-test",
                "--task", "Implement address validation step-by-step and wait for confirmation after each step",
                "--module", "address"
        }, quickstart.context());

        Path root = tempDir.resolve("stepwise-confirmation-project");
        assertEquals(ExitCodes.SUCCESS, exit);
        assertTrue(quickstart.stdout().contains("confirmation_required: true"));
        assertTrue(quickstart.stdout().contains("confirmation_reason: 任务要求分步或确认后推进，需要等待用户确认。"));

        String workBrief = read(PathUtil.workBrief(root));
        assertTrue(workBrief.contains("confirmation_required: true"));
        assertTrue(workBrief.contains("同意当前任务边界、实施范围和推进顺序"));
        assertTrue(workBrief.contains("如任务包含分步交付，同意每一步完成后先停下等待确认"));

        String agentBrief = read(PathUtil.agentBrief(root));
        assertTrue(agentBrief.contains("\"current_action\": \"wait_for_user_answer\""));

        String goalKey = firstValue(quickstart.stdout(), "goal_key: ");
        Harness next = new Harness(tempDir);
        int nextExit = new CommandRouter().run(new String[]{
                "goal", "next",
                "--project-root", "stepwise-confirmation-project",
                "--goal", goalKey
        }, next.context());
        assertEquals(ExitCodes.SUCCESS, nextExit);
        assertTrue(next.stdout().contains("current_action: wait_for_user_answer"));
        assertTrue(next.stdout().contains("blocking interaction requires user answer"));
        assertTrue(next.stdout().contains("dhk.sh brief answer --request"));

        String configText = read(PathUtil.devharnessConfig(root));
        assertTrue(configText.contains("\"workflow.pre_work.confirmation.required\": \"false\""));
    }

    @Test
    void preWorkFileWriteCheckFailsWhenBusinessFilesChangeBeforeAnswer() throws Exception {
        Harness quickstart = new Harness(tempDir);
        int exit = new CommandRouter().run(new String[]{
                "quickstart",
                "--project-root", "pre-work-violation-project",
                "--preset", "springboot-auto-test",
                "--task", "Implement address validation step-by-step and wait for confirmation after each step",
                "--module", "address"
        }, quickstart.context());

        Path root = tempDir.resolve("pre-work-violation-project");
        assertEquals(ExitCodes.SUCCESS, exit);
        String requestId = openPreWorkRequest(root);
        assertTrue(requestId.length() > 0);
        Path source = root.resolve("src/main/java/example/App.java");
        Files.createDirectories(source.getParent());
        Files.write(source, "package example;\npublic final class App {}\n".getBytes("UTF-8"));

        Harness answer = new Harness(tempDir);
        int answerExit = new CommandRouter().run(new String[]{
                "brief", "answer",
                "--project-root", "pre-work-violation-project",
                "--request", requestId,
                "--choice", "按建议继续"
        }, answer.context());
        assertEquals(ExitCodes.SUCCESS, answerExit);

        Harness check = new Harness(tempDir);
        int checkExit = new CommandRouter().run(new String[]{
                "goal", "check",
                "--project-root", "pre-work-violation-project",
                "--goal", firstValue(quickstart.stdout(), "goal_key: "),
                "--check", "pre-work-file-write"
        }, check.context());

        assertEquals(ExitCodes.SUCCESS, checkExit);
        assertTrue(check.stdout().contains("check_key: pre-work-file-write"));
        assertTrue(check.stdout().contains("status: failed"));
        assertTrue(check.stdout().contains("pre_work_file_write_violation"));
    }

    @Test
    void preWorkFileWriteCheckPassesWhenBusinessFilesChangeAfterAnswer() throws Exception {
        Harness quickstart = new Harness(tempDir);
        int exit = new CommandRouter().run(new String[]{
                "quickstart",
                "--project-root", "pre-work-allowed-project",
                "--preset", "springboot-auto-test",
                "--task", "Implement address validation step-by-step and wait for confirmation after each step",
                "--module", "address"
        }, quickstart.context());

        Path root = tempDir.resolve("pre-work-allowed-project");
        assertEquals(ExitCodes.SUCCESS, exit);
        String requestId = openPreWorkRequest(root);
        assertTrue(requestId.length() > 0);

        Harness answer = new Harness(tempDir);
        int answerExit = new CommandRouter().run(new String[]{
                "brief", "answer",
                "--project-root", "pre-work-allowed-project",
                "--request", requestId,
                "--choice", "按建议继续"
        }, answer.context());
        assertEquals(ExitCodes.SUCCESS, answerExit);

        Path source = root.resolve("src/main/java/example/App.java");
        Files.createDirectories(source.getParent());
        Files.write(source, "package example;\npublic final class App {}\n".getBytes("UTF-8"));

        Harness check = new Harness(tempDir);
        int checkExit = new CommandRouter().run(new String[]{
                "goal", "check",
                "--project-root", "pre-work-allowed-project",
                "--goal", firstValue(quickstart.stdout(), "goal_key: "),
                "--check", "pre-work-file-write"
        }, check.context());

        assertEquals(ExitCodes.SUCCESS, checkExit);
        assertTrue(check.stdout().contains("check_key: pre-work-file-write"));
        assertTrue(check.stdout().contains("status: passed"));
        assertTrue(check.stdout().contains("pre-work file write guard passed"));
    }

    @Test
    void legacyDemoNoBuildPresetNormalizesToInitialNewProject() throws Exception {
        Harness quickstart = new Harness(tempDir);
        int exit = new CommandRouter().run(new String[]{
                "quickstart",
                "--project-root", "legacy-initial-project",
                "--preset", "demo-no-build",
                "--task", "Create initial project skeleton",
                "--module", "app"
        }, quickstart.context());

        assertEquals(ExitCodes.SUCCESS, exit);
        assertTrue(quickstart.stdout().contains("preset: initial-new-project"));
        String configText = read(PathUtil.devharnessConfig(tempDir.resolve("legacy-initial-project")));
        assertTrue(configText.contains("\"preset\": \"initial-new-project\""));
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

    private String openPreWorkRequest(Path root) throws Exception {
        String[] lines = read(PathUtil.interactionRequests(root)).split("\\r?\\n");
        for (String line : lines) {
            if (line.contains("\tpre_work\t") && line.contains("\topen\t")) {
                return line.split("\\t", -1)[0];
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
                    com.devharnesskit.dhk.testsupport.Utf8HarnessSupport.printStream(out),
                    com.devharnesskit.dhk.testsupport.Utf8HarnessSupport.printStream(err),
                    new FixedClock()
            );
        }

        String stdout() {
            return com.devharnesskit.dhk.testsupport.Utf8HarnessSupport.text(out);
        }

        String stderr() {
            return com.devharnesskit.dhk.testsupport.Utf8HarnessSupport.text(err);
        }
    }

    private static final class FixedClock implements Clock {
        public Instant now() {
            return Instant.parse("2026-05-27T00:00:00Z");
        }
    }
}
