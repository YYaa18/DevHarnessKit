package com.devharnesskit.dhk.model.routine;

public final class RoutineCheckSummary {
    private final String checkKey;
    private final int total;
    private final int passed;
    private final int failed;
    private final int skipped;
    private final int waived;
    private final int stale;

    public RoutineCheckSummary(String checkKey, int total, int passed, int failed,
                               int skipped, int waived, int stale) {
        this.checkKey = value(checkKey);
        this.total = total;
        this.passed = passed;
        this.failed = failed;
        this.skipped = skipped;
        this.waived = waived;
        this.stale = stale;
    }

    public String checkKey() { return checkKey; }
    public int total() { return total; }
    public int passed() { return passed; }
    public int failed() { return failed; }
    public int skipped() { return skipped; }
    public int waived() { return waived; }
    public int stale() { return stale; }

    private String value(String raw) {
        return raw == null ? "" : raw;
    }
}
