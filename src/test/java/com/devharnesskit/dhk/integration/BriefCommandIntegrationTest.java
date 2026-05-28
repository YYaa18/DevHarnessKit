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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BriefCommandIntegrationTest {
    @TempDir
    Path tempDir;

    @Test
    void adviseWritesUserWorkBriefAndAgentExecutionBrief() throws Exception {
        Harness harness = new Harness(tempDir);
        int exit = new CommandRouter().run(new String[]{
                "advise",
                "--project-root", "brief-project",
                "--task", "Fix order query response mapping",
                "--module", "order",
                "--mode", "patch",
                "--json"
        }, harness.context());

        Path root = tempDir.resolve("brief-project");
        assertEquals(ExitCodes.SUCCESS, exit);
        assertTrue(harness.stdout().contains("\"brief\": \"ready\""));
        assertTrue(Files.isRegularFile(PathUtil.workBrief(root)));
        assertTrue(Files.isRegularFile(PathUtil.agentBrief(root)));

        String workBrief = read(PathUtil.workBrief(root));
        String agentBrief = read(PathUtil.agentBrief(root));
        assertTrue(workBrief.contains("recommendation: patch"));
        assertFalse(workBrief.contains("dhk goal"));
        assertTrue(workBrief.contains("## 专业注意事项"));
        assertTrue(agentBrief.contains("\"schema_version\": \"devharness-agent-brief/v1-alpha\""));
        assertTrue(agentBrief.contains("\"agent_internal_only\": true"));
        assertTrue(agentBrief.contains("\"show_commands_to_user\": false"));
        assertTrue(agentBrief.contains("\"harness_commands\""));
        assertTrue(agentBrief.contains("\"user_visible_summary_ref\""));
        assertTrue(agentBrief.contains("\"growth_context\""));
        assertTrue(agentBrief.contains("\"advisory_only\": true"));
        assertTrue(agentBrief.contains("\"knowledge_context\""));
        assertTrue(agentBrief.contains("\"pack_ref\""));
    }

    @Test
    void adviseEnumErrorsIncludeValidValues() {
        Harness missingTask = new Harness(tempDir);
        int missingTaskExit = new CommandRouter().run(new String[]{
                "advise", "--project-root", "brief-missing-task"
        }, missingTask.context());
        assertEquals(ExitCodes.USAGE_ERROR, missingTaskExit);
        assertTrue(missingTask.stderr().contains("error_code: ADVISE_TASK_MISSING"));
        assertTrue(missingTask.stderr().contains("missing:"));
        assertTrue(missingTask.stderr().contains("--task"));

        Harness invalidMode = new Harness(tempDir);
        int invalidModeExit = new CommandRouter().run(new String[]{
                "advise",
                "--project-root", "brief-invalid-mode",
                "--task", "Fix order query response mapping",
                "--mode", "robot"
        }, invalidMode.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, invalidModeExit);
        assertTrue(invalidMode.stderr().contains("error_code: INVALID_RECOMMENDATION_MODE"));
        assertTrue(invalidMode.stderr().contains("valid_values:"));
        assertTrue(invalidMode.stderr().contains("patch"));

        Harness invalidGraph = new Harness(tempDir);
        int invalidGraphExit = new CommandRouter().run(new String[]{
                "advise",
                "--project-root", "brief-invalid-graph",
                "--task", "Fix order query response mapping",
                "--graph", "maybe"
        }, invalidGraph.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, invalidGraphExit);
        assertTrue(invalidGraph.stderr().contains("error_code: INVALID_GRAPH_MODE"));
        assertTrue(invalidGraph.stderr().contains("valid_values:"));
        assertTrue(invalidGraph.stderr().contains("optional -> advisory"));
    }

    @Test
    void adviseAskCreatesBlockingInteractionAndAnswerUpdatesAgentBrief() throws Exception {
        Harness harness = new Harness(tempDir);
        int exit = new CommandRouter().run(new String[]{
                "advise",
                "--project-root", "ask-project",
                "--task", "优化一下",
                "--module", "order",
                "--mode", "recommend"
        }, harness.context());

        Path root = tempDir.resolve("ask-project");
        assertEquals(ExitCodes.SUCCESS, exit);
        assertTrue(Files.isRegularFile(PathUtil.interactionRequests(root)));
        String interactions = read(PathUtil.interactionRequests(root));
        assertTrue(interactions.contains("clarification"));
        assertTrue(interactions.contains("open"));
        assertEquals(1, countRowsWhere(root, "interaction_request",
                "interaction_type = 'clarification' AND status = 'open'"));
        assertTrue(read(PathUtil.agentBrief(root)).contains("\"current_action\": \"wait_for_user_answer\""));

        String requestId = interactions.split("\\t", -1)[0];
        Harness answer = new Harness(tempDir);
        int answerExit = new CommandRouter().run(new String[]{
                "brief", "answer",
                "--project-root", "ask-project",
                "--request", requestId,
                "--choice", "按建议继续"
        }, answer.context());

        assertEquals(ExitCodes.SUCCESS, answerExit);
        assertTrue(answer.stdout().contains("status: answered"));
        assertTrue(read(PathUtil.agentBrief(root)).contains("\"current_action\": \"answered_continue\""));
    }

    @Test
    void adviseRecommendsStrictForHighRiskFinancialPermissionWork() throws Exception {
        Harness harness = new Harness(tempDir);
        int exit = new CommandRouter().run(new String[]{
                "advise",
                "--project-root", "strict-project",
                "--task", "修改支付权限和资金风控校验",
                "--module", "payment",
                "--mode", "recommend"
        }, harness.context());

        assertEquals(ExitCodes.SUCCESS, exit);
        assertTrue(harness.stdout().contains("recommendation: strict"));
        String workBrief = read(PathUtil.workBrief(tempDir.resolve("strict-project")));
        assertTrue(workBrief.contains("financial_risk"));
        assertTrue(workBrief.contains("confirmation_required: true"));
    }

    @Test
    void quickstartAnalyzeOnlyWritesBriefButDoesNotCreateGoalState() throws Exception {
        Harness harness = new Harness(tempDir);
        int exit = new CommandRouter().run(new String[]{
                "quickstart",
                "--project-root", "analyze-project",
                "--task", "看看订单查询哪里可以优化",
                "--module", "order",
                "--mode", "analyze-only"
        }, harness.context());

        Path root = tempDir.resolve("analyze-project");
        assertEquals(ExitCodes.SUCCESS, exit);
        assertTrue(harness.stdout().contains("quickstart: analyze_only"));
        assertTrue(harness.stdout().contains("goal_key: none"));
        assertTrue(Files.isRegularFile(PathUtil.workBrief(root)));
        assertTrue(Files.isRegularFile(PathUtil.agentBrief(root)));
        assertTrue(Files.isRegularFile(PathUtil.memoryDb(root)));
        assertEquals(0, countRows(root, "goal_run"));
        assertFalse(Files.exists(PathUtil.devharnessConfig(root)));
    }

    @Test
    void quickstartPatchModeStartsPatchProfileAndUpdatesAgentBrief() throws Exception {
        Harness harness = new Harness(tempDir);
        int exit = new CommandRouter().run(new String[]{
                "quickstart",
                "--project-root", "patch-project",
                "--task", "Fix order total rounding logic",
                "--module", "order",
                "--mode", "patch",
                "--preset", "springboot-auto-test"
        }, harness.context());

        Path root = tempDir.resolve("patch-project");
        assertEquals(ExitCodes.SUCCESS, exit);
        assertTrue(harness.stdout().contains("quickstart: ready"));
        assertTrue(harness.stdout().contains("recommendation: patch"));
        assertTrue(harness.stdout().contains("profile: java-api-patch"));
        assertTrue(Files.isRegularFile(PathUtil.memoryDb(root)));

        String agentBrief = read(PathUtil.agentBrief(root));
        assertTrue(agentBrief.contains("\"profile_key\": \"java-api-patch\""));
        assertTrue(agentBrief.contains("\"name\": \"goal_next\""));
        assertTrue(agentBrief.contains("\"name\": \"goal_step\""));
        assertTrue(agentBrief.contains("\"agent_internal_only\": true"));
    }

    @Test
    void blockingInteractionStopsGoalStepUntilAnswered() throws Exception {
        Harness quickstart = new Harness(tempDir);
        int quickstartExit = new CommandRouter().run(new String[]{
                "quickstart",
                "--project-root", "blocking-project",
                "--task", "Fix order total rounding logic",
                "--module", "order",
                "--mode", "patch"
        }, quickstart.context());
        assertEquals(ExitCodes.SUCCESS, quickstartExit);
        String goalKey = firstValue(quickstart.stdout(), "goal_key: ");
        Path root = tempDir.resolve("blocking-project");
        String requestId = read(PathUtil.interactionRequests(root)).split("\\t", -1)[0];

        Harness blocked = new Harness(tempDir);
        int blockedExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", "blocking-project",
                "--goal", goalKey,
                "--summary", "Understood patch boundary",
                "--field", "goal_understanding=Fix order total rounding only",
                "--field", "assumptions=no API contract change",
                "--field", "read_files=OrderService.java"
        }, blocked.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, blockedExit);
        assertTrue(blocked.stderr().contains("blocking interaction requires user answer"));

        Harness answer = new Harness(tempDir);
        int answerExit = new CommandRouter().run(new String[]{
                "brief", "answer",
                "--project-root", "blocking-project",
                "--request", requestId,
                "--choice", "按建议继续"
        }, answer.context());
        assertEquals(ExitCodes.SUCCESS, answerExit);

        Harness allowed = new Harness(tempDir);
        int allowedExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", "blocking-project",
                "--goal", goalKey,
                "--summary", "Understood patch boundary",
                "--field", "goal_understanding=Fix order total rounding only",
                "--field", "assumptions=no API contract change",
                "--field", "read_files=OrderService.java"
        }, allowed.context());
        assertEquals(ExitCodes.SUCCESS, allowedExit);
        assertTrue(allowed.stdout().contains("progress_brief_path: "));
        assertTrue(Files.isRegularFile(PathUtil.progressBrief(root)));
    }

    @Test
    void goalStepAutoRecordsObjectiveEvidenceForPatchFlow() throws Exception {
        Harness quickstart = new Harness(tempDir);
        int quickstartExit = new CommandRouter().run(new String[]{
                "quickstart",
                "--project-root", "auto-step-project",
                "--task", "Fix order total rounding logic",
                "--module", "order",
                "--mode", "patch",
                "--preset", "springboot-auto-test"
        }, quickstart.context());
        assertEquals(ExitCodes.SUCCESS, quickstartExit);
        String goalKey = firstValue(quickstart.stdout(), "goal_key: ");

        Harness step = new Harness(tempDir);
        int stepExit = new CommandRouter().run(new String[]{
                "goal", "step",
                "--project-root", "auto-step-project",
                "--goal", goalKey,
                "--summary", "Understood patch boundary",
                "--auto",
                "--changed-files", "src/main/java/example/OrderService.java,pom.xml",
                "--field", "goal_understanding=Fix order total rounding only",
                "--field", "assumptions=no API contract change",
                "--field", "read_files=OrderService.java"
        }, step.context());

        assertEquals(ExitCodes.SUCCESS, stepExit);
        assertTrue(step.stdout().contains("step_id: "));
        assertTrue(step.stdout().contains("current_action: apply_patch"));
    }

    @Test
    void goalVerifyMarkdownProducesUserProgressBrief() throws Exception {
        Harness quickstart = new Harness(tempDir);
        int quickstartExit = new CommandRouter().run(new String[]{
                "quickstart",
                "--project-root", "progress-project",
                "--task", "Fix order total rounding logic",
                "--module", "order",
                "--mode", "patch",
                "--preset", "springboot-auto-test"
        }, quickstart.context());
        assertEquals(ExitCodes.SUCCESS, quickstartExit);
        String goalKey = firstValue(quickstart.stdout(), "goal_key: ");

        Harness verify = new Harness(tempDir);
        int verifyExit = new CommandRouter().run(new String[]{
                "goal", "verify",
                "--project-root", "progress-project",
                "--goal", goalKey,
                "--level", "fast",
                "--markdown"
        }, verify.context());

        assertEquals(ExitCodes.SUCCESS, verifyExit);
        assertTrue(verify.stdout().contains("# Goal Progress Brief"));
        assertTrue(verify.stdout().contains("## 用户下一步"));
        assertTrue(verify.stdout().contains("## Agent 调试信息"));
        assertTrue(Files.isRegularFile(PathUtil.verifyBrief(tempDir.resolve("progress-project"))));
        assertTrue(read(PathUtil.verifyBrief(tempDir.resolve("progress-project"))).contains("# Verify Brief"));
    }

    @Test
    void completionBriefKnowledgeCandidateAndGrowthCommandsWork() throws Exception {
        com.devharnesskit.dhk.service.brief.BriefLifecycleService service =
                new com.devharnesskit.dhk.service.brief.BriefLifecycleService();
        Path root = tempDir.resolve("knowledge-project");
        Files.createDirectories(root);
        com.devharnesskit.dhk.model.goal.GoalRun goal = new com.devharnesskit.dhk.model.goal.GoalRun(
                "goal-knowledge", "project", "", "", "java-api-patch",
                "Fix mapper SQL binding bug", "order", "api", "", "completed", "verify",
                3, 3, "", "", "");
        service.writeCompletionBrief(root, goal, 42L, root.resolve("GOAL_SUMMARY.md"));

        assertTrue(Files.isRegularFile(PathUtil.completionBrief(root)));
        assertTrue(Files.isRegularFile(PathUtil.knowledgeCandidatesBrief(root)));
        String candidates = read(PathUtil.knowledgeCandidatesBrief(root));
        assertTrue(candidates.contains("status: draft"));
        assertTrue(candidates.contains("suggested_destination: growth"));
        assertTrue(candidates.contains("source_rule_id: java-db-safe-binding"));
        assertEquals(3, countRows(root, "knowledge_candidate"));
        assertEquals(1, countRowsWhere(root, "knowledge_candidate",
                "source_rule_id = 'java-db-safe-binding' AND domain = 'database'"));

        Harness initMemory = new Harness(tempDir);
        int initExit = new CommandRouter().run(new String[]{
                "memory", "init",
                "--project-root", "knowledge-project"
        }, initMemory.context());
        assertEquals(ExitCodes.SUCCESS, initExit);

        Harness confirmMemory = new Harness(tempDir);
        int confirmMemoryExit = new CommandRouter().run(new String[]{
                "brief", "knowledge", "confirm",
                "--project-root", "knowledge-project",
                "--candidate", "kc-goal-knowledge-verification",
                "--destination", "project_memory"
        }, confirmMemory.context());
        assertEquals(ExitCodes.SUCCESS, confirmMemoryExit);
        assertTrue(confirmMemory.stdout().contains("note: confirmed candidate created draft destination only"));

        Harness confirmGrowth = new Harness(tempDir);
        int confirmGrowthExit = new CommandRouter().run(new String[]{
                "brief", "knowledge", "confirm",
                "--project-root", "knowledge-project",
                "--candidate", "kc-goal-knowledge-growth",
                "--destination", "growth"
        }, confirmGrowth.context());
        assertEquals(ExitCodes.SUCCESS, confirmGrowthExit);
        assertTrue(confirmGrowth.stdout().contains("destination: growth"));

        Harness growthExport = new Harness(tempDir);
        int growthExportExit = new CommandRouter().run(new String[]{
                "growth", "export",
                "--project-root", "knowledge-project"
        }, growthExport.context());
        assertEquals(ExitCodes.SUCCESS, growthExportExit);
        assertTrue(Files.isRegularFile(PathUtil.growthContext(root)));
        assertTrue(read(PathUtil.growthContext(root)).contains("advisory_only: true"));
        assertEquals(1, countRowsWhere(root, "growth_lesson",
                "lesson_id = 'growth-kc-goal-knowledge-growth' AND status = 'draft'"));
    }

    private int countRows(Path projectRoot, String tableName) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PathUtil.memoryDb(projectRoot));
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private int countRowsWhere(Path projectRoot, String tableName, String where) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PathUtil.memoryDb(projectRoot));
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + tableName + " WHERE " + where)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
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
