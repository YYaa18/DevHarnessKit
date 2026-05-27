package com.devharnesskit.dhk.model.skill;

public final class SkillDisciplineGateResult {
    private final String gateKey;
    private final String status;
    private final String summary;
    private final String output;

    public SkillDisciplineGateResult(String gateKey, String status, String summary, String output) {
        this.gateKey = value(gateKey);
        this.status = value(status);
        this.summary = value(summary);
        this.output = value(output);
    }

    public String gateKey() {
        return gateKey;
    }

    public String status() {
        return status;
    }

    public String summary() {
        return summary;
    }

    public String output() {
        return output;
    }

    public boolean passed() {
        return "passed".equals(status);
    }

    private String value(String raw) {
        return raw == null ? "" : raw;
    }
}
