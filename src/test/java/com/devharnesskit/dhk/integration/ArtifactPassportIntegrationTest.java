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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ArtifactPassportIntegrationTest {
    @TempDir
    Path tempDir;

    @Test
    void goalCompleteExportsArtifactPassportJson() throws Exception {
        writeGoalCheckPolicy("demo-passport",
                "{\n"
                        + "  \"required_checks\": \"sensitive\"\n"
                        + "}\n");
        String goalKey = startGoal("demo-passport");
        recordStep("demo-passport", goalKey, "Inspected code", "",
                "goal_understanding=Generate a completion passport; assumptions=No schema migration; "
                        + "existing_controller=GoalCompleteCommand; existing_service=GoalOrchestrator; "
                        + "existing_mapper=GoalArtifactRepository; existing_tests=ArtifactPassportIntegrationTest");
        recordStep("demo-passport", goalKey, "Planned passport files", "",
                "impacted_files=GoalOrchestrator,ArtifactPassportRenderer; risk_points=sensitive output; "
                        + "scope_justification=Passport needs renderer and complete hook; "
                        + "verification_plan=mvn test");
        recordStep("demo-passport", goalKey, "Implemented passport",
                "src/main/java/com/devharnesskit/dhk/export/ArtifactPassportRenderer.java,"
                        + "src/main/java/com/devharnesskit/dhk/service/goal/GoalOrchestrator.java",
                "implementation_summary=Generated artifact passport on completion; "
                        + "rollback_plan=Revert passport renderer and complete hook");
        recordStep("demo-passport", goalKey, "Verified passport", "",
                "compile_result=passed; test_result=passed; sensitive_result=passed");

        Harness verify = new Harness(tempDir);
        int verifyExit = new CommandRouter().run(new String[]{
                "goal", "verify",
                "--project-root", "demo-passport",
                "--goal", goalKey
        }, verify.context());
        assertEquals(ExitCodes.SUCCESS, verifyExit);
        assertTrue(verify.stdout().contains("ready_to_complete: true"));

        Path root = tempDir.resolve("demo-passport");
        updateRows(root, "UPDATE goal_check SET check_fingerprint = '"
                + "check:138001380001380013800138001380001380013800138001380001380013800"
                + "' WHERE goal_key = '" + goalKey + "'");

        Harness complete = new Harness(tempDir);
        int completeExit = new CommandRouter().run(new String[]{
                "goal", "complete",
                "--project-root", "demo-passport",
                "--goal", goalKey
        }, complete.context());
        assertEquals(ExitCodes.SUCCESS, completeExit, complete.stdout() + complete.stderr());

        Path summaryPath = PathUtil.goalSummary(root);
        assertTrue(Files.isRegularFile(summaryPath));
        String summary = new String(Files.readAllBytes(summaryPath), "UTF-8");
        assertTrue(summary.contains("- artifact_passport: ARTIFACT_PASSPORT.json"));
        assertTrue(summary.contains("dhk artifact passport verify --path .agents/memory/exports/ARTIFACT_PASSPORT.json"));

        Path passportPath = PathUtil.artifactPassport(root);
        assertTrue(Files.isRegularFile(passportPath));
        String passport = new String(Files.readAllBytes(passportPath), "UTF-8");
        assertTrue(passport.contains("\"schema_version\": \"artifact-passport/v1-alpha\""));
        assertTrue(passport.contains("\"goal_key\": \"" + goalKey + "\""));
        assertTrue(passport.contains("\"checks\""));
        assertTrue(passport.contains("\"check_key\": \"sensitive\""));
        assertTrue(passport.contains("\"steps\""));
        assertTrue(passport.contains("\"rollback_plan\": \"Revert passport renderer and complete hook\""));
        assertTrue(passport.contains("\"goal_summary\""));
        assertTrue(passport.contains("\"artifact_passport\""));
        assertEquals(1, countRows(root, "goal_artifact",
                "goal_key = '" + goalKey + "' AND artifact_type = 'artifact_passport'"));

        Harness passportVerify = new Harness(tempDir);
        int passportVerifyExit = new CommandRouter().run(new String[]{
                "artifact", "passport", "verify",
                "--path", passportPath.toString()
        }, passportVerify.context());
        assertEquals(ExitCodes.SUCCESS, passportVerifyExit);
        assertTrue(passportVerify.stdout().contains("artifact passport verify complete"));
        assertTrue(passportVerify.stdout().contains("status: passed"));
        assertTrue(passportVerify.stdout().contains("missing:"));
        assertTrue(passportVerify.stdout().contains("  - none"));
    }

    @Test
    void artifactPassportVerifyFailsMissingGraphBddAndCheckEvidence() throws Exception {
        Path passport = tempDir.resolve("bad-passport.json");
        Files.write(passport, ("{\n"
                + "  \"schema_version\": \"artifact-passport/v1-alpha\",\n"
                + "  \"generated_at\": \"2026-01-01T00:00:00Z\",\n"
                + "  \"goal\": {},\n"
                + "  \"completion\": {},\n"
                + "  \"checks\": [{\"check_key\": \"bdd\", \"status\": \"failed\", \"summary\": \"stale bdd evidence\"}],\n"
                + "  \"steps\": [],\n"
                + "  \"evidence\": {},\n"
                + "  \"graph\": {\"enabled\": true, \"graph_snapshot\": \"\", \"graph_context\": \"\", \"graph_snapshot_hash\": \"\"},\n"
                + "  \"bdd\": {\"enabled\": true, \"bdd_evidence\": \"\", \"bdd_coverage\": \"\"},\n"
                + "  \"rollback\": {},\n"
                + "  \"artifacts\": []\n"
                + "}\n").getBytes("UTF-8"));

        Harness verify = new Harness(tempDir);
        int exit = new CommandRouter().run(new String[]{
                "artifact", "passport", "verify",
                "--path", passport.toString()
        }, verify.context());

        assertEquals(ExitCodes.VALIDATION_ERROR, exit);
        assertTrue(verify.stdout().contains("status: failed"));
        assertTrue(verify.stdout().contains("check bdd status is failed"));
        assertTrue(verify.stdout().contains("check bdd missing check_fingerprint"));
        assertTrue(verify.stdout().contains("graph evidence missing: graph_snapshot"));
        assertTrue(verify.stdout().contains("bdd evidence missing: bdd_evidence"));
        assertTrue(verify.stdout().contains("check bdd summary indicates stale evidence"));
    }

    @Test
    void goalCompleteFailsWhenIntegrityPreGateFindsMissingCheckFingerprint() throws Exception {
        writeGoalCheckPolicy("demo-integrity",
                "{\n"
                        + "  \"required_checks\": \"sensitive\"\n"
                        + "}\n");
        String goalKey = startGoal("demo-integrity");
        recordStep("demo-integrity", goalKey, "Inspected code", "",
                "goal_understanding=Generate a completion passport; assumptions=No schema migration; "
                        + "existing_controller=GoalCompleteCommand; existing_service=GoalOrchestrator; "
                        + "existing_mapper=GoalArtifactRepository; existing_tests=ArtifactPassportIntegrationTest");
        recordStep("demo-integrity", goalKey, "Planned passport files", "",
                "impacted_files=GoalOrchestrator,ArtifactPassportRenderer; risk_points=sensitive output; "
                        + "scope_justification=Passport needs renderer and complete hook; "
                        + "verification_plan=mvn test");
        recordStep("demo-integrity", goalKey, "Implemented passport",
                "src/main/java/com/devharnesskit/dhk/export/ArtifactPassportRenderer.java",
                "implementation_summary=Generated artifact passport on completion");
        recordStep("demo-integrity", goalKey, "Verified passport", "",
                "compile_result=passed; test_result=passed; sensitive_result=passed");

        Harness verify = new Harness(tempDir);
        int verifyExit = new CommandRouter().run(new String[]{
                "goal", "verify",
                "--project-root", "demo-integrity",
                "--goal", goalKey
        }, verify.context());
        assertEquals(ExitCodes.SUCCESS, verifyExit);
        assertTrue(verify.stdout().contains("ready_to_complete: true"));

        Path root = tempDir.resolve("demo-integrity");
        updateRows(root, "UPDATE goal_check SET check_fingerprint = '' WHERE goal_key = '"
                + goalKey + "' AND check_key = 'sensitive'");

        Harness complete = new Harness(tempDir);
        int completeExit = new CommandRouter().run(new String[]{
                "goal", "complete",
                "--project-root", "demo-integrity",
                "--goal", goalKey
        }, complete.context());

        assertEquals(ExitCodes.VALIDATION_ERROR, completeExit);
        assertTrue(complete.stdout().contains("decision: not_ready"));
        assertTrue(complete.stdout().contains("integrity pre gate failed"));
        assertTrue(complete.stdout().contains("check sensitive missing check_fingerprint"));
    }

    private String startGoal(String projectRoot) throws Exception {
        Harness start = new Harness(tempDir);
        int exit = new CommandRouter().run(new String[]{
                "goal", "start",
                "--project-root", projectRoot,
                "--profile", "java-api-change",
                "--task", "Generate artifact passport",
                "--module", "artifact",
                "--mode", "api",
                "--condition", "sensitive check passes"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, exit);
        return firstValue(start.stdout(), "goal_key: ");
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

    private void writeGoalCheckPolicy(String projectRoot, String content) throws Exception {
        Path policy = tempDir.resolve(projectRoot).resolve(".agents/devharness/goal-check-policy.json");
        Files.createDirectories(policy.getParent());
        Files.write(policy, content.getBytes("UTF-8"));
    }

    private int countRows(Path projectRoot, String tableName, String where) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:sqlite:" + PathUtil.memoryDb(projectRoot).toString());
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT COUNT(*) FROM " + tableName + " WHERE " + where)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private void updateRows(Path projectRoot, String sql) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:sqlite:" + PathUtil.memoryDb(projectRoot).toString());
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
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

        private String stderr() throws Exception {
            return stderr.toString("UTF-8");
        }
    }
}
