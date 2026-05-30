package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.goal.GoalArtifact;
import com.devharnesskit.dhk.model.goal.GoalCheck;
import com.devharnesskit.dhk.model.goal.GoalEvent;
import com.devharnesskit.dhk.model.goal.GoalMetricsSnapshot;
import com.devharnesskit.dhk.model.goal.GoalReplayEntry;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.model.goal.GoalStep;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GoalMetricsServiceTest {
    private final GoalMetricsService service = new GoalMetricsService();

    @Test
    void snapshotDerivesCountsAndDurationFromGoalFacts() {
        GoalMetricsSnapshot snapshot = service.snapshot(goal(),
                Arrays.asList(step(1, "inspect", "recorded"), step(2, "verify", "rejected")),
                Arrays.asList(check("compile", "passed", true, 3),
                        check("test", "failed", true, 4),
                        check("workflow", "waived", false, 4),
                        check("db", "skipped", false, 2)),
                Arrays.asList(event("goal_started", "2026-01-01T00:00:01Z")),
                Arrays.asList(artifact("goal_summary", "2026-01-01T00:10:01Z")));

        assertEquals(GoalMetricsSnapshot.SCHEMA_VERSION, snapshot.schemaVersion());
        assertEquals("goal-1", snapshot.goalKey());
        assertEquals(4, snapshot.expectedSteps());
        assertEquals(4, snapshot.recordedSteps());
        assertEquals(1, snapshot.acceptedSteps());
        assertEquals(4, snapshot.totalChecks());
        assertEquals(2, snapshot.requiredChecks());
        assertEquals(1, snapshot.passedChecks());
        assertEquals(1, snapshot.failedChecks());
        assertEquals(1, snapshot.skippedChecks());
        assertEquals(1, snapshot.waivedChecks());
        assertEquals(2, snapshot.staleChecks());
        assertEquals(1, snapshot.totalEvents());
        assertEquals(1, snapshot.totalArtifacts());
        assertEquals(600000L, snapshot.durationMs());
        assertEquals("missing", snapshot.bddEvidenceFreshness());
    }

    @Test
    void snapshotDerivesBddQualityMetricsForEvalReports() {
        GoalMetricsSnapshot snapshot = service.snapshot(goal(),
                Arrays.asList(step(1, "inspect", "recorded")),
                Arrays.asList(check("bdd", "passed", true, 4,
                        "bdd scenarios covered; bound_scenarios=3 coverage=67% quality_score=80")),
                Arrays.asList(event("goal_started", "2026-01-01T00:00:01Z")),
                Arrays.asList(artifact("goal_summary", "2026-01-01T00:10:01Z")));

        assertEquals("passed", snapshot.bddStatus());
        assertEquals(67, snapshot.bddScenarioCoveragePercent());
        assertEquals("fresh", snapshot.bddEvidenceFreshness());
        assertEquals(80, snapshot.bddQualityScore());
        assertEquals("", snapshot.bddFailureReasons());
    }

    @Test
    void snapshotRecordsBddFailureReasonsAndFreshness() {
        GoalMetricsSnapshot snapshot = service.snapshot(goal(),
                Arrays.asList(step(1, "inspect", "recorded")),
                Arrays.asList(check("bdd", "failed", true, 3,
                        "bdd incomplete: [bdd coverage 50% is below threshold 100%, "
                                + "bdd quality score 70 is below threshold 90]")),
                Arrays.asList(event("goal_started", "2026-01-01T00:00:01Z")),
                Arrays.asList(artifact("goal_summary", "2026-01-01T00:10:01Z")));

        assertEquals("failed", snapshot.bddStatus());
        assertEquals(50, snapshot.bddScenarioCoveragePercent());
        assertEquals("stale", snapshot.bddEvidenceFreshness());
        assertEquals(70, snapshot.bddQualityScore());
        assertEquals("[bdd coverage 50% is below threshold 100%, bdd quality score 70 is below threshold 90]",
                snapshot.bddFailureReasons());
    }

    @Test
    void replayOrdersGoalFactsAndAssignsStableSequences() {
        List<GoalReplayEntry> replay = service.replay(goal(),
                Arrays.asList(step(1, "inspect", "recorded")),
                Arrays.asList(check("compile", "passed", true, 4)),
                Arrays.asList(event("goal_started", "2026-01-01T00:00:01Z")),
                Arrays.asList(artifact("goal_summary", "2026-01-01T00:04:00Z")));

        assertEquals(6, replay.size());
        assertEntry(replay.get(0), 1, "goal_run", "goal_created");
        assertEntry(replay.get(1), 2, "goal_event", "goal_started");
        assertEntry(replay.get(2), 3, "goal_step", "inspect");
        assertEntry(replay.get(3), 4, "goal_check", "compile");
        assertEntry(replay.get(4), 5, "goal_artifact", "goal_summary");
        assertEntry(replay.get(5), 6, "goal_run", "goal_completed");
        assertEquals(GoalReplayEntry.SCHEMA_VERSION, replay.get(0).schemaVersion());
    }

    @Test
    void replayUsesFullTieBreakersForDeterministicOrdering() {
        GoalStep inspect = stepAt(1, "inspect", "recorded", "2026-01-01T00:02:00Z");
        GoalStep verify = stepAt(2, "verify", "recorded", "2026-01-01T00:02:00Z");
        GoalCheck compile = checkAt("compile", "passed", true, 4, "compile summary",
                "2026-01-01T00:03:00Z");
        GoalCheck test = checkAt("test", "failed", true, 4, "test summary",
                "2026-01-01T00:03:00Z");

        String first = replaySignature(service.replay(goal(),
                Arrays.asList(verify, inspect),
                Arrays.asList(test, compile),
                Arrays.asList(event("z_event", "2026-01-01T00:01:00Z"),
                        event("a_event", "2026-01-01T00:01:00Z")),
                Arrays.asList(artifact("z_artifact", "2026-01-01T00:04:00Z"),
                        artifact("a_artifact", "2026-01-01T00:04:00Z"))));
        String second = replaySignature(service.replay(goal(),
                Arrays.asList(inspect, verify),
                Arrays.asList(compile, test),
                Arrays.asList(event("a_event", "2026-01-01T00:01:00Z"),
                        event("z_event", "2026-01-01T00:01:00Z")),
                Arrays.asList(artifact("a_artifact", "2026-01-01T00:04:00Z"),
                        artifact("z_artifact", "2026-01-01T00:04:00Z"))));

        assertEquals(first, second);
        assertEquals("1:goal_run:goal_created\n"
                        + "2:goal_event:a_event\n"
                        + "3:goal_event:z_event\n"
                        + "4:goal_step:inspect\n"
                        + "5:goal_step:verify\n"
                        + "6:goal_check:compile\n"
                        + "7:goal_check:test\n"
                        + "8:goal_artifact:a_artifact\n"
                        + "9:goal_artifact:z_artifact\n"
                        + "10:goal_run:goal_completed\n",
                first);
    }

    private static void assertEntry(GoalReplayEntry entry, int sequence, String source, String kind) {
        assertEquals(sequence, entry.sequence());
        assertEquals(source, entry.source());
        assertEquals(kind, entry.kind());
    }

    private static GoalRun goal() {
        return new GoalRun("goal-1", "project", "workflow", "spec", "java-api-change",
                "Task", "goal", "api", "done", "completed", "completed", 4, 4,
                "2026-01-01T00:00:00Z", "2026-01-01T00:10:00Z", "2026-01-01T00:10:00Z");
    }

    private static GoalStep step(int index, String action, String status) {
        return stepAt(index, action, status, "2026-01-01T00:0" + index + ":00Z");
    }

    private static GoalStep stepAt(int index, String action, String status, String createdAt) {
        return new GoalStep(index, "goal-1", index, action, action + " summary",
                "", "evidence", status, createdAt);
    }

    private static GoalCheck check(String key, String status, boolean required, int stepCount) {
        return check(key, status, required, stepCount, key + " summary");
    }

    private static GoalCheck check(String key, String status, boolean required, int stepCount, String summary) {
        return checkAt(key, status, required, stepCount, summary, "2026-01-01T00:03:00Z");
    }

    private static GoalCheck checkAt(String key, String status, boolean required, int stepCount,
                                     String summary, String checkedAt) {
        return new GoalCheck(1L, "goal-1", key, "command", required, stepCount,
                key + " command", status, summary, key + ".log",
                checkedAt, checkedAt, checkedAt);
    }

    private static GoalEvent event(String type, String createdAt) {
        return new GoalEvent(1L, "goal-1", type, "info", type + " message", "", createdAt);
    }

    private static GoalArtifact artifact(String type, String createdAt) {
        return new GoalArtifact(1L, "goal-1", type, type, "artifact.md", "", "summary", createdAt);
    }

    private static String replaySignature(List<GoalReplayEntry> replay) {
        StringBuilder builder = new StringBuilder();
        for (GoalReplayEntry entry : replay) {
            builder.append(entry.sequence()).append(':')
                    .append(entry.source()).append(':')
                    .append(entry.kind()).append('\n');
        }
        return builder.toString();
    }
}
