package com.devharnesskit.dhk.service.goal;

final class GoalCheckCommandResult {
    private final int exitCode;
    private final String output;
    private final long durationMs;
    private final boolean timedOut;
    private final boolean truncated;

    GoalCheckCommandResult(int exitCode, String output, long durationMs,
                           boolean timedOut, boolean truncated) {
        this.exitCode = exitCode;
        this.output = output == null ? "" : output;
        this.durationMs = durationMs;
        this.timedOut = timedOut;
        this.truncated = truncated;
    }

    int exitCode() { return exitCode; }
    String output() { return output; }
    long durationMs() { return durationMs; }
    boolean timedOut() { return timedOut; }
    boolean truncated() { return truncated; }
}
