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
    private final String sourceRuleId;
    private final String sourcePackKey;
    private final String domain;
    private final String severity;
    private final String applicableWhen;
    private final String notApplicableWhen;
    private final String destinationReason;
    private final String metadata;

    public KnowledgeCandidate(String candidateId, String goalKey, String type, String title,
                              String summary, String evidenceRefs, String suggestedDestination,
                              String confidence, boolean requiresConfirmation,
                              String sensitiveScanStatus, String status) {
        this(candidateId, goalKey, type, title, summary, evidenceRefs, suggestedDestination, confidence,
                requiresConfirmation, sensitiveScanStatus, status, "", "", "", "", "", "", "", "");
    }

    public KnowledgeCandidate(String candidateId, String goalKey, String type, String title,
                              String summary, String evidenceRefs, String suggestedDestination,
                              String confidence, boolean requiresConfirmation,
                              String sensitiveScanStatus, String status, String sourceRuleId,
                              String sourcePackKey, String domain, String severity,
                              String applicableWhen, String notApplicableWhen,
                              String destinationReason, String metadata) {
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
        this.sourceRuleId = value(sourceRuleId);
        this.sourcePackKey = value(sourcePackKey);
        this.domain = value(domain);
        this.severity = value(severity);
        this.applicableWhen = value(applicableWhen);
        this.notApplicableWhen = value(notApplicableWhen);
        this.destinationReason = value(destinationReason);
        this.metadata = value(metadata);
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
    public String sourceRuleId() { return sourceRuleId; }
    public String sourcePackKey() { return sourcePackKey; }
    public String domain() { return domain; }
    public String severity() { return severity; }
    public String applicableWhen() { return applicableWhen; }
    public String notApplicableWhen() { return notApplicableWhen; }
    public String destinationReason() { return destinationReason; }
    public String metadata() { return metadata; }

    public KnowledgeCandidate status(String status) {
        return new KnowledgeCandidate(candidateId, goalKey, type, title, summary, evidenceRefs,
                suggestedDestination, confidence, requiresConfirmation, sensitiveScanStatus, status,
                sourceRuleId, sourcePackKey, domain, severity, applicableWhen, notApplicableWhen,
                destinationReason, metadata);
    }

    public KnowledgeCandidate destination(String destination) {
        return new KnowledgeCandidate(candidateId, goalKey, type, title, summary, evidenceRefs,
                destination, confidence, requiresConfirmation, sensitiveScanStatus, status,
                sourceRuleId, sourcePackKey, domain, severity, applicableWhen, notApplicableWhen,
                destinationReason, metadata);
    }

    private static String value(String value) {
        return value == null ? "" : value.trim();
    }
}
