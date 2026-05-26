package com.devharnesskit.dhk.model.graph;

public final class GraphFileEntry {
    private final String relativePath;
    private final String language;
    private final String fileKind;
    private final String contentHash;
    private final long sizeBytes;
    private final boolean indexed;
    private final String skipReason;

    public GraphFileEntry(String relativePath, String language, String fileKind, String contentHash,
                          long sizeBytes, boolean indexed, String skipReason) {
        this.relativePath = relativePath;
        this.language = language;
        this.fileKind = fileKind;
        this.contentHash = contentHash;
        this.sizeBytes = sizeBytes;
        this.indexed = indexed;
        this.skipReason = skipReason == null ? "" : skipReason;
    }

    public String relativePath() {
        return relativePath;
    }

    public String language() {
        return language;
    }

    public String fileKind() {
        return fileKind;
    }

    public String contentHash() {
        return contentHash;
    }

    public long sizeBytes() {
        return sizeBytes;
    }

    public boolean indexed() {
        return indexed;
    }

    public String skipReason() {
        return skipReason;
    }
}
