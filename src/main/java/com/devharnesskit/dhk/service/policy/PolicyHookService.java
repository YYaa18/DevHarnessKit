package com.devharnesskit.dhk.service.policy;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.model.goal.GoalStep;
import com.devharnesskit.dhk.model.policy.DevHarnessPolicy;

import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

public final class PolicyHookService {
    private final DevHarnessPolicyService policyService;

    public PolicyHookService() {
        this(new DevHarnessPolicyService());
    }

    PolicyHookService(DevHarnessPolicyService policyService) {
        this.policyService = policyService;
    }

    public void requireGoalCompleteAllowed(Path projectRoot, List<GoalStep> steps) {
        if (!policyService.hasPolicy(projectRoot)) {
            return;
        }
        DevHarnessPolicy policy = policyService.load(projectRoot);
        PolicyDecision command = commandDecision(policy, "goal complete");
        if (!command.allowed()) {
            throw new PolicyViolationException("Policy blocked goal complete: " + command.reason());
        }
        for (GoalStep step : steps) {
            requireChangedFilesAllowed(policy, "goal complete", step.changedFiles(), false);
        }
    }

    public void requireGoalStepAllowed(Path projectRoot, String changedFiles) {
        if (!policyService.hasPolicy(projectRoot)) {
            return;
        }
        DevHarnessPolicy policy = policyService.load(projectRoot);
        PolicyDecision command = commandDecision(policy, "goal step");
        if (!command.allowed()) {
            throw new PolicyViolationException("Policy blocked goal step: " + command.reason());
        }
        requireChangedFilesAllowed(policy, "goal step", changedFiles, true);
    }

    public void requireGoalCheckAllowed(Path projectRoot) {
        if (!policyService.hasPolicy(projectRoot)) {
            return;
        }
        DevHarnessPolicy policy = policyService.load(projectRoot);
        PolicyDecision command = commandDecision(policy, "goal check");
        if (!command.allowed()) {
            throw new PolicyViolationException("Policy blocked goal check: " + command.reason());
        }
    }

    public void requireDbSqlAllowed(Path projectRoot, Args args, boolean dryRun) {
        if (!policyService.hasPolicy(projectRoot)) {
            return;
        }
        DevHarnessPolicy policy = policyService.load(projectRoot);
        PolicyDecision command = commandDecision(policy, "db sql");
        if (!command.allowed()) {
            throw new PolicyViolationException("Policy blocked db sql: " + command.reason());
        }
        if (!dryRun && policy.dbSqlRequiresExplicitRequest()
                && !args.hasFlag("i-understand-db-readonly-risk")) {
            throw new PolicyViolationException("Policy blocked db sql: "
                    + "db_sql_requires_explicit_request requires --i-understand-db-readonly-risk");
        }
    }

    public void requireGraphImpactAllowed(Path projectRoot, Args args) {
        boolean hasPolicy = policyService.hasPolicy(projectRoot);
        DevHarnessPolicy policy = policyService.load(projectRoot);
        if (hasPolicy) {
            PolicyDecision command = commandDecision(policy, "graph impact");
            if (!command.allowed()) {
                throw new PolicyViolationException("Policy blocked graph impact: " + command.reason());
            }
        }
        if (args.hasFlag("allow-stale") && policy.graphAllowStaleRequiresApproval()
                && args.option("allow-stale-evidence", "").trim().length() == 0) {
            throw new PolicyViolationException("Policy blocked graph impact: "
                    + "graph_allow_stale_requires_approval requires --allow-stale-evidence");
        }
    }

    public void requireContextExportAllowed(Path projectRoot, Path out, String content,
                                            List<String> sensitiveMatches) {
        if (!policyService.hasPolicy(projectRoot)) {
            return;
        }
        DevHarnessPolicy policy = policyService.load(projectRoot);
        String relative = relative(projectRoot, out);
        if (policy.contextExportAllowedFiles().length > 0
                && !matchesAny(relative, policy.contextExportAllowedFiles())) {
            throw new PolicyViolationException("Policy blocked context export: output path is not allowed: "
                    + relative);
        }
        if (matchesAny(relative, policy.contextExportForbiddenFiles())) {
            throw new PolicyViolationException("Policy blocked context export: output path is forbidden: "
                    + relative);
        }
        if (policy.contextExportBlockOnSensitive() && sensitiveMatches != null && !sensitiveMatches.isEmpty()) {
            throw new PolicyViolationException("Policy blocked context export: sensitive matches "
                    + sensitiveMatches);
        }
    }

    public static String humanCheckpointBlocker(DevHarnessPolicy policy, boolean approved,
                                                String checkpointType) {
        if (policy == null || !policy.humanCheckpointRequired() || approved) {
            return "";
        }
        String type = checkpointType == null || checkpointType.trim().length() == 0
                ? policy.humanCheckpointType() : checkpointType.trim();
        return "human_checkpoint_required: type=" + type + " status=missing_approved";
    }

    private PolicyDecision commandDecision(DevHarnessPolicy policy, String command) {
        if (matchesCommand(command, policy.forbiddenDhkCommands())) {
            return PolicyDecision.block("command is forbidden by policy: " + command);
        }
        if (policy.allowedDhkCommands().length > 0 && !matchesCommand(command, policy.allowedDhkCommands())) {
            return PolicyDecision.block("command is not in allowed_dhk_commands: " + command);
        }
        return PolicyDecision.allow();
    }

    private boolean matchesCommand(String command, String[] prefixes) {
        for (String prefix : prefixes) {
            if (command.equals(prefix) || command.startsWith(prefix + " ")) {
                return true;
            }
        }
        return false;
    }

    private void requireChangedFilesAllowed(DevHarnessPolicy policy, String hook, String changedFiles,
                                            boolean enforceAllowedWritePaths) {
        String[] files = splitChangedFiles(changedFiles);
        for (String file : files) {
            if (matchesAny(file, policy.protectedFiles())) {
                throw new PolicyViolationException("Policy blocked " + hook + ": protected file changed: "
                        + file);
            }
            if (enforceAllowedWritePaths && policy.allowedWritePaths().length > 0
                    && !matchesAny(file, policy.allowedWritePaths())) {
                throw new PolicyViolationException("Policy blocked " + hook
                        + ": changed file is outside allowed_write_paths: " + file);
            }
        }
    }

    private String[] splitChangedFiles(String changedFiles) {
        if (changedFiles == null || changedFiles.trim().length() == 0) {
            return new String[0];
        }
        String normalized = changedFiles.replace('\n', ',').replace(';', ',');
        String[] parts = normalized.split(",");
        java.util.List<String> result = new java.util.ArrayList<String>();
        for (String part : parts) {
            String file = part.trim();
            if (file.length() > 0) {
                result.add(file);
            }
        }
        return result.toArray(new String[result.size()]);
    }

    private String relative(Path projectRoot, Path path) {
        Path absoluteRoot = projectRoot.toAbsolutePath().normalize();
        Path absolutePath = path.toAbsolutePath().normalize();
        if (absolutePath.startsWith(absoluteRoot)) {
            return normalize(absoluteRoot.relativize(absolutePath).toString());
        }
        return normalize(absolutePath.toString());
    }

    private boolean matchesAny(String value, String[] globs) {
        String normalized = normalize(value);
        for (String glob : globs) {
            if (globMatches(normalized, normalize(glob))) {
                return true;
            }
        }
        return false;
    }

    private boolean globMatches(String value, String glob) {
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < glob.length(); i++) {
            char ch = glob.charAt(i);
            if (ch == '*') {
                if (i + 1 < glob.length() && glob.charAt(i + 1) == '*') {
                    regex.append(".*");
                    i++;
                } else {
                    regex.append("[^/]*");
                }
            } else {
                regex.append(Pattern.quote(String.valueOf(ch)));
            }
        }
        return value.matches(regex.toString());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replace('\\', '/');
    }
}
