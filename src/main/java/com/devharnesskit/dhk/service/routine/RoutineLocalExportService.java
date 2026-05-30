package com.devharnesskit.dhk.service.routine;

import com.devharnesskit.dhk.export.RoutineReportRenderer;
import com.devharnesskit.dhk.model.goal.GoalMetricsSnapshot;
import com.devharnesskit.dhk.model.goal.GoalReplayEntry;
import com.devharnesskit.dhk.model.routine.RoutineCiExportResult;
import com.devharnesskit.dhk.model.routine.RoutineExportResult;
import com.devharnesskit.dhk.model.routine.RoutineReport;
import com.devharnesskit.dhk.service.SensitiveDataGuard;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class RoutineLocalExportService {
    public static final String SUMMARY_MARKDOWN = "ROUTINE_SUMMARY.md";
    public static final String SUMMARY_JSON = "routine-summary.json";
    public static final String GOALS_NDJSON = "routine-goals.ndjson";
    public static final String REPLAY_DIRECTORY = "routine-replay";
    public static final String CI_README = "README.md";
    public static final String CI_CHECKS_NDJSON = "routine-checks.ndjson";
    public static final String CI_PROFILES_NDJSON = "routine-profiles.ndjson";

    private final RoutineReportRenderer renderer;
    private final SensitiveDataGuard sensitiveDataGuard;

    public RoutineLocalExportService() {
        this(new RoutineReportRenderer(), new SensitiveDataGuard());
    }

    public RoutineLocalExportService(RoutineReportRenderer renderer) {
        this(renderer, new SensitiveDataGuard());
    }

    RoutineLocalExportService(RoutineReportRenderer renderer, SensitiveDataGuard sensitiveDataGuard) {
        this.renderer = renderer;
        this.sensitiveDataGuard = sensitiveDataGuard;
    }

    public RoutineExportResult export(Path exportDirectory, RoutineReport report,
                                      List<GoalMetricsSnapshot> snapshots,
                                      Map<String, List<GoalReplayEntry>> replayByGoal) throws IOException {
        Files.createDirectories(exportDirectory);
        Path markdown = exportDirectory.resolve(SUMMARY_MARKDOWN);
        Path json = exportDirectory.resolve(SUMMARY_JSON);
        Path goals = exportDirectory.resolve(GOALS_NDJSON);
        Path replayDirectory = exportDirectory.resolve(REPLAY_DIRECTORY);

        Files.write(markdown, renderer.renderMarkdown(report).getBytes("UTF-8"));
        Files.write(json, renderer.renderJson(report).getBytes("UTF-8"));
        Files.write(goals, renderer.renderGoalsNdjson(snapshots).getBytes("UTF-8"));

        int replayFiles = 0;
        if (replayByGoal != null && !replayByGoal.isEmpty()) {
            Files.createDirectories(replayDirectory);
            for (Map.Entry<String, List<GoalReplayEntry>> entry : replayByGoal.entrySet()) {
                Path out = replayDirectory.resolve(safeFileName(entry.getKey()) + ".ndjson");
                Files.write(out, renderer.renderReplayNdjson(entry.getValue()).getBytes("UTF-8"));
                replayFiles++;
            }
        }
        return new RoutineExportResult(markdown, json, goals, replayDirectory, replayFiles);
    }

    public RoutineCiExportResult exportCi(Path exportDirectory, RoutineReport report) throws IOException {
        Files.createDirectories(exportDirectory);
        Path readme = exportDirectory.resolve(CI_README);
        Path json = exportDirectory.resolve(SUMMARY_JSON);
        Path checks = exportDirectory.resolve(CI_CHECKS_NDJSON);
        Path profiles = exportDirectory.resolve(CI_PROFILES_NDJSON);

        String readmeText = ciSafe(renderer.renderCiReadme(), CI_README);
        String jsonText = ciSafe(renderer.renderJson(report), SUMMARY_JSON);
        String checksText = ciSafe(renderer.renderChecksNdjson(report.checks()), CI_CHECKS_NDJSON);
        String profilesText = ciSafe(renderer.renderProfilesNdjson(report.profiles()), CI_PROFILES_NDJSON);

        Files.write(readme, readmeText.getBytes("UTF-8"));
        Files.write(json, jsonText.getBytes("UTF-8"));
        Files.write(checks, checksText.getBytes("UTF-8"));
        Files.write(profiles, profilesText.getBytes("UTF-8"));
        return new RoutineCiExportResult(readme, json, checks, profiles);
    }

    private String ciSafe(String text, String label) throws IOException {
        String output = sensitiveDataGuard.redact(text);
        List<String> matches = sensitiveDataGuard.findMatches(output);
        if (!matches.isEmpty()) {
            throw new IOException("Sensitive data rejected during routine CI export " + label + ": " + matches);
        }
        return output;
    }

    private String safeFileName(String value) {
        String raw = value == null || value.trim().length() == 0 ? "goal" : value.trim();
        return raw.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
