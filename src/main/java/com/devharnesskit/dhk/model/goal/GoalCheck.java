package com.devharnesskit.dhk.model.goal;

public final class GoalCheck {
    private final long id;
    private final String goalKey;
    private final String checkKey;
    private final String checkType;
    private final boolean required;
    private final int stepCountAtCheck;
    private final String command;
    private final String status;
    private final String resultSummary;
    private final String evidencePath;
    private final String checkedAt;
    private final String createdAt;
    private final String updatedAt;

    public GoalCheck(long id, String goalKey, String checkKey, String checkType,
                     boolean required, int stepCountAtCheck, String command, String status,
                     String resultSummary, String evidencePath, String checkedAt, String createdAt,
                     String updatedAt) {
        this.id = id;
        this.goalKey = value(goalKey);
        this.checkKey = value(checkKey);
        this.checkType = value(checkType);
        this.required = required;
        this.stepCountAtCheck = stepCountAtCheck;
        this.command = value(command);
        this.status = value(status);
        this.resultSummary = value(resultSummary);
        this.evidencePath = value(evidencePath);
        this.checkedAt = value(checkedAt);
        this.createdAt = value(createdAt);
        this.updatedAt = value(updatedAt);
    }

    public long id() { return id; }
    public String goalKey() { return goalKey; }
    public String checkKey() { return checkKey; }
    public String checkType() { return checkType; }
    public boolean required() { return required; }
    public int stepCountAtCheck() { return stepCountAtCheck; }
    public String command() { return command; }
    public String status() { return status; }
    public String resultSummary() { return resultSummary; }
    public String evidencePath() { return evidencePath; }
    public String checkedAt() { return checkedAt; }
    public String createdAt() { return createdAt; }
    public String updatedAt() { return updatedAt; }

    private String value(String raw) {
        return raw == null ? "" : raw;
    }
}
