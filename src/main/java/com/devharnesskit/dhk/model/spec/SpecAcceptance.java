package com.devharnesskit.dhk.model.spec;

public final class SpecAcceptance {
    private final long id;
    private final String changeKey;
    private final String acceptanceKey;
    private final int acceptanceOrder;
    private final String description;
    private final String expectedResult;
    private final String status;
    private final String evidence;
    private final String createdAt;
    private final String updatedAt;
    private final String verifiedAt;

    public SpecAcceptance(long id, String changeKey, String acceptanceKey, int acceptanceOrder,
                          String description, String expectedResult, String status,
                          String evidence, String createdAt, String updatedAt, String verifiedAt) {
        this.id = id;
        this.changeKey = changeKey;
        this.acceptanceKey = acceptanceKey;
        this.acceptanceOrder = acceptanceOrder;
        this.description = description;
        this.expectedResult = expectedResult;
        this.status = status;
        this.evidence = evidence;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.verifiedAt = verifiedAt;
    }

    public long id() {
        return id;
    }

    public String changeKey() {
        return changeKey;
    }

    public String acceptanceKey() {
        return acceptanceKey;
    }

    public int acceptanceOrder() {
        return acceptanceOrder;
    }

    public String description() {
        return description;
    }

    public String expectedResult() {
        return expectedResult;
    }

    public String status() {
        return status;
    }

    public String evidence() {
        return evidence;
    }

    public String createdAt() {
        return createdAt;
    }

    public String updatedAt() {
        return updatedAt;
    }

    public String verifiedAt() {
        return verifiedAt;
    }
}
