package com.devharnesskit.dhk.service.routine;

import com.devharnesskit.dhk.export.RoutineReportRenderer;
import com.devharnesskit.dhk.model.goal.GoalMetricsSnapshot;
import com.devharnesskit.dhk.model.goal.GoalReplayEntry;
import com.devharnesskit.dhk.model.routine.RoutineCiExportResult;
import com.devharnesskit.dhk.model.routine.RoutineCheckSummary;
import com.devharnesskit.dhk.model.routine.RoutineExportResult;
import com.devharnesskit.dhk.model.routine.RoutineInterventionSummary;
import com.devharnesskit.dhk.model.routine.RoutineOutcomeSummary;
import com.devharnesskit.dhk.model.routine.RoutineProfileSummary;
import com.devharnesskit.dhk.model.routine.RoutineReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RoutineReportServiceTest {
    @TempDir
    Path tempDir;

    private final RoutineReportService service = new RoutineReportService();
    private final RoutineReportRenderer renderer = new RoutineReportRenderer();

    @Test
    void summarizeBuildsStableRoutineSchemaFromGoalMetrics() {
        RoutineReport report = service.summarize(Arrays.asList(
                        snapshot("goal-b", "java-api-change", "in_progress", 4, 2, 1,
                                3, 1, 1, 1, 0, 1, -1L),
                        snapshot("goal-a", "java-api-change", "completed", 4, 4, 4,
                                4, 4, 0, 0, 0, 0, 600000L),
                        snapshot("goal-c", "bugfix", "failed", 3, 3, 2,
                                2, 1, 1, 0, 0, 0, 300000L)),
                "2026-05-30T00:00:00Z",
                "2026-05-01T00:00:00Z",
                "2026-05-30T00:00:00Z");

        assertEquals("routine-summary/v1", report.schemaVersion());
        assertEquals(3, report.outcomes().totalGoals());
        assertEquals(1, report.outcomes().completedGoals());
        assertEquals(1, report.outcomes().failedGoals());
        assertEquals(1, report.outcomes().inProgressGoals());
        assertEquals(3333, report.outcomes().completionRateBasisPoints());
        assertEquals(2, report.profiles().size());
        assertEquals("bugfix", report.profiles().get(0).profileKey());
        assertEquals("java-api-change", report.profiles().get(1).profileKey());
        assertEquals(600000L, report.profiles().get(1).medianDurationMs());
        assertEquals(3, report.profiles().get(1).medianRecordedSteps());
        assertEquals("all", report.checks().get(0).checkKey());
        assertEquals(9, report.checks().get(0).total());
        assertEquals(0, report.interventions().waivedChecks());
        assertEquals(1, report.interventions().skippedChecks());
        assertEquals(3, report.interventions().evidenceIncomplete());
    }

    @Test
    void rendererProducesStableJsonMarkdownAndNdjson() {
        GoalMetricsSnapshot snapshot = snapshot("goal-a", "java-api-change", "completed",
                4, 4, 4, 4, 4, 0, 0, 0, 0, 600000L);
        RoutineReport report = service.summarize(Collections.singletonList(snapshot),
                "2026-05-30T00:00:00Z", "2026-05-01T00:00:00Z", "2026-05-30T00:00:00Z");
        GoalReplayEntry entry = new GoalReplayEntry(1, "goal_run", "goal_created",
                "2026-05-30T00:00:00Z", "completed", "summary", "data");

        String json = renderer.renderJson(report);
        String markdown = renderer.renderMarkdown(report);
        String goals = renderer.renderGoalsNdjson(Collections.singletonList(snapshot));
        String replay = renderer.renderReplayNdjson(Collections.singletonList(entry));

        assertTrue(json.contains("\"schema_version\": \"routine-summary/v1\""), json);
        assertTrue(json.contains("\"metrics_schema\": \"goal-metrics/v1\""), json);
        assertTrue(json.contains("\"replay_schema\": \"goal-replay/v1\""), json);
        assertTrue(json.contains("\"completion_rate\": 1.0000"), json);
        assertTrue(markdown.contains("<routine>"), markdown);
        assertTrue(markdown.contains("- completion_rate: 1.0000"), markdown);
        assertTrue(goals.contains("\"schema_version\": \"goal-metrics/v1\""), goals);
        assertTrue(goals.contains("\"goal_key\": \"goal-a\""), goals);
        assertTrue(replay.contains("\"schema_version\": \"goal-replay/v1\""), replay);
        assertTrue(replay.contains("\"sequence\": 1"), replay);
    }

    @Test
    void localExportWritesRoutineArtifacts() throws Exception {
        GoalMetricsSnapshot snapshot = snapshot("goal-a", "java-api-change", "completed",
                4, 4, 4, 4, 4, 0, 0, 0, 0, 600000L);
        RoutineReport report = service.summarize(Collections.singletonList(snapshot),
                "2026-05-30T00:00:00Z", "2026-05-01T00:00:00Z", "2026-05-30T00:00:00Z");
        Map<String, List<GoalReplayEntry>> replayByGoal = new LinkedHashMap<String, List<GoalReplayEntry>>();
        replayByGoal.put("goal-a", Collections.singletonList(new GoalReplayEntry(1,
                "goal_run", "goal_created", "2026-05-30T00:00:00Z", "completed", "summary", "data")));

        RoutineExportResult result = new RoutineLocalExportService(renderer)
                .export(tempDir, report, Collections.singletonList(snapshot), replayByGoal);

        assertTrue(Files.isRegularFile(result.markdownPath()));
        assertTrue(Files.isRegularFile(result.jsonPath()));
        assertTrue(Files.isRegularFile(result.goalsPath()));
        assertTrue(Files.isRegularFile(result.replayDirectory().resolve("goal-a.ndjson")));
        assertEquals(1, result.replayFiles());
        assertTrue(new String(Files.readAllBytes(result.jsonPath()), "UTF-8")
                .contains("\"schema_version\": \"routine-summary/v1\""));
    }

    @Test
    void ciSafeExportWritesOnlyAggregateArtifacts() throws Exception {
        GoalMetricsSnapshot snapshot = snapshot("goal-a", "java-api-change", "completed",
                4, 4, 4, 4, 4, 0, 0, 0, 0, 600000L);
        RoutineReport report = service.summarize(Collections.singletonList(snapshot),
                "2026-05-30T00:00:00Z", "2026-05-01T00:00:00Z", "2026-05-30T00:00:00Z");
        Map<String, List<GoalReplayEntry>> replayByGoal = new LinkedHashMap<String, List<GoalReplayEntry>>();
        replayByGoal.put("goal-a", Collections.singletonList(new GoalReplayEntry(1,
                "goal_step", "verify", "2026-05-30T00:00:00Z", "recorded",
                "raw checkout task text",
                "# GOAL_CONTEXT\nCHAT_HISTORY_CONTENT\nSELECT * FROM orders\nSQL_RESULT.md")));
        new RoutineLocalExportService(renderer)
                .export(tempDir.resolve("local"), report, Collections.singletonList(snapshot), replayByGoal);

        RoutineCiExportResult result = new RoutineLocalExportService(renderer)
                .exportCi(tempDir.resolve("ci"), report);

        assertTrue(Files.isRegularFile(result.readmePath()));
        assertTrue(Files.isRegularFile(result.jsonPath()));
        assertTrue(Files.isRegularFile(result.checksPath()));
        assertTrue(Files.isRegularFile(result.profilesPath()));
        assertFalse(Files.exists(tempDir.resolve("ci").resolve(RoutineLocalExportService.GOALS_NDJSON)));
        assertFalse(Files.exists(tempDir.resolve("ci").resolve(RoutineLocalExportService.REPLAY_DIRECTORY)));

        String combined = read(result.readmePath()) + read(result.jsonPath())
                + read(result.checksPath()) + read(result.profilesPath());
        assertTrue(combined.contains("\"schema_version\": \"routine-summary/v1\""), combined);
        assertTrue(combined.contains("\"record_type\": \"check\""), combined);
        assertTrue(combined.contains("\"record_type\": \"profile\""), combined);
        assertFalse(combined.contains("raw checkout task text"), combined);
        assertFalse(combined.contains("# GOAL_CONTEXT"), combined);
        assertFalse(combined.contains("CHAT_HISTORY_CONTENT"), combined);
        assertFalse(combined.contains("SELECT * FROM orders"), combined);
        assertFalse(combined.contains("SQL_RESULT.md"), combined);
    }

    @Test
    void ciSafeExportRejectsSensitiveAggregateLabels() throws Exception {
        RoutineReport report = new RoutineReport("2026-05-30T00:00:00Z",
                "2026-05-01T00:00:00Z", "2026-05-30T00:00:00Z",
                "goal facts", GoalMetricsSnapshot.SCHEMA_VERSION, GoalReplayEntry.SCHEMA_VERSION,
                new RoutineOutcomeSummary(1, 1, 0, 0, 0),
                Collections.singletonList(new RoutineProfileSummary("owner@example.com", 1, 1, 0, 0, 0,
                        100L, 4, 0)),
                Collections.singletonList(new RoutineCheckSummary("all", 1, 1, 0, 0, 0, 0)),
                new RoutineInterventionSummary(0, 0, 0, 0, 0));

        IOException error = assertThrows(IOException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() throws Throwable {
                new RoutineLocalExportService(renderer).exportCi(tempDir.resolve("ci-sensitive"), report);
            }
        });
        assertTrue(error.getMessage().contains("email"), error.getMessage());
        assertFalse(Files.isRegularFile(tempDir.resolve("ci-sensitive")
                .resolve(RoutineLocalExportService.SUMMARY_JSON)));
    }

    private static GoalMetricsSnapshot snapshot(String goalKey, String profileKey, String status,
                                                int expectedSteps, int recordedSteps, int acceptedSteps,
                                                int totalChecks, int passedChecks, int failedChecks,
                                                int skippedChecks, int waivedChecks, int staleChecks,
                                                long durationMs) {
        return new GoalMetricsSnapshot(goalKey, profileKey, status, "completed",
                expectedSteps, recordedSteps, acceptedSteps, totalChecks, totalChecks,
                passedChecks, failedChecks, skippedChecks, waivedChecks, staleChecks,
                0, 0, "2026-05-30T00:00:00Z",
                durationMs >= 0L ? "2026-05-30T00:10:00Z" : "", durationMs);
    }

    private static String read(Path path) throws Exception {
        return new String(Files.readAllBytes(path), "UTF-8");
    }
}
