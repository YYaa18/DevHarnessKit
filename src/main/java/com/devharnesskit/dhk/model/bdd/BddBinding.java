package com.devharnesskit.dhk.model.bdd;

public final class BddBinding {
    private final long id;
    private final String scenarioKey;
    private final String bindingType;
    private final String bindingKey;
    private final String relation;
    private final String metadata;
    private final String createdAt;

    public BddBinding(long id, String scenarioKey, String bindingType, String bindingKey,
                      String relation, String metadata, String createdAt) {
        this.id = id;
        this.scenarioKey = value(scenarioKey);
        this.bindingType = value(bindingType);
        this.bindingKey = value(bindingKey);
        this.relation = value(relation);
        this.metadata = value(metadata);
        this.createdAt = value(createdAt);
    }

    public long id() {
        return id;
    }

    public String scenarioKey() {
        return scenarioKey;
    }

    public String bindingType() {
        return bindingType;
    }

    public String bindingKey() {
        return bindingKey;
    }

    public String relation() {
        return relation;
    }

    public String metadata() {
        return metadata;
    }

    public String createdAt() {
        return createdAt;
    }

    private String value(String raw) {
        return raw == null ? "" : raw;
    }
}
