package com.devharnesskit.dhk.model.brief;

public final class KnowledgeCandidate {
    private final String candidateId;
    private final String goalKey;
    private final String type;
    private final String title;
    private final String summary;
    private final String evidenceRefs;
    private final String suggestedDestination;
    private final String confidence;
    private final boolean requiresConfirmation;
    private final String sensitiveScanStatus;
    private final String status;

    public KnowledgeCandidate(String candidateId, String goalKey, String type, String title,
                              String summary, String evidenceRefs, String suggestedDestination,
                              String confidence, boolean requiresConfirmation,
                              String sensitiveScanStatus, String status) {
        this.candidateId = value(candidateId);
        this.goalKey = value(goalKey);
        this.type = value(type);
        this.title = value(title);
        this.summary = value(summary);
        this.evidenceRefs = value(evidenceRefs);
        this.suggestedDestination = value(suggestedDestination);
        this.confidence = value(confidence);
        this.requiresConfirmation = requiresConfirmation;
        this.sensitiveScanStatus = value(sensitiveScanStatus);
        this.status = value(status);
    }

    public String candidateId() { return candidateId; }
    public String goalKey() { return goalKey; }
    public String type() { return type; }
    public String title() { return title; }
    public String summary() { return summary; }
    public String evidenceRefs() { return evidenceRefs; }
    public String suggestedDestination() { return suggestedDestination; }
    public String confidence() { return confidence; }
    public boolean requiresConfirmation() { return requiresConfirmation; }
    public String sensitiveScanStatus() { return sensitiveScanStatus; }
    public String status() { return status; }

    public KnowledgeCandidate status(String status) {
        return new KnowledgeCandidate(candidateId, goalKey, type, title, summary, evidenceRefs,
                suggestedDestination, confidence, requiresConfirmation, sensitiveScanStatus, status);
    }

    public KnowledgeCandidate destination(String destination) {
        return new KnowledgeCandidate(candidateId, goalKey, type, title, summary, evidenceRefs,
                destination, confidence, requiresConfirmation, sensitiveScanStatus, status);
    }

    private static String value(String value) {
        return value == null ? "" : value.trim();
    }
}
