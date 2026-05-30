package com.devharnesskit.dhk.service.skill;

import com.devharnesskit.dhk.model.skill.SkillAuditIssue;
import com.devharnesskit.dhk.model.skill.SkillAuditResult;
import com.devharnesskit.dhk.model.skill.SkillContract;
import com.devharnesskit.dhk.model.skill.SkillContractLintIssue;
import com.devharnesskit.dhk.model.skill.SkillContractLintResult;
import com.devharnesskit.dhk.service.SensitiveDataGuard;
import com.devharnesskit.dhk.util.PathUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SkillAuditService {
    private static final Rule[] COMMAND_RULES = new Rule[]{
            rule("dangerous_command", "critical", "\\brm\\s+-rf\\s+(/|\\$\\{|\\*)",
                    "destructive recursive delete command"),
            rule("dangerous_command", "high", "\\bgit\\s+reset\\s+--hard\\b",
                    "destructive git reset command"),
            rule("dangerous_command", "high", "\\bdhk\\s+workflow\\s+gate\\s+waive\\b",
                    "workflow gate waiver is forbidden for skills"),
            rule("dangerous_command", "high", "\\bdhk\\s+memory\\s+confirm\\b",
                    "memory confirmation is forbidden for skills"),
            rule("dangerous_command", "high", "\\bdhk\\s+db\\s+sql\\b",
                    "direct DB SQL is forbidden for skills unless explicitly requested"),
            rule("dangerous_command", "high", "\\bgit\\s+push\\b",
                    "network write command requires human review"),
            rule("script_execution", "high", "\\b(curl|wget)\\b[^\\n|;]*[|]\\s*(sh|bash)\\b",
                    "remote download piped into shell"),
            rule("script_execution", "high", "\\bsudo\\b",
                    "privileged command execution"),
            rule("script_execution", "medium", "\\b(eval|bash\\s+-c|sh\\s+-c|python\\s+-c)\\b",
                    "dynamic command execution"),
            rule("script_execution", "medium", "\\b(os\\.system|subprocess\\.|child_process\\.|Runtime\\.getRuntime\\(\\)\\.exec)\\b",
                    "programmatic shell execution")
    };
    private static final long MAX_TEXT_BYTES = 512L * 1024L;

    private final SkillContractLintService lintService;
    private final SkillContractService contractService;
    private final SensitiveDataGuard sensitiveDataGuard;

    public SkillAuditService() {
        this(new SkillContractLintService(), new SkillContractService(), new SensitiveDataGuard());
    }

    public SkillAuditService(SkillContractLintService lintService, SkillContractService contractService,
                             SensitiveDataGuard sensitiveDataGuard) {
        this.lintService = lintService;
        this.contractService = contractService;
        this.sensitiveDataGuard = sensitiveDataGuard;
    }

    public SkillAuditResult audit(Path projectRoot, Path skillDirectory) {
        List<SkillAuditIssue> issues = new ArrayList<SkillAuditIssue>();
        SkillContractLintResult lint = lintService.lint(skillDirectory);
        for (SkillContractLintIssue issue : lint.issues()) {
            issues.add(new SkillAuditIssue("high", "contract", issue.field(),
                    issue.message(), issue.suggestion()));
        }

        SkillContract contract = lint.contract();
        if (contract != null && "high".equals(contract.riskLevel())) {
            issues.add(new SkillAuditIssue("medium", "contract", "contract.json",
                    "skill declares high risk", "Review and trust the skill source before use"));
        }

        auditSourceLocation(projectRoot, skillDirectory, issues);
        auditLicense(skillDirectory, issues);
        auditFiles(skillDirectory, issues);

        String skillKey = lint.skillKey();
        String sourcePath = PathUtil.displayPath(skillDirectory);
        String sourceHash = contract == null ? contractService.sourceHash(skillDirectory) : contract.sourceHash();
        return new SkillAuditResult(skillKey, sourcePath, sourceHash, issues);
    }

    private void auditSourceLocation(Path projectRoot, Path skillDirectory, List<SkillAuditIssue> issues) {
        if (projectRoot == null) {
            return;
        }
        Path normalizedRoot = projectRoot.toAbsolutePath().normalize();
        Path normalizedSkill = skillDirectory.toAbsolutePath().normalize();
        if (!normalizedSkill.startsWith(normalizedRoot)) {
            issues.add(new SkillAuditIssue("medium", "source", "",
                    "skill path is outside the project root",
                    "Review external skill sources before trusting them"));
        }
    }

    private void auditLicense(Path skillDirectory, List<SkillAuditIssue> issues) {
        if (!Files.isRegularFile(skillDirectory.resolve("LICENSE"))
                && !Files.isRegularFile(skillDirectory.resolve("LICENSE.md"))
                && !Files.isRegularFile(skillDirectory.resolve("NOTICE"))
                && !Files.isRegularFile(skillDirectory.resolve("THIRD_PARTY_NOTICES.md"))) {
            issues.add(new SkillAuditIssue("medium", "license", "",
                    "skill source has no local license or notice file",
                    "Add LICENSE/NOTICE or record manual license review before trusting"));
        }
    }

    private void auditFiles(Path skillDirectory, List<SkillAuditIssue> issues) {
        for (Path file : files(skillDirectory)) {
            String relative = relative(skillDirectory, file);
            byte[] bytes;
            try {
                bytes = Files.readAllBytes(file);
            } catch (IOException ex) {
                issues.add(new SkillAuditIssue("medium", "source", relative,
                        "file could not be read: " + ex.getMessage(), "Check file permissions"));
                continue;
            }
            if (bytes.length > MAX_TEXT_BYTES) {
                issues.add(new SkillAuditIssue("medium", "source", relative,
                        "file exceeds audit text limit", "Review large skill files manually"));
                continue;
            }
            String text = new String(bytes, StandardCharsets.UTF_8);
            auditSensitive(relative, text, issues);
            if (!"contract.json".equals(relative)) {
                auditCommandRules(relative, text, issues);
            }
            auditScriptFile(relative, text, issues);
        }
    }

    private List<Path> files(final Path skillDirectory) {
        final List<Path> result = new ArrayList<Path>();
        if (skillDirectory == null || !Files.isDirectory(skillDirectory)) {
            return result;
        }
        try {
            Files.walkFileTree(skillDirectory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (attrs.isRegularFile()) {
                        result.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
            return result;
        }
        Collections.sort(result, new Comparator<Path>() {
            public int compare(Path left, Path right) {
                return relative(skillDirectory, left).compareTo(relative(skillDirectory, right));
            }
        });
        return result;
    }

    private void auditSensitive(String relativePath, String text, List<SkillAuditIssue> issues) {
        List<String> matches = sensitiveDataGuard.findMatches(text);
        if (!matches.isEmpty()) {
            issues.add(new SkillAuditIssue("high", "sensitive", relativePath,
                    "sensitive patterns found: " + join(matches),
                    "Remove secrets from skill source; audit logs do not print raw sensitive values"));
        }
    }

    private void auditCommandRules(String relativePath, String text, List<SkillAuditIssue> issues) {
        String scanText = commandScanText(text);
        for (Rule rule : COMMAND_RULES) {
            if (rule.pattern.matcher(scanText).find()) {
                issues.add(new SkillAuditIssue(rule.severity, rule.category, relativePath,
                        rule.message, "Remove the command or require explicit human approval"));
            }
        }
    }

    private String commandScanText(String text) {
        StringBuilder builder = new StringBuilder();
        String[] lines = (text == null ? "" : text).split("\\r?\\n");
        for (String line : lines) {
            if (isForbiddenCommandDeclaration(line)) {
                builder.append('\n');
            } else {
                builder.append(line).append('\n');
            }
        }
        return builder.toString();
    }

    private boolean isForbiddenCommandDeclaration(String line) {
        String lower = line == null ? "" : line.trim().toLowerCase(Locale.ROOT);
        return lower.startsWith("- forbidden_commands:")
                || lower.startsWith("forbidden_commands:")
                || lower.startsWith("\"forbidden_commands\"");
    }

    private void auditScriptFile(String relativePath, String text, List<SkillAuditIssue> issues) {
        String lower = relativePath.toLowerCase(Locale.ROOT);
        boolean scriptExtension = lower.endsWith(".sh") || lower.endsWith(".bash")
                || lower.endsWith(".zsh") || lower.endsWith(".py") || lower.endsWith(".js")
                || lower.endsWith(".mjs") || lower.endsWith(".cjs") || lower.endsWith(".rb")
                || lower.endsWith(".pl") || lower.endsWith(".ps1") || lower.endsWith(".bat")
                || lower.endsWith(".cmd");
        boolean shebang = text.startsWith("#!");
        if (scriptExtension || shebang) {
            issues.add(new SkillAuditIssue("medium", "script", relativePath,
                    "executable script-like file requires review",
                    "Review script behavior before trusting this skill"));
        }
    }

    private String relative(Path root, Path file) {
        if (file == null) {
            return "";
        }
        if (root == null) {
            return file.toString();
        }
        try {
            return root.relativize(file).toString().replace('\\', '/');
        } catch (IllegalArgumentException ex) {
            return file.toString().replace('\\', '/');
        }
    }

    private String join(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(values.get(i));
        }
        return builder.toString();
    }

    private static Rule rule(String category, String severity, String regex, String message) {
        return new Rule(category, severity, Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL), message);
    }

    private static final class Rule {
        private final String category;
        private final String severity;
        private final Pattern pattern;
        private final String message;

        private Rule(String category, String severity, Pattern pattern, String message) {
            this.category = category;
            this.severity = severity;
            this.pattern = pattern;
            this.message = message;
        }
    }
}
