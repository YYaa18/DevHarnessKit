package com.devharnesskit.dhk.model.skill;

public final class SkillAuditIssue {
    private final String severity;
    private final String category;
    private final String path;
    private final String message;
    private final String suggestion;

    public SkillAuditIssue(String severity, String category, String path, String message, String suggestion) {
        this.severity = value(severity);
        this.category = value(category);
        this.path = value(path);
        this.message = value(message);
        this.suggestion = value(suggestion);
    }

    public String severity() { return severity; }
    public String category() { return category; }
    public String path() { return path; }
    public String message() { return message; }
    public String suggestion() { return suggestion; }

    private String value(String raw) {
        return raw == null ? "" : raw;
    }
}
