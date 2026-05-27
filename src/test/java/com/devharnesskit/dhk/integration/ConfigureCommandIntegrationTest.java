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

final class ConfigureCommandIntegrationTest {
    @TempDir
    Path tempDir;

    @Test
    void configureInitShowDoctorAndExplainManualSpringBootPreset() throws Exception {
        Harness init = new Harness(tempDir);
        int initExit = new CommandRouter().run(new String[]{
                "configure", "init",
                "--project-root", "manual-demo",
                "--preset", "springboot-manual-ide-test",
                "--target", "all",
                "--force"
        }, init.context());

        Path root = tempDir.resolve("manual-demo");
        Path config = PathUtil.devharnessConfig(root);
        assertEquals(ExitCodes.SUCCESS, initExit);
        assertTrue(Files.isRegularFile(config));
        String configText = read(config);
        assertTrue(configText.contains("\"schema_version\": \"devharness-config/v1-alpha\""));
        assertTrue(configText.contains("\"verification.compile.mode\": \"manual\""));
        assertTrue(configText.contains("\"verification.test.mode\": \"manual\""));
        assertTrue(configText.contains("\"verification.test.manual_trigger\": \"IDE test button\""));
        assertTrue(configText.contains("\"verification.graph.mode\": \"required\""));
        assertTrue(init.stdout().contains("compile_mode: manual"));
        assertTrue(init.stdout().contains("test_mode: manual"));
        assertTrue(init.stdout().contains("graph_mode: required"));
        assertTrue(init.stdout().contains("graph_required: true"));

        Harness show = new Harness(tempDir);
        int showExit = new CommandRouter().run(new String[]{
                "configure", "show",
                "--project-root", root.toString()
        }, show.context());
        assertEquals(ExitCodes.SUCCESS, showExit);
        assertTrue(show.stdout().contains("configure show"));
        assertTrue(show.stdout().contains("project_type: springboot-enterprise-large"));
        assertTrue(show.stdout().contains("test_trigger: IDE test button"));

        Harness doctor = new Harness(tempDir);
        int doctorExit = new CommandRouter().run(new String[]{
                "configure", "doctor",
                "--project-root", root.toString()
        }, doctor.context());
        assertEquals(ExitCodes.SUCCESS, doctorExit);
        assertTrue(doctor.stdout().contains("status: ok"));
        assertTrue(doctor.stdout().contains("warnings: none"));

        Harness explain = new Harness(tempDir);
        int explainExit = new CommandRouter().run(new String[]{
                "configure", "explain",
                "--key", "verification.test.mode"
        }, explain.context());
        assertEquals(ExitCodes.SUCCESS, explainExit);
        assertTrue(explain.stdout().contains("manual requires manual_evidence_status=passed"));
    }

    @Test
    void configureInitAutoPresetSupportsJsonOutput() {
        Harness init = new Harness(tempDir);
        int initExit = new CommandRouter().run(new String[]{
                "configure", "init",
                "--project-root", "auto-demo",
                "--preset", "springboot-auto-test",
                "--force",
                "--json"
        }, init.context());

        assertEquals(ExitCodes.SUCCESS, initExit);
        assertTrue(init.stdout().contains("\"command\": \"configure init\""));
        assertTrue(init.stdout().contains("\"compile_mode\": \"auto\""));
        assertTrue(init.stdout().contains("\"test_mode\": \"auto\""));
        assertTrue(init.stdout().contains("\"graph_mode\": \"off\""));
        assertTrue(init.stdout().contains("\"graph_required\": false"));
    }

    @Test
    void configureInitDryRunPrintsPlanWithoutWritingConfig() throws Exception {
        Harness dryRun = new Harness(tempDir);
        int dryRunExit = new CommandRouter().run(new String[]{
                "configure", "init",
                "--project-root", "dry-run-demo",
                "--preset", "springboot-manual-ide-test",
                "--dry-run",
                "--json"
        }, dryRun.context());

        Path root = tempDir.resolve("dry-run-demo");
        assertEquals(ExitCodes.SUCCESS, dryRunExit);
        assertTrue(dryRun.stdout().contains("\"command\": \"configure init dry-run\""));
        assertTrue(dryRun.stdout().contains("\"dry_run\": true"));
        assertTrue(dryRun.stdout().contains("\"would_write\": true"));
        assertTrue(dryRun.stdout().contains("\"config_created\": false"));
        assertTrue(Files.notExists(PathUtil.devharnessConfig(root)));

        Harness init = new Harness(tempDir);
        int initExit = new CommandRouter().run(new String[]{
                "configure", "init",
                "--project-root", "dry-run-demo",
                "--preset", "springboot-manual-ide-test"
        }, init.context());
        assertEquals(ExitCodes.SUCCESS, initExit);

        Harness overwritePlan = new Harness(tempDir);
        int overwritePlanExit = new CommandRouter().run(new String[]{
                "configure", "init",
                "--project-root", "dry-run-demo",
                "--preset", "springboot-auto-test",
                "--dry-run"
        }, overwritePlan.context());
        assertEquals(ExitCodes.SUCCESS, overwritePlanExit);
        assertTrue(overwritePlan.stdout().contains("configure init dry-run"));
        assertTrue(overwritePlan.stdout().contains("would_write: false"));
        assertTrue(overwritePlan.stdout().contains("force_required: true"));
    }

    @Test
    void configureInitSupportsManualAliasAndGraphAdvisoryPreset() throws Exception {
        Harness manualAlias = new Harness(tempDir);
        int manualAliasExit = new CommandRouter().run(new String[]{
                "configure", "init",
                "--project-root", "manual-alias-demo",
                "--preset", "manual-ide-test",
                "--force"
        }, manualAlias.context());
        assertEquals(ExitCodes.SUCCESS, manualAliasExit);
        assertTrue(manualAlias.stdout().contains("preset: springboot-manual-ide-test"));
        assertTrue(manualAlias.stdout().contains("compile_mode: manual"));
        assertTrue(manualAlias.stdout().contains("test_mode: manual"));

        Harness graphAdvisory = new Harness(tempDir);
        int graphAdvisoryExit = new CommandRouter().run(new String[]{
                "configure", "init",
                "--project-root", "graph-advisory-demo",
                "--preset", "graph-advisory",
                "--force"
        }, graphAdvisory.context());
        assertEquals(ExitCodes.SUCCESS, graphAdvisoryExit);
        assertTrue(graphAdvisory.stdout().contains("preset: graph-advisory"));
        assertTrue(graphAdvisory.stdout().contains("graph_mode: advisory"));
        assertTrue(graphAdvisory.stdout().contains("graph_required: false"));
    }

    @Test
    void configureInitAppliesVerificationOverridesAndAdapterTarget() throws Exception {
        Harness init = new Harness(tempDir);
        int initExit = new CommandRouter().run(new String[]{
                "configure", "init",
                "--project-root", "override-demo",
                "--preset", "springboot-auto-test",
                "--compile", "manual",
                "--test", "disabled",
                "--graph", "advisory",
                "--target", "codex",
                "--force",
                "--json"
        }, init.context());

        Path config = PathUtil.devharnessConfig(tempDir.resolve("override-demo"));
        String configText = read(config);
        assertEquals(ExitCodes.SUCCESS, initExit);
        assertTrue(init.stdout().contains("\"compile_mode\": \"manual\""));
        assertTrue(init.stdout().contains("\"test_mode\": \"disabled\""));
        assertTrue(init.stdout().contains("\"graph_mode\": \"advisory\""));
        assertTrue(init.stdout().contains("\"graph_required\": false"));
        assertTrue(configText.contains("\"verification.compile.mode\": \"manual\""));
        assertTrue(configText.contains("\"verification.test.mode\": \"disabled\""));
        assertTrue(configText.contains("\"verification.graph.mode\": \"advisory\""));
        assertTrue(configText.contains("\"adapter.target\": \"codex\""));
    }

    @Test
    void configureInitRejectsOverwriteWithoutForce() {
        Harness first = new Harness(tempDir);
        int firstExit = new CommandRouter().run(new String[]{
                "configure", "init",
                "--project-root", "overwrite-demo",
                "--preset", "springboot-auto-test"
        }, first.context());
        assertEquals(ExitCodes.SUCCESS, firstExit);

        Harness second = new Harness(tempDir);
        int secondExit = new CommandRouter().run(new String[]{
                "configure", "init",
                "--project-root", "overwrite-demo",
                "--preset", "springboot-manual-ide-test"
        }, second.context());
        assertEquals(ExitCodes.RUNTIME_ERROR, secondExit);
        assertTrue(second.stderr().contains("already exists. Use --force"));
    }

    @Test
    void configureDoctorReportsInvalidModeAndUnknownFields() throws Exception {
        Path root = tempDir.resolve("invalid-demo");
        Path config = PathUtil.devharnessConfig(root);
        Files.createDirectories(config.getParent());
        Files.write(config, ("{\n"
                + "  \"schema_version\": \"devharness-config/v1-alpha\",\n"
                + "  \"verification.compile.mode\": \"robot\",\n"
                + "  \"verification.test.mode\": \"manual\",\n"
                + "  \"verification.test.manual_trigger\": \"IDE test button\",\n"
                + "  \"verification.test.required_evidence\": \"test_scope\",\n"
                + "  \"verification.graph.required\": \"maybe\",\n"
                + "  \"unexpected\": \"value\"\n"
                + "}\n").getBytes("UTF-8"));

        Harness doctor = new Harness(tempDir);
        int doctorExit = new CommandRouter().run(new String[]{
                "configure", "doctor",
                "--project-root", root.toString()
        }, doctor.context());

        assertEquals(ExitCodes.VALIDATION_ERROR, doctorExit);
        assertTrue(doctor.stdout().contains("status: warning"));
        assertTrue(doctor.stdout().contains("unknown fields: unexpected"));
        assertTrue(doctor.stdout().contains("verification.compile.mode should be one of"));
        assertTrue(doctor.stdout().contains("verification.graph.required should be true/false"));
        assertTrue(doctor.stdout().contains("verification.test.required_evidence should include"));
    }

    @Test
    void configureDoctorJsonReportsInvalidJson() throws Exception {
        Path root = tempDir.resolve("invalid-json-demo");
        Path config = PathUtil.devharnessConfig(root);
        Files.createDirectories(config.getParent());
        Files.write(config, "{ invalid json".getBytes("UTF-8"));

        Harness doctor = new Harness(tempDir);
        int doctorExit = new CommandRouter().run(new String[]{
                "configure", "doctor",
                "--project-root", root.toString(),
                "--json"
        }, doctor.context());

        assertEquals(ExitCodes.VALIDATION_ERROR, doctorExit);
        assertTrue(doctor.stdout().contains("\"command\": \"configure doctor\""));
        assertTrue(doctor.stdout().contains("\"status\": \"warning\""));
        assertTrue(doctor.stdout().contains("invalid JSON"));
    }

    @Test
    void configureShowAndDoctorReportMissingConfig() {
        Harness show = new Harness(tempDir);
        int showExit = new CommandRouter().run(new String[]{
                "configure", "show",
                "--project-root", "missing-demo"
        }, show.context());
        assertEquals(ExitCodes.NOT_FOUND, showExit);
        assertTrue(show.stderr().contains("config missing"));

        Harness doctor = new Harness(tempDir);
        int doctorExit = new CommandRouter().run(new String[]{
                "configure", "doctor",
                "--project-root", "missing-demo"
        }, doctor.context());
        assertEquals(ExitCodes.NOT_FOUND, doctorExit);
        assertTrue(doctor.stdout().contains("status: missing"));
    }

    @Test
    void manualVerificationPolicyIsExportedIntoGoalContextAndRequiredChecks() throws Exception {
        Harness configure = new Harness(tempDir);
        int configureExit = new CommandRouter().run(new String[]{
                "configure", "init",
                "--project-root", "manual-goal",
                "--preset", "springboot-manual-ide-test",
                "--force"
        }, configure.context());
        assertEquals(ExitCodes.SUCCESS, configureExit);

        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "manual-goal",
                "--profile", "java-api-change",
                "--task", "Manual verification goal",
                "--module", "order",
                "--mode", "api"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");
        Path root = tempDir.resolve("manual-goal");
        String goalContext = read(PathUtil.goalContext(root));
        assertTrue(goalContext.contains("<verification-policy>"));
        assertTrue(goalContext.contains("- compile_mode: manual"));
        assertTrue(goalContext.contains("- test_mode: manual"));
        assertTrue(goalContext.contains("- auto_maven_test: disabled"));
        assertTrue(goalContext.contains("do not run mvn test automatically"));
        assertTrue(goalContext.contains("<manual-verification-contract>"));
        assertTrue(goalContext.contains("- manual_evidence_status=passed"));
        assertTrue(goalContext.contains("- compile_scope=<module or changed classes>"));
        assertTrue(goalContext.contains("- test_scope=<class or method>"));
        assertTrue(goalContext.contains("- manual_evidence_path=<path>"));

        Harness next = new Harness(tempDir);
        int nextExit = new CommandRouter().run(new String[]{
                "goal", "next",
                "--project-root", "manual-goal",
                "--goal", goalKey
        }, next.context());
        assertEquals(ExitCodes.SUCCESS, nextExit);
        assertTrue(next.stdout().contains("  - manual-compile"));
        assertTrue(next.stdout().contains("  - manual-test"));
    }

    @Test
    void autoVerificationPolicyKeepsCompileAndTestRequiredChecks() throws Exception {
        Harness configure = new Harness(tempDir);
        int configureExit = new CommandRouter().run(new String[]{
                "configure", "init",
                "--project-root", "auto-goal",
                "--preset", "springboot-auto-test",
                "--force"
        }, configure.context());
        assertEquals(ExitCodes.SUCCESS, configureExit);

        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "auto-goal",
                "--profile", "java-api-change",
                "--task", "Auto verification goal",
                "--module", "order",
                "--mode", "api"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");
        String goalContext = read(PathUtil.goalContext(tempDir.resolve("auto-goal")));
        assertTrue(goalContext.contains("- compile_mode: auto"));
        assertTrue(goalContext.contains("- test_mode: auto"));
        assertTrue(goalContext.contains("- auto_maven_test: enabled"));

        Harness next = new Harness(tempDir);
        int nextExit = new CommandRouter().run(new String[]{
                "goal", "next",
                "--project-root", "auto-goal",
                "--goal", goalKey
        }, next.context());
        assertEquals(ExitCodes.SUCCESS, nextExit);
        assertTrue(next.stdout().contains("  - compile"));
        assertTrue(next.stdout().contains("  - test"));
    }

    @Test
    void manualVerificationChecksFailClosedUntilEvidenceIsRecorded() throws Exception {
        Harness configure = new Harness(tempDir);
        int configureExit = new CommandRouter().run(new String[]{
                "configure", "init",
                "--project-root", "manual-check-goal",
                "--preset", "springboot-manual-ide-test",
                "--force"
        }, configure.context());
        assertEquals(ExitCodes.SUCCESS, configureExit);

        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "manual-check-goal",
                "--profile", "java-api-change",
                "--task", "Manual check verification goal",
                "--module", "order",
                "--mode", "api"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");

        recordJavaGoalSteps("manual-check-goal", goalKey,
                "compile_result=pending; test_result=pending; sensitive_result=ok");

        Harness verifyMissing = new Harness(tempDir);
        int verifyMissingExit = new CommandRouter().run(new String[]{
                "goal", "verify",
                "--project-root", "manual-check-goal",
                "--goal", goalKey
        }, verifyMissing.context());
        assertEquals(ExitCodes.SUCCESS, verifyMissingExit);
        assertTrue(verifyMissing.stdout().contains("ready_to_complete: false"));
        assertTrue(verifyMissing.stdout().contains("manual-compile: failed"));
        assertTrue(verifyMissing.stdout().contains("manual-test: failed"));
        assertTrue(verifyMissing.stdout().contains("manual evidence artifact missing"));

        Harness completeMissing = new Harness(tempDir);
        int completeMissingExit = new CommandRouter().run(new String[]{
                "goal", "complete",
                "--project-root", "manual-check-goal",
                "--goal", goalKey
        }, completeMissing.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, completeMissingExit);
        assertTrue(completeMissing.stdout().contains("decision: not_ready"));

        Path root = tempDir.resolve("manual-check-goal");
        Path manualEvidence = root.resolve(".agents/memory/artifacts/manual-evidence.md");
        Files.createDirectories(manualEvidence.getParent());
        Files.write(manualEvidence, "IDE build and focused test passed\n".getBytes("UTF-8"));
        recordStep("manual-check-goal", goalKey, "Recorded manual verification evidence", "",
                "manual_evidence_status=passed; compile_scope=order module; test_scope=OrderServiceTest; "
                        + "manual_evidence_path=.agents/memory/artifacts/manual-evidence.md; tester=developer; "
                        + "compile_result=manual passed; test_result=manual passed; sensitive_result=ok");

        Harness verifyReady = new Harness(tempDir);
        int verifyReadyExit = new CommandRouter().run(new String[]{
                "goal", "verify",
                "--project-root", "manual-check-goal",
                "--goal", goalKey
        }, verifyReady.context());
        assertEquals(ExitCodes.SUCCESS, verifyReadyExit);
        assertTrue(verifyReady.stdout().contains("manual-compile: passed"), verifyReady.stdout());
        assertTrue(verifyReady.stdout().contains("manual-test: passed"), verifyReady.stdout());
        assertTrue(verifyReady.stdout().contains("manual-test evidence passed"), verifyReady.stdout());
    }

    @Test
    void autoVerificationStillRunsCompileAndFailsClosedWhenPomIsMissing() {
        Harness configure = new Harness(tempDir);
        int configureExit = new CommandRouter().run(new String[]{
                "configure", "init",
                "--project-root", "auto-check-goal",
                "--preset", "springboot-auto-test",
                "--force"
        }, configure.context());
        assertEquals(ExitCodes.SUCCESS, configureExit);

        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "auto-check-goal",
                "--profile", "java-api-change",
                "--task", "Auto check verification goal",
                "--module", "order",
                "--mode", "api"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");
        recordJavaGoalSteps("auto-check-goal", goalKey,
                "compile_result=pending; test_result=pending; sensitive_result=ok");

        Harness verify = new Harness(tempDir);
        int verifyExit = new CommandRouter().run(new String[]{
                "goal", "verify",
                "--project-root", "auto-check-goal",
                "--goal", goalKey
        }, verify.context());

        assertEquals(ExitCodes.SUCCESS, verifyExit);
        assertTrue(verify.stdout().contains("compile: skipped"), verify.stdout());
        assertTrue(verify.stdout().contains("ready_to_complete: false"), verify.stdout());
        assertTrue(verify.stdout().contains("check compile is skipped; accepted_statuses=passed"), verify.stdout());
    }

    @Test
    void disabledVerificationRequiresRiskAndRollbackEvidence() throws Exception {
        Harness configure = new Harness(tempDir);
        int configureExit = new CommandRouter().run(new String[]{
                "configure", "init",
                "--project-root", "risk-check-goal",
                "--preset", "springboot-auto-test",
                "--compile", "disabled",
                "--test", "disabled",
                "--force"
        }, configure.context());
        assertEquals(ExitCodes.SUCCESS, configureExit);

        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "risk-check-goal",
                "--profile", "java-api-change",
                "--task", "Risk verification goal",
                "--module", "order",
                "--mode", "api"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");
        recordJavaGoalSteps("risk-check-goal", goalKey,
                "compile_result=disabled; test_result=disabled; sensitive_result=ok");

        Harness verifyMissing = new Harness(tempDir);
        int verifyMissingExit = new CommandRouter().run(new String[]{
                "goal", "verify",
                "--project-root", "risk-check-goal",
                "--goal", goalKey
        }, verifyMissing.context());
        assertEquals(ExitCodes.SUCCESS, verifyMissingExit);
        assertTrue(verifyMissing.stdout().contains("verification-risk: failed"), verifyMissing.stdout());
        assertTrue(verifyMissing.stdout().contains("waive_reason is required"), verifyMissing.stdout());

        Path root = tempDir.resolve("risk-check-goal");
        Path rollback = root.resolve(".agents/memory/artifacts/rollback-plan.md");
        Files.createDirectories(rollback.getParent());
        Files.write(rollback, "Rollback by reverting the changed file\n".getBytes("UTF-8"));
        recordStep("risk-check-goal", goalKey, "Recorded verification risk waiver", "",
                "waive_reason=CLI verification unavailable; approver=tech-lead; "
                        + "risk_scope=single module manual review; "
                        + "rollback_plan=.agents/memory/artifacts/rollback-plan.md; "
                        + "compile_result=disabled; test_result=disabled; sensitive_result=ok");

        Harness check = new Harness(tempDir);
        int checkExit = new CommandRouter().run(new String[]{
                "goal", "check",
                "--project-root", "risk-check-goal",
                "--goal", goalKey,
                "--check", "verification-risk"
        }, check.context());
        assertEquals(ExitCodes.SUCCESS, checkExit);
        assertTrue(check.stdout().contains("check_key: verification-risk"), check.stdout());
        assertTrue(check.stdout().contains("status: passed"), check.stdout());
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

    private void recordJavaGoalSteps(String projectRoot, String goalKey, String verifyEvidence) {
        recordStep(projectRoot, goalKey, "Inspected existing code", "",
                "goal_understanding=Verify manual evidence checks; assumptions=No product code changes; "
                        + "existing_controller=OrderController; existing_service=OrderService; "
                        + "existing_mapper=OrderMapper; existing_tests=OrderServiceTest");
        recordStep(projectRoot, goalKey, "Planned manual verification", "",
                "impacted_files=GoalCheckService; risk_points=manual evidence gaps; "
                        + "verification_plan=goal verify manual checks");
        recordStep(projectRoot, goalKey, "Implemented manual checks",
                "src/main/java/com/devharnesskit/dhk/service/goal/GoalCheckService.java",
                "implementation_summary=manual verification check support");
        recordStep(projectRoot, goalKey, "Recorded verification status", "", verifyEvidence);
    }

    private void recordStep(String projectRoot, String goalKey, String summary,
                            String changedFiles, String evidence) {
        java.util.List<String> args = new java.util.ArrayList<String>();
        args.add("goal");
        args.add("step");
        args.add("--project-root");
        args.add(projectRoot);
        args.add("--goal");
        args.add(goalKey);
        args.add("--summary");
        args.add(summary);
        if (changedFiles != null && changedFiles.length() > 0) {
            args.add("--changed-files");
            args.add(changedFiles);
        }
        args.add("--evidence");
        args.add(evidence);
        Harness step = new Harness(tempDir);
        int exit = new CommandRouter().run(args.toArray(new String[args.size()]), step.context());
        assertEquals(ExitCodes.SUCCESS, exit, step.stdout() + step.stderr());
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

        String stderr() {
            return err.toString();
        }
    }

    private static final class FixedClock implements Clock {
        public Instant now() {
            return Instant.parse("2026-05-27T00:00:00Z");
        }
    }
}
