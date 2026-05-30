package com.devharnesskit.dhk.export;

import com.devharnesskit.dhk.model.goal.GoalMetricsSnapshot;
import com.devharnesskit.dhk.model.goal.GoalReplayEntry;
import com.devharnesskit.dhk.model.routine.RoutineCheckSummary;
import com.devharnesskit.dhk.model.routine.RoutineOutcomeSummary;
import com.devharnesskit.dhk.model.routine.RoutineProfileSummary;
import com.devharnesskit.dhk.model.routine.RoutineReport;
import com.devharnesskit.dhk.util.JsonOutput;

import java.util.List;
import java.util.Locale;

public final class RoutineReportRenderer {
    public String renderMarkdown(RoutineReport report) {
        StringBuilder builder = new StringBuilder();
        RoutineOutcomeSummary outcomes = report.outcomes();
        builder.append("# Routine Summary\n\n");
        builder.append("<routine>\n");
        builder.append("- schema_version: ").append(report.schemaVersion()).append('\n');
        builder.append("- generated_at: ").append(report.generatedAt()).append('\n');
        builder.append("- window_since: ").append(report.since()).append('\n');
        builder.append("- window_until: ").append(report.until()).append('\n');
        builder.append("- metrics_schema: ").append(report.metricsSchema()).append('\n');
        builder.append("- replay_schema: ").append(report.replaySchema()).append('\n');
        builder.append("</routine>\n\n");
        builder.append("<outcomes>\n");
        builder.append("- total_goals: ").append(outcomes.totalGoals()).append('\n');
        builder.append("- completed_goals: ").append(outcomes.completedGoals()).append('\n');
        builder.append("- failed_goals: ").append(outcomes.failedGoals()).append('\n');
        builder.append("- abandoned_goals: ").append(outcomes.abandonedGoals()).append('\n');
        builder.append("- in_progress_goals: ").append(outcomes.inProgressGoals()).append('\n');
        builder.append("- completion_rate: ").append(rate(outcomes.completionRateBasisPoints())).append('\n');
        builder.append("</outcomes>\n\n");
        builder.append("<profiles>\n");
        for (RoutineProfileSummary profile : report.profiles()) {
            builder.append("- profile_key: ").append(profile.profileKey())
                    .append(" goals: ").append(profile.goals())
                    .append(" completed: ").append(profile.completed())
                    .append(" failed: ").append(profile.failed())
                    .append(" median_duration_ms: ").append(profile.medianDurationMs())
                    .append(" median_recorded_steps: ").append(profile.medianRecordedSteps())
                    .append(" stale_checks: ").append(profile.staleChecks()).append('\n');
        }
        builder.append("</profiles>\n\n");
        builder.append("<checks>\n");
        for (RoutineCheckSummary check : report.checks()) {
            builder.append("- check_key: ").append(check.checkKey())
                    .append(" total: ").append(check.total())
                    .append(" passed: ").append(check.passed())
                    .append(" failed: ").append(check.failed())
                    .append(" skipped: ").append(check.skipped())
                    .append(" waived: ").append(check.waived())
                    .append(" stale: ").append(check.stale()).append('\n');
        }
        builder.append("</checks>\n");
        return builder.toString();
    }

    public String renderJson(RoutineReport report) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        field(builder, "schema_version", JsonOutput.quote(report.schemaVersion()), true);
        field(builder, "generated_at", JsonOutput.quote(report.generatedAt()), true);
        builder.append("  \"window\": {\n");
        field(builder, "since", JsonOutput.quote(report.since()), true, 4);
        field(builder, "until", JsonOutput.quote(report.until()), false, 4);
        builder.append("  },\n");
        field(builder, "source_schema", JsonOutput.quote(report.sourceSchema()), true);
        field(builder, "metrics_schema", JsonOutput.quote(report.metricsSchema()), true);
        field(builder, "replay_schema", JsonOutput.quote(report.replaySchema()), true);
        outcomes(builder, report.outcomes());
        builder.append(",\n");
        profiles(builder, report.profiles());
        builder.append(",\n");
        checks(builder, report.checks());
        builder.append(",\n");
        builder.append("  \"interventions\": {\n");
        field(builder, "waived_checks", String.valueOf(report.interventions().waivedChecks()), true, 4);
        field(builder, "skipped_checks", String.valueOf(report.interventions().skippedChecks()), true, 4);
        field(builder, "stale_checks", String.valueOf(report.interventions().staleChecks()), true, 4);
        field(builder, "failed_checks", String.valueOf(report.interventions().failedChecks()), true, 4);
        field(builder, "evidence_incomplete", String.valueOf(report.interventions().evidenceIncomplete()), false, 4);
        builder.append("  }\n");
        builder.append("}\n");
        return builder.toString();
    }

    public String renderGoalsNdjson(List<GoalMetricsSnapshot> snapshots) {
        StringBuilder builder = new StringBuilder();
        if (snapshots == null) {
            return "";
        }
        for (GoalMetricsSnapshot snapshot : snapshots) {
            builder.append(JsonOutput.object(
                    JsonOutput.stringField("schema_version", snapshot.schemaVersion()),
                    JsonOutput.stringField("goal_key", snapshot.goalKey()),
                    JsonOutput.stringField("profile_key", snapshot.profileKey()),
                    JsonOutput.stringField("status", snapshot.status()),
                    JsonOutput.numberField("recorded_steps", snapshot.recordedSteps()),
                    JsonOutput.numberField("passed_checks", snapshot.passedChecks()),
                    JsonOutput.numberField("failed_checks", snapshot.failedChecks()),
                    JsonOutput.numberField("stale_checks", snapshot.staleChecks()),
                    JsonOutput.numberField("duration_ms", snapshot.durationMs())
            ).trim()).append('\n');
        }
        return builder.toString();
    }

    public String renderReplayNdjson(List<GoalReplayEntry> replay) {
        StringBuilder builder = new StringBuilder();
        if (replay == null) {
            return "";
        }
        for (GoalReplayEntry entry : replay) {
            builder.append(JsonOutput.object(
                    JsonOutput.stringField("schema_version", entry.schemaVersion()),
                    JsonOutput.numberField("sequence", entry.sequence()),
                    JsonOutput.stringField("source", entry.source()),
                    JsonOutput.stringField("kind", entry.kind()),
                    JsonOutput.stringField("timestamp", entry.timestamp()),
                    JsonOutput.stringField("status", entry.status()),
                    JsonOutput.stringField("summary", entry.summary()),
                    JsonOutput.stringField("data", entry.data())
            ).trim()).append('\n');
        }
        return builder.toString();
    }

    public String renderChecksNdjson(List<RoutineCheckSummary> checks) {
        StringBuilder builder = new StringBuilder();
        if (checks == null) {
            return "";
        }
        for (RoutineCheckSummary check : checks) {
            builder.append(JsonOutput.object(
                    JsonOutput.stringField("schema_version", RoutineReport.SCHEMA_VERSION),
                    JsonOutput.stringField("record_type", "check"),
                    JsonOutput.stringField("check_key", check.checkKey()),
                    JsonOutput.numberField("total", check.total()),
                    JsonOutput.numberField("passed", check.passed()),
                    JsonOutput.numberField("failed", check.failed()),
                    JsonOutput.numberField("skipped", check.skipped()),
                    JsonOutput.numberField("waived", check.waived()),
                    JsonOutput.numberField("stale", check.stale())
            ).trim()).append('\n');
        }
        return builder.toString();
    }

    public String renderProfilesNdjson(List<RoutineProfileSummary> profiles) {
        StringBuilder builder = new StringBuilder();
        if (profiles == null) {
            return "";
        }
        for (RoutineProfileSummary profile : profiles) {
            builder.append(JsonOutput.object(
                    JsonOutput.stringField("schema_version", RoutineReport.SCHEMA_VERSION),
                    JsonOutput.stringField("record_type", "profile"),
                    JsonOutput.stringField("profile_key", profile.profileKey()),
                    JsonOutput.numberField("goals", profile.goals()),
                    JsonOutput.numberField("completed", profile.completed()),
                    JsonOutput.numberField("failed", profile.failed()),
                    JsonOutput.numberField("abandoned", profile.abandoned()),
                    JsonOutput.numberField("in_progress", profile.inProgress()),
                    JsonOutput.numberField("median_duration_ms", profile.medianDurationMs()),
                    JsonOutput.numberField("median_recorded_steps", profile.medianRecordedSteps()),
                    JsonOutput.numberField("stale_checks", profile.staleChecks())
            ).trim()).append('\n');
        }
        return builder.toString();
    }

    public String renderCiReadme() {
        return "# DevHarnessKit Routine CI Export\n\n"
                + "This directory contains CI-safe aggregate routine reporting artifacts.\n\n"
                + "Included files:\n\n"
                + "- routine-summary.json\n"
                + "- routine-checks.ndjson\n"
                + "- routine-profiles.ndjson\n\n"
                + "Excluded by design: task text, step evidence, replay data, SQL text or results, "
                + "context Markdown, chat history, and paths outside this export directory.\n";
    }

    private void outcomes(StringBuilder builder, RoutineOutcomeSummary outcomes) {
        builder.append("  \"outcomes\": {\n");
        field(builder, "total_goals", String.valueOf(outcomes.totalGoals()), true, 4);
        field(builder, "completed_goals", String.valueOf(outcomes.completedGoals()), true, 4);
        field(builder, "failed_goals", String.valueOf(outcomes.failedGoals()), true, 4);
        field(builder, "abandoned_goals", String.valueOf(outcomes.abandonedGoals()), true, 4);
        field(builder, "in_progress_goals", String.valueOf(outcomes.inProgressGoals()), true, 4);
        field(builder, "completion_rate", rate(outcomes.completionRateBasisPoints()), false, 4);
        builder.append("  }");
    }

    private void profiles(StringBuilder builder, List<RoutineProfileSummary> profiles) {
        builder.append("  \"profiles\": [");
        for (int i = 0; i < profiles.size(); i++) {
            RoutineProfileSummary profile = profiles.get(i);
            if (i > 0) {
                builder.append(',');
            }
            builder.append("\n    {\n");
            field(builder, "profile_key", JsonOutput.quote(profile.profileKey()), true, 6);
            field(builder, "goals", String.valueOf(profile.goals()), true, 6);
            field(builder, "completed", String.valueOf(profile.completed()), true, 6);
            field(builder, "failed", String.valueOf(profile.failed()), true, 6);
            field(builder, "abandoned", String.valueOf(profile.abandoned()), true, 6);
            field(builder, "in_progress", String.valueOf(profile.inProgress()), true, 6);
            field(builder, "median_duration_ms", String.valueOf(profile.medianDurationMs()), true, 6);
            field(builder, "median_recorded_steps", String.valueOf(profile.medianRecordedSteps()), true, 6);
            field(builder, "stale_checks", String.valueOf(profile.staleChecks()), false, 6);
            builder.append("    }");
        }
        if (!profiles.isEmpty()) {
            builder.append('\n');
        }
        builder.append("  ]");
    }

    private void checks(StringBuilder builder, List<RoutineCheckSummary> checks) {
        builder.append("  \"checks\": [");
        for (int i = 0; i < checks.size(); i++) {
            RoutineCheckSummary check = checks.get(i);
            if (i > 0) {
                builder.append(',');
            }
            builder.append("\n    {\n");
            field(builder, "check_key", JsonOutput.quote(check.checkKey()), true, 6);
            field(builder, "total", String.valueOf(check.total()), true, 6);
            field(builder, "passed", String.valueOf(check.passed()), true, 6);
            field(builder, "failed", String.valueOf(check.failed()), true, 6);
            field(builder, "skipped", String.valueOf(check.skipped()), true, 6);
            field(builder, "waived", String.valueOf(check.waived()), true, 6);
            field(builder, "stale", String.valueOf(check.stale()), false, 6);
            builder.append("    }");
        }
        if (!checks.isEmpty()) {
            builder.append('\n');
        }
        builder.append("  ]");
    }

    private void field(StringBuilder builder, String name, String rawValue, boolean comma) {
        field(builder, name, rawValue, comma, 2);
    }

    private void field(StringBuilder builder, String name, String rawValue, boolean comma, int spaces) {
        for (int i = 0; i < spaces; i++) {
            builder.append(' ');
        }
        builder.append(JsonOutput.quote(name)).append(": ").append(rawValue);
        if (comma) {
            builder.append(',');
        }
        builder.append('\n');
    }

    private String rate(int basisPoints) {
        return String.format(Locale.ROOT, "%.4f", basisPoints / 10000.0d);
    }
}
