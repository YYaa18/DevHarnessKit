package com.devharnesskit.dhk.model.knowledge;

public final class KnowledgePack {
    private final String packKey;
    private final String title;
    private final String version;
    private final String status;
    private final String sourcePolicy;
    private final KnowledgePackEntry[] entries;

    public KnowledgePack(String packKey, String title, String version, String status,
                         String sourcePolicy, KnowledgePackEntry[] entries) {
        this.packKey = value(packKey);
        this.title = value(title);
        this.version = value(version);
        this.status = value(status);
        this.sourcePolicy = value(sourcePolicy);
        this.entries = entries == null ? new KnowledgePackEntry[0] : entries;
    }

    public String packKey() { return packKey; }
    public String title() { return title; }
    public String version() { return version; }
    public String status() { return status; }
    public String sourcePolicy() { return sourcePolicy; }
    public KnowledgePackEntry[] entries() { return entries; }

    private static String value(String value) {
        return value == null ? "" : value.trim();
    }
}
