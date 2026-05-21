package com.devharnesskit.dhk.model.spec;

public final class SpecDocument {
    private final long id;
    private final String changeKey;
    private final String documentType;
    private final String title;
    private final String content;
    private final String contentHash;
    private final String status;
    private final int version;
    private final String createdAt;
    private final String updatedAt;

    public SpecDocument(long id, String changeKey, String documentType, String title,
                        String content, String contentHash, String status, int version,
                        String createdAt, String updatedAt) {
        this.id = id;
        this.changeKey = changeKey;
        this.documentType = documentType;
        this.title = title;
        this.content = content;
        this.contentHash = contentHash;
        this.status = status;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long id() {
        return id;
    }

    public String changeKey() {
        return changeKey;
    }

    public String documentType() {
        return documentType;
    }

    public String title() {
        return title;
    }

    public String content() {
        return content;
    }

    public String contentHash() {
        return contentHash;
    }

    public String status() {
        return status;
    }

    public int version() {
        return version;
    }

    public String createdAt() {
        return createdAt;
    }

    public String updatedAt() {
        return updatedAt;
    }
}
