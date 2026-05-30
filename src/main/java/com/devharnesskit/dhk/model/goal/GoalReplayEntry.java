package com.devharnesskit.dhk.model.goal;

public final class GoalReplayEntry {
    public static final String SCHEMA_VERSION = "goal-replay/v1";

    private final int sequence;
    private final String source;
    private final String kind;
    private final String timestamp;
    private final String status;
    private final String summary;
    private final String data;

    public GoalReplayEntry(int sequence, String source, String kind, String timestamp,
                           String status, String summary, String data) {
        this.sequence = sequence;
        this.source = value(source);
        this.kind = value(kind);
        this.timestamp = value(timestamp);
        this.status = value(status);
        this.summary = value(summary);
        this.data = value(data);
    }

    public String schemaVersion() { return SCHEMA_VERSION; }
    public int sequence() { return sequence; }
    public String source() { return source; }
    public String kind() { return kind; }
    public String timestamp() { return timestamp; }
    public String status() { return status; }
    public String summary() { return summary; }
    public String data() { return data; }

    public GoalReplayEntry withSequence(int newSequence) {
        return new GoalReplayEntry(newSequence, source, kind, timestamp, status, summary, data);
    }

    private String value(String raw) {
        return raw == null ? "" : raw;
    }
}
