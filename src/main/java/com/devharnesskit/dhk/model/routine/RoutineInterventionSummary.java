package com.devharnesskit.dhk.model.routine;

public final class RoutineInterventionSummary {
    private final int waivedChecks;
    private final int skippedChecks;
    private final int staleChecks;
    private final int failedChecks;
    private final int evidenceIncomplete;

    public RoutineInterventionSummary(int waivedChecks, int skippedChecks, int staleChecks,
                                      int failedChecks, int evidenceIncomplete) {
        this.waivedChecks = waivedChecks;
        this.skippedChecks = skippedChecks;
        this.staleChecks = staleChecks;
        this.failedChecks = failedChecks;
        this.evidenceIncomplete = evidenceIncomplete;
    }

    public int waivedChecks() { return waivedChecks; }
    public int skippedChecks() { return skippedChecks; }
    public int staleChecks() { return staleChecks; }
    public int failedChecks() { return failedChecks; }
    public int evidenceIncomplete() { return evidenceIncomplete; }
}
