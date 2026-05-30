package com.devharnesskit.dhk.service.routine;

import com.devharnesskit.dhk.model.goal.GoalMetricsSnapshot;
import com.devharnesskit.dhk.model.goal.GoalReplayEntry;
import com.devharnesskit.dhk.model.routine.RoutineCheckSummary;
import com.devharnesskit.dhk.model.routine.RoutineInterventionSummary;
import com.devharnesskit.dhk.model.routine.RoutineOutcomeSummary;
import com.devharnesskit.dhk.model.routine.RoutineProfileSummary;
import com.devharnesskit.dhk.model.routine.RoutineReport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class RoutineReportService {
    public RoutineReport summarize(List<GoalMetricsSnapshot> snapshots,
                                   String generatedAt, String since, String until) {
        List<GoalMetricsSnapshot> safeSnapshots = sortedSnapshots(snapshots);
        OutcomeAccumulator outcomes = new OutcomeAccumulator();
        Map<String, ProfileAccumulator> profiles = new TreeMap<String, ProfileAccumulator>();
        CheckAccumulator checks = new CheckAccumulator();
        int waivedChecks = 0;
        int skippedChecks = 0;
        int staleChecks = 0;
        int failedChecks = 0;

        for (GoalMetricsSnapshot snapshot : safeSnapshots) {
            outcomes.add(snapshot);
            String profileKey = snapshot.profileKey().length() == 0 ? "unknown" : snapshot.profileKey();
            ProfileAccumulator profile = profiles.get(profileKey);
            if (profile == null) {
                profile = new ProfileAccumulator(profileKey);
                profiles.put(profileKey, profile);
            }
            profile.add(snapshot);
            checks.add(snapshot);
            waivedChecks += snapshot.waivedChecks();
            skippedChecks += snapshot.skippedChecks();
            staleChecks += snapshot.staleChecks();
            failedChecks += snapshot.failedChecks();
        }

        List<RoutineProfileSummary> profileSummaries = new ArrayList<RoutineProfileSummary>();
        for (ProfileAccumulator profile : profiles.values()) {
            profileSummaries.add(profile.summary());
        }
        List<RoutineCheckSummary> checkSummaries = new ArrayList<RoutineCheckSummary>();
        checkSummaries.add(checks.summary());
        RoutineInterventionSummary interventions = new RoutineInterventionSummary(
                waivedChecks, skippedChecks, staleChecks, failedChecks, failedChecks + staleChecks);

        return new RoutineReport(generatedAt, since, until, "goal facts",
                GoalMetricsSnapshot.SCHEMA_VERSION, GoalReplayEntry.SCHEMA_VERSION,
                outcomes.summary(), profileSummaries, checkSummaries, interventions);
    }

    private List<GoalMetricsSnapshot> sortedSnapshots(List<GoalMetricsSnapshot> snapshots) {
        List<GoalMetricsSnapshot> result = new ArrayList<GoalMetricsSnapshot>();
        if (snapshots != null) {
            result.addAll(snapshots);
        }
        Collections.sort(result, new Comparator<GoalMetricsSnapshot>() {
            public int compare(GoalMetricsSnapshot left, GoalMetricsSnapshot right) {
                int profile = left.profileKey().compareTo(right.profileKey());
                if (profile != 0) {
                    return profile;
                }
                return left.goalKey().compareTo(right.goalKey());
            }
        });
        return result;
    }

    private boolean completed(GoalMetricsSnapshot snapshot) {
        return "completed".equals(snapshot.status());
    }

    private boolean failed(GoalMetricsSnapshot snapshot) {
        return "failed".equals(snapshot.status());
    }

    private boolean abandoned(GoalMetricsSnapshot snapshot) {
        return "abandoned".equals(snapshot.status()) || "cancelled".equals(snapshot.status())
                || "canceled".equals(snapshot.status());
    }

    private int medianInt(List<Integer> values) {
        if (values.isEmpty()) {
            return -1;
        }
        Collections.sort(values);
        int middle = values.size() / 2;
        if (values.size() % 2 == 1) {
            return values.get(middle).intValue();
        }
        return (values.get(middle - 1).intValue() + values.get(middle).intValue()) / 2;
    }

    private long medianLong(List<Long> values) {
        if (values.isEmpty()) {
            return -1L;
        }
        Collections.sort(values);
        int middle = values.size() / 2;
        if (values.size() % 2 == 1) {
            return values.get(middle).longValue();
        }
        return (values.get(middle - 1).longValue() + values.get(middle).longValue()) / 2L;
    }

    private final class OutcomeAccumulator {
        private int total;
        private int completed;
        private int failed;
        private int abandoned;
        private int inProgress;

        private void add(GoalMetricsSnapshot snapshot) {
            total++;
            if (completed(snapshot)) {
                completed++;
            } else if (failed(snapshot)) {
                failed++;
            } else if (abandoned(snapshot)) {
                abandoned++;
            } else {
                inProgress++;
            }
        }

        private RoutineOutcomeSummary summary() {
            return new RoutineOutcomeSummary(total, completed, failed, abandoned, inProgress);
        }
    }

    private final class ProfileAccumulator {
        private final String profileKey;
        private int total;
        private int completed;
        private int failed;
        private int abandoned;
        private int inProgress;
        private int staleChecks;
        private final List<Long> durations = new ArrayList<Long>();
        private final List<Integer> steps = new ArrayList<Integer>();

        private ProfileAccumulator(String profileKey) {
            this.profileKey = profileKey;
        }

        private void add(GoalMetricsSnapshot snapshot) {
            total++;
            if (completed(snapshot)) {
                completed++;
            } else if (failed(snapshot)) {
                failed++;
            } else if (abandoned(snapshot)) {
                abandoned++;
            } else {
                inProgress++;
            }
            staleChecks += snapshot.staleChecks();
            if (snapshot.durationMs() >= 0L) {
                durations.add(Long.valueOf(snapshot.durationMs()));
            }
            steps.add(Integer.valueOf(snapshot.recordedSteps()));
        }

        private RoutineProfileSummary summary() {
            return new RoutineProfileSummary(profileKey, total, completed, failed, abandoned, inProgress,
                    medianLong(durations), medianInt(steps), staleChecks);
        }
    }

    private static final class CheckAccumulator {
        private int total;
        private int passed;
        private int failed;
        private int skipped;
        private int waived;
        private int stale;

        private void add(GoalMetricsSnapshot snapshot) {
            total += snapshot.totalChecks();
            passed += snapshot.passedChecks();
            failed += snapshot.failedChecks();
            skipped += snapshot.skippedChecks();
            waived += snapshot.waivedChecks();
            stale += snapshot.staleChecks();
        }

        private RoutineCheckSummary summary() {
            return new RoutineCheckSummary("all", total, passed, failed, skipped, waived, stale);
        }
    }
}
