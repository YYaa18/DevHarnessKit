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
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GoalIntegrationTest {
    @TempDir
    Path tempDir;

    @Test
    void goalStartNextStepStatusAndExportCreateGoalContext() throws Exception {
        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "demo",
                "--profile", "java-api-change",
                "--task", "Implement order query API",
                "--module", "order",
                "--mode", "api",
                "--condition", "compile and focused tests pass"
        }, start.context());

        Path root = tempDir.resolve("demo");
        Path goalContext = PathUtil.goalContext(root);
        String goalKey = firstValue(start.stdout(), "goal_key: ");

        assertEquals(ExitCodes.SUCCESS, startExit);
        assertTrue(goalKey.length() > 0);
        assertTrue(start.stdout().contains("workflow_run:"));
        assertTrue(start.stdout().contains("spec_change:"));
        assertTrue(start.stdout().contains("current_action: inspect_existing_code"));
        assertTrue(Files.isRegularFile(goalContext));
        String initialContext = new String(Files.readAllBytes(goalContext), "UTF-8");
        assertTrue(initialContext.contains("# GOAL_CONTEXT"));
        assertSectionOrder(initialContext, "# GOAL_CONTEXT", "<generated-at>", "<goal>",
                "<current-action>", "<next-instruction>", "<allowed-actions>", "<forbidden-actions>",
                "<required-evidence>", "<context-files>", "<completion-condition>", "<next-command>");
        assertTrue(initialContext.contains("inspect_existing_code"));
        assertTrue(initialContext.contains("- perform_current_action_only"));
        assertTrue(initialContext.contains("- do_not_archive_spec"));
        assertTrue(initialContext.contains("- do_not_claim_completion_before_goal_evaluate"));
        assertTrue(initialContext.contains("- existing_controller"));
        assertTrue(initialContext.contains("- .agents/memory/exports/SPEC_CONTEXT.md"));
        assertTrue(initialContext.contains("dhk goal step --goal " + goalKey));
        assertTrue(initialContext.length() <= 16 * 1024);
        assertTrue(Files.isRegularFile(PathUtil.currentContext(root)));
        assertTrue(Files.isRegularFile(PathUtil.workflowContext(root)));
        assertTrue(Files.isRegularFile(PathUtil.specContext(root)));

        assertGoalRows(root, goalKey);

        Harness next = new Harness(tempDir);
        int nextExit = new CommandRouter().run(new String[]{
                "goal", "next", "--project-root", "demo", "--goal", goalKey
        }, next.context());
        assertEquals(ExitCodes.SUCCESS, nextExit);
        assertTrue(next.stdout().contains("current_action: inspect_existing_code"));
        assertTrue(next.stdout().contains("required_evidence:"));

        Harness step = new Harness(tempDir);
        int stepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", "demo",
                "--goal", goalKey,
                "--summary", "Inspected existing controller/service/mapper/tests",
                "--changed-files", "",
                "--evidence", "existing_controller,existing_service,existing_mapper,existing_tests"
        }, step.context());
        assertEquals(ExitCodes.SUCCESS, stepExit);
        assertTrue(step.stdout().contains("step_id: 1"));
        assertTrue(step.stdout().contains("current_action: create_change_plan"));

        Harness status = new Harness(tempDir);
        int statusExit = new CommandRouter().run(new String[]{
                "goal", "status", "--project-root", "demo", "--goal", goalKey
        }, status.context());
        assertEquals(ExitCodes.SUCCESS, statusExit);
        assertTrue(status.stdout().contains("step_count: 1"));
        assertTrue(status.stdout().contains("current_action: create_change_plan"));

        Harness export = new Harness(tempDir);
        int exportExit = new CommandRouter().run(new String[]{
                "goal", "export", "--project-root", "demo", "--goal", goalKey
        }, export.context());
        assertEquals(ExitCodes.SUCCESS, exportExit);
        assertTrue(export.stdout().contains("context_path: " + goalContext));

        Harness evaluateBeforeChecks = new Harness(tempDir);
        int evaluateBeforeChecksExit = new CommandRouter().run(new String[]{
                "goal", "evaluate", "--project-root", "demo", "--goal", goalKey
        }, evaluateBeforeChecks.context());
        assertEquals(ExitCodes.SUCCESS, evaluateBeforeChecksExit);
        assertTrue(evaluateBeforeChecks.stdout().contains("decision: not_ready"));
        assertTrue(evaluateBeforeChecks.stdout().contains("goal steps incomplete: expected 4 actions, recorded 1"));
        assertTrue(evaluateBeforeChecks.stdout().contains("check compile is pending"));

        Harness planStep = new Harness(tempDir);
        int planStepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", "demo",
                "--goal", goalKey,
                "--summary", "Planned impacted files, risks, and verification",
                "--evidence", "impacted_files=GoalIntegrationTest; risk_points=evidence model; verification_plan=goal tests"
        }, planStep.context());
        assertEquals(ExitCodes.SUCCESS, planStepExit);
        assertTrue(planStep.stdout().contains("current_action: implement_minimal_change"));

        Harness implementStep = new Harness(tempDir);
        int implementStepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", "demo",
                "--goal", goalKey,
                "--summary", "Implemented minimal goal evidence changes",
                "--changed-files", "src/main/java/com/devharnesskit/dhk/service/goal/GoalCompletionEvaluator.java",
                "--evidence", "implementation_summary=evaluator requires action evidence"
        }, implementStep.context());
        assertEquals(ExitCodes.SUCCESS, implementStepExit);
        assertTrue(implementStep.stdout().contains("current_action: verify"));

        Harness verifyStep = new Harness(tempDir);
        int verifyStepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", "demo",
                "--goal", goalKey,
                "--summary", "Recorded verification evidence",
                "--evidence", "compile_result=will run; test_result=goal integration; sensitive_result=ok"
        }, verifyStep.context());
        assertEquals(ExitCodes.SUCCESS, verifyStepExit);
        assertTrue(verifyStep.stdout().contains("current_action: verify"));

        writeMinimalPom(root);
        Harness check = new Harness(tempDir);
        int checkExit = new CommandRouter().run(new String[]{
                "goal", "check", "--project-root", "demo", "--goal", goalKey, "--all"
        }, check.context());
        assertEquals(ExitCodes.SUCCESS, checkExit);
        assertTrue(check.stdout().contains("check_key: compile"));
        assertTrue(check.stdout().contains("status: passed"));
        assertTrue(check.stdout().contains("step_count_at_check: 4"));
        assertTrue(check.stdout().contains("check_key: sensitive"));
        assertTrue(Files.isRegularFile(PathUtil.goalCheckArtifactsDirectory(root, goalKey).resolve("compile.log")));

        Harness evaluateReady = new Harness(tempDir);
        int evaluateReadyExit = new CommandRouter().run(new String[]{
                "goal", "evaluate", "--project-root", "demo", "--goal", goalKey
        }, evaluateReady.context());
        assertEquals(ExitCodes.SUCCESS, evaluateReadyExit);
        assertTrue(evaluateReady.stdout().contains("decision: ready_to_complete"));
        assertTrue(evaluateReady.stdout().contains("next_command: dhk goal complete --goal " + goalKey));

        Harness complete = new Harness(tempDir);
        int completeExit = new CommandRouter().run(new String[]{
                "goal", "complete", "--project-root", "demo", "--goal", goalKey
        }, complete.context());
        assertEquals(ExitCodes.SUCCESS, completeExit);
        assertTrue(complete.stdout().contains("status: completed"));
        assertTrue(complete.stdout().contains("checkpoint_id: 1"));
        assertTrue(Files.isRegularFile(PathUtil.goalSummary(root)));
        String summary = new String(Files.readAllBytes(PathUtil.goalSummary(root)), "UTF-8");
        assertTrue(summary.contains("# GOAL_SUMMARY"));
        assertTrue(summary.contains("checkpoint_id: 1"));
        assertSectionOrder(summary, "# GOAL_SUMMARY", "<generated-at>", "<goal>",
                "<steps>", "<checks>", "<agent-instructions>");
        assertTrue(summary.contains("- status: completed"));
        assertTrue(summary.contains("- #1 inspect_existing_code: Inspected existing controller/service/mapper/tests"));
        assertTrue(summary.contains("- [passed] sensitive: sensitive scan passed"));
        assertTrue(summary.contains("evidence_path:"));
        assertTrue(summary.contains("Do not treat generated summary text as confirmed long-term memory"));
        assertFalse(summary.contains("password="));
        assertCompletedRows(root, goalKey);
    }

    @Test
    void goalStepRequiresEvidenceForCurrentAction() throws Exception {
        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "demo",
                "--profile", "java-api-change",
                "--task", "Evidence hardening",
                "--module", "goal"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");

        Harness step = new Harness(tempDir);
        int stepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", "demo",
                "--goal", goalKey,
                "--summary", "Inspected something",
                "--evidence", "existing_controller"
        }, step.context());

        assertEquals(ExitCodes.VALIDATION_ERROR, stepExit);
        assertTrue(step.stderr().contains("Goal step evidence missing required items"));
        assertTrue(step.stderr().contains("existing_service"));
        assertTrue(step.stderr().contains("existing_mapper"));
        assertTrue(step.stderr().contains("existing_tests"));
    }

    @Test
    void goalStepRejectsSensitiveEvidenceBeforeSummaryExport() throws Exception {
        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "demo",
                "--profile", "java-api-change",
                "--task", "Sensitive goal evidence",
                "--module", "goal"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");

        Harness step = new Harness(tempDir);
        int stepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", "demo",
                "--goal", goalKey,
                "--summary", "Inspected existing code",
                "--evidence", "existing_controller,existing_service,existing_mapper,existing_tests,password=abc"
        }, step.context());

        assertEquals(ExitCodes.VALIDATION_ERROR, stepExit);
        assertTrue(step.stderr().contains("Sensitive data rejected in goal step"));
        assertFalse(Files.isRegularFile(PathUtil.goalSummary(tempDir.resolve("demo"))));
    }

    @Test
    void goalSensitiveCheckScansOriginalContextAndKeepsLogsRedacted() throws Exception {
        Path root = tempDir.resolve("demo");
        Files.createDirectories(PathUtil.goalProfilesDirectory(root));
        Files.write(PathUtil.goalProfile(root, "custom-sensitive"), ("{\n"
                + "  \"workflow_key\": \"api-change\",\n"
                + "  \"requires_spec\": \"false\",\n"
                + "  \"default_mode\": \"api\",\n"
                + "  \"actions\": \"inspect,verify\"\n"
                + "}\n").getBytes("UTF-8"));
        Files.write(PathUtil.goalCheckPolicy(root), ("{\n"
                + "  \"required_checks\": \"sensitive\",\n"
                + "  \"fail_pending_hard_gates\": \"true\"\n"
                + "}\n").getBytes("UTF-8"));

        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "demo",
                "--profile", "custom-sensitive",
                "--task", "Sensitive check original context",
                "--module", "goal"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");

        Harness inspectStep = new Harness(tempDir);
        int inspectStepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", "demo",
                "--goal", goalKey,
                "--summary", "Inspection complete",
                "--evidence", "evidence=inspection"
        }, inspectStep.context());
        assertEquals(ExitCodes.SUCCESS, inspectStepExit);

        Harness verifyStep = new Harness(tempDir);
        int verifyStepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", "demo",
                "--goal", goalKey,
                "--summary", "Verification complete",
                "--evidence", "compile_result=not required; test_result=not required; sensitive_result=pending"
        }, verifyStep.context());
        assertEquals(ExitCodes.SUCCESS, verifyStepExit);

        String rawSecret = "pass" + "word=sample-value";
        Files.write(PathUtil.goalContext(root), ("\n" + rawSecret + "\n").getBytes("UTF-8"),
                StandardOpenOption.APPEND);

        Harness check = new Harness(tempDir);
        int checkExit = new CommandRouter().run(new String[]{
                "goal", "check", "--project-root", "demo", "--goal", goalKey, "--all"
        }, check.context());
        assertEquals(ExitCodes.SUCCESS, checkExit);
        assertTrue(check.stdout().contains("check_key: sensitive"));
        assertTrue(check.stdout().contains("status: failed"));
        assertTrue(check.stdout().contains("sensitive scan failed"));

        Path log = PathUtil.goalCheckArtifactsDirectory(root, goalKey).resolve("sensitive.log");
        assertTrue(Files.isRegularFile(log));
        String logContent = new String(Files.readAllBytes(log), "UTF-8");
        assertTrue(logContent.contains("GOAL_CONTEXT.md"));
        assertFalse(logContent.contains(rawSecret));
        assertFalse(check.stdout().contains(rawSecret));

        Harness evaluate = new Harness(tempDir);
        int evaluateExit = new CommandRouter().run(new String[]{
                "goal", "evaluate", "--project-root", "demo", "--goal", goalKey
        }, evaluate.context());
        assertEquals(ExitCodes.SUCCESS, evaluateExit);
        assertTrue(evaluate.stdout().contains("decision: not_ready"));
        assertTrue(evaluate.stdout().contains("check sensitive is failed"));

        Harness complete = new Harness(tempDir);
        int completeExit = new CommandRouter().run(new String[]{
                "goal", "complete", "--project-root", "demo", "--goal", goalKey
        }, complete.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, completeExit);
        assertTrue(complete.stdout().contains("decision: not_ready"));
        assertTrue(complete.stdout().contains("check sensitive is failed"));
        assertFalse(complete.stdout().contains(rawSecret));
    }

    @Test
    void javaGoalCannotCompleteWithSkippedCompileOrTestChecks() throws Exception {
        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "demo",
                "--profile", "java-api-change",
                "--task", "Java no pom guard",
                "--module", "goal",
                "--mode", "api"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");

        recordJavaGoalSteps(goalKey);

        Harness check = new Harness(tempDir);
        int checkExit = new CommandRouter().run(new String[]{
                "goal", "check", "--project-root", "demo", "--goal", goalKey, "--all"
        }, check.context());
        assertEquals(ExitCodes.SUCCESS, checkExit);
        assertTrue(check.stdout().contains("check_key: compile"));
        assertTrue(check.stdout().contains("status: skipped"));
        assertTrue(check.stdout().contains("check_key: test"));

        Harness evaluate = new Harness(tempDir);
        int evaluateExit = new CommandRouter().run(new String[]{
                "goal", "evaluate", "--project-root", "demo", "--goal", goalKey, "--json"
        }, evaluate.context());
        assertEquals(ExitCodes.SUCCESS, evaluateExit);
        assertTrue(evaluate.stdout().contains("\"decision\": \"not_ready\""));
        assertTrue(evaluate.stdout().contains("check compile is skipped; accepted_statuses=passed"));
        assertTrue(evaluate.stdout().contains("check test is skipped; accepted_statuses=passed"));

        Harness complete = new Harness(tempDir);
        int completeExit = new CommandRouter().run(new String[]{
                "goal", "complete", "--project-root", "demo", "--goal", goalKey
        }, complete.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, completeExit);
        assertTrue(complete.stdout().contains("decision: not_ready"));
        assertTrue(complete.stdout().contains("check compile is skipped; accepted_statuses=passed"));
    }

    @Test
    void customPolicyCanAcceptSkippedCompileForNonMavenFlow() throws Exception {
        Path root = tempDir.resolve("demo");
        Files.createDirectories(PathUtil.goalProfilesDirectory(root));
        Files.write(PathUtil.goalProfile(root, "custom-non-maven"), ("{\n"
                + "  \"workflow_key\": \"api-change\",\n"
                + "  \"requires_spec\": \"false\",\n"
                + "  \"default_mode\": \"api\",\n"
                + "  \"actions\": \"inspect,verify\"\n"
                + "}\n").getBytes("UTF-8"));
        Files.write(PathUtil.goalCheckPolicy(root), ("{\n"
                + "  \"required_checks\": \"compile,sensitive\",\n"
                + "  \"accepted_compile_statuses\": \"passed,skipped\",\n"
                + "  \"accepted_sensitive_statuses\": \"passed\"\n"
                + "}\n").getBytes("UTF-8"));

        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "demo",
                "--profile", "custom-non-maven",
                "--task", "Non Maven accepted skipped compile",
                "--module", "goal"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");

        Harness inspectStep = new Harness(tempDir);
        int inspectStepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", "demo",
                "--goal", goalKey,
                "--summary", "Inspection complete",
                "--evidence", "evidence=inspection"
        }, inspectStep.context());
        assertEquals(ExitCodes.SUCCESS, inspectStepExit);

        Harness verifyStep = new Harness(tempDir);
        int verifyStepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", "demo",
                "--goal", goalKey,
                "--summary", "Verification complete",
                "--evidence", "compile_result=skipped by policy; test_result=not required; sensitive_result=ok"
        }, verifyStep.context());
        assertEquals(ExitCodes.SUCCESS, verifyStepExit);

        Harness check = new Harness(tempDir);
        int checkExit = new CommandRouter().run(new String[]{
                "goal", "check", "--project-root", "demo", "--goal", goalKey, "--all"
        }, check.context());
        assertEquals(ExitCodes.SUCCESS, checkExit);
        assertTrue(check.stdout().contains("check_key: compile"));
        assertTrue(check.stdout().contains("status: skipped"));
        assertTrue(check.stdout().contains("step_count_at_check: 2"));

        Harness evaluate = new Harness(tempDir);
        int evaluateExit = new CommandRouter().run(new String[]{
                "goal", "evaluate", "--project-root", "demo", "--goal", goalKey
        }, evaluate.context());
        assertEquals(ExitCodes.SUCCESS, evaluateExit);
        assertTrue(evaluate.stdout().contains("decision: ready_to_complete"));
    }

    @Test
    void goalEvaluateRejectsStaleChecksUntilTheyAreRerun() throws Exception {
        Path root = tempDir.resolve("demo");
        Files.createDirectories(PathUtil.goalProfilesDirectory(root));
        Files.write(PathUtil.goalProfile(root, "custom-stale"), ("{\n"
                + "  \"workflow_key\": \"api-change\",\n"
                + "  \"requires_spec\": \"false\",\n"
                + "  \"default_mode\": \"api\",\n"
                + "  \"actions\": \"inspect,verify\"\n"
                + "}\n").getBytes("UTF-8"));
        Files.write(PathUtil.goalCheckPolicy(root), ("{\n"
                + "  \"required_checks\": \"sensitive\",\n"
                + "  \"fail_pending_hard_gates\": \"true\"\n"
                + "}\n").getBytes("UTF-8"));

        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "demo",
                "--profile", "custom-stale",
                "--task", "Stale check guard",
                "--module", "goal"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");

        Harness inspectStep = new Harness(tempDir);
        int inspectStepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", "demo",
                "--goal", goalKey,
                "--summary", "Inspection complete",
                "--evidence", "evidence=inspection"
        }, inspectStep.context());
        assertEquals(ExitCodes.SUCCESS, inspectStepExit);

        Harness earlyCheck = new Harness(tempDir);
        int earlyCheckExit = new CommandRouter().run(new String[]{
                "goal", "check", "--project-root", "demo", "--goal", goalKey, "--all"
        }, earlyCheck.context());
        assertEquals(ExitCodes.SUCCESS, earlyCheckExit);
        assertTrue(earlyCheck.stdout().contains("step_count_at_check: 1"));

        Harness verifyStep = new Harness(tempDir);
        int verifyStepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", "demo",
                "--goal", goalKey,
                "--summary", "Verification evidence recorded",
                "--evidence", "compile_result=not required; test_result=not required; sensitive_result=early check needs rerun"
        }, verifyStep.context());
        assertEquals(ExitCodes.SUCCESS, verifyStepExit);

        Harness staleEvaluate = new Harness(tempDir);
        int staleEvaluateExit = new CommandRouter().run(new String[]{
                "goal", "evaluate", "--project-root", "demo", "--goal", goalKey, "--json"
        }, staleEvaluate.context());
        assertEquals(ExitCodes.SUCCESS, staleEvaluateExit);
        assertTrue(staleEvaluate.stdout().contains("\"decision\": \"not_ready\""));
        assertTrue(staleEvaluate.stdout().contains("check sensitive is stale: checked_at_step=1 current_step=2"));
        assertTrue(staleEvaluate.stdout().contains("\"stale_count\": 1"));
        assertTrue(staleEvaluate.stdout().contains("\"sensitive\""));
        assertTrue(staleEvaluate.stdout().contains("\"next_command\": \"dhk goal check --goal " + goalKey
                + " --check sensitive\""));

        Harness staleComplete = new Harness(tempDir);
        int staleCompleteExit = new CommandRouter().run(new String[]{
                "goal", "complete", "--project-root", "demo", "--goal", goalKey, "--json"
        }, staleComplete.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, staleCompleteExit);
        assertTrue(staleComplete.stdout().contains("\"status\": \"not_ready\""));
        assertTrue(staleComplete.stdout().contains("\"stale_count\": 1"));

        Harness freshCheck = new Harness(tempDir);
        int freshCheckExit = new CommandRouter().run(new String[]{
                "goal", "check", "--project-root", "demo", "--goal", goalKey, "--all"
        }, freshCheck.context());
        assertEquals(ExitCodes.SUCCESS, freshCheckExit);
        assertTrue(freshCheck.stdout().contains("step_count_at_check: 2"));

        Harness readyEvaluate = new Harness(tempDir);
        int readyEvaluateExit = new CommandRouter().run(new String[]{
                "goal", "evaluate", "--project-root", "demo", "--goal", goalKey, "--json"
        }, readyEvaluate.context());
        assertEquals(ExitCodes.SUCCESS, readyEvaluateExit);
        assertTrue(readyEvaluate.stdout().contains("\"decision\": \"ready_to_complete\""));
        assertTrue(readyEvaluate.stdout().contains("\"stale_count\": 0"));
    }

    @Test
    void goalUsesConfiguredProfileCheckPolicyAndJsonOutput() throws Exception {
        Path root = tempDir.resolve("demo");
        Files.createDirectories(PathUtil.goalProfilesDirectory(root));
        Files.write(PathUtil.goalProfile(root, "custom-api"), ("{\n"
                + "  \"workflow_key\": \"api-change\",\n"
                + "  \"requires_spec\": \"false\",\n"
                + "  \"default_mode\": \"api\",\n"
                + "  \"actions\": \"custom_inspect,verify\"\n"
                + "}\n").getBytes("UTF-8"));
        Files.write(PathUtil.goalCheckPolicy(root), ("{\n"
                + "  \"required_checks\": \"sensitive\",\n"
                + "  \"fail_pending_hard_gates\": \"true\"\n"
                + "}\n").getBytes("UTF-8"));

        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "demo",
                "--profile", "custom-api",
                "--task", "Configured goal",
                "--module", "order"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");
        assertTrue(start.stdout().contains("spec_change: "));
        assertTrue(start.stdout().contains("current_action: custom_inspect"));

        Harness completeNotReadyJson = new Harness(tempDir);
        int completeNotReadyExit = new CommandRouter().run(new String[]{
                "goal", "complete", "--project-root", "demo", "--goal", goalKey, "--json"
        }, completeNotReadyJson.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, completeNotReadyExit);
        assertTrue(completeNotReadyJson.stdout().contains("\"command\": \"goal complete\""));
        assertTrue(completeNotReadyJson.stdout().contains("\"status\": \"not_ready\""));
        assertTrue(completeNotReadyJson.stdout().contains("\"ready_to_complete\": false"));
        assertTrue(completeNotReadyJson.stdout().contains("\"missing_count\": 2"));
        assertTrue(completeNotReadyJson.stdout().contains("goal steps incomplete: expected 2 actions, recorded 0"));
        assertTrue(completeNotReadyJson.stdout().contains("check sensitive is pending"));

        Harness statusJson = new Harness(tempDir);
        int statusExit = new CommandRouter().run(new String[]{
                "goal", "status", "--project-root", "demo", "--goal", goalKey, "--json"
        }, statusJson.context());
        assertEquals(ExitCodes.SUCCESS, statusExit);
        assertTrue(statusJson.stdout().contains("\"command\": \"goal status\""));
        assertTrue(statusJson.stdout().contains("\"current_action\": \"custom_inspect\""));

        Harness checkJson = new Harness(tempDir);
        int checkExit = new CommandRouter().run(new String[]{
                "goal", "check", "--project-root", "demo", "--goal", goalKey, "--all", "--json"
        }, checkJson.context());
        assertEquals(ExitCodes.SUCCESS, checkExit);
        assertTrue(checkJson.stdout().contains("\"count\": 1"));
        assertTrue(checkJson.stdout().contains("\"check_key\": \"sensitive\""));
        assertTrue(checkJson.stdout().contains("\"step_count_at_check\": 0"));

        Harness evaluateBeforeStepsJson = new Harness(tempDir);
        int evaluateBeforeStepsExit = new CommandRouter().run(new String[]{
                "goal", "evaluate", "--project-root", "demo", "--goal", goalKey, "--json"
        }, evaluateBeforeStepsJson.context());
        assertEquals(ExitCodes.SUCCESS, evaluateBeforeStepsExit);
        assertTrue(evaluateBeforeStepsJson.stdout().contains("\"decision\": \"not_ready\""));
        assertTrue(evaluateBeforeStepsJson.stdout().contains("\"missing_count\": 1"));

        Harness customStep = new Harness(tempDir);
        int customStepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", "demo",
                "--goal", goalKey,
                "--summary", "Custom inspection complete",
                "--evidence", "evidence=custom inspection"
        }, customStep.context());
        assertEquals(ExitCodes.SUCCESS, customStepExit);
        assertTrue(customStep.stdout().contains("current_action: verify"));

        Harness customVerify = new Harness(tempDir);
        int customVerifyExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", "demo",
                "--goal", goalKey,
                "--summary", "Custom verification complete",
                "--evidence", "compile_result=skipped; test_result=custom goal check; sensitive_result=ok"
        }, customVerify.context());
        assertEquals(ExitCodes.SUCCESS, customVerifyExit);

        Harness freshCheckJson = new Harness(tempDir);
        int freshCheckExit = new CommandRouter().run(new String[]{
                "goal", "check", "--project-root", "demo", "--goal", goalKey, "--all", "--json"
        }, freshCheckJson.context());
        assertEquals(ExitCodes.SUCCESS, freshCheckExit);
        assertTrue(freshCheckJson.stdout().contains("\"step_count_at_check\": 2"));

        Harness evaluateJson = new Harness(tempDir);
        int evaluateExit = new CommandRouter().run(new String[]{
                "goal", "evaluate", "--project-root", "demo", "--goal", goalKey, "--json"
        }, evaluateJson.context());
        assertEquals(ExitCodes.SUCCESS, evaluateExit);
        assertTrue(evaluateJson.stdout().contains("\"decision\": \"ready_to_complete\""));
        assertTrue(evaluateJson.stdout().contains("\"ready_to_complete\": true"));
        assertTrue(evaluateJson.stdout().contains("\"missing_count\": 0"));

        Harness completeJson = new Harness(tempDir);
        int completeExit = new CommandRouter().run(new String[]{
                "goal", "complete", "--project-root", "demo", "--goal", goalKey, "--json"
        }, completeJson.context());
        assertEquals(ExitCodes.SUCCESS, completeExit);
        assertTrue(completeJson.stdout().contains("\"command\": \"goal complete\""));
        assertTrue(completeJson.stdout().contains("\"status\": \"completed\""));
        assertTrue(completeJson.stdout().contains("\"summary_path\": "));
    }

    private void recordJavaGoalSteps(String goalKey) {
        Harness inspectStep = new Harness(tempDir);
        int inspectStepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", "demo",
                "--goal", goalKey,
                "--summary", "Inspected existing controller/service/mapper/tests",
                "--changed-files", "",
                "--evidence", "existing_controller,existing_service,existing_mapper,existing_tests"
        }, inspectStep.context());
        assertEquals(ExitCodes.SUCCESS, inspectStepExit);

        Harness planStep = new Harness(tempDir);
        int planStepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", "demo",
                "--goal", goalKey,
                "--summary", "Planned impacted files, risks, and verification",
                "--evidence", "impacted_files=GoalIntegrationTest; risk_points=check policy; verification_plan=goal tests"
        }, planStep.context());
        assertEquals(ExitCodes.SUCCESS, planStepExit);

        Harness implementStep = new Harness(tempDir);
        int implementStepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", "demo",
                "--goal", goalKey,
                "--summary", "Implemented minimal goal policy changes",
                "--changed-files", "src/main/java/com/devharnesskit/dhk/service/goal/GoalCheckPolicy.java",
                "--evidence", "implementation_summary=goal check policy accepts statuses"
        }, implementStep.context());
        assertEquals(ExitCodes.SUCCESS, implementStepExit);

        Harness verifyStep = new Harness(tempDir);
        int verifyStepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", "demo",
                "--goal", goalKey,
                "--summary", "Recorded verification evidence",
                "--evidence", "compile_result=pending; test_result=pending; sensitive_result=ok"
        }, verifyStep.context());
        assertEquals(ExitCodes.SUCCESS, verifyStepExit);
    }

    private void writeMinimalPom(Path root) throws Exception {
        Files.write(root.resolve("pom.xml"), ("<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n"
                + "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 "
                + "https://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
                + "  <modelVersion>4.0.0</modelVersion>\n"
                + "  <groupId>demo</groupId>\n"
                + "  <artifactId>demo</artifactId>\n"
                + "  <version>1.0.0</version>\n"
                + "</project>\n").getBytes("UTF-8"));
    }

    private void assertGoalRows(Path root, String goalKey) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PathUtil.memoryDb(root));
             Statement statement = connection.createStatement()) {
            assertEquals(1, count(statement, "SELECT COUNT(*) FROM goal_run WHERE goal_key = '" + goalKey + "'"));
            assertEquals(1, count(statement, "SELECT COUNT(*) FROM workflow_run"));
            assertEquals(1, count(statement, "SELECT COUNT(*) FROM spec_change"));
            assertEquals(1, count(statement, "SELECT COUNT(*) FROM workflow_spec_binding"));
            assertEquals(1, count(statement, "SELECT COUNT(*) FROM goal_event WHERE goal_key = '" + goalKey + "'"));
        }
    }

    private void assertCompletedRows(Path root, String goalKey) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PathUtil.memoryDb(root));
             Statement statement = connection.createStatement()) {
            assertEquals(5, count(statement, "SELECT COUNT(*) FROM goal_check WHERE goal_key = '" + goalKey + "'"));
            assertEquals(5, count(statement, "SELECT COUNT(*) FROM goal_check WHERE goal_key = '" + goalKey
                    + "' AND step_count_at_check = 4"));
            assertEquals(1, count(statement, "SELECT COUNT(*) FROM checkpoint"));
            assertEquals(1, count(statement, "SELECT COUNT(*) FROM goal_run WHERE goal_key = '" + goalKey + "' AND status = 'completed'"));
            assertTrue(count(statement, "SELECT COUNT(*) FROM goal_artifact WHERE goal_key = '" + goalKey + "'") >= 3);
        }
    }

    private int count(Statement statement, String sql) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private String firstValue(String output, String prefix) {
        String[] lines = output.split("\\r?\\n");
        for (String line : lines) {
            if (line.startsWith(prefix)) {
                return line.substring(prefix.length()).trim();
            }
        }
        return "";
    }

    private void assertSectionOrder(String text, String... markers) {
        int previous = -1;
        for (String marker : markers) {
            int current = text.indexOf(marker);
            assertTrue(current > previous, "Expected marker in order: " + marker);
            previous = current;
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
