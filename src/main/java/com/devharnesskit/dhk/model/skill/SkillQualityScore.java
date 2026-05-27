package com.devharnesskit.dhk.model.skill;

public final class SkillQualityScore {
    private final String goalKey;
    private final int score;
    private final int gatePassRate;
    private final int evidenceCompleteness;
    private final int rollbackQuality;
    private final String decision;
    private final String summary;

    public SkillQualityScore(String goalKey, int score, int gatePassRate,
                             int evidenceCompleteness, int rollbackQuality, String summary) {
        this.goalKey = value(goalKey);
        this.score = score;
        this.gatePassRate = gatePassRate;
        this.evidenceCompleteness = evidenceCompleteness;
        this.rollbackQuality = rollbackQuality;
        this.decision = decision(score);
        this.summary = value(summary);
    }

    public String goalKey() { return goalKey; }
    public int score() { return score; }
    public int gatePassRate() { return gatePassRate; }
    public int evidenceCompleteness() { return evidenceCompleteness; }
    public int rollbackQuality() { return rollbackQuality; }
    public String decision() { return decision; }
    public String summary() { return summary; }

    private String decision(int score) {
        if (score >= 85) {
            return "strong";
        }
        if (score >= 70) {
            return "acceptable";
        }
        return "review_required";
    }

    private String value(String raw) {
        return raw == null ? "" : raw;
    }
}
