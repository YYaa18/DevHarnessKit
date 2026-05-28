package com.devharnesskit.dhk.model.brief;

public final class GrowthLesson {
    private final String lessonId;
    private final String sourceCandidateId;
    private final String title;
    private final String summary;
    private final String status;
    private final boolean advisoryOnly;

    public GrowthLesson(String lessonId, String sourceCandidateId, String title,
                        String summary, String status, boolean advisoryOnly) {
        this.lessonId = value(lessonId);
        this.sourceCandidateId = value(sourceCandidateId);
        this.title = value(title);
        this.summary = value(summary);
        this.status = value(status);
        this.advisoryOnly = advisoryOnly;
    }

    public String lessonId() { return lessonId; }
    public String sourceCandidateId() { return sourceCandidateId; }
    public String title() { return title; }
    public String summary() { return summary; }
    public String status() { return status; }
    public boolean advisoryOnly() { return advisoryOnly; }

    public GrowthLesson status(String status) {
        return new GrowthLesson(lessonId, sourceCandidateId, title, summary, status, advisoryOnly);
    }

    private static String value(String value) {
        return value == null ? "" : value.trim();
    }
}
