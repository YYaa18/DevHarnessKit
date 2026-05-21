package com.devharnesskit.dhk.model.spec;

public final class SpecEvent {
    private final long id;
    private final String projectKey;
    private final String changeKey;
    private final String eventType;
    private final String level;
    private final String message;
    private final String data;
    private final String createdAt;

    public SpecEvent(long id, String projectKey, String changeKey, String eventType,
                     String level, String message, String data, String createdAt) {
        this.id = id;
        this.projectKey = projectKey;
        this.changeKey = changeKey;
        this.eventType = eventType;
        this.level = level;
        this.message = message;
        this.data = data;
        this.createdAt = createdAt;
    }

    public long id() {
        return id;
    }

    public String projectKey() {
        return projectKey;
    }

    public String changeKey() {
        return changeKey;
    }

    public String eventType() {
        return eventType;
    }

    public String level() {
        return level;
    }

    public String message() {
        return message;
    }

    public String data() {
        return data;
    }

    public String createdAt() {
        return createdAt;
    }
}
