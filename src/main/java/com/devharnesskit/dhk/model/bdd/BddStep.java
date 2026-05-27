package com.devharnesskit.dhk.model.bdd;

public final class BddStep {
    private final long id;
    private final String scenarioKey;
    private final int stepOrder;
    private final String stepType;
    private final String stepText;
    private final String normalizedText;
    private final String createdAt;

    public BddStep(long id, String scenarioKey, int stepOrder, String stepType,
                   String stepText, String normalizedText, String createdAt) {
        this.id = id;
        this.scenarioKey = scenarioKey;
        this.stepOrder = stepOrder;
        this.stepType = stepType;
        this.stepText = stepText;
        this.normalizedText = normalizedText;
        this.createdAt = createdAt;
    }

    public long id() {
        return id;
    }

    public String scenarioKey() {
        return scenarioKey;
    }

    public int stepOrder() {
        return stepOrder;
    }

    public String stepType() {
        return stepType;
    }

    public String stepText() {
        return stepText;
    }

    public String normalizedText() {
        return normalizedText;
    }

    public String createdAt() {
        return createdAt;
    }
}
