package com.devharnesskit.dhk.model.goal;

public final class GoalEvent {
    private final long id;
    private final String goalKey;
    private final String eventType;
    private final String level;
    private final String message;
    private final String data;
    private final String createdAt;

    public GoalEvent(long id, String goalKey, String eventType, String level,
                     String message, String data, String createdAt) {
        this.id = id;
        this.goalKey = value(goalKey);
        this.eventType = value(eventType);
        this.level = value(level);
        this.message = value(message);
        this.data = value(data);
        this.createdAt = value(createdAt);
    }

    public long id() { return id; }
    public String goalKey() { return goalKey; }
    public String eventType() { return eventType; }
    public String level() { return level; }
    public String message() { return message; }
    public String data() { return data; }
    public String createdAt() { return createdAt; }

    private String value(String raw) {
        return raw == null ? "" : raw;
    }
}
