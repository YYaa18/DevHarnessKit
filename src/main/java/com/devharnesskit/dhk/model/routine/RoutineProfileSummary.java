package com.devharnesskit.dhk.model.routine;

public final class RoutineProfileSummary {
    private final String profileKey;
    private final int goals;
    private final int completed;
    private final int failed;
    private final int abandoned;
    private final int inProgress;
    private final long medianDurationMs;
    private final int medianRecordedSteps;
    private final int staleChecks;

    public RoutineProfileSummary(String profileKey, int goals, int completed, int failed,
                                 int abandoned, int inProgress, long medianDurationMs,
                                 int medianRecordedSteps, int staleChecks) {
        this.profileKey = value(profileKey);
        this.goals = goals;
        this.completed = completed;
        this.failed = failed;
        this.abandoned = abandoned;
        this.inProgress = inProgress;
        this.medianDurationMs = medianDurationMs;
        this.medianRecordedSteps = medianRecordedSteps;
        this.staleChecks = staleChecks;
    }

    public String profileKey() { return profileKey; }
    public int goals() { return goals; }
    public int completed() { return completed; }
    public int failed() { return failed; }
    public int abandoned() { return abandoned; }
    public int inProgress() { return inProgress; }
    public long medianDurationMs() { return medianDurationMs; }
    public int medianRecordedSteps() { return medianRecordedSteps; }
    public int staleChecks() { return staleChecks; }

    private String value(String raw) {
        return raw == null ? "" : raw;
    }
}
