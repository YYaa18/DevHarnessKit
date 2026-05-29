package com.devharnesskit.dhk.integration;

import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.CommandRouter;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.util.Clock;
import com.devharnesskit.dhk.util.PathUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GoalIntegrationTest {
    /*
     * V0.4.1 goal hardening matrix:
     * stale checks, sensitive leaks, skipped Java checks, workflow hard gates,
     * spec closure, export recovery, and successful completion bindings.
     */
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
        String workflowRun = firstValue(start.stdout(), "workflow_run: ");
        String specChange = firstValue(start.stdout(), "spec_change: ");

        assertEquals(ExitCodes.SUCCESS, startExit,
                "stdout=" + start.stdout() + "\nstderr=" + start.stderr());
        assertTrue(goalKey.length() > 0);
        assertTrue(start.stdout().contains("workflow_run:"));
        assertTrue(start.stdout().contains("spec_change:"));
        assertTrue(start.stdout().contains("current_action: inspect_existing_code"));
        assertTrue(Files.isRegularFile(goalContext));
        String initialContext = new String(Files.readAllBytes(goalContext), "UTF-8");
        assertTrue(initialContext.contains("# GOAL_CONTEXT"));
        assertSectionOrder(initialContext, "# GOAL_CONTEXT", "<generated-at>", "<goal>",
                "<current-action>", "<next-instruction>", "<allowed-actions>", "<allowed-commands>",
                "<forbidden-actions>", "<required-evidence>", "<evidence-contract>",
                "<structured-evidence-fields>", "<required-checks>", "<context-files>",
                "<completion-blockers>", "<freshness-status>", "<completion-condition>", "<next-command>");
        assertTrue(initialContext.contains("inspect_existing_code"));
        assertTrue(initialContext.contains("- perform_current_action_only"));
        assertTrue(initialContext.contains("<allowed-commands>"));
        assertTrue(initialContext.contains(".agents/skills/devharness-goal-development/scripts/goal-verify.sh --goal " + goalKey));
        assertTrue(initialContext.contains("- do_not_archive_spec"));
        assertTrue(initialContext.contains("- do_not_claim_completion_before_goal_evaluate"));
        assertTrue(initialContext.contains("- existing_controller"));
        assertTrue(initialContext.contains("<evidence-contract>"));
        assertTrue(initialContext.contains("- current_action: inspect_existing_code"));
        assertTrue(initialContext.contains("- --read-files"));
        assertTrue(initialContext.contains("- --compile-result"));
        assertTrue(initialContext.contains("- compile"));
        assertTrue(initialContext.contains("- check compile is pending"));
        assertTrue(initialContext.contains("- .agents/memory/exports/SPEC_CONTEXT.md"));
        assertTrue(initialContext.contains("<freshness-status>"));
        assertTrue(initialContext.contains("- status: fresh"));
        assertTrue(initialContext.contains(".agents/skills/devharness-goal-development/scripts/goal-step.sh --goal " + goalKey));
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
        assertTrue(next.stdout().contains("allowed_actions:"));
        assertTrue(next.stdout().contains("forbidden_actions:"));
        assertTrue(next.stdout().contains("required_evidence:"));
        assertTrue(next.stdout().contains("structured_evidence_fields:"));
        assertTrue(next.stdout().contains("--read-files"));
        assertTrue(next.stdout().contains("required_checks:"));
        assertTrue(next.stdout().contains("context_files:"));
        assertTrue(next.stdout().contains("completion_blockers:"));
        assertTrue(next.stdout().contains("check compile is pending"));

        Harness nextJson = new Harness(tempDir);
        int nextJsonExit = new CommandRouter().run(new String[]{
                "goal", "next", "--project-root", "demo", "--goal", goalKey, "--json"
        }, nextJson.context());
        assertEquals(ExitCodes.SUCCESS, nextJsonExit);
        assertTrue(nextJson.stdout().contains("\"command\": \"goal next\""));
        assertTrue(nextJson.stdout().contains("\"goal_key\": \"" + goalKey + "\""));
        assertTrue(nextJson.stdout().contains("\"current_action\": \"inspect_existing_code\""));
        assertTrue(nextJson.stdout().contains("\"evidence_contract\": {"));
        assertTrue(nextJson.stdout().contains("\"required_evidence\": [\"existing_controller\", \"existing_service\", \"existing_mapper\", \"existing_tests\"]"));
        assertTrue(nextJson.stdout().contains("\"structured_evidence_fields\": [\"--read-files\", \"--changed-files\", \"--tests-run\", \"--compile-result\", \"--risks\", \"--pending\", \"--field existing_controller=<value>\""));
        assertTrue(nextJson.stdout().contains("\"--field existing_tests=<value>\""));
        assertTrue(nextJson.stdout().contains("\"allowed_actions\": [\"perform_current_action_only\", \"record_goal_step_after_work\", \"run_goal_next_before_continuing\"]"));
        assertTrue(nextJson.stdout().contains("\"forbidden_actions\": [\"do_not_archive_spec\""));
        assertTrue(nextJson.stdout().contains("\"required_checks\": ["));
        assertTrue(nextJson.stdout().contains("\"compile\""));
        assertTrue(nextJson.stdout().contains("\"test\""));
        assertTrue(nextJson.stdout().contains("\"sensitive\""));
        assertTrue(nextJson.stdout().contains("\"workflow\""));
        assertTrue(nextJson.stdout().contains("\"spec\""));
        assertTrue(nextJson.stdout().contains("\"completion_blockers\": [\"goal steps incomplete: expected 4 actions, recorded 0\""));
        assertTrue(nextJson.stdout().contains("\"next_command\": \".agents/skills/devharness-goal-development/scripts/goal-step.sh --goal " + goalKey));

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
        assertTrue(step.stdout().contains("step_number: 1"));
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

        Harness verifyNext = new Harness(tempDir);
        int verifyNextExit = new CommandRouter().run(new String[]{
                "goal", "next", "--project-root", "demo", "--goal", goalKey
        }, verifyNext.context());
        assertEquals(ExitCodes.SUCCESS, verifyNextExit);
        assertTrue(verifyNext.stdout().contains("--field compile_result=<value>"));
        assertTrue(verifyNext.stdout().contains("--field test_result=<value>"));
        assertTrue(verifyNext.stdout().contains("--field sensitive_result=<value>"));

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

        Harness verifyReady = new Harness(tempDir);
        int verifyReadyExit = new CommandRouter().run(new String[]{
                "goal", "verify", "--project-root", "demo", "--goal", goalKey
        }, verifyReady.context());
        assertEquals(ExitCodes.SUCCESS, verifyReadyExit);
        assertTrue(verifyReady.stdout().contains("goal_key: " + goalKey));
        assertTrue(verifyReady.stdout().contains("level: standard"));
        assertTrue(verifyReady.stdout().contains("check_scope: all_required"));
        assertTrue(verifyReady.stdout().contains("decision: ready_to_complete"));
        assertTrue(verifyReady.stdout().contains("ready_to_complete: true"));
        assertTrue(verifyReady.stdout().contains("checks:"));
        assertTrue(verifyReady.stdout().contains("compile: passed"));
        assertTrue(verifyReady.stdout().contains("failed_checks:"));
        assertTrue(verifyReady.stdout().contains("freshness_status: fresh"));
        assertTrue(verifyReady.stdout().contains("completion_blockers:"));
        assertTrue(verifyReady.stdout().contains("next_command: dhk goal complete --goal " + goalKey));
        assertTrue(verifyReady.stdout().contains("context_path: " + goalContext));

        Harness verifyFast = new Harness(tempDir);
        int verifyFastExit = new CommandRouter().run(new String[]{
                "goal", "verify", "--project-root", "demo", "--goal", goalKey, "--level", "fast"
        }, verifyFast.context());
        assertEquals(ExitCodes.SUCCESS, verifyFastExit);
        assertTrue(verifyFast.stdout().contains("level: fast"));
        assertTrue(verifyFast.stdout().contains("check_scope: fast"));
        assertTrue(verifyFast.stdout().contains("selected_checks:"));
        assertTrue(verifyFast.stdout().contains("sensitive: passed"));

        Harness verifyRelease = new Harness(tempDir);
        int verifyReleaseExit = new CommandRouter().run(new String[]{
                "goal", "verify", "--project-root", "demo", "--goal", goalKey, "--level", "release"
        }, verifyRelease.context());
        assertEquals(ExitCodes.SUCCESS, verifyReleaseExit);
        assertTrue(verifyRelease.stdout().contains("level: release"));
        assertTrue(verifyRelease.stdout().contains("check_scope: release"));
        assertTrue(verifyRelease.stdout().contains("release_checks:"));
        assertTrue(verifyRelease.stdout().contains("package: passed"));
        assertTrue(verifyRelease.stdout().contains("artifact_passport: missing_until_goal_complete"));

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
                "<completion-bindings>", "<steps>", "<checks>", "<agent-instructions>");
        assertTrue(summary.contains("- status: completed"));
        assertTrue(summary.contains("- workflow_run:"));
        assertTrue(summary.contains("- spec_change:"));
        assertTrue(summary.contains("- workflow_checkpoint_binding: created"));
        assertTrue(summary.contains("- workflow_artifact: GOAL_SUMMARY.md"));
        assertFalse(summary.contains("<graph-artifacts>"));
        assertTrue(summary.contains("- #1 inspect_existing_code: Inspected existing controller/service/mapper/tests"));
        assertTrue(summary.contains("- [passed] sensitive: sensitive scan passed"));
        assertTrue(summary.contains("evidence_path:"));
        assertTrue(summary.contains("Do not treat generated summary text as confirmed long-term memory"));
        assertFalse(summary.contains("password="));
        assertCompletedRows(root, goalKey);

        Harness audit = new Harness(tempDir);
        int auditExit = new CommandRouter().run(new String[]{
                "goal", "audit", "--project-root", "demo", "--goal", goalKey
        }, audit.context());
        assertEquals(ExitCodes.SUCCESS, auditExit);
        assertTrue(audit.stdout().contains("goal audit complete"));
        assertTrue(audit.stdout().contains("status: completed"));
        assertTrue(audit.stdout().contains("step_count: 4"));
        assertTrue(audit.stdout().contains("artifact_passport: present"));
        assertTrue(audit.stdout().contains("decision: ready_to_complete"));
        assertTrue(audit.stdout().contains("missing:\n  - none"));
        assertTrue(audit.stdout().contains("invalid:\n  - none"));

        Harness auditJson = new Harness(tempDir);
        int auditJsonExit = new CommandRouter().run(new String[]{
                "goal", "audit", "--project-root", "demo", "--goal", goalKey, "--json"
        }, auditJson.context());
        assertEquals(ExitCodes.SUCCESS, auditJsonExit);
        assertTrue(auditJson.stdout().contains("\"command\": \"goal audit\""));
        assertTrue(auditJson.stdout().contains("\"status\": \"completed\""));
        assertTrue(auditJson.stdout().contains("\"artifact_passport\": \"present\""));

        Harness recheck = new Harness(tempDir);
        int recheckExit = new CommandRouter().run(new String[]{
                "goal", "recheck", "--project-root", "demo", "--goal", goalKey
        }, recheck.context());
        assertEquals(ExitCodes.SUCCESS, recheckExit);
        assertTrue(recheck.stdout().contains("goal recheck complete"));
        assertTrue(recheck.stdout().contains("status_before: completed"));
        assertTrue(recheck.stdout().contains("status_after: completed"));
        assertTrue(recheck.stdout().contains("step_count_before: 4"));
        assertTrue(recheck.stdout().contains("step_count_after: 4"));
        assertTrue(recheck.stdout().contains("decision: ready_to_complete"));

        Harness recheckJson = new Harness(tempDir);
        int recheckJsonExit = new CommandRouter().run(new String[]{
                "goal", "recheck", "--project-root", "demo", "--goal", goalKey, "--json"
        }, recheckJson.context());
        assertEquals(ExitCodes.SUCCESS, recheckJsonExit);
        assertTrue(recheckJson.stdout().contains("\"command\": \"goal recheck\""));
        assertTrue(recheckJson.stdout().contains("\"goal_key\": \"" + goalKey + "\""));
        assertTrue(recheckJson.stdout().contains("\"status_before\": \"completed\""));
        assertTrue(recheckJson.stdout().contains("\"ready_to_complete\": true"));
        assertTrue(recheckJson.stdout().contains("\"checks\": ["));
    }

    @Test
    void goalStepPrintsGoalLocalStepNumberAcrossGoals() {
        Harness firstStart = new Harness(tempDir);
        int firstStartExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "local-step-demo",
                "--profile", "java-api-patch",
                "--task", "First patch",
                "--module", "account",
                "--mode", "api"
        }, firstStart.context());
        assertEquals(ExitCodes.SUCCESS, firstStartExit);
        String firstGoal = firstValue(firstStart.stdout(), "goal_key: ");

        Harness firstStep = new Harness(tempDir);
        int firstStepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", "local-step-demo",
                "--goal", firstGoal,
                "--summary", "Understood first patch",
                "--field", "goal_understanding=First patch boundary",
                "--field", "assumptions=None",
                "--field", "read_files=README.md"
        }, firstStep.context());
        assertEquals(ExitCodes.SUCCESS, firstStepExit);
        assertTrue(firstStep.stdout().contains("step_number: 1"));

        Harness secondStart = new Harness(tempDir);
        int secondStartExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "local-step-demo",
                "--profile", "java-api-patch",
                "--task", "Second patch",
                "--module", "account",
                "--mode", "api"
        }, secondStart.context());
        assertEquals(ExitCodes.SUCCESS, secondStartExit);
        String secondGoal = firstValue(secondStart.stdout(), "goal_key: ");
        assertFalse(firstGoal.equals(secondGoal));

        Harness secondStep = new Harness(tempDir);
        int secondStepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", "local-step-demo",
                "--goal", secondGoal,
                "--summary", "Understood second patch",
                "--field", "goal_understanding=Second patch boundary",
                "--field", "assumptions=None",
                "--field", "read_files=README.md"
        }, secondStep.context());
        assertEquals(ExitCodes.SUCCESS, secondStepExit);
        assertTrue(secondStep.stdout().contains("step_number: 1"));
        assertFalse(firstValue(secondStep.stdout(), "step_id: ").equals("1"));
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
    void goalStepAcceptsStructuredEvidenceFieldsAndPersistsThem() throws Exception {
        Path root = tempDir.resolve("demo");
        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "demo",
                "--profile", "java-api-change",
                "--task", "Structured evidence",
                "--module", "goal"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");

        Harness inspectStep = new Harness(tempDir);
        int inspectStepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", "demo",
                "--goal", goalKey,
                "--summary", "Inspected files with structured evidence",
                "--read-files", "GoalStepCommand.java,GoalOrchestrator.java,GoalIntegrationTest.java"
        }, inspectStep.context());
        assertEquals(ExitCodes.SUCCESS, inspectStepExit,
                "stdout=" + inspectStep.stdout() + "\nstderr=" + inspectStep.stderr());
        assertTrue(inspectStep.stdout().contains("current_action: create_change_plan"));

        String evidence = singleString(root, "SELECT evidence FROM goal_step WHERE goal_key = '" + goalKey + "'");
        assertTrue(evidence.contains("read_files=GoalStepCommand.java,GoalOrchestrator.java,GoalIntegrationTest.java"));

        Harness planStep = new Harness(tempDir);
        int planStepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", "demo",
                "--goal", goalKey,
                "--summary", "Planned with structured risks",
                "--changed-files", "src/main/java/com/devharnesskit/dhk/command/goal/GoalStepCommand.java",
                "--risks", "Evidence aliases must remain compatible",
                "--evidence", "verification_plan=GoalIntegrationTest"
        }, planStep.context());
        assertEquals(ExitCodes.SUCCESS, planStepExit);
        String planEvidence = singleString(root, "SELECT evidence FROM goal_step WHERE goal_key = '"
                + goalKey + "' AND step_index = 2");
        assertTrue(planEvidence.contains("changed_files=src/main/java/com/devharnesskit/dhk/command/goal/GoalStepCommand.java"));
        assertTrue(planEvidence.contains("risks=Evidence aliases must remain compatible"));
        assertTrue(planEvidence.contains("risk_points=Evidence aliases must remain compatible"));

        Harness implementStep = new Harness(tempDir);
        int implementStepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", "demo",
                "--goal", goalKey,
                "--summary", "Implemented structured evidence support",
                "--changed-files", "src/main/java/com/devharnesskit/dhk/command/goal/GoalStepCommand.java"
        }, implementStep.context());
        assertEquals(ExitCodes.SUCCESS, implementStepExit);

        Harness verifyStep = new Harness(tempDir);
        int verifyStepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", "demo",
                "--goal", goalKey,
                "--summary", "Verified with structured fields",
                "--compile-result", "passed",
                "--tests-run", "GoalIntegrationTest",
                "--pending", "none",
                "--evidence", "sensitive_result=passed"
        }, verifyStep.context());
        assertEquals(ExitCodes.SUCCESS, verifyStepExit,
                "stdout=" + verifyStep.stdout() + "\nstderr=" + verifyStep.stderr());
        String verifyEvidence = singleString(root, "SELECT evidence FROM goal_step WHERE goal_key = '"
                + goalKey + "' AND step_index = 4");
        assertTrue(verifyEvidence.contains("compile_result=passed"));
        assertTrue(verifyEvidence.contains("tests_run=GoalIntegrationTest"));
        assertTrue(verifyEvidence.contains("test_result=GoalIntegrationTest"));
        assertTrue(verifyEvidence.contains("pending=none"));
    }

    @Test
    void goalStepTemplateFieldAndDryRunAvoidFragileEvidenceFormatting() throws Exception {
        Path root = tempDir.resolve("demo");
        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "demo",
                "--profile", "java-api-change",
                "--task", "Field evidence",
                "--module", "goal"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");

        Harness template = new Harness(tempDir);
        int templateExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", "demo",
                "--goal", goalKey,
                "--template"
        }, template.context());
        assertEquals(ExitCodes.SUCCESS, templateExit);
        assertTrue(template.stdout().contains("goal step template"));
        assertTrue(template.stdout().contains("current_action: inspect_existing_code"));
        assertTrue(template.stdout().contains("--field existing_controller=<value>"));
        assertTrue(template.stdout().contains("--field existing_service=<value>"));
        assertTrue(template.stdout().contains("example_command: dhk goal step --goal " + goalKey));
        assertTrue(template.stdout().contains("--field \"existing_controller=<value>\""));
        assertTrue(template.stdout().contains("dry_run_command: .agents/skills/devharness-goal-development/scripts/goal-step.sh --goal " + goalKey));

        Harness evidenceTemplate = new Harness(tempDir);
        int evidenceTemplateExit = new CommandRouter().run(new String[]{
                "goal", "evidence-template",
                "--project-root", "demo",
                "--goal", goalKey
        }, evidenceTemplate.context());
        assertEquals(ExitCodes.SUCCESS, evidenceTemplateExit);
        assertTrue(evidenceTemplate.stdout().contains("goal evidence template"));
        assertTrue(evidenceTemplate.stdout().contains("current_action: inspect_existing_code"));
        assertTrue(evidenceTemplate.stdout().contains("existing_controller=<value>"));

        Harness missing = new Harness(tempDir);
        int missingExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", "demo",
                "--goal", goalKey,
                "--summary", "Missing evidence"
        }, missing.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, missingExit);
        assertTrue(missing.stderr().contains("error_code: GOAL_STEP_EVIDENCE_MISSING"));
        assertTrue(missing.stderr().contains("missing:"));
        assertTrue(missing.stderr().contains("next_command: .agents/skills/devharness-goal-development/scripts/dhk.sh goal evidence-template --goal " + goalKey));

        Harness dryRun = new Harness(tempDir);
        int dryRunExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", "demo",
                "--goal", goalKey,
                "--summary", "Inspected with field evidence",
                "--field", "existing_controller=GoalStepCommand",
                "--field", "existing_service=GoalOrchestrator",
                "--field", "existing_mapper=GoalStepRepository",
                "--field", "existing_tests=GoalIntegrationTest",
                "--dry-run"
        }, dryRun.context());
        assertEquals(ExitCodes.SUCCESS, dryRunExit,
                "stdout=" + dryRun.stdout() + "\nstderr=" + dryRun.stderr());
        assertTrue(dryRun.stdout().contains("goal step dry-run complete"));
        assertTrue(dryRun.stdout().contains("status: passed"));
        assertTrue(dryRun.stdout().contains("would_record: true"));
        assertTrue(dryRun.stdout().contains("step_count: 0"));
        assertEquals(0, countRows(root, "goal_step"));

        Harness step = new Harness(tempDir);
        int stepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", "demo",
                "--goal", goalKey,
                "--summary", "Inspected with field evidence",
                "--field", "existing_controller=GoalStepCommand",
                "--field", "existing_service=GoalOrchestrator",
                "--field", "existing_mapper=GoalStepRepository",
                "--field", "existing_tests=GoalIntegrationTest"
        }, step.context());
        assertEquals(ExitCodes.SUCCESS, stepExit,
                "stdout=" + step.stdout() + "\nstderr=" + step.stderr());
        assertTrue(step.stdout().contains("current_action: create_change_plan"));
        String evidence = singleString(root, "SELECT evidence FROM goal_step WHERE goal_key = '" + goalKey + "'");
        assertTrue(evidence.contains("existing_controller=GoalStepCommand"));
        assertTrue(evidence.contains("existing_service=GoalOrchestrator"));
        assertTrue(evidence.contains("existing_mapper=GoalStepRepository"));
        assertTrue(evidence.contains("existing_tests=GoalIntegrationTest"));
    }

    @Test
    void goalVerifyInvalidLevelShowsValidValues() {
        Harness invalid = new Harness(tempDir);
        int exit = new CommandRouter().run(new String[]{
                "goal", "verify",
                "--project-root", "demo",
                "--level", "deep"
        }, invalid.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, exit);
        assertTrue(invalid.stderr().contains("error_code: INVALID_GOAL_VERIFY_LEVEL"));
        assertTrue(invalid.stderr().contains("valid_values:"));
        assertTrue(invalid.stderr().contains("release"));
    }

    @Test
    void goalStepStructuredEvidenceStillReportsMissingRequiredItems() throws Exception {
        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "demo",
                "--profile", "java-api-change",
                "--task", "Structured evidence missing",
                "--module", "goal"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");

        Harness inspectStep = new Harness(tempDir);
        int inspectStepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", "demo",
                "--goal", goalKey,
                "--summary", "Inspected files",
                "--read-files", "GoalStepCommand.java"
        }, inspectStep.context());
        assertEquals(ExitCodes.SUCCESS, inspectStepExit);

        Harness planStep = new Harness(tempDir);
        int planStepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", "demo",
                "--goal", goalKey,
                "--summary", "Planned with only structured risks",
                "--changed-files", "GoalStepCommand.java",
                "--risks", "Missing verification plan"
        }, planStep.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, planStepExit);
        assertTrue(planStep.stderr().contains("Goal step evidence missing required items"));
        assertTrue(planStep.stderr().contains("verification_plan"));
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
    void goalStartExportFailureCanResumeWithoutDuplicateRuns() throws Exception {
        Path root = tempDir.resolve("demo");
        PathUtil.createMemoryDirectories(root);
        Files.deleteIfExists(PathUtil.goalContext(root));
        Files.createDirectory(PathUtil.goalContext(root));

        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "demo",
                "--profile", "java-api-change",
                "--task", "Recover failed start export",
                "--module", "goal"
        }, start.context());
        assertEquals(ExitCodes.RUNTIME_ERROR, startExit);
        assertTrue(start.stderr().contains("ERROR goal start failed"));

        String goalKey = singleString(root, "SELECT goal_key FROM goal_run");
        assertEquals("context_export_failed", singleString(root,
                "SELECT status FROM goal_run WHERE goal_key = '" + goalKey + "'"));
        assertEquals(1, countRows(root, "workflow_run"));
        assertEquals(1, countRows(root, "spec_change"));

        Harness next = new Harness(tempDir);
        int nextExit = new CommandRouter().run(new String[]{
                "goal", "next", "--project-root", "demo"
        }, next.context());
        assertEquals(ExitCodes.SUCCESS, nextExit);
        assertTrue(next.stdout().contains("current_action: recover_context_export"));
        assertTrue(next.stdout().contains("next_command: .agents/skills/devharness-goal-development/scripts/goal-resume.sh --goal " + goalKey));

        Files.delete(PathUtil.goalContext(root));
        Harness resume = new Harness(tempDir);
        int resumeExit = new CommandRouter().run(new String[]{
                "goal", "resume", "--project-root", "demo"
        }, resume.context());
        assertEquals(ExitCodes.SUCCESS, resumeExit);
        assertTrue(resume.stdout().contains("goal_key: " + goalKey));
        assertTrue(resume.stdout().contains("status: context_ready"));
        assertTrue(resume.stdout().contains("current_action: inspect_existing_code"));
        assertTrue(Files.isRegularFile(PathUtil.goalContext(root)));
        assertEquals(1, countRows(root, "goal_run"));
        assertEquals(1, countRows(root, "workflow_run"));
        assertEquals(1, countRows(root, "spec_change"));
    }

    @Test
    void goalStepExportFailureCanResumeWithoutDuplicateSteps() throws Exception {
        Path root = tempDir.resolve("demo");
        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "demo",
                "--profile", "java-api-change",
                "--task", "Recover failed step export",
                "--module", "goal"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");

        Files.delete(PathUtil.goalContext(root));
        Files.createDirectory(PathUtil.goalContext(root));
        Harness step = new Harness(tempDir);
        int stepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", "demo",
                "--goal", goalKey,
                "--summary", "Inspected existing controller/service/mapper/tests",
                "--evidence", "existing_controller,existing_service,existing_mapper,existing_tests"
        }, step.context());
        assertEquals(ExitCodes.RUNTIME_ERROR, stepExit);
        assertTrue(step.stderr().contains("ERROR goal step failed"));
        assertEquals("context_export_failed", singleString(root,
                "SELECT status FROM goal_run WHERE goal_key = '" + goalKey + "'"));
        assertEquals("create_change_plan", singleString(root,
                "SELECT current_action FROM goal_run WHERE goal_key = '" + goalKey + "'"));
        assertEquals(1, countRows(root, "goal_step"));

        Harness next = new Harness(tempDir);
        int nextExit = new CommandRouter().run(new String[]{
                "goal", "next", "--project-root", "demo", "--goal", goalKey
        }, next.context());
        assertEquals(ExitCodes.SUCCESS, nextExit);
        assertTrue(next.stdout().contains("current_action: recover_context_export"));

        Files.delete(PathUtil.goalContext(root));
        Harness resume = new Harness(tempDir);
        int resumeExit = new CommandRouter().run(new String[]{
                "goal", "resume", "--project-root", "demo", "--goal", goalKey
        }, resume.context());
        assertEquals(ExitCodes.SUCCESS, resumeExit);
        assertTrue(resume.stdout().contains("status: planning"));
        assertTrue(resume.stdout().contains("current_action: create_change_plan"));
        assertTrue(Files.isRegularFile(PathUtil.goalContext(root)));
        assertEquals(1, countRows(root, "goal_step"));
        assertEquals(1, countRows(root, "workflow_run"));
        assertEquals(1, countRows(root, "spec_change"));
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

        Harness verify = new Harness(tempDir);
        int verifyExit = new CommandRouter().run(new String[]{
                "goal", "verify", "--project-root", "demo", "--goal", goalKey, "--json"
        }, verify.context());
        assertEquals(ExitCodes.SUCCESS, verifyExit);
        assertTrue(verify.stdout().contains("\"command\": \"goal verify\""));
        assertTrue(verify.stdout().contains("\"decision\": \"not_ready\""));
        assertTrue(verify.stdout().contains("\"failed_count\": 1"));
        assertTrue(verify.stdout().contains("\"sensitive: sensitive scan failed"));
        assertFalse(verify.stdout().contains(rawSecret));

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
        Path root = tempDir.resolve("demo");
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

        Harness verify = new Harness(tempDir);
        int verifyExit = new CommandRouter().run(new String[]{
                "goal", "verify", "--project-root", "demo", "--goal", goalKey, "--json"
        }, verify.context());
        assertEquals(ExitCodes.SUCCESS, verifyExit);
        assertTrue(verify.stdout().contains("\"command\": \"goal verify\""));
        assertTrue(verify.stdout().contains("\"decision\": \"not_ready\""));
        assertTrue(verify.stdout().contains("\"ready_to_complete\": false"));
        assertTrue(verify.stdout().contains("\"freshness_status\": \"fresh\""));
        assertTrue(verify.stdout().contains("\"missing_count\": 3"));
        assertTrue(verify.stdout().contains("\"completion_blocker_count\": 3"));
        assertTrue(verify.stdout().contains("\"completion_blockers\": ["));
        assertTrue(verify.stdout().contains("\"blocker_summary\": \"skipped_required_check: check compile is skipped; accepted_statuses=passed\""));
        assertTrue(verify.stdout().contains("\"blocker_categories\": ["));
        assertTrue(verify.stdout().contains("\"category\": \"skipped_required_check\""));
        assertTrue(verify.stdout().contains("\"category\": \"failed_check\""));
        assertTrue(verify.stdout().contains("check compile is skipped; accepted_statuses=passed"));
        assertTrue(verify.stdout().contains("check test is skipped; accepted_statuses=passed"));
        assertTrue(verify.stdout().contains("check spec is failed; accepted_statuses=passed"));
        assertTrue(verify.stdout().contains("\"context_path\": "));

        Harness complete = new Harness(tempDir);
        int completeExit = new CommandRouter().run(new String[]{
                "goal", "complete", "--project-root", "demo", "--goal", goalKey
        }, complete.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, completeExit);
        assertTrue(complete.stdout().contains("decision: not_ready"));
        assertTrue(complete.stdout().contains("check compile is skipped; accepted_statuses=passed"));
    }

    @Test
    void mvcVerifyPhaseIsCheckDrivenNotStepDriven() throws Exception {
        Path root = tempDir.resolve("demo");
        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "demo",
                "--profile", "java-mvc-change",
                "--task", "MVC verify strictness",
                "--module", "view",
                "--mode", "mvc"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");
        String workflowRun = firstValue(start.stdout(), "workflow_run: ");

        recordJavaGoalSteps(goalKey);

        assertEquals("pending", singleString(root, "SELECT status FROM workflow_phase_run "
                + "WHERE run_key = '" + workflowRun + "' AND phase_key = 'verify_view_flow'"));
        assertEquals("pending", singleString(root, "SELECT status FROM workflow_gate_run "
                + "WHERE run_key = '" + workflowRun + "' AND gate_key = 'view_name_checked'"));
    }

    @Test
    void strictWorkflowPhaseOrderBlocksOutOfOrderMappedPhase() throws Exception {
        Path root = tempDir.resolve("demo");
        Files.createDirectories(PathUtil.goalProfilesDirectory(root));
        Files.write(PathUtil.goalProfile(root, "custom-out-of-order"), ("{\n"
                + "  \"workflow_key\": \"api-change\",\n"
                + "  \"requires_spec\": \"false\",\n"
                + "  \"default_mode\": \"api\",\n"
                + "  \"actions\": \"implement_minimal_change\",\n"
                + "  \"strict_workflow_phase_order\": \"true\",\n"
                + "  \"mapping.implement_minimal_change.workflow_phase\": \"implement_minimal_change\",\n"
                + "  \"mapping.implement_minimal_change.phase_pass_mode\": \"step\"\n"
                + "}\n").getBytes("UTF-8"));

        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "demo",
                "--profile", "custom-out-of-order",
                "--task", "Out of order strict workflow",
                "--module", "goal",
                "--mode", "api"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");
        String workflowRun = firstValue(start.stdout(), "workflow_run: ");

        Harness step = new Harness(tempDir);
        int stepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", "demo",
                "--goal", goalKey,
                "--summary", "Tried to implement before planning",
                "--changed-files", "src/main/java/Demo.java",
                "--evidence", "implementation_summary=out of order"
        }, step.context());
        assertEquals(ExitCodes.SUCCESS, stepExit);

        assertEquals("pending", singleString(root, "SELECT status FROM workflow_phase_run "
                + "WHERE run_key = '" + workflowRun + "' AND phase_key = 'implement_minimal_change'"));
        assertEquals("inspect_existing_code", singleString(root, "SELECT current_phase_key FROM workflow_run "
                + "WHERE run_key = '" + workflowRun + "'"));
        assertTrue(countRows(root, "workflow_event WHERE run_key = '" + workflowRun
                + "' AND event_type = 'custom' AND level = 'warn' "
                + "AND message LIKE 'Phase sync blocked%'") >= 1);
    }

    @Test
    void customSpecRequiredGoalRejectsEmptySpecTasksAndAcceptance() throws Exception {
        Path root = tempDir.resolve("demo");
        Files.createDirectories(PathUtil.goalProfilesDirectory(root));
        Files.write(PathUtil.goalProfile(root, "custom-empty-spec"), ("{\n"
                + "  \"workflow_key\": \"api-change\",\n"
                + "  \"requires_spec\": \"true\",\n"
                + "  \"default_mode\": \"api\",\n"
                + "  \"actions\": \"inspect,verify\",\n"
                + "  \"required_checks\": \"spec\",\n"
                + "  \"spec_require_non_empty_tasks\": \"true\",\n"
                + "  \"spec_require_non_empty_acceptance\": \"true\"\n"
                + "}\n").getBytes("UTF-8"));
        Files.write(PathUtil.goalCheckPolicy(root), ("{\n"
                + "  \"required_checks\": \"spec\",\n"
                + "  \"accepted_spec_statuses\": \"passed\"\n"
                + "}\n").getBytes("UTF-8"));

        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "demo",
                "--profile", "custom-empty-spec",
                "--task", "Empty spec guard",
                "--module", "goal",
                "--mode", "api"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");

        Harness inspect = new Harness(tempDir);
        int inspectExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", "demo",
                "--goal", goalKey,
                "--summary", "Inspection complete",
                "--evidence", "evidence=inspection"
        }, inspect.context());
        assertEquals(ExitCodes.SUCCESS, inspectExit);

        Harness verifyStep = new Harness(tempDir);
        int verifyStepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", "demo",
                "--goal", goalKey,
                "--summary", "Verification complete",
                "--evidence", "compile_result=not required; test_result=not required; sensitive_result=not required"
        }, verifyStep.context());
        assertEquals(ExitCodes.SUCCESS, verifyStepExit);

        Harness check = new Harness(tempDir);
        int checkExit = new CommandRouter().run(new String[]{
                "goal", "check", "--project-root", "demo", "--goal", goalKey, "--all"
        }, check.context());
        assertEquals(ExitCodes.SUCCESS, checkExit);
        assertTrue(check.stdout().contains("check_key: spec"));
        assertTrue(check.stdout().contains("status: failed"));
        assertTrue(check.stdout().contains("spec has no tasks"));
        assertTrue(check.stdout().contains("spec has no acceptance criteria"));

        Harness verify = new Harness(tempDir);
        int verifyExit = new CommandRouter().run(new String[]{
                "goal", "verify", "--project-root", "demo", "--goal", goalKey, "--json"
        }, verify.context());
        assertEquals(ExitCodes.SUCCESS, verifyExit);
        assertTrue(verify.stdout().contains("\"decision\": \"not_ready\""));
        assertTrue(verify.stdout().contains("check spec is failed; accepted_statuses=passed"));
        assertTrue(verify.stdout().contains("\"next_command\": \"dhk goal check --goal " + goalKey
                + " --check spec\""));
        assertTrue(verify.stdout().contains("\"context_path\": "));
    }

    @Test
    void profileAcceptanceMappingPassesFromFreshChecksSource() throws Exception {
        Path root = tempDir.resolve("demo");
        Files.createDirectories(PathUtil.goalProfilesDirectory(root));
        Files.write(PathUtil.goalProfile(root, "custom-acceptance-checks"), ("{\n"
                + "  \"workflow_key\": \"api-change\",\n"
                + "  \"requires_spec\": \"true\",\n"
                + "  \"default_mode\": \"api\",\n"
                + "  \"actions\": \"inspect,verify\",\n"
                + "  \"required_checks\": \"sensitive,spec\",\n"
                + "  \"completion_allow_skipped_checks\": \"false\",\n"
                + "  \"mapping.inspect.spec_task\": \"inspect\",\n"
                + "  \"mapping.verify.spec_task\": \"verify\",\n"
                + "  \"acceptance.business_rule.description\": \"Business rule is verified\",\n"
                + "  \"acceptance.business_rule.expected\": \"Sensitive and spec checks prove the rule\",\n"
                + "  \"acceptance.business_rule.source\": \"checks\",\n"
                + "  \"acceptance.business_rule.required_checks\": \"sensitive\"\n"
                + "}\n").getBytes("UTF-8"));

        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "demo",
                "--profile", "custom-acceptance-checks",
                "--task", "Checks-backed acceptance",
                "--module", "goal"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");
        String specChange = firstValue(start.stdout(), "spec_change: ");

        recordTwoStepGoal(goalKey);

        Harness verify = new Harness(tempDir);
        int verifyExit = new CommandRouter().run(new String[]{
                "goal", "verify", "--project-root", "demo", "--goal", goalKey, "--json"
        }, verify.context());
        assertEquals(ExitCodes.SUCCESS, verifyExit);
        assertTrue(verify.stdout().contains("\"decision\": \"ready_to_complete\""));
        assertTrue(verify.stdout().contains("\"check_key\": \"spec\""));
        assertEquals("passed", singleString(root, "SELECT status FROM spec_acceptance "
                + "WHERE change_key = '" + specChange + "' AND acceptance_key = 'business_rule'"));
    }

    @Test
    void profileAcceptanceMappingPassesFromTestSource() throws Exception {
        Path root = tempDir.resolve("demo");
        Files.createDirectories(PathUtil.goalProfilesDirectory(root));
        Files.write(PathUtil.goalProfile(root, "custom-acceptance-test"), ("{\n"
                + "  \"workflow_key\": \"api-change\",\n"
                + "  \"requires_spec\": \"true\",\n"
                + "  \"default_mode\": \"api\",\n"
                + "  \"actions\": \"inspect,verify\",\n"
                + "  \"required_checks\": \"test,spec\",\n"
                + "  \"completion_allow_skipped_checks\": \"true\",\n"
                + "  \"mapping.inspect.spec_task\": \"inspect\",\n"
                + "  \"mapping.verify.spec_task\": \"verify\",\n"
                + "  \"acceptance.test_rule.description\": \"Test-backed business rule is verified\",\n"
                + "  \"acceptance.test_rule.expected\": \"The test check is accepted by policy\",\n"
                + "  \"acceptance.test_rule.source\": \"test\"\n"
                + "}\n").getBytes("UTF-8"));

        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "demo",
                "--profile", "custom-acceptance-test",
                "--task", "Test-backed acceptance",
                "--module", "goal"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");
        String specChange = firstValue(start.stdout(), "spec_change: ");

        recordTwoStepGoal(goalKey);

        Harness verify = new Harness(tempDir);
        int verifyExit = new CommandRouter().run(new String[]{
                "goal", "verify", "--project-root", "demo", "--goal", goalKey, "--json"
        }, verify.context());
        assertEquals(ExitCodes.SUCCESS, verifyExit);
        assertTrue(verify.stdout().contains("\"decision\": \"ready_to_complete\""));
        assertEquals("passed", singleString(root, "SELECT status FROM spec_acceptance "
                + "WHERE change_key = '" + specChange + "' AND acceptance_key = 'test_rule'"));
    }

    @Test
    void profileAcceptanceMappingPassesFromExplicitEvidenceSource() throws Exception {
        Path root = tempDir.resolve("demo");
        Files.createDirectories(PathUtil.goalProfilesDirectory(root));
        Files.write(PathUtil.goalProfile(root, "custom-acceptance-evidence"), ("{\n"
                + "  \"workflow_key\": \"api-change\",\n"
                + "  \"requires_spec\": \"true\",\n"
                + "  \"default_mode\": \"api\",\n"
                + "  \"actions\": \"inspect,verify\",\n"
                + "  \"required_checks\": \"spec\",\n"
                + "  \"completion_allow_skipped_checks\": \"false\",\n"
                + "  \"mapping.inspect.spec_task\": \"inspect\",\n"
                + "  \"mapping.verify.spec_task\": \"verify\",\n"
                + "  \"acceptance.business_verified.description\": \"Business verification evidence exists\",\n"
                + "  \"acceptance.business_verified.expected\": \"A goal step explicitly records business_verified\",\n"
                + "  \"acceptance.business_verified.source\": \"evidence\",\n"
                + "  \"acceptance.business_verified.evidence_key\": \"business_verified\"\n"
                + "}\n").getBytes("UTF-8"));

        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "demo",
                "--profile", "custom-acceptance-evidence",
                "--task", "Evidence-backed acceptance",
                "--module", "goal"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");
        String specChange = firstValue(start.stdout(), "spec_change: ");

        recordTwoStepGoal(goalKey, "business_verified=confirmed");

        Harness verify = new Harness(tempDir);
        int verifyExit = new CommandRouter().run(new String[]{
                "goal", "verify", "--project-root", "demo", "--goal", goalKey, "--json"
        }, verify.context());
        assertEquals(ExitCodes.SUCCESS, verifyExit);
        assertTrue(verify.stdout().contains("\"decision\": \"ready_to_complete\""));
        assertEquals("passed", singleString(root, "SELECT status FROM spec_acceptance "
                + "WHERE change_key = '" + specChange + "' AND acceptance_key = 'business_verified'"));
    }

    @Test
    void profileAcceptanceMappingManualSourceBlocksCompletionUntilConfirmed() throws Exception {
        Path root = tempDir.resolve("demo");
        Files.createDirectories(PathUtil.goalProfilesDirectory(root));
        Files.write(PathUtil.goalProfile(root, "custom-acceptance-manual"), ("{\n"
                + "  \"workflow_key\": \"api-change\",\n"
                + "  \"requires_spec\": \"true\",\n"
                + "  \"default_mode\": \"api\",\n"
                + "  \"actions\": \"inspect,verify\",\n"
                + "  \"required_checks\": \"spec\",\n"
                + "  \"completion_allow_skipped_checks\": \"false\",\n"
                + "  \"mapping.inspect.spec_task\": \"inspect\",\n"
                + "  \"mapping.verify.spec_task\": \"verify\",\n"
                + "  \"acceptance.manual_business.description\": \"Manual business owner sign-off\",\n"
                + "  \"acceptance.manual_business.expected\": \"Business owner has approved the behavior\",\n"
                + "  \"acceptance.manual_business.source\": \"manual\"\n"
                + "}\n").getBytes("UTF-8"));

        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "demo",
                "--profile", "custom-acceptance-manual",
                "--task", "Manual acceptance guard",
                "--module", "goal"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");
        String specChange = firstValue(start.stdout(), "spec_change: ");

        recordTwoStepGoal(goalKey);

        Harness verify = new Harness(tempDir);
        int verifyExit = new CommandRouter().run(new String[]{
                "goal", "verify", "--project-root", "demo", "--goal", goalKey, "--json"
        }, verify.context());
        assertEquals(ExitCodes.SUCCESS, verifyExit);
        assertTrue(verify.stdout().contains("\"decision\": \"not_ready\""));
        assertTrue(verify.stdout().contains("check spec is failed; accepted_statuses=passed"));
        assertTrue(verify.stdout().contains("acceptance manual_business is pending"));
        assertEquals("pending", singleString(root, "SELECT status FROM spec_acceptance "
                + "WHERE change_key = '" + specChange + "' AND acceptance_key = 'manual_business'"));

        Harness complete = new Harness(tempDir);
        int completeExit = new CommandRouter().run(new String[]{
                "goal", "complete", "--project-root", "demo", "--goal", goalKey
        }, complete.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, completeExit);
        assertTrue(complete.stdout().contains("decision: not_ready"));
        assertTrue(complete.stdout().contains("check spec is failed; accepted_statuses=passed"));
    }

    @Test
    void javaGoalSyncsMappedWorkflowGatesByDefault() throws Exception {
        Path root = tempDir.resolve("demo");
        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "demo",
                "--profile", "java-api-change",
                "--task", "Default strict workflow guard",
                "--module", "goal",
                "--mode", "api"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");

        recordJavaGoalSteps(goalKey);
        writeMinimalPom(root);

        Harness check = new Harness(tempDir);
        int checkExit = new CommandRouter().run(new String[]{
                "goal", "check", "--project-root", "demo", "--goal", goalKey, "--all"
        }, check.context());
        assertEquals(ExitCodes.SUCCESS, checkExit);
        assertTrue(check.stdout().contains("check_key: workflow"));
        assertTrue(check.stdout().contains("status: passed"));
        assertTrue(check.stdout().contains("pending_hard_gates=0"));

        Harness verify = new Harness(tempDir);
        int verifyExit = new CommandRouter().run(new String[]{
                "goal", "verify", "--project-root", "demo", "--goal", goalKey
        }, verify.context());
        assertEquals(ExitCodes.SUCCESS, verifyExit);
        assertTrue(verify.stdout().contains("decision: ready_to_complete"));
        assertTrue(verify.stdout().contains("workflow: passed"));
        assertTrue(verify.stdout().contains("context_path: " + PathUtil.goalContext(root)));
    }

    @Test
    void policyCanBlockGoalCompleteWhenProtectedFileChanged() throws Exception {
        Path root = tempDir.resolve("demo");
        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "demo",
                "--profile", "java-api-change",
                "--task", "Protected file completion guard",
                "--module", "goal",
                "--mode", "api"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");
        String specChange = firstValue(start.stdout(), "spec_change: ");

        recordJavaGoalSteps(goalKey);
        writeMinimalPom(root);

        Harness check = new Harness(tempDir);
        int checkExit = new CommandRouter().run(new String[]{
                "goal", "check", "--project-root", "demo", "--goal", goalKey, "--all"
        }, check.context());
        assertEquals(ExitCodes.SUCCESS, checkExit);

        Files.write(PathUtil.devharnessPolicy(root), ("{\n"
                + "  \"mode\": \"strict\",\n"
                + "  \"protected_files\": \"src/main/java/com/devharnesskit/dhk/service/goal/GoalCheckPolicy.java\"\n"
                + "}\n").getBytes("UTF-8"));

        Harness freshCheck = new Harness(tempDir);
        int freshCheckExit = new CommandRouter().run(new String[]{
                "goal", "check", "--project-root", "demo", "--goal", goalKey, "--all"
        }, freshCheck.context());
        assertEquals(ExitCodes.SUCCESS, freshCheckExit);

        Harness complete = new Harness(tempDir);
        int completeExit = new CommandRouter().run(new String[]{
                "goal", "complete", "--project-root", "demo", "--goal", goalKey
        }, complete.context());

        assertEquals(ExitCodes.VALIDATION_ERROR, completeExit);
        assertTrue(complete.stderr().contains("Policy blocked goal complete"));
        assertTrue(complete.stderr().contains("protected file changed"));
    }

    @Test
    void policyCanBlockGoalStepBeforeStateMutation() throws Exception {
        Path root = tempDir.resolve("demo");
        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "demo",
                "--profile", "java-api-change",
                "--task", "Step policy guard",
                "--module", "goal"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");

        Files.write(PathUtil.devharnessPolicy(root), ("{\n"
                + "  \"mode\": \"strict\",\n"
                + "  \"forbidden_dhk_commands\": \"goal step\"\n"
                + "}\n").getBytes("UTF-8"));

        Harness step = new Harness(tempDir);
        int stepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", "demo",
                "--goal", goalKey,
                "--summary", "Inspected existing controller/service/mapper/tests",
                "--evidence", "existing_controller,existing_service,existing_mapper,existing_tests"
        }, step.context());

        assertEquals(ExitCodes.VALIDATION_ERROR, stepExit);
        assertTrue(step.stderr().contains("Policy blocked goal step"));
        assertTrue(step.stderr().contains("command is forbidden by policy"));
        assertEquals(0, countRows(root, "goal_step WHERE goal_key = '" + goalKey + "'"));
        assertEquals("inspect_existing_code", singleString(root,
                "SELECT current_action FROM goal_run WHERE goal_key = '" + goalKey + "'"));
    }

    @Test
    void policyCanBlockGoalStepProtectedFileBeforeStateMutation() throws Exception {
        Path root = tempDir.resolve("demo");
        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "demo",
                "--profile", "java-api-change",
                "--task", "Step protected file guard",
                "--module", "goal"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");

        Files.write(PathUtil.devharnessPolicy(root), ("{\n"
                + "  \"mode\": \"strict\",\n"
                + "  \"protected_files\": \"src/main/java/Secret.java\"\n"
                + "}\n").getBytes("UTF-8"));

        Harness step = new Harness(tempDir);
        int stepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", "demo",
                "--goal", goalKey,
                "--summary", "Inspected existing controller/service/mapper/tests",
                "--changed-files", "src/main/java/Secret.java",
                "--evidence", "existing_controller,existing_service,existing_mapper,existing_tests"
        }, step.context());

        assertEquals(ExitCodes.VALIDATION_ERROR, stepExit);
        assertTrue(step.stderr().contains("Policy blocked goal step"));
        assertTrue(step.stderr().contains("protected file changed"));
        assertEquals(0, countRows(root, "goal_step WHERE goal_key = '" + goalKey + "'"));
    }

    @Test
    void policyCanBlockGoalStepOutsideAllowedWritePathsBeforeStateMutation() throws Exception {
        Path root = tempDir.resolve("demo");
        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "demo",
                "--profile", "java-api-change",
                "--task", "Step allowed write guard",
                "--module", "goal"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");

        Files.write(PathUtil.devharnessPolicy(root), ("{\n"
                + "  \"mode\": \"strict\",\n"
                + "  \"allowed_write_paths\": \"src/main/java/**\"\n"
                + "}\n").getBytes("UTF-8"));

        Harness step = new Harness(tempDir);
        int stepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", "demo",
                "--goal", goalKey,
                "--summary", "Inspected existing controller/service/mapper/tests",
                "--changed-files", "docs/README.md",
                "--evidence", "existing_controller,existing_service,existing_mapper,existing_tests"
        }, step.context());

        assertEquals(ExitCodes.VALIDATION_ERROR, stepExit);
        assertTrue(step.stderr().contains("Policy blocked goal step"));
        assertTrue(step.stderr().contains("outside allowed_write_paths"));
        assertEquals(0, countRows(root, "goal_step WHERE goal_key = '" + goalKey + "'"));
    }

    @Test
    void policyCanBlockGoalCheckBeforeRows() throws Exception {
        Path root = tempDir.resolve("demo");
        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "demo",
                "--profile", "java-api-change",
                "--task", "Check policy guard",
                "--module", "goal"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");

        Files.write(PathUtil.devharnessPolicy(root), ("{\n"
                + "  \"mode\": \"strict\",\n"
                + "  \"forbidden_dhk_commands\": \"goal check\"\n"
                + "}\n").getBytes("UTF-8"));

        Harness check = new Harness(tempDir);
        int checkExit = new CommandRouter().run(new String[]{
                "goal", "check",
                "--project-root", "demo",
                "--goal", goalKey,
                "--check", "sensitive"
        }, check.context());

        assertEquals(ExitCodes.VALIDATION_ERROR, checkExit);
        assertTrue(check.stderr().contains("Policy blocked goal check"));
        assertTrue(check.stderr().contains("command is forbidden by policy"));
        assertEquals(0, countRows(root, "goal_check WHERE goal_key = '" + goalKey + "'"));
    }

    @Test
    void policyCanReturnValidationForGoalExportHookFailure() throws Exception {
        Path root = tempDir.resolve("demo");
        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "demo",
                "--profile", "java-api-change",
                "--task", "Export policy guard",
                "--module", "goal"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");

        Files.write(PathUtil.devharnessPolicy(root), ("{\n"
                + "  \"mode\": \"strict\",\n"
                + "  \"context_export_forbidden_files\": \".agents/memory/exports/GOAL_CONTEXT.md\"\n"
                + "}\n").getBytes("UTF-8"));

        Harness export = new Harness(tempDir);
        int exportExit = new CommandRouter().run(new String[]{
                "goal", "export",
                "--project-root", "demo",
                "--goal", goalKey
        }, export.context());

        assertEquals(ExitCodes.VALIDATION_ERROR, exportExit);
        assertTrue(export.stderr().contains("Policy blocked context export"));
        assertTrue(export.stderr().contains("output path is forbidden"));
        assertFalse(export.stderr().contains("ERROR goal export failed"));
    }

    @Test
    void goalCompleteFailsWhenWorkflowCheckReportsPendingHardGates() throws Exception {
        Path root = tempDir.resolve("demo");
        Files.createDirectories(PathUtil.goalProfilesDirectory(root));
        Files.write(PathUtil.goalProfile(root, "custom-workflow-strict"), ("{\n"
                + "  \"workflow_key\": \"api-change\",\n"
                + "  \"requires_spec\": \"false\",\n"
                + "  \"default_mode\": \"api\",\n"
                + "  \"actions\": \"inspect,verify\"\n"
                + "}\n").getBytes("UTF-8"));
        Files.write(PathUtil.goalCheckPolicy(root), ("{\n"
                + "  \"required_checks\": \"workflow\",\n"
                + "  \"accepted_workflow_statuses\": \"passed\",\n"
                + "  \"fail_pending_hard_gates\": \"true\"\n"
                + "}\n").getBytes("UTF-8"));

        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "demo",
                "--profile", "custom-workflow-strict",
                "--task", "Strict workflow pending gates",
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
                "--evidence", "compile_result=not required; test_result=not required; sensitive_result=not required"
        }, verifyStep.context());
        assertEquals(ExitCodes.SUCCESS, verifyStepExit);

        Harness check = new Harness(tempDir);
        int checkExit = new CommandRouter().run(new String[]{
                "goal", "check", "--project-root", "demo", "--goal", goalKey, "--all"
        }, check.context());
        assertEquals(ExitCodes.SUCCESS, checkExit);
        assertTrue(check.stdout().contains("check_key: workflow"));
        assertTrue(check.stdout().contains("status: failed"));
        assertTrue(check.stdout().contains("workflow has pending hard gates"));

        Harness complete = new Harness(tempDir);
        int completeExit = new CommandRouter().run(new String[]{
                "goal", "complete", "--project-root", "demo", "--goal", goalKey
        }, complete.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, completeExit);
        assertTrue(complete.stdout().contains("decision: not_ready"));
        assertTrue(complete.stdout().contains("check workflow is failed; accepted_statuses=passed"));
    }

    @Test
    void goalCompleteFailsWhenSpecCheckReportsOpenTaskAndAcceptance() throws Exception {
        Path root = tempDir.resolve("demo");
        Files.createDirectories(PathUtil.goalProfilesDirectory(root));
        Files.write(PathUtil.goalProfile(root, "custom-spec-strict"), ("{\n"
                + "  \"workflow_key\": \"api-change\",\n"
                + "  \"requires_spec\": \"true\",\n"
                + "  \"default_mode\": \"api\",\n"
                + "  \"actions\": \"inspect,verify\"\n"
                + "}\n").getBytes("UTF-8"));
        Files.write(PathUtil.goalCheckPolicy(root), ("{\n"
                + "  \"required_checks\": \"spec\",\n"
                + "  \"accepted_spec_statuses\": \"passed\"\n"
                + "}\n").getBytes("UTF-8"));

        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "demo",
                "--profile", "custom-spec-strict",
                "--task", "Strict spec closure",
                "--module", "goal"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");
        String specChange = firstValue(start.stdout(), "spec_change: ");

        Harness task = new Harness(tempDir);
        int taskExit = new CommandRouter().run(new String[]{
                "spec", "task", "add",
                "--project-root", "demo",
                "--change", specChange,
                "--task", "T001",
                "--title", "Close implementation task",
                "--description", "Must be done before completion",
                "--phase", "verify"
        }, task.context());
        assertEquals(ExitCodes.SUCCESS, taskExit);

        Harness acceptance = new Harness(tempDir);
        int acceptanceExit = new CommandRouter().run(new String[]{
                "spec", "acceptance", "add",
                "--project-root", "demo",
                "--change", specChange,
                "--acceptance", "A001",
                "--description", "Completion acceptance must pass",
                "--expected", "Spec acceptance status is passed or waived"
        }, acceptance.context());
        assertEquals(ExitCodes.SUCCESS, acceptanceExit);

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
                "--evidence", "compile_result=not required; test_result=not required; sensitive_result=not required"
        }, verifyStep.context());
        assertEquals(ExitCodes.SUCCESS, verifyStepExit);

        Harness check = new Harness(tempDir);
        int checkExit = new CommandRouter().run(new String[]{
                "goal", "check", "--project-root", "demo", "--goal", goalKey, "--all"
        }, check.context());
        assertEquals(ExitCodes.SUCCESS, checkExit);
        assertTrue(check.stdout().contains("check_key: spec"));
        assertTrue(check.stdout().contains("status: failed"));
        assertTrue(check.stdout().contains("task T001 is pending"));
        assertTrue(check.stdout().contains("acceptance A001 is pending"));

        Harness evaluate = new Harness(tempDir);
        int evaluateExit = new CommandRouter().run(new String[]{
                "goal", "evaluate", "--project-root", "demo", "--goal", goalKey
        }, evaluate.context());
        assertEquals(ExitCodes.SUCCESS, evaluateExit);
        assertTrue(evaluate.stdout().contains("decision: not_ready"));
        assertTrue(evaluate.stdout().contains("check spec is failed; accepted_statuses=passed"));

        Harness complete = new Harness(tempDir);
        int completeExit = new CommandRouter().run(new String[]{
                "goal", "complete", "--project-root", "demo", "--goal", goalKey
        }, complete.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, completeExit);
        assertTrue(complete.stdout().contains("decision: not_ready"));
        assertTrue(complete.stdout().contains("check spec is failed; accepted_statuses=passed"));
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

        Harness verifyFresh = new Harness(tempDir);
        int verifyFreshExit = new CommandRouter().run(new String[]{
                "goal", "verify", "--project-root", "demo", "--goal", goalKey, "--json"
        }, verifyFresh.context());
        assertEquals(ExitCodes.SUCCESS, verifyFreshExit);
        assertTrue(verifyFresh.stdout().contains("\"command\": \"goal verify\""));
        assertTrue(verifyFresh.stdout().contains("\"decision\": \"ready_to_complete\""));
        assertTrue(verifyFresh.stdout().contains("\"check_count\": 1"));
        assertTrue(verifyFresh.stdout().contains("\"stale_count\": 0"));

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
    void goalEvaluateRejectsWorkspaceChangesAfterChecksUntilRerun() throws Exception {
        Path root = tempDir.resolve("demo");
        Files.createDirectories(PathUtil.goalProfilesDirectory(root));
        Files.write(PathUtil.goalProfile(root, "custom-workspace-stale"), ("{\n"
                + "  \"workflow_key\": \"api-change\",\n"
                + "  \"requires_spec\": \"false\",\n"
                + "  \"default_mode\": \"api\",\n"
                + "  \"actions\": \"inspect,verify\"\n"
                + "}\n").getBytes("UTF-8"));
        Files.write(PathUtil.goalCheckPolicy(root), ("{\n"
                + "  \"required_checks\": \"sensitive\",\n"
                + "  \"accepted_sensitive_statuses\": \"passed\"\n"
                + "}\n").getBytes("UTF-8"));

        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "demo",
                "--profile", "custom-workspace-stale",
                "--task", "Workspace stale guard",
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
                "--summary", "Verification evidence recorded",
                "--evidence", "compile_result=not required; test_result=not required; sensitive_result=ok"
        }, verifyStep.context());
        assertEquals(ExitCodes.SUCCESS, verifyStepExit);

        Harness check = new Harness(tempDir);
        int checkExit = new CommandRouter().run(new String[]{
                "goal", "check", "--project-root", "demo", "--goal", goalKey, "--all"
        }, check.context());
        assertEquals(ExitCodes.SUCCESS, checkExit);
        assertTrue(check.stdout().contains("workspace_fingerprint: fallback:"));
        assertTrue(check.stdout().contains("context_fingerprint: context:"));
        assertTrue(check.stdout().contains("check_fingerprint: check:"));
        assertEquals(1, countRows(root, "schema_version WHERE version = 8"));

        Harness readyEvaluate = new Harness(tempDir);
        int readyEvaluateExit = new CommandRouter().run(new String[]{
                "goal", "evaluate", "--project-root", "demo", "--goal", goalKey, "--json"
        }, readyEvaluate.context());
        assertEquals(ExitCodes.SUCCESS, readyEvaluateExit);
        assertTrue(readyEvaluate.stdout().contains("\"decision\": \"ready_to_complete\""));

        Files.createDirectories(root.resolve("src/main/java"));
        Files.write(root.resolve("src/main/java/Demo.java"),
                "final class Demo {}\n".getBytes("UTF-8"));

        Harness staleEvaluate = new Harness(tempDir);
        int staleEvaluateExit = new CommandRouter().run(new String[]{
                "goal", "evaluate", "--project-root", "demo", "--goal", goalKey, "--json"
        }, staleEvaluate.context());
        assertEquals(ExitCodes.SUCCESS, staleEvaluateExit);
        assertTrue(staleEvaluate.stdout().contains("\"decision\": \"not_ready\""));
        assertTrue(staleEvaluate.stdout().contains("check sensitive is stale: workspace fingerprint changed"));
        assertTrue(staleEvaluate.stdout().contains("\"stale_count\": 1"));

        Harness staleComplete = new Harness(tempDir);
        int staleCompleteExit = new CommandRouter().run(new String[]{
                "goal", "complete", "--project-root", "demo", "--goal", goalKey, "--json"
        }, staleComplete.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, staleCompleteExit);
        assertTrue(staleComplete.stdout().contains("workspace fingerprint changed"));

        Harness verifyFresh = new Harness(tempDir);
        int verifyFreshExit = new CommandRouter().run(new String[]{
                "goal", "verify", "--project-root", "demo", "--goal", goalKey, "--json"
        }, verifyFresh.context());
        assertEquals(ExitCodes.SUCCESS, verifyFreshExit);
        assertTrue(verifyFresh.stdout().contains("\"decision\": \"ready_to_complete\""));
        assertTrue(verifyFresh.stdout().contains("\"stale_count\": 0"));
    }

    @Test
    void goalUsesFormalProfileSchemaForEvidenceChecksAndMappings() throws Exception {
        Path root = tempDir.resolve("demo");
        Files.createDirectories(PathUtil.goalProfilesDirectory(root));
        Files.write(PathUtil.goalProfile(root, "custom-formal"), ("{\n"
                + "  \"profile_key\": \"custom-formal\",\n"
                + "  \"workflow_key\": \"api-change\",\n"
                + "  \"requires_spec\": \"false\",\n"
                + "  \"default_mode\": \"api\",\n"
                + "  \"actions\": \"custom_inspect,verify\",\n"
                + "  \"required_checks\": \"sensitive\",\n"
                + "  \"completion_require_fresh_checks\": \"true\",\n"
                + "  \"completion_allow_skipped_checks\": \"false\",\n"
                + "  \"completion_require_checkpoint\": \"true\",\n"
                + "  \"required_evidence.custom_inspect\": \"custom_read,custom_pattern_summary\",\n"
                + "  \"required_evidence.verify\": \"compile_result,test_result,sensitive_result\",\n"
                + "  \"mapping.custom_inspect.workflow_phase\": \"inspect_existing_code\",\n"
                + "  \"mapping.verify.workflow_phase\": \"verify_tests\",\n"
                + "  \"mapping.verify.required_gates\": \"tests_recorded\",\n"
                + "  \"mapping.verify.phase_pass_mode\": \"check\",\n"
                + "  \"mapping.verify.gate_pass_mode\": \"check\",\n"
                + "  \"mapping.verify.spec_acceptance_update\": \"manual\",\n"
                + "  \"mapping.verify.acceptance_source\": \"manual\",\n"
                + "  \"mapping.verify.required_checks\": \"sensitive\"\n"
                + "}\n").getBytes("UTF-8"));

        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "demo",
                "--profile", "custom-formal",
                "--task", "Formal custom profile",
                "--module", "goal"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");
        assertTrue(start.stdout().contains("current_action: custom_inspect"));

        Harness next = new Harness(tempDir);
        int nextExit = new CommandRouter().run(new String[]{
                "goal", "next", "--project-root", "demo", "--goal", goalKey
        }, next.context());
        assertEquals(ExitCodes.SUCCESS, nextExit);
        assertTrue(next.stdout().contains("custom_read"));
        assertTrue(next.stdout().contains("custom_pattern_summary"));
        assertTrue(next.stdout().contains("required_checks:"));
        assertTrue(next.stdout().contains("  - sensitive"));
        assertFalse(next.stdout().contains("  - compile"));

        Harness missingEvidence = new Harness(tempDir);
        int missingEvidenceExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", "demo",
                "--goal", goalKey,
                "--summary", "Custom inspect incomplete",
                "--evidence", "custom_read=GoalProfileService"
        }, missingEvidence.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, missingEvidenceExit);
        assertTrue(missingEvidence.stderr().contains("custom_pattern_summary"));

        Harness inspectStep = new Harness(tempDir);
        int inspectStepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", "demo",
                "--goal", goalKey,
                "--summary", "Custom inspect complete",
                "--evidence", "custom_read=GoalProfileService; custom_pattern_summary=formal schema"
        }, inspectStep.context());
        assertEquals(ExitCodes.SUCCESS, inspectStepExit);
        assertTrue(inspectStep.stdout().contains("current_action: verify"));

        Harness verifyStep = new Harness(tempDir);
        int verifyStepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", "demo",
                "--goal", goalKey,
                "--summary", "Custom verification evidence recorded",
                "--evidence", "compile_result=not required; test_result=not required; sensitive_result=pending"
        }, verifyStep.context());
        assertEquals(ExitCodes.SUCCESS, verifyStepExit);

        Harness check = new Harness(tempDir);
        int checkExit = new CommandRouter().run(new String[]{
                "goal", "check", "--project-root", "demo", "--goal", goalKey, "--all", "--json"
        }, check.context());
        assertEquals(ExitCodes.SUCCESS, checkExit);
        assertTrue(check.stdout().contains("\"count\": 1"));
        assertTrue(check.stdout().contains("\"check_key\": \"sensitive\""));
        assertTrue(check.stdout().contains("\"step_count_at_check\": 2"));
        assertFalse(check.stdout().contains("\"check_key\": \"compile\""));

        Harness evaluate = new Harness(tempDir);
        int evaluateExit = new CommandRouter().run(new String[]{
                "goal", "evaluate", "--project-root", "demo", "--goal", goalKey, "--json"
        }, evaluate.context());
        assertEquals(ExitCodes.SUCCESS, evaluateExit);
        assertTrue(evaluate.stdout().contains("\"decision\": \"ready_to_complete\""));
        assertTrue(evaluate.stdout().contains("\"missing_count\": 0"));
    }

    @Test
    void builtInGraphAwareJavaProfileKeepsGraphInsideMainActionContract() throws Exception {
        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "demo",
                "--profile", "java-api-change-with-graph",
                "--task", "Graph aware API change",
                "--module", "order"
        }, start.context());

        assertEquals(ExitCodes.SUCCESS, startExit);
        assertTrue(start.stdout().contains("profile: java-api-change-with-graph"));
        assertTrue(start.stdout().contains("current_action: inspect_existing_code"));
        String goalKey = firstValue(start.stdout(), "goal_key: ");

        Harness next = new Harness(tempDir);
        int nextExit = new CommandRouter().run(new String[]{
                "goal", "next", "--project-root", "demo", "--goal", goalKey
        }, next.context());

        assertEquals(ExitCodes.SUCCESS, nextExit);
        assertTrue(next.stdout().contains("current_action: inspect_existing_code"));
        assertTrue(next.stdout().contains("graph_assist:"));
        assertTrue(next.stdout().contains("integrated_into_main_flow: true"));
        assertTrue(next.stdout().contains("recommended_internal_action: refresh_graph_context"));
        assertTrue(next.stdout().contains("internal_helper: .agents/skills/devharness-graph-aware-development/scripts/graph-index-export.sh"));
        assertTrue(next.stdout().contains("next_command: .agents/skills/devharness-goal-development/scripts/goal-step.sh"));
        assertTrue(next.stdout().contains("graph_snapshot"));
        assertTrue(next.stdout().contains("graph_context"));
        assertTrue(next.stdout().contains("impact_map"));
        assertTrue(next.stdout().contains("recommended_read_files"));
        assertTrue(next.stdout().contains("  - architecture"));
        assertFalse(next.stdout().contains("current_action: graph_index_or_refresh"));

        String context = new String(Files.readAllBytes(PathUtil.goalContext(tempDir.resolve("demo"))), "UTF-8");
        assertTrue(context.contains("<graph-snapshot>"));
        assertTrue(context.contains("<graph-context>"));
        assertTrue(context.contains("<graph-assist>"));
        assertTrue(context.contains("- integrated_into_main_flow: true"));
        assertTrue(context.contains("- recommended_internal_action: refresh_graph_context"));
        assertFalse(context.contains("<required-graph-action>"));
    }

    @Test
    void graphAwareJavaProfileRequestsImpactMapInsideInspectAction() throws Exception {
        Path root = tempDir.resolve("demo-impact");
        Path source = root.resolve("src/main/java/com/example/App.java");
        Files.createDirectories(source.getParent());
        Files.write(source, "package com.example;\npublic class App { public void run() {} }\n".getBytes("UTF-8"));

        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "demo-impact",
                "--profile", "java-api-change-with-graph",
                "--task", "Graph impact API change",
                "--module", "order"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit,
                "stdout=" + start.stdout() + "\nstderr=" + start.stderr());
        String goalKey = firstValue(start.stdout(), "goal_key: ");

        Harness index = new Harness(tempDir);
        int indexExit = new CommandRouter().run(new String[]{
                "graph", "index", "--project-root", "demo-impact"
        }, index.context());
        assertEquals(ExitCodes.SUCCESS, indexExit);
        Harness export = new Harness(tempDir);
        int exportExit = new CommandRouter().run(new String[]{
                "graph", "export", "--project-root", "demo-impact"
        }, export.context());
        assertEquals(ExitCodes.SUCCESS, exportExit);

        Harness next = new Harness(tempDir);
        int nextExit = new CommandRouter().run(new String[]{
                "goal", "next", "--project-root", "demo-impact", "--goal", goalKey
        }, next.context());

        assertEquals(ExitCodes.SUCCESS, nextExit);
        assertTrue(next.stdout().contains("current_action: inspect_existing_code"));
        assertTrue(next.stdout().contains("recommended_internal_action: prepare_impact_map"));
        assertTrue(next.stdout().contains("internal_helper: .agents/skills/devharness-graph-aware-development/scripts/graph-impact.sh"));
        assertTrue(next.stdout().contains("next_command: .agents/skills/devharness-goal-development/scripts/goal-step.sh"));
    }

    @Test
    void graphAwareGoalNextRequiresGraphRefreshWhenSnapshotIsStale() throws Exception {
        Path root = tempDir.resolve("demo-stale-preflight");
        Path source = root.resolve("src/main/java/com/example/App.java");
        Files.createDirectories(source.getParent());
        Files.write(source, "package com.example;\npublic class App { public void run() {} }\n".getBytes("UTF-8"));

        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "demo-stale-preflight",
                "--profile", "java-api-change-with-graph",
                "--task", "Graph stale preflight",
                "--module", "order"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");

        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "graph", "index", "--project-root", "demo-stale-preflight"
        }, new Harness(tempDir).context()));
        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "graph", "export", "--project-root", "demo-stale-preflight"
        }, new Harness(tempDir).context()));

        Files.write(source, "\n// changed after graph snapshot\n".getBytes("UTF-8"), StandardOpenOption.APPEND);

        Harness next = new Harness(tempDir);
        int nextExit = new CommandRouter().run(new String[]{
                "goal", "next", "--project-root", "demo-stale-preflight", "--goal", goalKey
        }, next.context());

        assertEquals(ExitCodes.SUCCESS, nextExit);
        assertTrue(next.stdout().contains("current_action: inspect_existing_code"));
        assertTrue(next.stdout().contains("graph_stale: true"));
        assertTrue(next.stdout().contains("freshness_status: stale"));
        assertTrue(next.stdout().contains("snapshot_workspace_fingerprint: fallback:"));
        assertTrue(next.stdout().contains("current_workspace_fingerprint: fallback:"));
        assertTrue(next.stdout().contains("recommended_internal_action: refresh_graph_context"));
        assertTrue(next.stdout().contains("internal_helper: .agents/skills/devharness-graph-aware-development/scripts/graph-index-export.sh"));
        assertTrue(next.stdout().contains("next_command: .agents/skills/devharness-goal-development/scripts/goal-step.sh"));

        String context = new String(Files.readAllBytes(PathUtil.goalContext(root)), "UTF-8");
        assertTrue(context.contains("- graph_stale: true"));
        assertTrue(context.contains("- freshness_status: stale"));
        assertTrue(context.contains("- recommended_internal_action: refresh_graph_context"));
        assertTrue(context.contains("- warning: STALE_GRAPH_SNAPSHOT"));
        assertTrue(context.contains("- precision: heuristic"));
        assertTrue(context.contains("- graph_usage: advisory_preflight_not_completion_proof"));
    }

    @Test
    void graphAwareGoalVerifyFailsWhenImpactMapIsMissing() throws Exception {
        Path root = tempDir.resolve("demo-missing-impact");
        writeSource(root, "src/main/java/com/example/App.java",
                "package com.example;\npublic class App { public void run() {} }\n");
        writeGraphProfile(root, "custom-graph-impact", true);

        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "demo-missing-impact",
                "--profile", "custom-graph-impact",
                "--task", "Graph impact required",
                "--module", "graph"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");

        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "graph", "index", "--project-root", "demo-missing-impact"
        }, new Harness(tempDir).context()));
        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "graph", "export", "--project-root", "demo-missing-impact"
        }, new Harness(tempDir).context()));
        recordCustomGraphGoalSteps("demo-missing-impact", goalKey,
                "src/main/java/com/example/App.java", false);

        Harness verify = new Harness(tempDir);
        int verifyExit = new CommandRouter().run(new String[]{
                "goal", "verify", "--project-root", "demo-missing-impact", "--goal", goalKey
        }, verify.context());

        assertEquals(ExitCodes.SUCCESS, verifyExit);
        assertTrue(verify.stdout().contains("decision: not_ready"));
        assertTrue(verify.stdout().contains("impact: failed - impact freshness failed"));
        assertTrue(verify.stdout().contains("IMPACT_MAP.md missing"));
        assertTrue(verify.stdout().contains("failed_checks:"));
        assertTrue(verify.stdout().contains("blocker_summary: failed_check: check impact is failed; accepted_statuses=passed"));
        assertTrue(verify.stdout().contains("blocker_categories:"));
        assertTrue(verify.stdout().contains("  - failed_check"));
        assertTrue(verify.stdout().contains("blocker_details:"));
        assertTrue(verify.stdout().contains("    check_key: impact"));
        assertTrue(verify.stdout().contains("next_command: dhk goal check --goal " + goalKey
                + " --check impact"));

        Harness complete = new Harness(tempDir);
        int completeExit = new CommandRouter().run(new String[]{
                "goal", "complete", "--project-root", "demo-missing-impact", "--goal", goalKey
        }, complete.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, completeExit);
        assertTrue(complete.stdout().contains("check impact is failed"));
    }

    @Test
    void graphAwareGoalVerifyFailsWhenGraphSnapshotWorkspaceFingerprintChanges() throws Exception {
        Path root = tempDir.resolve("demo-stale-graph");
        Path source = writeSource(root, "src/main/java/com/example/App.java",
                "package com.example;\npublic class App { public void run() {} }\n");
        writeGraphProfile(root, "custom-graph-only", false);

        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "demo-stale-graph",
                "--profile", "custom-graph-only",
                "--task", "Graph snapshot must be fresh",
                "--module", "graph"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");

        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "graph", "index", "--project-root", "demo-stale-graph"
        }, new Harness(tempDir).context()));
        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "graph", "export", "--project-root", "demo-stale-graph"
        }, new Harness(tempDir).context()));
        recordCustomGraphOnlyGoalSteps("demo-stale-graph", goalKey,
                "src/main/java/com/example/App.java");
        Files.write(source, "\n// changed after graph snapshot\n".getBytes("UTF-8"), StandardOpenOption.APPEND);

        Harness verify = new Harness(tempDir);
        int verifyExit = new CommandRouter().run(new String[]{
                "goal", "verify", "--project-root", "demo-stale-graph", "--goal", goalKey, "--json"
        }, verify.context());

        assertEquals(ExitCodes.SUCCESS, verifyExit);
        assertTrue(verify.stdout().contains("\"decision\": \"not_ready\""));
        assertTrue(verify.stdout().contains("\"failed_count\": 1"));
        assertTrue(verify.stdout().contains("graph: graph freshness failed"));
        assertTrue(verify.stdout().contains("workspace fingerprint changed"));
        assertTrue(verify.stdout().contains("dhk graph index --project-root"));
        assertTrue(verify.stdout().contains("\"next_command\": \"dhk goal check --goal " + goalKey
                + " --check graph\""));
    }

    @Test
    void graphAwareGoalVerifyFailsWhenChangedFilesAreOutsideImpactMap() throws Exception {
        Path root = tempDir.resolve("demo-impact-coverage");
        writeSource(root, "src/main/java/com/example/App.java",
                "package com.example;\npublic class App { public void run() {} }\n");
        writeSource(root, "src/main/java/org/acme/Other.java",
                "package org.acme;\npublic class Other { public void run() {} }\n");
        writeGraphProfile(root, "custom-graph-coverage", true);

        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "demo-impact-coverage",
                "--profile", "custom-graph-coverage",
                "--task", "Impact map must cover changes",
                "--module", "graph"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");

        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "graph", "index", "--project-root", "demo-impact-coverage"
        }, new Harness(tempDir).context()));
        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "graph", "export", "--project-root", "demo-impact-coverage"
        }, new Harness(tempDir).context()));
        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "graph", "impact", "--project-root", "demo-impact-coverage",
                "--file", "src/main/java/com/example/App.java"
        }, new Harness(tempDir).context()));
        recordCustomGraphGoalSteps("demo-impact-coverage", goalKey,
                "src/main/java/org/acme/Other.java", true);

        Harness verify = new Harness(tempDir);
        int verifyExit = new CommandRouter().run(new String[]{
                "goal", "verify", "--project-root", "demo-impact-coverage", "--goal", goalKey, "--json"
        }, verify.context());

        assertEquals(ExitCodes.SUCCESS, verifyExit);
        assertTrue(verify.stdout().contains("\"decision\": \"not_ready\""));
        assertTrue(verify.stdout().contains("impact: impact freshness failed"));
        assertTrue(verify.stdout().contains("changed files not covered by impact map"));
        assertTrue(verify.stdout().contains("src/main/java/org/acme/Other.java"));
        assertTrue(verify.stdout().contains("dhk graph impact --project-root"));
        assertTrue(verify.stdout().contains("\"next_command\": \"dhk goal check --goal " + goalKey
                + " --check impact\""));
    }

    @Test
    void goalVerifyFailsArchitectureCheckWhenModeIsFail() throws Exception {
        Path root = copyFixture("modern-java-api", tempDir.resolve("modern-java-api-arch-fail"));
        String projectRoot = root.toString();
        Files.createDirectories(PathUtil.graphDirectory(root));
        Files.write(PathUtil.graphArchitectureConfig(root), ("{\n"
                + "  \"schema_version\": \"devharness-graph-architecture/v1-alpha\",\n"
                + "  \"mode\": \"fail\",\n"
                + "  \"controller_patterns\": [\"src/main/java/**/controller/**\"],\n"
                + "  \"service_patterns\": [\"src/main/java/**/service/**\"],\n"
                + "  \"repository_patterns\": [\"src/main/java/**/repository/**\"],\n"
                + "  \"public_api_patterns\": [\"src/main/java/**/controller/**\"],\n"
                + "  \"forbidden_dependencies\": [\"controller->repository\"]\n"
                + "}\n").getBytes("UTF-8"));
        writeArchitectureProfile(root, "custom-architecture");
        Path controller = root.resolve("src/main/java/com/acme/modern/account/controller/AccountController.java");
        String controllerText = new String(Files.readAllBytes(controller), "UTF-8");
        controllerText = insertLineAfter(controllerText,
                "import com.acme.modern.account.service.AccountService;",
                "import com.acme.modern.account.repository.AccountRepository;");
        Files.write(controller, controllerText.getBytes("UTF-8"));

        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", projectRoot,
                "--profile", "custom-architecture",
                "--task", "Architecture violation must fail",
                "--module", "modern"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");

        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "graph", "index", "--project-root", projectRoot
        }, new Harness(tempDir).context()));
        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "graph", "export", "--project-root", projectRoot
        }, new Harness(tempDir).context()));
        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "graph", "impact", "--project-root", projectRoot,
                "--file", "src/main/java/com/acme/modern/account/controller/AccountController.java"
        }, new Harness(tempDir).context()));
        recordTwoStepGoalForProject(projectRoot, goalKey);

        Harness verify = new Harness(tempDir);
        int verifyExit = new CommandRouter().run(new String[]{
                "goal", "verify", "--project-root", projectRoot, "--goal", goalKey
        }, verify.context());

        assertEquals(ExitCodes.SUCCESS, verifyExit, verify.stderr());
        assertTrue(verify.stdout().contains("decision: not_ready"));
        assertTrue(verify.stdout().contains("architecture: failed - architecture failed"));
        assertTrue(verify.stdout().contains("violations=1"));
        assertTrue(verify.stdout().contains("public_api_impact=true"));

        String log = new String(Files.readAllBytes(PathUtil.goalCheckArtifactsDirectory(root, goalKey)
                .resolve("architecture.log")), "UTF-8");
        assertTrue(log.contains("controller -> repository"));
        assertTrue(log.contains("com.acme.modern.account.controller.AccountController"));
        assertTrue(log.contains("com.acme.modern.account.repository.AccountRepository"));
        assertTrue(log.contains("public_api_impact_files: [src/main/java/com/acme/modern/account/controller/AccountController.java]"));
    }

    @Test
    void goalVerifyAcceptsArchitectureWarningByDefault() throws Exception {
        Path root = tempDir.resolve("demo-arch-warn");
        String projectRoot = root.toString();
        writeModernArchitectureFixture(root, false);
        writeArchitectureProfile(root, "custom-architecture");

        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", projectRoot,
                "--profile", "custom-architecture",
                "--task", "Architecture warning accepted",
                "--module", "modern"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");

        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "graph", "index", "--project-root", projectRoot
        }, new Harness(tempDir).context()));
        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "graph", "export", "--project-root", projectRoot
        }, new Harness(tempDir).context()));
        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "graph", "impact", "--project-root", projectRoot,
                "--file", "src/main/java/com/example/account/controller/AccountController.java"
        }, new Harness(tempDir).context()));
        recordTwoStepGoalForProject(projectRoot, goalKey);

        Harness verify = new Harness(tempDir);
        int verifyExit = new CommandRouter().run(new String[]{
                "goal", "verify", "--project-root", projectRoot, "--goal", goalKey
        }, verify.context());

        assertEquals(ExitCodes.SUCCESS, verifyExit, verify.stderr());
        assertTrue(verify.stdout().contains("decision: ready_to_complete"));
        assertTrue(verify.stdout().contains("architecture: passed - architecture warning"));
        assertTrue(verify.stdout().contains("violations=1"));
        assertTrue(verify.stdout().contains("public_api_impact=true"));
    }

    @Test
    void safeRefactorWithGraphVerifyFailsWhenImpactExpandedWithoutRiskEvidence() throws Exception {
        Path root = copyFixture("modern-java-api", tempDir.resolve("modern-safe-refactor-risk"));
        String projectRoot = root.toString();

        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", projectRoot,
                "--profile", "safe-refactor-with-graph",
                "--task", "Safe refactor requires expansion risk evidence",
                "--module", "modern"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");

        indexExportImpact(projectRoot, "src/main/java/com/acme/modern/account/service/AccountService.java");
        recordSafeRefactorGraphGoalSteps(projectRoot, goalKey, false);

        Harness verify = new Harness(tempDir);
        int verifyExit = new CommandRouter().run(new String[]{
                "goal", "verify", "--project-root", projectRoot, "--goal", goalKey
        }, verify.context());

        assertEquals(ExitCodes.SUCCESS, verifyExit);
        assertTrue(verify.stdout().contains("decision: not_ready"));
        assertTrue(verify.stdout().contains("impact: failed - impact freshness failed"));
        assertTrue(verify.stdout().contains("impact expansion risk evidence missing for safe-refactor"));

        String log = new String(Files.readAllBytes(PathUtil.goalCheckArtifactsDirectory(root, goalKey)
                .resolve("impact.log")), "UTF-8");
        assertTrue(log.contains("safe_refactor_reimpact_required: true"));
        assertTrue(log.contains("missing_related_tests_count: 1"));
    }

    @Test
    void safeRefactorWithGraphSummaryRecordsTestGapAndReimpactResult() throws Exception {
        Path root = copyFixture("modern-java-api", tempDir.resolve("modern-safe-refactor-summary"));
        String projectRoot = root.toString();

        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", projectRoot,
                "--profile", "safe-refactor-with-graph",
                "--task", "Safe refactor summary records graph evidence",
                "--module", "modern"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");

        indexExportImpact(projectRoot, "src/main/java/com/acme/modern/account/service/AccountService.java");
        recordSafeRefactorGraphGoalSteps(projectRoot, goalKey, true);

        Harness verify = new Harness(tempDir);
        int verifyExit = new CommandRouter().run(new String[]{
                "goal", "verify", "--project-root", projectRoot, "--goal", goalKey
        }, verify.context());
        assertEquals(ExitCodes.SUCCESS, verifyExit);
        assertTrue(verify.stdout().contains("decision: ready_to_complete"));

        Harness complete = new Harness(tempDir);
        int completeExit = new CommandRouter().run(new String[]{
                "goal", "complete", "--project-root", projectRoot, "--goal", goalKey
        }, complete.context());
        assertEquals(ExitCodes.SUCCESS, completeExit);

        String summary = new String(Files.readAllBytes(PathUtil.goalSummary(root)), "UTF-8");
        assertTrue(summary.contains("<graph-artifacts>"));
        assertTrue(summary.contains("- missing_related_tests: 1"));
        assertTrue(summary.contains("src/test/java/com/acme/modern/account/repository/AccountRepositoryTest.java"));
        assertTrue(summary.contains("impact_delta=expanded"));
        assertTrue(summary.contains("impact_expansion_risk=reviewed expanded repository test gap"));
    }

    @Test
    void graphAwareGoalCompleteBindsGraphArtifactsAndSummary() throws Exception {
        Path root = tempDir.resolve("demo-graph-complete");
        writeSource(root, "src/main/java/com/example/App.java",
                "package com.example;\npublic class App { public void run() {} }\n");
        writeGraphProfile(root, "custom-graph-complete", true);

        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "demo-graph-complete",
                "--profile", "custom-graph-complete",
                "--task", "Graph artifacts must bind on complete",
                "--module", "graph"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");

        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "graph", "index", "--project-root", "demo-graph-complete"
        }, new Harness(tempDir).context()));
        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "graph", "export", "--project-root", "demo-graph-complete"
        }, new Harness(tempDir).context()));
        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "graph", "impact", "--project-root", "demo-graph-complete",
                "--file", "src/main/java/com/example/App.java"
        }, new Harness(tempDir).context()));
        Files.createDirectories(PathUtil.bddExportsDirectory(root));
        Files.write(PathUtil.scenarioImpactMap(root),
                "# SCENARIO_IMPACT_MAP\n\n<summary>\n- scenario_key: graph-complete\n</summary>\n"
                        .getBytes("UTF-8"));
        recordCustomGraphGoalSteps("demo-graph-complete", goalKey,
                "src/main/java/com/example/App.java", true);

        Harness verify = new Harness(tempDir);
        int verifyExit = new CommandRouter().run(new String[]{
                "goal", "verify", "--project-root", "demo-graph-complete", "--goal", goalKey
        }, verify.context());
        assertEquals(ExitCodes.SUCCESS, verifyExit);
        assertTrue(verify.stdout().contains("decision: ready_to_complete"));

        Harness complete = new Harness(tempDir);
        int completeExit = new CommandRouter().run(new String[]{
                "goal", "complete", "--project-root", "demo-graph-complete", "--goal", goalKey
        }, complete.context());
        assertEquals(ExitCodes.SUCCESS, completeExit);
        assertTrue(complete.stdout().contains("status: completed"));

        String summary = new String(Files.readAllBytes(PathUtil.goalSummary(root)), "UTF-8");
        assertTrue(summary.contains("<graph-artifacts>"));
        assertTrue(summary.contains("- snapshot_key: graph-"));
        assertTrue(summary.contains("- graph_snapshot: "));
        assertTrue(summary.contains("GRAPH_SNAPSHOT.json"));
        assertTrue(summary.contains("GRAPH_CONTEXT.md"));
        assertTrue(summary.contains("IMPACT_MAP.md"));
        assertTrue(summary.contains("SCENARIO_IMPACT_MAP.md"));
        assertTrue(summary.contains("- goal_artifact: scenario_impact_map"));
        assertTrue(summary.contains("- goal_graph_binding: used,summary,impact_map"));
        assertTrue(summary.contains("impact map is query-scoped"));
        assertFalse(summary.contains("password="));
        assertGraphCompletionRows(root, goalKey);
    }

    @Test
    void legacyGraphProfileVerifyFailsWithoutRollbackAndManualEvidence() throws Exception {
        Path root = tempDir.resolve("demo-legacy-missing");
        writeSource(root, "src/main/java/com/example/App.java",
                "package com.example;\npublic class App { public void run() {} }\n");

        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "demo-legacy-missing",
                "--profile", "legacy-java-small-fix-with-graph",
                "--task", "Legacy graph evidence required",
                "--module", "legacy"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");

        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "graph", "index", "--project-root", "demo-legacy-missing"
        }, new Harness(tempDir).context()));
        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "graph", "export", "--project-root", "demo-legacy-missing"
        }, new Harness(tempDir).context()));
        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "graph", "impact", "--project-root", "demo-legacy-missing",
                "--file", "src/main/java/com/example/App.java"
        }, new Harness(tempDir).context()));

        recordLegacyGraphGoalSteps("demo-legacy-missing", goalKey,
                "src/main/java/com/example/App.java",
                ".agents/memory/artifacts/goals/" + goalKey + "/ROLLBACK_PLAN.md",
                "pending",
                ".agents/memory/artifacts/goals/" + goalKey + "/MANUAL_EVIDENCE.md",
                "");

        Harness verify = new Harness(tempDir);
        int verifyExit = new CommandRouter().run(new String[]{
                "goal", "verify", "--project-root", "demo-legacy-missing", "--goal", goalKey
        }, verify.context());

        assertEquals(ExitCodes.SUCCESS, verifyExit);
        assertTrue(verify.stdout().contains("decision: not_ready"));
        assertTrue(verify.stdout().contains("legacy: failed - legacy evidence failed"));
        assertTrue(verify.stdout().contains("rollback plan artifact missing"));
        assertTrue(verify.stdout().contains("manual evidence is not passed"));
        assertTrue(verify.stdout().contains("manual evidence artifact missing"));
    }

    @Test
    void manualCompileVerifyCreatesChoiceInteractionAndManualAnswerPassesCheck() throws Exception {
        Path root = tempDir.resolve("demo-manual-choice");
        writeManualCompilePolicy(root, "java -version");

        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "demo-manual-choice",
                "--profile", "java-api-patch",
                "--task", "Fix small mapping bug",
                "--module", "order"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");

        Harness verify = new Harness(tempDir);
        int verifyExit = new CommandRouter().run(new String[]{
                "goal", "verify", "--project-root", "demo-manual-choice", "--goal", goalKey
        }, verify.context());
        assertEquals(ExitCodes.SUCCESS, verifyExit);
        assertTrue(verify.stdout().contains("manual-compile: failed"));

        String requestId = "interaction-" + goalKey + "-manual-compile";
        Harness show = new Harness(tempDir);
        int showExit = new CommandRouter().run(new String[]{
                "brief", "show", "--project-root", "demo-manual-choice", "--request", requestId
        }, show.context());
        assertEquals(ExitCodes.SUCCESS, showExit);
        assertTrue(show.stdout().contains("type: manual_verification"));
        assertTrue(show.stdout().contains("manual_passed:"));
        assertTrue(show.stdout().contains("try_auto:"));
        assertTrue(show.stdout().contains("waive_verification:"));

        Path evidence = root.resolve(".agents/verification/manual-compile.md");
        Files.createDirectories(evidence.getParent());
        Files.write(evidence, "IDE compile passed".getBytes("UTF-8"));
        Harness answer = new Harness(tempDir);
        int answerExit = new CommandRouter().run(new String[]{
                "brief", "answer",
                "--project-root", "demo-manual-choice",
                "--request", requestId,
                "--choice", "manual_passed",
                "--scope", "IDE full compile",
                "--evidence-path", ".agents/verification/manual-compile.md",
                "--tester", "yangyang"
        }, answer.context());
        assertEquals(ExitCodes.SUCCESS, answerExit);
        assertTrue(answer.stdout().contains("next_action: rerun goal verify"));

        Harness verifyAgain = new Harness(tempDir);
        int verifyAgainExit = new CommandRouter().run(new String[]{
                "goal", "verify", "--project-root", "demo-manual-choice", "--goal", goalKey
        }, verifyAgain.context());
        assertEquals(ExitCodes.SUCCESS, verifyAgainExit);
        assertTrue(verifyAgain.stdout().contains("manual-compile: passed"));
        assertTrue(verifyAgain.stdout().contains("user-confirmed manual evidence passed"));
    }

    @Test
    void manualCompileInteractionCanRunAutoFallbackOrRecordWaiver() throws Exception {
        Path autoRoot = tempDir.resolve("demo-manual-auto");
        writeManualCompilePolicy(autoRoot, "java -version");
        String autoGoal = startPatchGoal("demo-manual-auto", "Auto fallback manual compile");
        String autoRequest = createManualCompileInteraction("demo-manual-auto", autoGoal);

        Harness autoAnswer = new Harness(tempDir);
        int autoAnswerExit = new CommandRouter().run(new String[]{
                "brief", "answer",
                "--project-root", "demo-manual-auto",
                "--request", autoRequest,
                "--choice", "try_auto"
        }, autoAnswer.context());
        assertEquals(ExitCodes.SUCCESS, autoAnswerExit);

        Harness autoVerify = new Harness(tempDir);
        int autoVerifyExit = new CommandRouter().run(new String[]{
                "goal", "verify", "--project-root", "demo-manual-auto", "--goal", autoGoal
        }, autoVerify.context());
        assertEquals(ExitCodes.SUCCESS, autoVerifyExit);
        assertTrue(autoVerify.stdout().contains("manual-compile: passed"));
        assertTrue(autoVerify.stdout().contains("user-selected auto fallback exit_code=0"));

        Path waiverRoot = tempDir.resolve("demo-manual-waiver");
        writeManualCompilePolicy(waiverRoot, "java -version");
        String waiverGoal = startPatchGoal("demo-manual-waiver", "Waiver manual compile");
        String waiverRequest = createManualCompileInteraction("demo-manual-waiver", waiverGoal);
        Path rollback = waiverRoot.resolve(".agents/verification/rollback.md");
        Files.createDirectories(rollback.getParent());
        Files.write(rollback, "Rollback by reverting the patch".getBytes("UTF-8"));

        Harness waiverAnswer = new Harness(tempDir);
        int waiverAnswerExit = new CommandRouter().run(new String[]{
                "brief", "answer",
                "--project-root", "demo-manual-waiver",
                "--request", waiverRequest,
                "--choice", "waive_verification",
                "--reason", "No build tool available in this environment",
                "--approver", "yangyang",
                "--risk-scope", "manual compile skipped for local harness test",
                "--rollback-plan", ".agents/verification/rollback.md"
        }, waiverAnswer.context());
        assertEquals(ExitCodes.SUCCESS, waiverAnswerExit);

        Harness waiverVerify = new Harness(tempDir);
        int waiverVerifyExit = new CommandRouter().run(new String[]{
                "goal", "verify", "--project-root", "demo-manual-waiver", "--goal", waiverGoal
        }, waiverVerify.context());
        assertEquals(ExitCodes.SUCCESS, waiverVerifyExit);
        assertTrue(waiverVerify.stdout().contains("manual-compile: waived"));
        assertTrue(waiverVerify.stdout().contains("verification waived with approval"));
    }

    @Test
    void legacyGraphProfileSurfacesProtectedImpactRiskAndCompletesWithEvidence() throws Exception {
        Path root = tempDir.resolve("demo-legacy-protected");
        writeSource(root, "src/main/java/com/example/App.java",
                "package com.example;\npublic class App { public void run() {} }\n");

        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", "demo-legacy-protected",
                "--profile", "legacy-java-small-fix-with-graph",
                "--task", "Legacy protected impact",
                "--module", "legacy"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = firstValue(start.stdout(), "goal_key: ");

        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "graph", "index", "--project-root", "demo-legacy-protected"
        }, new Harness(tempDir).context()));
        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "graph", "export", "--project-root", "demo-legacy-protected"
        }, new Harness(tempDir).context()));
        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "graph", "impact", "--project-root", "demo-legacy-protected",
                "--file", "src/main/java/com/example/App.java"
        }, new Harness(tempDir).context()));
        Files.write(PathUtil.graphImpactMap(root),
                "\n<related-files>\n- src/main/resources/application-prod.properties [protected_file]\n</related-files>\n"
                        .getBytes("UTF-8"), StandardOpenOption.APPEND);

        Harness export = new Harness(tempDir);
        int exportExit = new CommandRouter().run(new String[]{
                "goal", "export", "--project-root", "demo-legacy-protected", "--goal", goalKey
        }, export.context());
        assertEquals(ExitCodes.SUCCESS, exportExit);
        String context = new String(Files.readAllBytes(PathUtil.goalContext(root)), "UTF-8");
        assertTrue(context.contains("protected-impact-risk"));
        assertTrue(context.contains("src/main/resources/application-prod.properties"));

        Path rollback = root.resolve(".agents/memory/artifacts/goals").resolve(goalKey).resolve("ROLLBACK_PLAN.md");
        Path manual = root.resolve(".agents/memory/artifacts/goals").resolve(goalKey).resolve("MANUAL_EVIDENCE.md");
        Files.createDirectories(rollback.getParent());
        Files.write(rollback, "Rollback: revert App.java and redeploy previous package.\n".getBytes("UTF-8"));
        Files.write(manual, "Manual evidence: legacy path checked and passed.\n".getBytes("UTF-8"));

        recordLegacyGraphGoalSteps("demo-legacy-protected", goalKey,
                "src/main/java/com/example/App.java",
                ".agents/memory/artifacts/goals/" + goalKey + "/ROLLBACK_PLAN.md",
                "passed",
                ".agents/memory/artifacts/goals/" + goalKey + "/MANUAL_EVIDENCE.md",
                "approved");

        Harness verify = new Harness(tempDir);
        int verifyExit = new CommandRouter().run(new String[]{
                "goal", "verify", "--project-root", "demo-legacy-protected", "--goal", goalKey
        }, verify.context());
        assertEquals(ExitCodes.SUCCESS, verifyExit);
        assertTrue(verify.stdout().contains("decision: ready_to_complete"));

        Harness complete = new Harness(tempDir);
        int completeExit = new CommandRouter().run(new String[]{
                "goal", "complete", "--project-root", "demo-legacy-protected", "--goal", goalKey
        }, complete.context());
        assertEquals(ExitCodes.SUCCESS, completeExit);
        String summary = new String(Files.readAllBytes(PathUtil.goalSummary(root)), "UTF-8");
        assertTrue(summary.contains("legacy evidence passed"));
        assertTrue(summary.contains("protected_impact_files=[src/main/resources/application-prod.properties]"));
        assertTrue(summary.contains("ROLLBACK_PLAN.md"));
        assertTrue(summary.contains("MANUAL_EVIDENCE.md"));
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

    private void recordTwoStepGoal(String goalKey) {
        recordTwoStepGoal("demo", goalKey, "verification=done");
    }

    private void recordTwoStepGoalForProject(String projectRoot, String goalKey) {
        recordTwoStepGoal(projectRoot, goalKey, "verification=done");
    }

    private void recordTwoStepGoal(String goalKey, String verifyEvidence) {
        recordTwoStepGoal("demo", goalKey, verifyEvidence);
    }

    private void recordTwoStepGoal(String projectRoot, String goalKey, String verifyEvidence) {
        Harness inspectStep = new Harness(tempDir);
        int inspectStepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", projectRoot,
                "--goal", goalKey,
                "--summary", "Inspection complete",
                "--evidence", "inspection=done"
        }, inspectStep.context());
        assertEquals(ExitCodes.SUCCESS, inspectStepExit,
                "stdout=" + inspectStep.stdout() + "\nstderr=" + inspectStep.stderr());

        Harness verifyStep = new Harness(tempDir);
        int verifyStepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", projectRoot,
                "--goal", goalKey,
                "--summary", "Verification evidence complete",
                "--evidence", "compile_result=not required; test_result=not required; "
                        + "sensitive_result=not required; " + verifyEvidence
        }, verifyStep.context());
        assertEquals(ExitCodes.SUCCESS, verifyStepExit,
                "stdout=" + verifyStep.stdout() + "\nstderr=" + verifyStep.stderr());
    }

    private Path writeSource(Path root, String relativePath, String content) throws Exception {
        Path file = root.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes("UTF-8"));
        return file;
    }

    private Path copyFixture(String fixtureName, Path target) throws Exception {
        final Path source = Paths.get("testbeds/fixtures").resolve(fixtureName).toAbsolutePath().normalize();
        final Path destination = target.toAbsolutePath().normalize();
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws java.io.IOException {
                Files.createDirectories(destination.resolve(source.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws java.io.IOException {
                Files.copy(file, destination.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
        return destination;
    }

    private void writeGraphProfile(Path root, String profileKey, boolean requireImpactMap) throws Exception {
        Files.createDirectories(PathUtil.goalProfilesDirectory(root));
        String actions = requireImpactMap
                ? "graph_index_or_refresh,graph_impact_analysis,implement_minimal_change,verify"
                : "graph_index_or_refresh,implement_minimal_change,verify";
        String graphActions = requireImpactMap
                ? "graph_index_or_refresh,graph_impact_analysis"
                : "graph_index_or_refresh";
        Files.write(PathUtil.goalProfile(root, profileKey), ("{\n"
                + "  \"workflow_key\": \"api-change\",\n"
                + "  \"requires_spec\": \"false\",\n"
                + "  \"default_mode\": \"api\",\n"
                + "  \"actions\": \"" + actions + "\",\n"
                + "  \"required_checks\": \"graph\",\n"
                + "  \"completion_allow_skipped_checks\": \"false\",\n"
                + "  \"graph_required\": \"true\",\n"
                + "  \"graph_provider\": \"lite\",\n"
                + "  \"graph_require_fresh_snapshot\": \"true\",\n"
                + "  \"graph_require_impact_map\": \"" + requireImpactMap + "\",\n"
                + "  \"graph_max_staleness_minutes\": \"60\",\n"
                + "  \"graph_actions\": \"" + graphActions + "\"\n"
                + "}\n").getBytes("UTF-8"));
    }

    private void writeArchitectureProfile(Path root, String profileKey) throws Exception {
        Files.createDirectories(PathUtil.goalProfilesDirectory(root));
        Files.write(PathUtil.goalProfile(root, profileKey), ("{\n"
                + "  \"workflow_key\": \"api-change\",\n"
                + "  \"requires_spec\": \"false\",\n"
                + "  \"default_mode\": \"api\",\n"
                + "  \"actions\": \"inspect,verify\",\n"
                + "  \"required_checks\": \"architecture\",\n"
                + "  \"completion_allow_skipped_checks\": \"false\"\n"
                + "}\n").getBytes("UTF-8"));
    }

    private void writeModernArchitectureFixture(Path root, boolean failMode) throws Exception {
        writeSource(root, "src/main/java/com/example/account/controller/AccountController.java",
                "package com.example.account.controller;\n"
                        + "import com.example.account.repository.AccountRepository;\n"
                        + "import com.example.account.service.AccountService;\n"
                        + "public class AccountController {\n"
                        + "  private final AccountService accountService;\n"
                        + "  private final AccountRepository accountRepository;\n"
                        + "  public AccountController(AccountService accountService, AccountRepository accountRepository) {\n"
                        + "    this.accountService = accountService;\n"
                        + "    this.accountRepository = accountRepository;\n"
                        + "  }\n"
                        + "  public String get(String id) { return accountService.get(id); }\n"
                        + "}\n");
        writeSource(root, "src/main/java/com/example/account/service/AccountService.java",
                "package com.example.account.service;\n"
                        + "import com.example.account.repository.AccountRepository;\n"
                        + "public class AccountService {\n"
                        + "  private final AccountRepository accountRepository;\n"
                        + "  public AccountService(AccountRepository accountRepository) {\n"
                        + "    this.accountRepository = accountRepository;\n"
                        + "  }\n"
                        + "  public String get(String id) { return accountRepository.find(id); }\n"
                        + "}\n");
        writeSource(root, "src/main/java/com/example/account/repository/AccountRepository.java",
                "package com.example.account.repository;\n"
                        + "public interface AccountRepository { String find(String id); }\n");
        Files.createDirectories(PathUtil.graphDirectory(root));
        Files.write(PathUtil.graphArchitectureConfig(root), ("{\n"
                + "  \"schema_version\": \"devharness-graph-architecture/v1-alpha\",\n"
                + "  \"mode\": \"" + (failMode ? "fail" : "warn") + "\",\n"
                + "  \"controller_patterns\": [\"src/main/java/**/controller/**\"],\n"
                + "  \"service_patterns\": [\"src/main/java/**/service/**\"],\n"
                + "  \"repository_patterns\": [\"src/main/java/**/repository/**\"],\n"
                + "  \"public_api_patterns\": [\"src/main/java/**/controller/**\"],\n"
                + "  \"forbidden_dependencies\": [\"controller->repository\"]\n"
                + "}\n").getBytes("UTF-8"));
    }

    private void recordCustomGraphOnlyGoalSteps(String projectRoot, String goalKey, String changedFile) {
        Harness graphStep = new Harness(tempDir);
        int graphStepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", projectRoot,
                "--goal", goalKey,
                "--summary", "Graph snapshot exported",
                "--evidence", "graph_snapshot=GRAPH_SNAPSHOT.json; graph_context=GRAPH_CONTEXT.md"
        }, graphStep.context());
        assertEquals(ExitCodes.SUCCESS, graphStepExit);

        Harness implementStep = new Harness(tempDir);
        int implementStepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", projectRoot,
                "--goal", goalKey,
                "--summary", "Implementation changed graph indexed file",
                "--changed-files", changedFile,
                "--evidence", "implementation_summary=changed graph indexed file"
        }, implementStep.context());
        assertEquals(ExitCodes.SUCCESS, implementStepExit);

        Harness verifyStep = new Harness(tempDir);
        int verifyStepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", projectRoot,
                "--goal", goalKey,
                "--summary", "Verification evidence recorded",
                "--evidence", "compile_result=not required; test_result=not required; sensitive_result=not required"
        }, verifyStep.context());
        assertEquals(ExitCodes.SUCCESS, verifyStepExit);
    }

    private void recordCustomGraphGoalSteps(String projectRoot, String goalKey, String changedFile,
                                            boolean impactMapReady) {
        Harness graphStep = new Harness(tempDir);
        int graphStepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", projectRoot,
                "--goal", goalKey,
                "--summary", "Graph snapshot exported",
                "--evidence", "graph_snapshot=GRAPH_SNAPSHOT.json; graph_context=GRAPH_CONTEXT.md"
        }, graphStep.context());
        assertEquals(ExitCodes.SUCCESS, graphStepExit);

        Harness impactStep = new Harness(tempDir);
        int impactStepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", projectRoot,
                "--goal", goalKey,
                "--summary", "Graph impact analysis recorded",
                "--evidence", "impact_map=" + (impactMapReady ? "IMPACT_MAP.md" : "missing")
        }, impactStep.context());
        assertEquals(ExitCodes.SUCCESS, impactStepExit);

        Harness implementStep = new Harness(tempDir);
        int implementStepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", projectRoot,
                "--goal", goalKey,
                "--summary", "Implementation changed graph indexed file",
                "--changed-files", changedFile,
                "--evidence", "implementation_summary=changed graph indexed file"
        }, implementStep.context());
        assertEquals(ExitCodes.SUCCESS, implementStepExit);

        Harness verifyStep = new Harness(tempDir);
        int verifyStepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", projectRoot,
                "--goal", goalKey,
                "--summary", "Verification evidence recorded",
                "--evidence", "compile_result=not required; test_result=not required; sensitive_result=not required"
        }, verifyStep.context());
        assertEquals(ExitCodes.SUCCESS, verifyStepExit);
    }

    private void indexExportImpact(String projectRoot, String impactFile) {
        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "graph", "index", "--project-root", projectRoot
        }, new Harness(tempDir).context()));
        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "graph", "export", "--project-root", projectRoot
        }, new Harness(tempDir).context()));
        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "graph", "impact", "--project-root", projectRoot,
                "--file", impactFile, "--depth", "4"
        }, new Harness(tempDir).context()));
    }

    private void recordSafeRefactorGraphGoalSteps(String projectRoot, String goalKey,
                                                  boolean includeExpansionRisk) {
        step(projectRoot, goalKey, "Behavior boundary identified with graph context", "",
                "behavior_boundary=AccountService public behavior; preserved_behavior=account lookup semantics"
                        + "; related_tests=AccountServiceTest; graph_snapshot=GRAPH_SNAPSHOT.json"
                        + "; graph_context=GRAPH_CONTEXT.md; impact_map=IMPACT_MAP.md"
                        + "; recommended_read_files=src/main/java/com/acme/modern/account/service/AccountService.java");
        step(projectRoot, goalKey, "Refactor plan created", "",
                "refactor_plan=small service cleanup; rollback_plan=revert service change"
                        + "; risk_points=repository test gap remains visible"
                        + "; graph_risk_nodes=repository");
        step(projectRoot, goalKey, "Applied bounded refactor", "src/main/java/com/acme/modern/account/service/AccountService.java",
                "changed_files=src/main/java/com/acme/modern/account/service/AccountService.java"
                        + "; implementation_summary=small safe refactor; scope_guard=single service boundary");
        String risk = includeExpansionRisk
                ? "; impact_expansion_risk=reviewed expanded repository test gap" : "";
        step(projectRoot, goalKey, "Verification evidence and post-change impact recorded", "",
                "compile_result=pending; test_result=pending; sensitive_result=pending"
                        + "; graph_result=pending; impact_result=pending"
                        + "; post_change_impact_map=IMPACT_MAP.md; impact_delta=expanded: repository test gap surfaced"
                        + "; changed_files_covered=covered" + risk);
    }

    private void recordLegacyGraphGoalSteps(String projectRoot, String goalKey, String changedFile,
                                            String rollbackPlan, String manualStatus,
                                            String manualEvidencePath, String protectedConfirmation) {
        step(projectRoot, goalKey, "Inspected legacy entrypoints and graph context", "",
                "existing_entrypoints=App; existing_service=App; existing_data_access=none; existing_tests=manual"
                        + "; graph_snapshot=GRAPH_SNAPSHOT.json; graph_context=GRAPH_CONTEXT.md"
                        + "; impact_map=IMPACT_MAP.md; recommended_read_files=" + changedFile);
        step(projectRoot, goalKey, "Planned bounded legacy change", "",
                "impacted_files=" + changedFile
                        + "; risk_points=legacy behavior; verification_plan=manual evidence"
                        + "; rollback_strategy=revert changed file; graph_risk_nodes=route");
        step(projectRoot, goalKey, "Implemented minimal legacy change", changedFile,
                "changed_files=" + changedFile
                        + "; implementation_summary=small change; scope_guard=single impacted file");
        step(projectRoot, goalKey, "Rollback plan recorded", "",
                "rollback_plan=" + rollbackPlan + "; rollback_scope=single impacted file");
        String confirmation = protectedConfirmation == null || protectedConfirmation.length() == 0
                ? "" : "; protected_file_confirmation=" + protectedConfirmation;
        step(projectRoot, goalKey, "Manual legacy evidence recorded", "",
                "manual_evidence=legacy behavior checked; manual_evidence_status=" + manualStatus
                        + "; manual_evidence_path=" + manualEvidencePath + confirmation);
        step(projectRoot, goalKey, "Verification evidence and post-change impact recorded", "",
                "sensitive_result=pending; graph_result=pending; impact_result=pending; legacy_result=pending"
                        + "; post_change_impact_map=IMPACT_MAP.md"
                        + "; impact_delta=changed files remain inside related-files"
                        + "; changed_files_covered=" + changedFile);
    }

    private void step(String projectRoot, String goalKey, String summary, String changedFiles, String evidence) {
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
        Harness harness = new Harness(tempDir);
        int exit = new CommandRouter().run(args.toArray(new String[args.size()]), harness.context());
        assertEquals(ExitCodes.SUCCESS, exit);
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

    private void writeManualCompilePolicy(Path root, String compileCommand) throws Exception {
        Files.createDirectories(PathUtil.devharnessDirectory(root));
        Files.write(PathUtil.devharnessConfig(root), ("{\n"
                + "  \"schema_version\": \"devharness-config/v1-alpha\",\n"
                + "  \"verification.compile.mode\": \"manual\",\n"
                + "  \"verification.test.mode\": \"disabled\"\n"
                + "}\n").getBytes("UTF-8"));
        Files.write(PathUtil.goalCheckPolicy(root), ("{\n"
                + "  \"required_checks\": \"compile\",\n"
                + "  \"compile_command\": \"" + compileCommand + "\"\n"
                + "}\n").getBytes("UTF-8"));
    }

    private String startPatchGoal(String projectRoot, String task) {
        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", projectRoot,
                "--profile", "java-api-patch",
                "--task", task,
                "--module", "order"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        return firstValue(start.stdout(), "goal_key: ");
    }

    private String createManualCompileInteraction(String projectRoot, String goalKey) {
        Harness verify = new Harness(tempDir);
        int verifyExit = new CommandRouter().run(new String[]{
                "goal", "verify", "--project-root", projectRoot, "--goal", goalKey
        }, verify.context());
        assertEquals(ExitCodes.SUCCESS, verifyExit);
        assertTrue(verify.stdout().contains("manual-compile: failed"));
        return "interaction-" + goalKey + "-manual-compile";
    }

    private void closeSpec(Path root, String specChange) {
        String projectRoot = tempDir.relativize(root).toString();
        Harness taskAdd = new Harness(tempDir);
        int taskAddExit = new CommandRouter().run(new String[]{
                "spec", "task", "add",
                "--project-root", projectRoot,
                "--change", specChange,
                "--task", "T001",
                "--title", "Complete goal implementation",
                "--description", "Implementation task must be closed before completion",
                "--phase", "verify"
        }, taskAdd.context());
        assertEquals(ExitCodes.SUCCESS, taskAddExit);

        Harness taskDone = new Harness(tempDir);
        int taskDoneExit = new CommandRouter().run(new String[]{
                "spec", "task", "update",
                "--project-root", projectRoot,
                "--change", specChange,
                "--task", "T001",
                "--status", "done",
                "--evidence", "Goal integration test closed task"
        }, taskDone.context());
        assertEquals(ExitCodes.SUCCESS, taskDoneExit);

        Harness acceptanceAdd = new Harness(tempDir);
        int acceptanceAddExit = new CommandRouter().run(new String[]{
                "spec", "acceptance", "add",
                "--project-root", projectRoot,
                "--change", specChange,
                "--acceptance", "A001",
                "--description", "Goal can complete only after checks pass",
                "--expected", "Spec acceptance is passed"
        }, acceptanceAdd.context());
        assertEquals(ExitCodes.SUCCESS, acceptanceAddExit);

        Harness acceptancePassed = new Harness(tempDir);
        int acceptancePassedExit = new CommandRouter().run(new String[]{
                "spec", "acceptance", "update",
                "--project-root", projectRoot,
                "--change", specChange,
                "--acceptance", "A001",
                "--status", "passed",
                "--evidence", "Goal integration test passed acceptance"
        }, acceptancePassed.context());
        assertEquals(ExitCodes.SUCCESS, acceptancePassedExit);
    }

    private void passHardWorkflowGates(Path root, String workflowRun) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PathUtil.memoryDb(root));
             Statement statement = connection.createStatement()) {
            int updated = statement.executeUpdate("UPDATE workflow_gate_run "
                    + "SET status = 'passed', result_summary = 'Passed by goal integration test', "
                    + "evidence = 'integration-test', checked_at = '2026-01-01T00:00:00Z', "
                    + "updated_at = '2026-01-01T00:00:00Z' "
                    + "WHERE run_key = '" + workflowRun + "' AND severity = 'hard'");
            assertTrue(updated > 0);
        }
    }

    private void assertGoalRows(Path root, String goalKey) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PathUtil.memoryDb(root));
             Statement statement = connection.createStatement()) {
            assertEquals(1, count(statement, "SELECT COUNT(*) FROM goal_run WHERE goal_key = '" + goalKey + "'"));
            assertEquals(1, count(statement, "SELECT COUNT(*) FROM workflow_run"));
            assertEquals(1, count(statement, "SELECT COUNT(*) FROM spec_change"));
            assertEquals(1, count(statement, "SELECT COUNT(*) FROM workflow_spec_binding"));
            assertTrue(count(statement, "SELECT COUNT(*) FROM goal_event WHERE goal_key = '" + goalKey + "'") >= 2);
            assertEquals(1, count(statement, "SELECT COUNT(*) FROM goal_event WHERE goal_key = '" + goalKey
                    + "' AND event_type = 'goal_started'"));
            assertEquals(1, count(statement, "SELECT COUNT(*) FROM goal_event WHERE goal_key = '" + goalKey
                    + "' AND event_type = 'goal_context_exported'"));
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
            assertEquals(1, count(statement, "SELECT COUNT(*) FROM workflow_run WHERE status = 'completed'"));
            assertEquals(1, count(statement, "SELECT COUNT(*) FROM spec_change WHERE status = 'verified'"));
            assertTrue(count(statement, "SELECT COUNT(*) FROM goal_artifact WHERE goal_key = '" + goalKey + "'") >= 3);
            assertEquals(1, count(statement, "SELECT COUNT(*) FROM workflow_checkpoint_binding "
                    + "WHERE binding_type = 'created'"));
            assertEquals(1, count(statement, "SELECT COUNT(*) FROM workflow_artifact "
                    + "WHERE artifact_type = 'custom' AND produced_by_phase = 'goal_complete' "
                    + "AND title = 'GOAL_SUMMARY.md'"));
        }
    }

    private void assertGraphCompletionRows(Path root, String goalKey) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PathUtil.memoryDb(root));
             Statement statement = connection.createStatement()) {
            assertEquals(3, count(statement, "SELECT COUNT(*) FROM goal_graph_binding "
                    + "WHERE goal_key = '" + goalKey + "'"));
            assertEquals(1, count(statement, "SELECT COUNT(*) FROM goal_graph_binding "
                    + "WHERE goal_key = '" + goalKey + "' AND binding_type = 'used' "
                    + "AND artifact_path LIKE '%GRAPH_SNAPSHOT.json' AND impact_hash LIKE 'sha256:%'"));
            assertEquals(1, count(statement, "SELECT COUNT(*) FROM goal_graph_binding "
                    + "WHERE goal_key = '" + goalKey + "' AND binding_type = 'summary' "
                    + "AND artifact_path LIKE '%GRAPH_CONTEXT.md' AND impact_hash LIKE 'sha256:%'"));
            assertEquals(1, count(statement, "SELECT COUNT(*) FROM goal_graph_binding "
                    + "WHERE goal_key = '" + goalKey + "' AND binding_type = 'impact_map' "
                    + "AND artifact_path LIKE '%IMPACT_MAP.md' AND impact_hash LIKE 'sha256:%'"));
            assertEquals(1, count(statement, "SELECT COUNT(*) FROM goal_artifact "
                    + "WHERE goal_key = '" + goalKey + "' AND artifact_type = 'graph_snapshot'"));
            assertEquals(1, count(statement, "SELECT COUNT(*) FROM goal_artifact "
                    + "WHERE goal_key = '" + goalKey + "' AND artifact_type = 'graph_context'"));
            assertEquals(1, count(statement, "SELECT COUNT(*) FROM goal_artifact "
                    + "WHERE goal_key = '" + goalKey + "' AND artifact_type = 'graph_impact_map'"));
            assertEquals(1, count(statement, "SELECT COUNT(*) FROM goal_artifact "
                    + "WHERE goal_key = '" + goalKey + "' AND artifact_type = 'scenario_impact_map'"));
            assertEquals(4, count(statement, "SELECT COUNT(*) FROM workflow_artifact "
                    + "WHERE produced_by_phase = 'goal_complete' AND tags LIKE '%graph%'"));
            assertEquals(1, count(statement, "SELECT COUNT(*) FROM checkpoint "
                    + "WHERE verify_status LIKE 'goal checks accepted; graph evidence bound%'"));
        }
    }

    private int count(Statement statement, String sql) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private int countRows(Path root, String tableName) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PathUtil.memoryDb(root));
             Statement statement = connection.createStatement()) {
            return count(statement, "SELECT COUNT(*) FROM " + tableName);
        }
    }

    private String singleString(Path root, String sql) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PathUtil.memoryDb(root));
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getString(1);
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

    private String insertLineAfter(String text, String existingLine, String insertedLine) {
        String unixNeedle = existingLine + "\n";
        String windowsNeedle = existingLine + "\r\n";
        if (text.contains(windowsNeedle)) {
            return text.replace(windowsNeedle, windowsNeedle + insertedLine + "\r\n");
        }
        assertTrue(text.contains(unixNeedle), "Expected fixture line: " + existingLine);
        return text.replace(unixNeedle, unixNeedle + insertedLine + "\n");
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
