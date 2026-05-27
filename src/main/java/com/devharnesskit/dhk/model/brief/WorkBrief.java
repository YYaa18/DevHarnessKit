package com.devharnesskit.dhk.model.brief;

public final class WorkBrief {
    private final String briefId;
    private final String recommendationId;
    private final String modeId;
    private final String taskSummary;
    private final String recommendation;
    private final String confidence;
    private final String[] why;
    private final String[] riskFlags;
    private final String[] expectedWork;
    private final String[] willNotDo;
    private final String[] userChoices;
    private final boolean confirmationRequired;
    private final String confirmationReason;
    private final boolean safeToStart;
    private final int riskScore;

    public WorkBrief(String briefId, String recommendationId, String modeId,
                     String taskSummary, String recommendation, String confidence,
                     String[] why, String[] riskFlags, String[] expectedWork,
                     String[] willNotDo, String[] userChoices, boolean confirmationRequired,
                     String confirmationReason, boolean safeToStart, int riskScore) {
        this.briefId = value(briefId);
        this.recommendationId = value(recommendationId);
        this.modeId = value(modeId);
        this.taskSummary = value(taskSummary);
        this.recommendation = value(recommendation);
        this.confidence = value(confidence);
        this.why = array(why);
        this.riskFlags = array(riskFlags);
        this.expectedWork = array(expectedWork);
        this.willNotDo = array(willNotDo);
        this.userChoices = array(userChoices);
        this.confirmationRequired = confirmationRequired;
        this.confirmationReason = value(confirmationReason);
        this.safeToStart = safeToStart;
        this.riskScore = riskScore;
    }

    public String briefId() { return briefId; }
    public String recommendationId() { return recommendationId; }
    public String modeId() { return modeId; }
    public String taskSummary() { return taskSummary; }
    public String recommendation() { return recommendation; }
    public String confidence() { return confidence; }
    public String[] why() { return why; }
    public String[] riskFlags() { return riskFlags; }
    public String[] expectedWork() { return expectedWork; }
    public String[] willNotDo() { return willNotDo; }
    public String[] userChoices() { return userChoices; }
    public boolean confirmationRequired() { return confirmationRequired; }
    public String confirmationReason() { return confirmationReason; }
    public boolean safeToStart() { return safeToStart; }
    public int riskScore() { return riskScore; }

    private static String value(String value) {
        return value == null ? "" : value.trim();
    }

    private static String[] array(String[] values) {
        return values == null ? new String[0] : values;
    }
}
