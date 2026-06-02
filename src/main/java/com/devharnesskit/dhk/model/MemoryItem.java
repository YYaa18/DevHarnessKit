package com.devharnesskit.dhk.model;

public final class MemoryItem {
    private final long id;
    private final String projectKey;
    private final String moduleName;
    private final String memoryType;
    private final String scope;
    private final String title;
    private final String content;
    private final String tags;
    private final String status;
    private final int confidence;
    private final String sourceKind;
    private final String confirmedAt;
    private final String confirmedBy;
    private final String sourceFiles;
    private final String evidence;
    private final String effectiveFrom;
    private final String effectiveTo;
    private final String createdAt;
    private final String updatedAt;
    private final String lastUsedAt;
    private final int useCount;
    private final String fingerprint;
    private final String canonicalKey;
    private final long supersededBy;
    private final String staleReason;
    private final String lastVerifiedAt;
    private final String sourceRef;

    public MemoryItem(long id, String projectKey, String moduleName, String memoryType, String scope,
                      String title, String content, String tags, String status, int confidence,
                      String sourceKind, String confirmedAt, String confirmedBy, String sourceFiles,
                      String evidence, String effectiveFrom, String effectiveTo, String createdAt,
                      String updatedAt, String lastUsedAt, int useCount) {
        this(id, projectKey, moduleName, memoryType, scope, title, content, tags, status, confidence,
                sourceKind, confirmedAt, confirmedBy, sourceFiles, evidence, effectiveFrom, effectiveTo,
                createdAt, updatedAt, lastUsedAt, useCount, "", "", 0L, "", "", "");
    }

    public MemoryItem(long id, String projectKey, String moduleName, String memoryType, String scope,
                      String title, String content, String tags, String status, int confidence,
                      String sourceKind, String confirmedAt, String confirmedBy, String sourceFiles,
                      String evidence, String effectiveFrom, String effectiveTo, String createdAt,
                      String updatedAt, String lastUsedAt, int useCount, String fingerprint,
                      String canonicalKey, long supersededBy, String staleReason, String lastVerifiedAt,
                      String sourceRef) {
        this.id = id;
        this.projectKey = value(projectKey);
        this.moduleName = value(moduleName);
        this.memoryType = value(memoryType);
        this.scope = value(scope);
        this.title = value(title);
        this.content = value(content);
        this.tags = value(tags);
        this.status = value(status);
        this.confidence = confidence;
        this.sourceKind = value(sourceKind);
        this.confirmedAt = value(confirmedAt);
        this.confirmedBy = value(confirmedBy);
        this.sourceFiles = value(sourceFiles);
        this.evidence = value(evidence);
        this.effectiveFrom = value(effectiveFrom);
        this.effectiveTo = value(effectiveTo);
        this.createdAt = value(createdAt);
        this.updatedAt = value(updatedAt);
        this.lastUsedAt = value(lastUsedAt);
        this.useCount = useCount;
        this.fingerprint = value(fingerprint);
        this.canonicalKey = value(canonicalKey);
        this.supersededBy = supersededBy;
        this.staleReason = value(staleReason);
        this.lastVerifiedAt = value(lastVerifiedAt);
        this.sourceRef = value(sourceRef);
    }

    public long id() {
        return id;
    }

    public String projectKey() {
        return projectKey;
    }

    public String moduleName() {
        return moduleName;
    }

    public String memoryType() {
        return memoryType;
    }

    public String scope() {
        return scope;
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

    public String status() {
        return status;
    }

    public int confidence() {
        return confidence;
    }

    public String sourceKind() {
        return sourceKind;
    }

    public String confirmedAt() {
        return confirmedAt;
    }

    public String confirmedBy() {
        return confirmedBy;
    }

    public String sourceFiles() {
        return sourceFiles;
    }

    public String evidence() {
        return evidence;
    }

    public String effectiveFrom() {
        return effectiveFrom;
    }

    public String effectiveTo() {
        return effectiveTo;
    }

    public String createdAt() {
        return createdAt;
    }

    public String updatedAt() {
        return updatedAt;
    }

    public String lastUsedAt() {
        return lastUsedAt;
    }

    public int useCount() {
        return useCount;
    }

    public String fingerprint() {
        return fingerprint;
    }

    public String canonicalKey() {
        return canonicalKey;
    }

    public long supersededBy() {
        return supersededBy;
    }

    public String staleReason() {
        return staleReason;
    }

    public String lastVerifiedAt() {
        return lastVerifiedAt;
    }

    public String sourceRef() {
        return sourceRef;
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
