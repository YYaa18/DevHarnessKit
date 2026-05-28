package com.devharnesskit.dhk.model.knowledge;

public final class KnowledgeSnippet {
    private final String packRef;
    private final String ruleId;
    private final String domain;
    private final String severity;
    private final String fullRef;
    private final String[] summaryLines;
    private final String[] evidenceChecklist;

    public KnowledgeSnippet(String packRef, String ruleId, String domain, String severity,
                            String fullRef, String[] summaryLines, String[] evidenceChecklist) {
        this.packRef = value(packRef);
        this.ruleId = value(ruleId);
        this.domain = value(domain);
        this.severity = value(severity);
        this.fullRef = value(fullRef);
        this.summaryLines = array(summaryLines);
        this.evidenceChecklist = array(evidenceChecklist);
    }

    public String packRef() { return packRef; }
    public String ruleId() { return ruleId; }
    public String domain() { return domain; }
    public String severity() { return severity; }
    public String fullRef() { return fullRef; }
    public String[] summaryLines() { return summaryLines; }
    public String[] evidenceChecklist() { return evidenceChecklist; }

    private static String value(String value) {
        return value == null ? "" : value.trim();
    }

    private static String[] array(String[] values) {
        return values == null ? new String[0] : values;
    }
}
