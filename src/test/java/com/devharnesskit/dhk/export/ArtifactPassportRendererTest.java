package com.devharnesskit.dhk.export;

import com.devharnesskit.dhk.model.goal.GoalGraphArtifacts;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.model.goal.GoalStep;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ArtifactPassportRendererTest {
    @Test
    void evidenceChangedFilesSkipUnavailableSentinels() {
        GoalRun goal = new GoalRun("goal", "project", "workflow", "spec",
                "java-api-patch", "Task", "module", "api", "", "completed",
                "completed", 3, 3, "2026-01-01T00:00:00Z",
                "2026-01-01T00:00:00Z", "2026-01-01T00:00:00Z");
        java.util.List<GoalStep> steps = Arrays.asList(
                new GoalStep(1L, "goal", 1, "understand_patch", "Understood",
                        "unavailable", "", "recorded", "2026-01-01T00:00:00Z"),
                new GoalStep(2L, "goal", 2, "apply_patch", "Implemented",
                        "src/main/java/Demo.java",
                        "changed_files=unknown; rollback_plan=Revert Demo.java",
                        "recorded", "2026-01-01T00:00:00Z"));

        String passport = new ArtifactPassportRenderer().render(Paths.get("."),
                goal, null, steps, Collections.emptyList(), 1L,
                "2026-01-01T00:00:00Z", GoalGraphArtifacts.none(),
                Paths.get("GOAL_SUMMARY.md"), Paths.get("ARTIFACT_PASSPORT.json"));

        assertTrue(passport.contains("\"changed_files\": [\"src/main/java/Demo.java\"]"));
        assertFalse(passport.contains("\"changed_files\": [\"unavailable\""));
    }
}
