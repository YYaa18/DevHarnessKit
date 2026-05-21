package com.devharnesskit.dhk.model.spec;

public final class SpecTask {
    private final long id;
    private final String changeKey;
    private final String taskKey;
    private final int taskOrder;
    private final String title;
    private final String description;
    private final String status;
    private final String phaseKey;
    private final String evidence;
    private final String createdAt;
    private final String updatedAt;
    private final String completedAt;

    public SpecTask(long id, String changeKey, String taskKey, int taskOrder, String title,
                    String description, String status, String phaseKey, String evidence,
                    String createdAt, String updatedAt, String completedAt) {
        this.id = id;
        this.changeKey = changeKey;
        this.taskKey = taskKey;
        this.taskOrder = taskOrder;
        this.title = title;
        this.description = description;
        this.status = status;
        this.phaseKey = phaseKey;
        this.evidence = evidence;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.completedAt = completedAt;
    }

    public long id() {
        return id;
    }

    public String changeKey() {
        return changeKey;
    }

    public String taskKey() {
        return taskKey;
    }

    public int taskOrder() {
        return taskOrder;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public String status() {
        return status;
    }

    public String phaseKey() {
        return phaseKey;
    }

    public String evidence() {
        return evidence;
    }

    public String createdAt() {
        return createdAt;
    }

    public String updatedAt() {
        return updatedAt;
    }

    public String completedAt() {
        return completedAt;
    }
}
