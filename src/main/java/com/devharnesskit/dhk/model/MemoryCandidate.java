package com.devharnesskit.dhk.model;

public final class MemoryCandidate {
    private final long id;
    private final String projectKey;
    private final String candidateStatus;
    private final String moduleName;
    private final String memoryType;
    private final String title;
    private final String content;
    private final String tags;
    private final int confidence;
    private final String sourceKind;
    private final String sourceRef;
    private final String reason;
    private final String evidence;
    private final String fingerprint;
    private final String canonicalKey;
    private final String createdAt;
    private final String updatedAt;
    private final String decidedAt;
    private final String decisionReason;
    private final long acceptedMemoryId;

    public MemoryCandidate(long id, String projectKey, String candidateStatus, String moduleName,
                           String memoryType, String title, String content, String tags, int confidence,
                           String sourceKind, String sourceRef, String reason, String evidence,
                           String fingerprint, String canonicalKey, String createdAt, String updatedAt,
                           String decidedAt, String decisionReason, long acceptedMemoryId) {
        this.id = id;
        this.projectKey = value(projectKey);
        this.candidateStatus = value(candidateStatus);
        this.moduleName = value(moduleName);
        this.memoryType = value(memoryType);
        this.title = value(title);
        this.content = value(content);
        this.tags = value(tags);
        this.confidence = confidence;
        this.sourceKind = value(sourceKind);
        this.sourceRef = value(sourceRef);
        this.reason = value(reason);
        this.evidence = value(evidence);
        this.fingerprint = value(fingerprint);
        this.canonicalKey = value(canonicalKey);
        this.createdAt = value(createdAt);
        this.updatedAt = value(updatedAt);
        this.decidedAt = value(decidedAt);
        this.decisionReason = value(decisionReason);
        this.acceptedMemoryId = acceptedMemoryId;
    }

    public long id() {
        return id;
    }

    public String projectKey() {
        return projectKey;
    }

    public String candidateStatus() {
        return candidateStatus;
    }

    public String moduleName() {
        return moduleName;
    }

    public String memoryType() {
        return memoryType;
    }

    public String title() {
        return title;
    }

    public String content() {
        return content;
    }

    public String tags() {
        return tags;
    }

    public int confidence() {
        return confidence;
    }

    public String sourceKind() {
        return sourceKind;
    }

    public String sourceRef() {
        return sourceRef;
    }

    public String reason() {
        return reason;
    }

    public String evidence() {
        return evidence;
    }

    public String fingerprint() {
        return fingerprint;
    }

    public String canonicalKey() {
        return canonicalKey;
    }

    public String createdAt() {
        return createdAt;
    }

    public String updatedAt() {
        return updatedAt;
    }

    public String decidedAt() {
        return decidedAt;
    }

    public String decisionReason() {
        return decisionReason;
    }

    public long acceptedMemoryId() {
        return acceptedMemoryId;
    }

    public String summary(int maxChars) {
        String singleLine = content.replace('\n', ' ').replace('\r', ' ').trim();
        if (singleLine.length() <= maxChars) {
            return singleLine;
        }
        if (maxChars <= 3) {
            return singleLine.substring(0, maxChars);
        }
        return singleLine.substring(0, maxChars - 3) + "...";
    }

    private String value(String raw) {
        return raw == null ? "" : raw;
    }
}
