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

    public DevHarnessPolicy(String mode, String[] allowedDhkCommands, String[] forbiddenDhkCommands,
                            String[] protectedFiles, String[] allowedWritePaths,
                            boolean dbSqlRequiresExplicitRequest, boolean dbRequireReadonlyCredentials,
                            String[] dbAllowedEnvironments, boolean contextExportRequireSensitiveScan,
                            boolean contextExportBlockOnSensitive, String[] contextExportAllowedFiles,
                            String[] contextExportForbiddenFiles) {
        this(mode, allowedDhkCommands, forbiddenDhkCommands, protectedFiles, allowedWritePaths,
                dbSqlRequiresExplicitRequest, dbRequireReadonlyCredentials, dbAllowedEnvironments,
                contextExportRequireSensitiveScan, contextExportBlockOnSensitive, contextExportAllowedFiles,
                contextExportForbiddenFiles, true);
    }

    public DevHarnessPolicy(String mode, String[] allowedDhkCommands, String[] forbiddenDhkCommands,
                            String[] protectedFiles, String[] allowedWritePaths,
                            boolean dbSqlRequiresExplicitRequest, boolean dbRequireReadonlyCredentials,
                            String[] dbAllowedEnvironments, boolean contextExportRequireSensitiveScan,
                            boolean contextExportBlockOnSensitive, String[] contextExportAllowedFiles,
                            String[] contextExportForbiddenFiles, boolean graphAllowStaleRequiresApproval) {
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
