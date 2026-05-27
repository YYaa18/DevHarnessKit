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

final class HumanCheckpointIntegrationTest {
    @TempDir
    Path tempDir;

    @Test
    void checkpointRequestApproveAndListPersistAuditFields() throws Exception {
        String goalKey = startGoal("demo-checkpoint");

        Harness request = new Harness(tempDir);
        int requestExit = new CommandRouter().run(new String[]{
                "checkpoint", "request",
                "--project-root", "demo-checkpoint",
                "--goal", goalKey,
                "--type", "before_complete",
                "--reason", "High risk release needs review",
                "--requested-by", "codex"
        }, request.context());
        assertEquals(ExitCodes.SUCCESS, requestExit, request.stdout() + request.stderr());
        assertTrue(request.stdout().contains("human checkpoint requested"));
        assertTrue(request.stdout().contains("status: pending"));
        String id = firstValue(request.stdout(), "id: ");

        Harness approve = new Harness(tempDir);
        int approveExit = new CommandRouter().run(new String[]{
                "checkpoint", "approve",
                "--project-root", "demo-checkpoint",
                "--goal", goalKey,
                "--id", id,
                "--approver", "yangyang",
                "--reason", "Reviewed risk and approved"
        }, approve.context());
        assertEquals(ExitCodes.SUCCESS, approveExit, approve.stdout() + approve.stderr());
        assertTrue(approve.stdout().contains("human checkpoint approved"));
        assertTrue(approve.stdout().contains("status: approved"));
        assertTrue(approve.stdout().contains("approver: yangyang"));

        Harness list = new Harness(tempDir);
        int listExit = new CommandRouter().run(new String[]{
                "checkpoint", "list",
                "--project-root", "demo-checkpoint",
                "--goal", goalKey
        }, list.context());
        assertEquals(ExitCodes.SUCCESS, listExit, list.stdout() + list.stderr());
        assertTrue(list.stdout().contains("human checkpoints:"));
        assertTrue(list.stdout().contains("status: approved"));
        assertTrue(list.stdout().contains("decision_reason: Reviewed risk and approved"));
    }

    @Test
    void humanCheckpointPolicyBlocksCompleteUntilApproved() throws Exception {
        writePolicy("demo-required",
                "{\n"
                        + "  \"human_checkpoint_required\": \"true\",\n"
                        + "  \"human_checkpoint_type\": \"before_complete\"\n"
                        + "}\n");
        writeGoalCheckPolicy("demo-required",
                "{\n"
                        + "  \"required_checks\": \"sensitive\"\n"
                        + "}\n");
        String goalKey = startGoal("demo-required");
        recordCompletionSteps("demo-required", goalKey);

        Harness verifyBlocked = new Harness(tempDir);
        int verifyBlockedExit = new CommandRouter().run(new String[]{
                "goal", "verify",
                "--project-root", "demo-required",
                "--goal", goalKey
        }, verifyBlocked.context());
        assertEquals(ExitCodes.SUCCESS, verifyBlockedExit, verifyBlocked.stdout() + verifyBlocked.stderr());
        assertTrue(verifyBlocked.stdout().contains("ready_to_complete: false"));
        assertTrue(verifyBlocked.stdout().contains("human_checkpoint_required: type=before_complete status=missing_approved"));

        Harness export = new Harness(tempDir);
        int exportExit = new CommandRouter().run(new String[]{
                "goal", "export",
                "--project-root", "demo-required",
                "--goal", goalKey
        }, export.context());
        assertEquals(ExitCodes.SUCCESS, exportExit, export.stdout() + export.stderr());
        String context = new String(Files.readAllBytes(PathUtil.goalContext(tempDir.resolve("demo-required"))),
                "UTF-8");
        assertTrue(context.contains("<required-checkpoints>"));
        assertTrue(context.contains("- human_checkpoint_required: true"));
        assertTrue(context.contains("- status: missing_approved"));
        assertTrue(context.contains("dhk checkpoint request --goal " + goalKey));

        Harness completeBlocked = new Harness(tempDir);
        int completeBlockedExit = new CommandRouter().run(new String[]{
                "goal", "complete",
                "--project-root", "demo-required",
                "--goal", goalKey
        }, completeBlocked.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, completeBlockedExit,
                completeBlocked.stdout() + completeBlocked.stderr());
        assertTrue(completeBlocked.stdout().contains("human_checkpoint_required: type=before_complete status=missing_approved"));

        Harness request = new Harness(tempDir);
        int requestExit = new CommandRouter().run(new String[]{
                "checkpoint", "request",
                "--project-root", "demo-required",
                "--goal", goalKey,
                "--reason", "Manual review required before completion"
        }, request.context());
        assertEquals(ExitCodes.SUCCESS, requestExit, request.stdout() + request.stderr());
        String id = firstValue(request.stdout(), "id: ");

        Harness approve = new Harness(tempDir);
        int approveExit = new CommandRouter().run(new String[]{
                "checkpoint", "approve",
                "--project-root", "demo-required",
                "--goal", goalKey,
                "--id", id,
                "--approver", "yangyang",
                "--reason", "Approved high-risk completion"
        }, approve.context());
        assertEquals(ExitCodes.SUCCESS, approveExit, approve.stdout() + approve.stderr());

        Harness verifyReady = new Harness(tempDir);
        int verifyReadyExit = new CommandRouter().run(new String[]{
                "goal", "verify",
                "--project-root", "demo-required",
                "--goal", goalKey
        }, verifyReady.context());
        assertEquals(ExitCodes.SUCCESS, verifyReadyExit, verifyReady.stdout() + verifyReady.stderr());
        assertTrue(verifyReady.stdout().contains("ready_to_complete: true"));

        Harness complete = new Harness(tempDir);
        int completeExit = new CommandRouter().run(new String[]{
                "goal", "complete",
                "--project-root", "demo-required",
                "--goal", goalKey
        }, complete.context());
        assertEquals(ExitCodes.SUCCESS, completeExit, complete.stdout() + complete.stderr());
        assertTrue(complete.stdout().contains("status: completed"));
    }

    @Test
    void skillTrustPolicyBlocksHighRiskUntrustedSkillUntilApproved() throws Exception {
        writePolicy("demo-skill-trust",
                "{\n"
                        + "  \"skill_contract_required\": \"true\",\n"
                        + "  \"skill_key\": \"devharness-risky\",\n"
                        + "  \"skill_trust_required_for_high_risk\": \"true\",\n"
                        + "  \"skill_trust_override_checkpoint_type\": \"skill_trust_override\"\n"
                        + "}\n");
        writeGoalCheckPolicy("demo-skill-trust",
                "{\n"
                        + "  \"required_checks\": \"sensitive\"\n"
                        + "}\n");
        writeHighRiskSkill("demo-skill-trust", "devharness-risky");
        Harness verifySkill = new Harness(tempDir);
        int verifySkillExit = new CommandRouter().run(new String[]{
                "skill", "verify",
                "--project-root", "demo-skill-trust",
                "--skill", "devharness-risky"
        }, verifySkill.context());
        assertEquals(ExitCodes.SUCCESS, verifySkillExit, verifySkill.stdout() + verifySkill.stderr());
        assertTrue(verifySkill.stdout().contains("trust_status: unknown"));

        String goalKey = startGoal("demo-skill-trust");
        recordCompletionSteps("demo-skill-trust", goalKey);

        Harness verifyBlocked = new Harness(tempDir);
        int verifyBlockedExit = new CommandRouter().run(new String[]{
                "goal", "verify",
                "--project-root", "demo-skill-trust",
                "--goal", goalKey
        }, verifyBlocked.context());
        assertEquals(ExitCodes.SUCCESS, verifyBlockedExit, verifyBlocked.stdout() + verifyBlocked.stderr());
        assertTrue(verifyBlocked.stdout().contains("ready_to_complete: false"));
        assertTrue(verifyBlocked.stdout().contains("skill_trust_required: skill=devharness-risky"));
        assertTrue(verifyBlocked.stdout().contains("trust_status=unknown"));
        assertTrue(verifyBlocked.stdout().contains("status=missing_approved"));
        assertTrue(verifyBlocked.stdout().contains("dhk checkpoint request --goal " + goalKey
                + " --type skill_trust_override"));

        Harness request = new Harness(tempDir);
        int requestExit = new CommandRouter().run(new String[]{
                "checkpoint", "request",
                "--project-root", "demo-skill-trust",
                "--goal", goalKey,
                "--type", "skill_trust_override",
                "--reason", "Manual approval for untrusted high-risk skill"
        }, request.context());
        assertEquals(ExitCodes.SUCCESS, requestExit, request.stdout() + request.stderr());
        String id = firstValue(request.stdout(), "id: ");

        Harness approve = new Harness(tempDir);
        int approveExit = new CommandRouter().run(new String[]{
                "checkpoint", "approve",
                "--project-root", "demo-skill-trust",
                "--goal", goalKey,
                "--id", id,
                "--approver", "yangyang",
                "--reason", "Reviewed high-risk skill source manually"
        }, approve.context());
        assertEquals(ExitCodes.SUCCESS, approveExit, approve.stdout() + approve.stderr());

        Harness verifyReady = new Harness(tempDir);
        int verifyReadyExit = new CommandRouter().run(new String[]{
                "goal", "verify",
                "--project-root", "demo-skill-trust",
                "--goal", goalKey
        }, verifyReady.context());
        assertEquals(ExitCodes.SUCCESS, verifyReadyExit, verifyReady.stdout() + verifyReady.stderr());
        assertTrue(verifyReady.stdout().contains("ready_to_complete: true"));
    }

    private String startGoal(String projectRoot) throws Exception {
        Harness start = new Harness(tempDir);
        int exit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", projectRoot,
                "--profile", "java-api-change",
                "--task", "Implement high risk change",
                "--module", "order",
                "--mode", "api",
                "--condition", "manual approval when required"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, exit, start.stdout() + start.stderr());
        return firstValue(start.stdout(), "goal_key: ");
    }

    private void recordCompletionSteps(String projectRoot, String goalKey) throws Exception {
        recordStep(projectRoot, goalKey, "Inspected code", "",
                "goal_understanding=Implement a high risk change; assumptions=Manual approval can be requested; "
                        + "existing_controller=OrderController; existing_service=OrderService; "
                        + "existing_mapper=OrderMapper; existing_tests=OrderServiceTest");
        recordStep(projectRoot, goalKey, "Planned change", "",
                "impacted_files=OrderController; risk_points=high risk completion; "
                        + "scope_justification=Need checkpoint gate test; verification_plan=mvn test");
        recordStep(projectRoot, goalKey, "Implemented change",
                "src/main/java/OrderController.java",
                "implementation_summary=Recorded high risk change; rollback_plan=Revert OrderController");
        recordStep(projectRoot, goalKey, "Verified change", "",
                "compile_result=passed; test_result=passed; sensitive_result=passed");
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
        assertEquals(ExitCodes.SUCCESS, exit, step.stdout() + step.stderr());
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

    private void writeHighRiskSkill(String projectRoot, String skillKey) throws Exception {
        Path skill = tempDir.resolve(projectRoot).resolve(".agents/skills").resolve(skillKey);
        Files.createDirectories(skill);
        Files.write(skill.resolve("LICENSE"), "MIT\n".getBytes("UTF-8"));
        Files.write(skill.resolve("contract.json"), ("{\n"
                + "  \"schema_version\": \"skill-contract/v1-alpha\",\n"
                + "  \"skill_key\": \"" + skillKey + "\",\n"
                + "  \"version\": \"0.7.5\",\n"
                + "  \"task_type\": \"coding\",\n"
                + "  \"risk_level\": \"high\",\n"
                + "  \"mode\": \"strict\",\n"
                + "  \"data_access_level\": \"context\",\n"
                + "  \"allowed_commands\": [\"dhk goal next\", \"dhk goal step\", \"dhk goal verify\"],\n"
                + "  \"forbidden_commands\": [\"dhk db sql\", \"dhk memory confirm\"],\n"
                + "  \"declared_commands\": [\"dhk goal next\", \"dhk goal step\", \"dhk goal verify\"]\n"
                + "}\n").getBytes("UTF-8"));
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
            return new CommandContext(workingDirectory, new PrintStream(stdout), new PrintStream(stderr),
                    new Clock() {
                        public Instant now() {
                            return Instant.parse("2026-01-01T00:00:00Z");
                        }
                    });
        }

        private String stdout() throws Exception {
            return stdout.toString("UTF-8");
        }

        private String stderr() throws Exception {
            return stderr.toString("UTF-8");
        }
    }
}
