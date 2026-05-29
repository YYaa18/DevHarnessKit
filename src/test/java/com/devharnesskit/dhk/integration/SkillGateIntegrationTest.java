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
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SkillGateIntegrationTest {
    @TempDir
    Path tempDir;

    @Test
    void thinkBeforeCodingGateFailsWhenGoalUnderstandingEvidenceIsMissing() throws Exception {
        String goalKey = startGoal("demo-missing");
        recordInspectStep("demo-missing", goalKey,
                "existing_controller=OrderController; existing_service=OrderService; "
                        + "existing_mapper=OrderMapper; existing_tests=OrderServiceTest");

        Harness gate = new Harness(tempDir);
        int exit = new CommandRouter().run(new String[]{
                "skill", "gate", "think-before-coding",
                "--project-root", "demo-missing",
                "--goal", goalKey
        }, gate.context());

        assertEquals(ExitCodes.VALIDATION_ERROR, exit);
        assertTrue(gate.stdout().contains("skill gate complete"));
        assertTrue(gate.stdout().contains("gate: think-before-coding"));
        assertTrue(gate.stdout().contains("status: failed"));
        assertTrue(gate.stdout().contains("goal understanding evidence missing"));
    }

    @Test
    void thinkBeforeCodingAndGoalDrivenGatesPassWithStructuredEvidence() throws Exception {
        String goalKey = startGoal("demo-pass");
        recordInspectStep("demo-pass", goalKey,
                "goal_understanding=Implement a focused order lookup change; "
                        + "assumptions=No schema or DB changes; "
                        + "existing_controller=OrderController; existing_service=OrderService; "
                        + "existing_mapper=OrderMapper; existing_tests=OrderServiceTest");

        Harness think = new Harness(tempDir);
        int thinkExit = new CommandRouter().run(new String[]{
                "skill", "gate", "think-before-coding",
                "--project-root", "demo-pass",
                "--goal", goalKey
        }, think.context());

        assertEquals(ExitCodes.SUCCESS, thinkExit);
        assertTrue(think.stdout().contains("status: passed"));

        Harness goalDriven = new Harness(tempDir);
        int goalDrivenExit = new CommandRouter().run(new String[]{
                "skill", "gate", "goal-driven",
                "--project-root", "demo-pass",
                "--goal", goalKey
        }, goalDriven.context());

        assertEquals(ExitCodes.SUCCESS, goalDrivenExit);
        assertTrue(goalDriven.stdout().contains("gate: goal-driven"));
        assertTrue(goalDriven.stdout().contains("status: passed"));
    }

    @Test
    void thinkBeforeCodingGateAcceptsBugfixPreCodingEvidence() throws Exception {
        String goalKey = startGoal("demo-bugfix", "bugfix");
        recordStep("demo-bugfix", goalKey, "Collected release hardening symptoms", "",
                "goal_understanding=Fix release version drift; assumptions=No product behavior change; "
                        + "failing_symptom=release workflow hardcodes artifact version; "
                        + "reproduction_or_log=rg finds hardcoded version");
        recordStep("demo-bugfix", goalKey, "Listed release hardening hypothesis", "",
                "root_cause_hypothesis=release scripts do not read project.version; "
                        + "supporting_evidence=pom uses project.version for artifact names");
        recordStep("demo-bugfix", goalKey, "Implemented release hardening fix",
                "RELEASE.md,scripts/release-gate.sh",
                "fix_summary=Use Maven project.version and release gate");

        Harness think = new Harness(tempDir);
        int thinkExit = new CommandRouter().run(new String[]{
                "skill", "gate", "think-before-coding",
                "--project-root", "demo-bugfix",
                "--goal", goalKey
        }, think.context());

        assertEquals(ExitCodes.SUCCESS, thinkExit);
        assertTrue(think.stdout().contains("status: passed"));
    }

    @Test
    void goalDrivenGateAllowsAdditionalVerifyEvidenceStep() throws Exception {
        String goalKey = startGoal("demo-repeat-action");
        recordInspectStep("demo-repeat-action", goalKey,
                "goal_understanding=Implement a focused order lookup change; "
                        + "assumptions=No schema or DB changes; "
                        + "existing_controller=OrderController; existing_service=OrderService; "
                        + "existing_mapper=OrderMapper; existing_tests=OrderServiceTest");
        recordStep("demo-repeat-action", goalKey, "Planned impacted files", "",
                "impacted_files=OrderController; risk_points=none; verification_plan=mvn test");
        recordStep("demo-repeat-action", goalKey, "Implemented change",
                "src/main/java/OrderController.java",
                "implementation_summary=Changed controller");
        recordStep("demo-repeat-action", goalKey, "Verified change", "",
                "compile_result=passed; test_result=passed; sensitive_result=passed");
        recordStep("demo-repeat-action", goalKey, "Added verify evidence after gate feedback", "",
                "compile_result=passed; test_result=passed; sensitive_result=passed; "
                        + "simplicity_justification=No extra abstraction introduced");

        Harness goalDriven = new Harness(tempDir);
        int goalDrivenExit = new CommandRouter().run(new String[]{
                "skill", "gate", "goal-driven",
                "--project-root", "demo-repeat-action",
                "--goal", goalKey
        }, goalDriven.context());

        assertEquals(ExitCodes.SUCCESS, goalDrivenExit);
        assertTrue(goalDriven.stdout().contains("gate: goal-driven"));
        assertTrue(goalDriven.stdout().contains("status: passed"));
    }

    @Test
    void simplicityGateFailsBroadChangeWithoutScopeEvidence() throws Exception {
        String goalKey = startGoal("demo-broad");
        recordInspectStep("demo-broad", goalKey,
                "goal_understanding=Keep the order change focused; assumptions=No platform migration; "
                        + "existing_controller=OrderController; existing_service=OrderService; "
                        + "existing_mapper=OrderMapper; existing_tests=OrderServiceTest");
        recordStep("demo-broad", goalKey, "Planned impacted files", "",
                "impacted_files=OrderController,OrderService; risk_points=scope drift; verification_plan=mvn test");
        recordStep("demo-broad", goalKey, "Implemented broad change",
                "src/main/java/OrderController.java,src/main/java/OrderService.java,"
                        + "src/main/java/OrderMapper.java,src/main/java/OrderDto.java,"
                        + "src/main/java/OrderFactory.java,src/test/java/OrderServiceTest.java,"
                        + "src/test/java/OrderControllerTest.java",
                "implementation_summary=Changed many files without explaining scope");

        Harness gate = new Harness(tempDir);
        int exit = new CommandRouter().run(new String[]{
                "skill", "gate", "simplicity",
                "--project-root", "demo-broad",
                "--goal", goalKey
        }, gate.context());

        assertEquals(ExitCodes.VALIDATION_ERROR, exit);
        assertTrue(gate.stdout().contains("gate: simplicity"));
        assertTrue(gate.stdout().contains("status: failed"));
        assertTrue(gate.stdout().contains("large change set lacks scope_justification"));
        assertTrue(gate.stdout().contains("abstraction risk lacks simplicity_justification"));
    }

    @Test
    void surgicalChangeGateFailsProtectedFileWithoutManualEvidence() throws Exception {
        String goalKey = startGoal("demo-protected");
        recordInspectStep("demo-protected", goalKey,
                "goal_understanding=Keep config untouched unless approved; assumptions=No deployment change; "
                        + "existing_controller=OrderController; existing_service=OrderService; "
                        + "existing_mapper=OrderMapper; existing_tests=OrderServiceTest");
        recordStep("demo-protected", goalKey, "Planned impacted files", "",
                "impacted_files=src/main/resources/application-prod.yml; risk_points=protected config; verification_plan=mvn test");
        recordStep("demo-protected", goalKey, "Changed protected config",
                "src/main/resources/application-prod.yml",
                "implementation_summary=Changed protected config without manual confirmation");
        writePolicy("demo-protected",
                "{\n"
                        + "  \"protected_files\": \"src/main/resources/application-prod.yml\"\n"
                        + "}\n");

        Harness gate = new Harness(tempDir);
        int exit = new CommandRouter().run(new String[]{
                "skill", "gate", "surgical-change",
                "--project-root", "demo-protected",
                "--goal", goalKey
        }, gate.context());

        assertEquals(ExitCodes.VALIDATION_ERROR, exit);
        assertTrue(gate.stdout().contains("gate: surgical-change"));
        assertTrue(gate.stdout().contains("status: failed"));
        assertTrue(gate.stdout().contains("protected files changed without manual evidence"));
    }

    @Test
    void requiredDisciplineGateFailureBlocksGoalVerifyAndComplete() throws Exception {
        writeGoalCheckPolicy("demo-strict",
                "{\n"
                        + "  \"required_checks\": \"think-before-coding\"\n"
                        + "}\n");
        String goalKey = startGoal("demo-strict");
        recordInspectStep("demo-strict", goalKey,
                "existing_controller=OrderController; existing_service=OrderService; "
                        + "existing_mapper=OrderMapper; existing_tests=OrderServiceTest");
        recordStep("demo-strict", goalKey, "Planned impacted files", "",
                "impacted_files=OrderController; risk_points=missing goal understanding; verification_plan=mvn test");
        recordStep("demo-strict", goalKey, "Implemented change", "src/main/java/OrderController.java",
                "implementation_summary=Implemented without goal understanding evidence");
        recordStep("demo-strict", goalKey, "Verified change", "",
                "compile_result=passed; test_result=passed; sensitive_result=passed");

        Harness verify = new Harness(tempDir);
        int verifyExit = new CommandRouter().run(new String[]{
                "goal", "verify",
                "--project-root", "demo-strict",
                "--goal", goalKey
        }, verify.context());

        assertEquals(ExitCodes.SUCCESS, verifyExit);
        assertTrue(verify.stdout().contains("ready_to_complete: false"));
        assertTrue(verify.stdout().contains("think-before-coding: failed"));
        assertTrue(verify.stdout().contains("check think-before-coding is failed"));

        Harness export = new Harness(tempDir);
        int exportExit = new CommandRouter().run(new String[]{
                "goal", "export",
                "--project-root", "demo-strict",
                "--goal", goalKey
        }, export.context());
        assertEquals(ExitCodes.SUCCESS, exportExit);
        String context = new String(Files.readAllBytes(PathUtil.goalContext(tempDir.resolve("demo-strict"))),
                "UTF-8");
        assertTrue(context.contains("<discipline-gates>"));
        assertTrue(context.contains("- think-before-coding: failed"));

        Harness complete = new Harness(tempDir);
        int completeExit = new CommandRouter().run(new String[]{
                "goal", "complete",
                "--project-root", "demo-strict",
                "--goal", goalKey
        }, complete.context());

        assertEquals(ExitCodes.VALIDATION_ERROR, completeExit);
        assertTrue(complete.stdout().contains("decision: not_ready"));
        assertTrue(complete.stdout().contains("check think-before-coding is failed"));
    }

    private String startGoal(String projectRoot) throws Exception {
        return startGoal(projectRoot, "java-api-change");
    }

    private String startGoal(String projectRoot, String profile) throws Exception {
        Harness start = new Harness(tempDir);
        int exit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", projectRoot,
                "--profile", profile,
                "--task", "Implement order query API",
                "--module", "order",
                "--mode", "api",
                "--condition", "compile and focused tests pass"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, exit);
        return firstValue(start.stdout(), "goal_key: ");
    }

    private void recordInspectStep(String projectRoot, String goalKey, String evidence) throws Exception {
        recordStep(projectRoot, goalKey, "Inspected existing code before editing", "", evidence);
    }

    private void recordStep(String projectRoot, String goalKey, String summary,
                            String changedFiles, String evidence) throws Exception {
        Harness step = new Harness(tempDir);
        int exit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", projectRoot,
                "--goal", goalKey,
                "--summary", summary,
                "--changed-files", changedFiles,
                "--evidence", evidence
        }, step.context());
        assertEquals(ExitCodes.SUCCESS, exit);
    }

    private void writePolicy(String projectRoot, String content) throws Exception {
        Path policy = tempDir.resolve(projectRoot).resolve(".agents/devharness/policy.json");
        Files.createDirectories(policy.getParent());
        Files.write(policy, content.getBytes("UTF-8"));
    }

    private void writeGoalCheckPolicy(String projectRoot, String content) throws Exception {
        Path policy = tempDir.resolve(projectRoot).resolve(".agents/devharness/goal-check-policy.json");
        Files.createDirectories(policy.getParent());
        Files.write(policy, content.getBytes("UTF-8"));
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
        private final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        private final ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        private Harness(Path workingDirectory) {
            this.workingDirectory = workingDirectory;
        }

        private CommandContext context() {
            return new CommandContext(workingDirectory, com.devharnesskit.dhk.testsupport.Utf8HarnessSupport.printStream(stdout), com.devharnesskit.dhk.testsupport.Utf8HarnessSupport.printStream(stderr),
                    new Clock() {
                        public Instant now() {
                            return Instant.parse("2026-01-01T00:00:00Z");
                        }
                    });
        }

        private String stdout() throws Exception {
            return stdout.toString("UTF-8");
        }
    }
}
