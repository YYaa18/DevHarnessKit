package com.devharnesskit.dhk.context.artifact;

public final class ContextArtifact {
    private final long id;
    private final String projectKey;
    private final String goalKey;
    private final String artifactKey;
    private final String sourceType;
    private final String sourcePath;
    private final String originalSha256;
    private final String originalText;
    private final String compressedText;
    private final String retainedSpansJson;
    private final int omittedLines;
    private final int tokenBefore;
    private final int tokenAfter;
    private final String createdAt;

    public ContextArtifact(long id, String projectKey, String goalKey, String artifactKey, String sourceType,
                           String sourcePath, String originalSha256, String originalText, String compressedText,
                           String retainedSpansJson, int omittedLines, int tokenBefore, int tokenAfter,
                           String createdAt) {
        this.id = id;
        this.projectKey = value(projectKey);
        this.goalKey = value(goalKey);
        this.artifactKey = value(artifactKey);
        this.sourceType = value(sourceType);
        this.sourcePath = value(sourcePath);
        this.originalSha256 = value(originalSha256);
        this.originalText = value(originalText);
        this.compressedText = value(compressedText);
        this.retainedSpansJson = value(retainedSpansJson);
        this.omittedLines = Math.max(0, omittedLines);
        this.tokenBefore = Math.max(0, tokenBefore);
        this.tokenAfter = Math.max(0, tokenAfter);
        this.createdAt = value(createdAt);
    }

    public long id() {
        return id;
    }

    public String projectKey() {
        return projectKey;
    }

    public String goalKey() {
        return goalKey;
    }

    public String artifactKey() {
        return artifactKey;
    }

    public String sourceType() {
        return sourceType;
    }

    public String sourcePath() {
        return sourcePath;
    }

    public String originalSha256() {
        return originalSha256;
    }

    public String originalText() {
        return originalText;
    }

    public String compressedText() {
        return compressedText;
    }

    public String retainedSpansJson() {
        return retainedSpansJson;
    }

    public int omittedLines() {
        return omittedLines;
    }

    public int tokenBefore() {
        return tokenBefore;
    }

    public int tokenAfter() {
        return tokenAfter;
    }

    public String createdAt() {
        return createdAt;
    }

    private String value(String text) {
        return text == null ? "" : text;
    }
}
