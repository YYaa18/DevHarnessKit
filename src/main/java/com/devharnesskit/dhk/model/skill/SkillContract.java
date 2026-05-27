package com.devharnesskit.dhk.model.skill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SkillContract {
    public static final String SCHEMA_VERSION = "skill-contract/v1-alpha";

    private final String skillKey;
    private final String version;
    private final String taskType;
    private final String riskLevel;
    private final String mode;
    private final String dataAccessLevel;
    private final List<String> allowedCommands;
    private final List<String> forbiddenCommands;
    private final String contractJson;
    private final String sourcePath;
    private final String sourceHash;
    private final String trustedSourceHash;
    private final String trustStatus;
    private final boolean trusted;
    private final String createdAt;
    private final String updatedAt;

    public SkillContract(String skillKey, String version, String taskType, String riskLevel, String mode,
                         String dataAccessLevel, List<String> allowedCommands, List<String> forbiddenCommands,
                         String contractJson, String sourcePath, boolean trusted, String createdAt,
                         String updatedAt) {
        this(skillKey, version, taskType, riskLevel, mode, dataAccessLevel, allowedCommands,
                forbiddenCommands, contractJson, sourcePath, "", trusted ? "" : "",
                trusted ? "trusted" : "unknown", trusted, createdAt, updatedAt);
    }

    public SkillContract(String skillKey, String version, String taskType, String riskLevel, String mode,
                         String dataAccessLevel, List<String> allowedCommands, List<String> forbiddenCommands,
                         String contractJson, String sourcePath, String sourceHash,
                         String trustedSourceHash, String trustStatus, boolean trusted,
                         String createdAt, String updatedAt) {
        this.skillKey = value(skillKey);
        this.version = value(version);
        this.taskType = value(taskType);
        this.riskLevel = value(riskLevel);
        this.mode = value(mode);
        this.dataAccessLevel = value(dataAccessLevel);
        this.allowedCommands = immutable(allowedCommands);
        this.forbiddenCommands = immutable(forbiddenCommands);
        this.contractJson = value(contractJson);
        this.sourcePath = value(sourcePath);
        this.sourceHash = value(sourceHash);
        this.trustedSourceHash = value(trustedSourceHash);
        this.trustStatus = normalizeTrustStatus(trustStatus, trusted);
        this.trusted = trusted;
        this.createdAt = value(createdAt);
        this.updatedAt = value(updatedAt);
    }

    public String schemaVersion() { return SCHEMA_VERSION; }
    public String skillKey() { return skillKey; }
    public String version() { return version; }
    public String taskType() { return taskType; }
    public String riskLevel() { return riskLevel; }
    public String mode() { return mode; }
    public String dataAccessLevel() { return dataAccessLevel; }
    public List<String> allowedCommands() { return allowedCommands; }
    public List<String> forbiddenCommands() { return forbiddenCommands; }
    public String contractJson() { return contractJson; }
    public String sourcePath() { return sourcePath; }
    public String sourceHash() { return sourceHash; }
    public String trustedSourceHash() { return trustedSourceHash; }
    public String trustStatus() { return trustStatus; }
    public boolean trusted() { return trusted; }
    public String createdAt() { return createdAt; }
    public String updatedAt() { return updatedAt; }

    public String allowedCommandsText() {
        return join(allowedCommands);
    }

    public String forbiddenCommandsText() {
        return join(forbiddenCommands);
    }

    private String join(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append('\n');
            }
            builder.append(values.get(i));
        }
        return builder.toString();
    }

    private List<String> immutable(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<String>(values));
    }

    private String value(String raw) {
        return raw == null ? "" : raw;
    }

    private String normalizeTrustStatus(String raw, boolean trusted) {
        String value = value(raw);
        if ("trusted".equals(value) || "untrusted".equals(value)
                || "unknown".equals(value) || "review_required".equals(value)) {
            return value;
        }
        return trusted ? "trusted" : "unknown";
    }
}
