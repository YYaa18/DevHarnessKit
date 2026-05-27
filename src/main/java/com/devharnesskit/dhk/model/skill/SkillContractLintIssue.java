package com.devharnesskit.dhk.model.skill;

public final class SkillContractLintIssue {
    private final String category;
    private final String field;
    private final String message;
    private final String suggestion;

    public SkillContractLintIssue(String category, String field, String message, String suggestion) {
        this.category = value(category);
        this.field = value(field);
        this.message = value(message);
        this.suggestion = value(suggestion);
    }

    public String category() { return category; }
    public String field() { return field; }
    public String message() { return message; }
    public String suggestion() { return suggestion; }

    private String value(String raw) {
        return raw == null ? "" : raw;
    }
}
