package com.devharnesskit.dhk.model.knowledge;

public final class KnowledgePackEntry {
    private final String packKey;
    private final String packVersion;
    private final String file;
    private final String ruleId;
    private final String domain;
    private final String severity;
    private final String[] appliesToProfiles;
    private final String[] appliesToActions;
    private final String[] riskFlags;
    private final int budgetLines;

    public KnowledgePackEntry(String packKey, String packVersion, String file, String ruleId,
                              String domain, String severity, String[] appliesToProfiles,
                              String[] appliesToActions, String[] riskFlags, int budgetLines) {
        this.packKey = value(packKey);
        this.packVersion = value(packVersion);
        this.file = value(file);
        this.ruleId = value(ruleId);
        this.domain = value(domain);
        this.severity = value(severity);
        this.appliesToProfiles = array(appliesToProfiles);
        this.appliesToActions = array(appliesToActions);
        this.riskFlags = array(riskFlags);
        this.budgetLines = budgetLines <= 0 ? 8 : budgetLines;
    }

    public String packKey() { return packKey; }
    public String packVersion() { return packVersion; }
    public String file() { return file; }
    public String ruleId() { return ruleId; }
    public String domain() { return domain; }
    public String severity() { return severity; }
    public String[] appliesToProfiles() { return appliesToProfiles; }
    public String[] appliesToActions() { return appliesToActions; }
    public String[] riskFlags() { return riskFlags; }
    public int budgetLines() { return budgetLines; }

    private static String value(String value) {
        return value == null ? "" : value.trim();
    }

    private static String[] array(String[] values) {
        return values == null ? new String[0] : values;
    }
}
