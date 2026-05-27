package com.devharnesskit.dhk.model.policy;

public final class DevHarnessPolicy {
    private final String mode;
    private final String[] allowedDhkCommands;
    private final String[] forbiddenDhkCommands;
    private final String[] protectedFiles;
    private final String[] allowedWritePaths;
    private final boolean dbSqlRequiresExplicitRequest;
    private final boolean dbRequireReadonlyCredentials;
    private final String[] dbAllowedEnvironments;
    private final boolean contextExportRequireSensitiveScan;
    private final boolean contextExportBlockOnSensitive;
    private final String[] contextExportAllowedFiles;
    private final String[] contextExportForbiddenFiles;
    private final boolean graphAllowStaleRequiresApproval;
    private final boolean skillContractRequired;
    private final String skillKey;
    private final boolean skillTrustRequiredForHighRisk;
    private final String skillTrustOverrideCheckpointType;
    private final boolean humanCheckpointRequired;
    private final String humanCheckpointType;

    public DevHarnessPolicy(String mode, String[] allowedDhkCommands, String[] forbiddenDhkCommands,
                            String[] protectedFiles, String[] allowedWritePaths,
                            boolean dbSqlRequiresExplicitRequest, boolean dbRequireReadonlyCredentials,
                            String[] dbAllowedEnvironments, boolean contextExportRequireSensitiveScan,
                            boolean contextExportBlockOnSensitive, String[] contextExportAllowedFiles,
                            String[] contextExportForbiddenFiles) {
        this(mode, allowedDhkCommands, forbiddenDhkCommands, protectedFiles, allowedWritePaths,
                dbSqlRequiresExplicitRequest, dbRequireReadonlyCredentials, dbAllowedEnvironments,
                contextExportRequireSensitiveScan, contextExportBlockOnSensitive, contextExportAllowedFiles,
                contextExportForbiddenFiles, true, false, "", false, "before_complete");
    }

    public DevHarnessPolicy(String mode, String[] allowedDhkCommands, String[] forbiddenDhkCommands,
                            String[] protectedFiles, String[] allowedWritePaths,
                            boolean dbSqlRequiresExplicitRequest, boolean dbRequireReadonlyCredentials,
                            String[] dbAllowedEnvironments, boolean contextExportRequireSensitiveScan,
                            boolean contextExportBlockOnSensitive, String[] contextExportAllowedFiles,
                            String[] contextExportForbiddenFiles, boolean graphAllowStaleRequiresApproval) {
        this(mode, allowedDhkCommands, forbiddenDhkCommands, protectedFiles, allowedWritePaths,
                dbSqlRequiresExplicitRequest, dbRequireReadonlyCredentials, dbAllowedEnvironments,
                contextExportRequireSensitiveScan, contextExportBlockOnSensitive, contextExportAllowedFiles,
                contextExportForbiddenFiles, graphAllowStaleRequiresApproval, false, "",
                false, "before_complete");
    }

    public DevHarnessPolicy(String mode, String[] allowedDhkCommands, String[] forbiddenDhkCommands,
                            String[] protectedFiles, String[] allowedWritePaths,
                            boolean dbSqlRequiresExplicitRequest, boolean dbRequireReadonlyCredentials,
                            String[] dbAllowedEnvironments, boolean contextExportRequireSensitiveScan,
                            boolean contextExportBlockOnSensitive, String[] contextExportAllowedFiles,
                            String[] contextExportForbiddenFiles, boolean graphAllowStaleRequiresApproval,
                            boolean skillContractRequired, String skillKey) {
        this(mode, allowedDhkCommands, forbiddenDhkCommands, protectedFiles, allowedWritePaths,
                dbSqlRequiresExplicitRequest, dbRequireReadonlyCredentials, dbAllowedEnvironments,
                contextExportRequireSensitiveScan, contextExportBlockOnSensitive, contextExportAllowedFiles,
                contextExportForbiddenFiles, graphAllowStaleRequiresApproval, skillContractRequired, skillKey,
                false, "skill_trust_override", false, "before_complete");
    }

    public DevHarnessPolicy(String mode, String[] allowedDhkCommands, String[] forbiddenDhkCommands,
                            String[] protectedFiles, String[] allowedWritePaths,
                            boolean dbSqlRequiresExplicitRequest, boolean dbRequireReadonlyCredentials,
                            String[] dbAllowedEnvironments, boolean contextExportRequireSensitiveScan,
                            boolean contextExportBlockOnSensitive, String[] contextExportAllowedFiles,
                            String[] contextExportForbiddenFiles, boolean graphAllowStaleRequiresApproval,
                            boolean skillContractRequired, String skillKey,
                            boolean humanCheckpointRequired, String humanCheckpointType) {
        this(mode, allowedDhkCommands, forbiddenDhkCommands, protectedFiles, allowedWritePaths,
                dbSqlRequiresExplicitRequest, dbRequireReadonlyCredentials, dbAllowedEnvironments,
                contextExportRequireSensitiveScan, contextExportBlockOnSensitive, contextExportAllowedFiles,
                contextExportForbiddenFiles, graphAllowStaleRequiresApproval, skillContractRequired, skillKey,
                false, "skill_trust_override", humanCheckpointRequired, humanCheckpointType);
    }

    public DevHarnessPolicy(String mode, String[] allowedDhkCommands, String[] forbiddenDhkCommands,
                            String[] protectedFiles, String[] allowedWritePaths,
                            boolean dbSqlRequiresExplicitRequest, boolean dbRequireReadonlyCredentials,
                            String[] dbAllowedEnvironments, boolean contextExportRequireSensitiveScan,
                            boolean contextExportBlockOnSensitive, String[] contextExportAllowedFiles,
                            String[] contextExportForbiddenFiles, boolean graphAllowStaleRequiresApproval,
                            boolean skillContractRequired, String skillKey,
                            boolean skillTrustRequiredForHighRisk, String skillTrustOverrideCheckpointType,
                            boolean humanCheckpointRequired, String humanCheckpointType) {
        this.mode = value(mode, "guided");
        this.allowedDhkCommands = array(allowedDhkCommands);
        this.forbiddenDhkCommands = array(forbiddenDhkCommands);
        this.protectedFiles = array(protectedFiles);
        this.allowedWritePaths = array(allowedWritePaths);
        this.dbSqlRequiresExplicitRequest = dbSqlRequiresExplicitRequest;
        this.dbRequireReadonlyCredentials = dbRequireReadonlyCredentials;
        this.dbAllowedEnvironments = array(dbAllowedEnvironments);
        this.contextExportRequireSensitiveScan = contextExportRequireSensitiveScan;
        this.contextExportBlockOnSensitive = contextExportBlockOnSensitive;
        this.contextExportAllowedFiles = array(contextExportAllowedFiles);
        this.contextExportForbiddenFiles = array(contextExportForbiddenFiles);
        this.graphAllowStaleRequiresApproval = graphAllowStaleRequiresApproval;
        this.skillContractRequired = skillContractRequired;
        this.skillKey = value(skillKey, "");
        this.skillTrustRequiredForHighRisk = skillTrustRequiredForHighRisk;
        this.skillTrustOverrideCheckpointType = value(skillTrustOverrideCheckpointType, "skill_trust_override");
        this.humanCheckpointRequired = humanCheckpointRequired;
        this.humanCheckpointType = value(humanCheckpointType, "before_complete");
    }

    public static DevHarnessPolicy defaults() {
        return new DevHarnessPolicy("guided", new String[0], new String[0],
                new String[0], new String[0], true, true, new String[0],
                true, true, new String[0], new String[0]);
    }

    public String mode() { return mode; }
    public String[] allowedDhkCommands() { return allowedDhkCommands; }
    public String[] forbiddenDhkCommands() { return forbiddenDhkCommands; }
    public String[] protectedFiles() { return protectedFiles; }
    public String[] allowedWritePaths() { return allowedWritePaths; }
    public boolean dbSqlRequiresExplicitRequest() { return dbSqlRequiresExplicitRequest; }
    public boolean dbRequireReadonlyCredentials() { return dbRequireReadonlyCredentials; }
    public String[] dbAllowedEnvironments() { return dbAllowedEnvironments; }
    public boolean contextExportRequireSensitiveScan() { return contextExportRequireSensitiveScan; }
    public boolean contextExportBlockOnSensitive() { return contextExportBlockOnSensitive; }
    public String[] contextExportAllowedFiles() { return contextExportAllowedFiles; }
    public String[] contextExportForbiddenFiles() { return contextExportForbiddenFiles; }
    public boolean graphAllowStaleRequiresApproval() { return graphAllowStaleRequiresApproval; }
    public boolean skillContractRequired() { return skillContractRequired; }
    public String skillKey() { return skillKey; }
    public boolean skillTrustRequiredForHighRisk() { return skillTrustRequiredForHighRisk; }
    public String skillTrustOverrideCheckpointType() { return skillTrustOverrideCheckpointType; }
    public boolean humanCheckpointRequired() { return humanCheckpointRequired; }
    public String humanCheckpointType() { return humanCheckpointType; }

    private static String value(String value, String defaultValue) {
        if (value == null || value.trim().length() == 0) {
            return defaultValue;
        }
        return value.trim();
    }

    private static String[] array(String[] values) {
        return values == null ? new String[0] : values;
    }
}
