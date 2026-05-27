package com.devharnesskit.dhk.model.skill;

public final class SkillEvaluationReport {
    private final String goalKey;
    private final String group;
    private final int baselineScore;
    private final SkillQualityScore qualityScore;
    private final boolean skillContractPresent;
    private final String skillKey;
    private final String trustStatus;
    private final boolean trusted;

    public SkillEvaluationReport(String goalKey, String group, int baselineScore,
                                 SkillQualityScore qualityScore, boolean skillContractPresent,
                                 String skillKey, String trustStatus, boolean trusted) {
        this.goalKey = value(goalKey);
        this.group = value(group);
        this.baselineScore = baselineScore;
        this.qualityScore = qualityScore;
        this.skillContractPresent = skillContractPresent;
        this.skillKey = value(skillKey);
        this.trustStatus = value(trustStatus);
        this.trusted = trusted;
    }

    public String goalKey() { return goalKey; }
    public String group() { return group; }
    public int baselineScore() { return baselineScore; }
    public SkillQualityScore qualityScore() { return qualityScore; }
    public boolean skillContractPresent() { return skillContractPresent; }
    public String skillKey() { return skillKey; }
    public String trustStatus() { return trustStatus; }
    public boolean trusted() { return trusted; }

    public int dqiScore() {
        return qualityScore == null ? 0 : qualityScore.score();
    }

    public int dqiDelta() {
        return baselineScore < 0 ? 0 : dqiScore() - baselineScore;
    }

    private String value(String raw) {
        return raw == null ? "" : raw;
    }
}
