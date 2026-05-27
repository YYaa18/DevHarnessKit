package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.goal.GoalArtifact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class GoalIntegrityGateServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void finalCompleteFailuresReportMissingArtifactsAndHashMismatch() throws Exception {
        GoalIntegrityGateService service = new GoalIntegrityGateService();
        Path summary = tempDir.resolve("GOAL_SUMMARY.md");
        Path passport = tempDir.resolve("ARTIFACT_PASSPORT.json");
        Files.write(summary, "# GOAL_SUMMARY\n".getBytes("UTF-8"));
        Files.write(passport, "{}\n".getBytes("UTF-8"));

        List<GoalArtifact> artifacts = new ArrayList<GoalArtifact>();
        artifacts.add(new GoalArtifact(1L, "goal", "goal_summary", "GOAL_SUMMARY.md",
                summary.toString(), "", "summary", "2026-01-01T00:00:00Z"));
        artifacts.add(new GoalArtifact(2L, "goal", "artifact_passport", "ARTIFACT_PASSPORT.json",
                passport.toString(), "wrong-hash", "passport", "2026-01-01T00:00:00Z"));

        List<String> failures = service.finalCompleteFailures(summary, passport, artifacts);

        assertTrue(failures.contains("artifact_passport content_hash mismatch"));
        assertTrue(failures.contains("goal_artifact row missing: checkpoint"));
    }

    @Test
    void finalCompleteFailuresReportMissingFilesAndRows() {
        GoalIntegrityGateService service = new GoalIntegrityGateService();

        List<String> failures = service.finalCompleteFailures(tempDir.resolve("missing-summary.md"),
                tempDir.resolve("missing-passport.json"), new ArrayList<GoalArtifact>());

        assertTrue(failures.contains("GOAL_SUMMARY.md missing"));
        assertTrue(failures.contains("ARTIFACT_PASSPORT.json missing"));
        assertTrue(failures.contains("goal_artifact row missing: goal_summary"));
        assertTrue(failures.contains("goal_artifact row missing: artifact_passport"));
        assertTrue(failures.contains("goal_artifact row missing: checkpoint"));
    }
}
