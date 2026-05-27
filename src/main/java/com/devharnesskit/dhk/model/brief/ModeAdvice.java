package com.devharnesskit.dhk.model.brief;

public final class ModeAdvice {
    public static final String PATCH = "patch";
    public static final String STANDARD = "standard";
    public static final String STRICT = "strict";
    public static final String ANALYZE_ONLY = "analyze_only";
    public static final String ASK = "ask";

    private final String recommendation;
    private final String confidence;
    private final int riskScore;
    private final int scoreRiskSecurity;
    private final int scoreRiskDb;
    private final int scoreRiskConfig;
    private final int scoreRiskCrossModule;
    private final int scoreTestGap;
    private final int scoreGraphScope;
    private final String[] riskFlags;
    private final String[] why;
    private final String[] softEscalationRules;
    private final String[] hardEscalationRules;
    private final boolean confirmationRequired;
    private final String confirmationReason;
    private final boolean safeToStart;
    private final String profileKey;

    public ModeAdvice(String recommendation, String confidence, int riskScore,
                      int scoreRiskSecurity, int scoreRiskDb, int scoreRiskConfig,
                      int scoreRiskCrossModule, int scoreTestGap, int scoreGraphScope,
                      String[] riskFlags, String[] why, String[] softEscalationRules,
                      String[] hardEscalationRules, boolean confirmationRequired,
                      String confirmationReason, boolean safeToStart, String profileKey) {
        this.recommendation = value(recommendation, ASK);
        this.confidence = value(confidence, "low");
        this.riskScore = riskScore;
        this.scoreRiskSecurity = scoreRiskSecurity;
        this.scoreRiskDb = scoreRiskDb;
        this.scoreRiskConfig = scoreRiskConfig;
        this.scoreRiskCrossModule = scoreRiskCrossModule;
        this.scoreTestGap = scoreTestGap;
        this.scoreGraphScope = scoreGraphScope;
        this.riskFlags = array(riskFlags);
        this.why = array(why);
        this.softEscalationRules = array(softEscalationRules);
        this.hardEscalationRules = array(hardEscalationRules);
        this.confirmationRequired = confirmationRequired;
        this.confirmationReason = value(confirmationReason, "none");
        this.safeToStart = safeToStart;
        this.profileKey = value(profileKey, "java-api-change");
    }

    public String recommendation() { return recommendation; }
    public String modeId() { return recommendation; }
    public String confidence() { return confidence; }
    public int riskScore() { return riskScore; }
    public int scoreRiskSecurity() { return scoreRiskSecurity; }
    public int scoreRiskDb() { return scoreRiskDb; }
    public int scoreRiskConfig() { return scoreRiskConfig; }
    public int scoreRiskCrossModule() { return scoreRiskCrossModule; }
    public int scoreTestGap() { return scoreTestGap; }
    public int scoreGraphScope() { return scoreGraphScope; }
    public String[] riskFlags() { return riskFlags; }
    public String[] why() { return why; }
    public String[] softEscalationRules() { return softEscalationRules; }
    public String[] hardEscalationRules() { return hardEscalationRules; }
    public boolean confirmationRequired() { return confirmationRequired; }
    public String confirmationReason() { return confirmationReason; }
    public boolean safeToStart() { return safeToStart; }
    public String profileKey() { return profileKey; }

    private static String value(String value, String fallback) {
        return value == null || value.trim().length() == 0 ? fallback : value.trim();
    }

    private static String[] array(String[] values) {
        return values == null ? new String[0] : values;
    }
}
