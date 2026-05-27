package com.devharnesskit.dhk.service.skill;

import com.devharnesskit.dhk.model.skill.SkillContract;
import com.devharnesskit.dhk.model.skill.SkillContractLintIssue;
import com.devharnesskit.dhk.model.skill.SkillContractLintResult;
import com.devharnesskit.dhk.util.PathUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SkillContractLintService {
    private final SkillContractService contractService;

    public SkillContractLintService() {
        this(new SkillContractService());
    }

    public SkillContractLintService(SkillContractService contractService) {
        this.contractService = contractService;
    }

    public SkillContractLintResult lint(Path skillDirectory) {
        Path contractPath = skillDirectory.resolve(PathUtil.CONTRACT_JSON);
        List<SkillContractLintIssue> issues = new ArrayList<SkillContractLintIssue>();
        if (!Files.isRegularFile(contractPath)) {
            issues.add(issue("missing", "contract.json", "contract.json was not found",
                    "Create .agents/skills/<skill-key>/contract.json"));
            return new SkillContractLintResult(skillDirectory.getFileName().toString(),
                    contractPath.toAbsolutePath().normalize().toString(), null, issues);
        }

        String json;
        try {
            json = new String(Files.readAllBytes(contractPath), "UTF-8");
        } catch (IOException ex) {
            issues.add(issue("invalid", "contract.json", "contract.json could not be read: " + ex.getMessage(),
                    "Check file permissions and encoding"));
            return new SkillContractLintResult(skillDirectory.getFileName().toString(),
                    contractPath.toAbsolutePath().normalize().toString(), null, issues);
        }

        addMissing(issues, json, "skill_key", "Add a stable skill_key");
        addMissing(issues, json, "version", "Add a skill contract version");
        addMissing(issues, json, "task_type", "Add the primary task_type");
        addMissing(issues, json, "data_access_level", "Use none, metadata, context, or raw");
        addMissingList(issues, json, "allowed_commands", "List commands this skill may run");
        addMissingList(issues, json, "forbidden_commands", "List commands this skill must not run");

        String dataAccess = stringValue(json, "data_access_level");
        if (dataAccess.length() > 0 && !isAllowedDataAccessLevel(dataAccess)) {
            issues.add(issue("invalid", "data_access_level",
                    "data_access_level must be one of none, metadata, context, raw",
                    "Set data_access_level to the minimum required access level"));
        }

        List<String> allowed = listValue(json, "allowed_commands");
        List<String> forbidden = listValue(json, "forbidden_commands");
        List<String> declared = listValue(json, "declared_commands");
        for (String command : allowed) {
            if (matchesForbidden(command, forbidden)) {
                issues.add(issue("forbidden", "allowed_commands",
                        "allowed command conflicts with forbidden_commands: " + command,
                        "Remove the command from allowed_commands or narrow the forbidden prefix"));
            }
        }
        for (String command : declared) {
            if (matchesForbidden(command, forbidden)) {
                issues.add(issue("forbidden", "declared_commands",
                        "declared command is forbidden by contract: " + command,
                        "Remove the command from skill scripts/protocol or change the contract"));
            } else if (!matchesAllowed(command, allowed)) {
                issues.add(issue("forbidden", "declared_commands",
                        "declared command is outside allowed_commands: " + command,
                        "Add the command to allowed_commands only if the skill should be allowed to use it"));
            }
        }

        SkillContract contract = null;
        if (issues.isEmpty()) {
            contract = contractService.load(skillDirectory);
        }
        String skillKey = stringValue(json, "skill_key");
        if (skillKey.length() == 0) {
            skillKey = skillDirectory.getFileName().toString();
        }
        return new SkillContractLintResult(skillKey, contractPath.toAbsolutePath().normalize().toString(),
                contract, issues);
    }

    private void addMissing(List<SkillContractLintIssue> issues, String json, String field, String suggestion) {
        if (stringValue(json, field).length() == 0) {
            issues.add(issue("missing", field, "Missing required field: " + field, suggestion));
        }
    }

    private void addMissingList(List<SkillContractLintIssue> issues, String json, String field, String suggestion) {
        if (listValue(json, field).isEmpty()) {
            issues.add(issue("missing", field, "Missing required command list: " + field, suggestion));
        }
    }

    private boolean isAllowedDataAccessLevel(String value) {
        return "none".equals(value)
                || "metadata".equals(value)
                || "context".equals(value)
                || "raw".equals(value);
    }

    private boolean matchesForbidden(String command, List<String> forbidden) {
        for (String prefix : forbidden) {
            if (command.equals(prefix) || command.startsWith(prefix + " ")) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesAllowed(String command, List<String> allowed) {
        if (allowed.isEmpty()) {
            return false;
        }
        for (String prefix : allowed) {
            if (command.equals(prefix) || command.startsWith(prefix + " ")) {
                return true;
            }
        }
        return false;
    }

    private SkillContractLintIssue issue(String category, String field, String message, String suggestion) {
        return new SkillContractLintIssue(category, field, message, suggestion);
    }

    private String stringValue(String json, String key) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"",
                Pattern.DOTALL).matcher(json == null ? "" : json);
        if (!matcher.find()) {
            return "";
        }
        return unescape(matcher.group(1)).trim();
    }

    private List<String> listValue(String json, String key) {
        String scalar = stringValue(json, key);
        if (scalar.length() > 0) {
            return splitList(scalar);
        }
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\\[(.*?)\\]",
                Pattern.DOTALL).matcher(json == null ? "" : json);
        List<String> result = new ArrayList<String>();
        if (!matcher.find()) {
            return result;
        }
        Matcher itemMatcher = Pattern.compile("\"((?:\\\\.|[^\"])*)\"").matcher(matcher.group(1));
        while (itemMatcher.find()) {
            String item = unescape(itemMatcher.group(1)).trim();
            if (item.length() > 0) {
                result.add(item);
            }
        }
        return result;
    }

    private List<String> splitList(String value) {
        List<String> result = new ArrayList<String>();
        String[] parts = value.split("[,\\n\\r]+");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.length() > 0) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private String unescape(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch != '\\' || i + 1 >= value.length()) {
                builder.append(ch);
                continue;
            }
            char escaped = value.charAt(++i);
            if (escaped == 'n') {
                builder.append('\n');
            } else if (escaped == 'r') {
                builder.append('\r');
            } else if (escaped == 't') {
                builder.append('\t');
            } else {
                builder.append(escaped);
            }
        }
        return builder.toString();
    }
}
