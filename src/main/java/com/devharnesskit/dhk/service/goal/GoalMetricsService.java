package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.goal.GoalArtifact;
import com.devharnesskit.dhk.model.goal.GoalCheck;
import com.devharnesskit.dhk.model.goal.GoalEvent;
import com.devharnesskit.dhk.model.goal.GoalMetricsSnapshot;
import com.devharnesskit.dhk.model.goal.GoalReplayEntry;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.model.goal.GoalStep;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GoalMetricsService {
    public GoalMetricsSnapshot snapshot(GoalRun goal, List<GoalStep> steps, List<GoalCheck> checks,
                                        List<GoalEvent> events, List<GoalArtifact> artifacts) {
        int acceptedSteps = 0;
        for (GoalStep step : safeSteps(steps)) {
            if ("accepted".equals(step.status()) || "recorded".equals(step.status())) {
                acceptedSteps++;
            }
        }

        int requiredChecks = 0;
        int passedChecks = 0;
        int failedChecks = 0;
        int skippedChecks = 0;
        int waivedChecks = 0;
        int staleChecks = 0;
        GoalCheck latestBddCheck = null;
        for (GoalCheck check : safeChecks(checks)) {
            if (check.required()) {
                requiredChecks++;
            }
            if ("passed".equals(check.status())) {
                passedChecks++;
            } else if ("failed".equals(check.status())) {
                failedChecks++;
            } else if ("skipped".equals(check.status())) {
                skippedChecks++;
            } else if ("waived".equals(check.status())) {
                waivedChecks++;
            }
            if (check.stepCountAtCheck() < goal.stepCount()) {
                staleChecks++;
            }
            if (isBddCheck(check)) {
                if (latestBddCheck == null || check.checkedAt().compareTo(latestBddCheck.checkedAt()) >= 0) {
                    latestBddCheck = check;
                }
            }
        }
        BddMetrics bddMetrics = bddMetrics(goal, latestBddCheck);

        return new GoalMetricsSnapshot(
                goal.goalKey(),
                goal.profileKey(),
                goal.status(),
                goal.currentAction(),
                goal.maxSteps(),
                goal.stepCount(),
                acceptedSteps,
                safeChecks(checks).size(),
                requiredChecks,
                passedChecks,
                failedChecks,
                skippedChecks,
                waivedChecks,
                staleChecks,
                safeEvents(events).size(),
                safeArtifacts(artifacts).size(),
                goal.createdAt(),
                goal.completedAt(),
                durationMs(goal.createdAt(), goal.completedAt()),
                bddMetrics.status,
                bddMetrics.coveragePercent,
                bddMetrics.freshness,
                bddMetrics.qualityScore,
                bddMetrics.failureReasons);
    }

    public List<GoalReplayEntry> replay(GoalRun goal, List<GoalStep> steps, List<GoalCheck> checks,
                                        List<GoalEvent> events, List<GoalArtifact> artifacts) {
        List<GoalReplayEntry> entries = new ArrayList<GoalReplayEntry>();
        entries.add(new GoalReplayEntry(0, "goal_run", "goal_created", goal.createdAt(), goal.status(),
                goal.taskName(), goal.conditionText()));

        for (GoalEvent event : safeEvents(events)) {
            entries.add(new GoalReplayEntry(0, "goal_event", event.eventType(), event.createdAt(),
                    event.level(), event.message(), event.data()));
        }
        for (GoalStep step : safeSteps(steps)) {
            entries.add(new GoalReplayEntry(0, "goal_step", step.actionKey(), step.createdAt(),
                    step.status(), step.summary(), step.evidence()));
        }
        for (GoalCheck check : safeChecks(checks)) {
            entries.add(new GoalReplayEntry(0, "goal_check", check.checkKey(), check.checkedAt(),
                    check.status(), check.resultSummary(), check.evidencePath()));
        }
        for (GoalArtifact artifact : safeArtifacts(artifacts)) {
            entries.add(new GoalReplayEntry(0, "goal_artifact", artifact.artifactType(), artifact.createdAt(),
                    "", artifact.title(), artifact.filePath()));
        }
        if (goal.completedAt().length() > 0) {
            entries.add(new GoalReplayEntry(0, "goal_run", "goal_completed", goal.completedAt(),
                    "completed", goal.taskName(), goal.goalKey()));
        }

        Collections.sort(entries, new Comparator<GoalReplayEntry>() {
            public int compare(GoalReplayEntry left, GoalReplayEntry right) {
                int timestamp = left.timestamp().compareTo(right.timestamp());
                if (timestamp != 0) {
                    return timestamp;
                }
                int source = Integer.compare(sourceRank(left.source()), sourceRank(right.source()));
                if (source != 0) {
                    return source;
                }
                return left.kind().compareTo(right.kind());
            }
        });

        List<GoalReplayEntry> sequenced = new ArrayList<GoalReplayEntry>();
        for (int i = 0; i < entries.size(); i++) {
            sequenced.add(entries.get(i).withSequence(i + 1));
        }
        return sequenced;
    }

    private long durationMs(String startedAt, String completedAt) {
        if (startedAt == null || startedAt.length() == 0 || completedAt == null || completedAt.length() == 0) {
            return -1L;
        }
        try {
            return Duration.between(Instant.parse(startedAt), Instant.parse(completedAt)).toMillis();
        } catch (DateTimeParseException ignored) {
            return -1L;
        }
    }

    private BddMetrics bddMetrics(GoalRun goal, GoalCheck check) {
        if (check == null) {
            return new BddMetrics("", -1, "missing", -1, "");
        }
        String freshness = "fresh";
        if ("skipped".equals(check.status()) && check.resultSummary().contains("not required")) {
            freshness = "not_required";
        } else if (check.stepCountAtCheck() < goal.stepCount()) {
            freshness = "stale";
        }
        int coveragePercent = parsePercent(check.resultSummary(), "coverage=(\\d+)%", "bdd coverage (\\d+)%");
        int qualityScore = parsePercent(check.resultSummary(), "quality_score=(\\d+)", "bdd quality score (\\d+)");
        String failureReasons = "";
        if ("failed".equals(check.status())) {
            failureReasons = stripPrefix(check.resultSummary(), "bdd incomplete:");
        } else if ("stale".equals(freshness)) {
            failureReasons = "bdd check is stale";
        }
        return new BddMetrics(check.status(), coveragePercent, freshness, qualityScore, failureReasons);
    }

    private boolean isBddCheck(GoalCheck check) {
        return check != null && ("bdd".equals(check.checkKey()) || "bdd".equals(check.checkType()));
    }

    private int parsePercent(String text, String firstPattern, String secondPattern) {
        int value = parseFirstInt(text, firstPattern);
        if (value >= 0) {
            return value;
        }
        return parseFirstInt(text, secondPattern);
    }

    private int parseFirstInt(String text, String pattern) {
        Matcher matcher = Pattern.compile(pattern).matcher(text == null ? "" : text);
        if (!matcher.find()) {
            return -1;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private String stripPrefix(String text, String prefix) {
        String value = text == null ? "" : text.trim();
        if (value.startsWith(prefix)) {
            return value.substring(prefix.length()).trim();
        }
        return value;
    }

    private int sourceRank(String source) {
        if ("goal_run".equals(source)) {
            return 0;
        }
        if ("goal_event".equals(source)) {
            return 1;
        }
        if ("goal_step".equals(source)) {
            return 2;
        }
        if ("goal_check".equals(source)) {
            return 3;
        }
        if ("goal_artifact".equals(source)) {
            return 4;
        }
        return 9;
    }

    private List<GoalStep> safeSteps(List<GoalStep> steps) {
        return steps == null ? Collections.<GoalStep>emptyList() : steps;
    }

    private List<GoalCheck> safeChecks(List<GoalCheck> checks) {
        return checks == null ? Collections.<GoalCheck>emptyList() : checks;
    }

    private List<GoalEvent> safeEvents(List<GoalEvent> events) {
        return events == null ? Collections.<GoalEvent>emptyList() : events;
    }

    private List<GoalArtifact> safeArtifacts(List<GoalArtifact> artifacts) {
        return artifacts == null ? Collections.<GoalArtifact>emptyList() : artifacts;
    }

    private static final class BddMetrics {
        private final String status;
        private final int coveragePercent;
        private final String freshness;
        private final int qualityScore;
        private final String failureReasons;

        private BddMetrics(String status, int coveragePercent, String freshness, int qualityScore,
                           String failureReasons) {
            this.status = status;
            this.coveragePercent = coveragePercent;
            this.freshness = freshness;
            this.qualityScore = qualityScore;
            this.failureReasons = failureReasons;
        }
    }
}
