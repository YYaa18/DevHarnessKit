package com.devharnesskit.dhk.model.skill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SkillContractLintResult {
    private final String skillKey;
    private final String sourcePath;
    private final SkillContract contract;
    private final List<SkillContractLintIssue> issues;

    public SkillContractLintResult(String skillKey, String sourcePath, SkillContract contract,
                                   List<SkillContractLintIssue> issues) {
        this.skillKey = value(skillKey);
        this.sourcePath = value(sourcePath);
        this.contract = contract;
        this.issues = issues == null
                ? Collections.<SkillContractLintIssue>emptyList()
                : Collections.unmodifiableList(new ArrayList<SkillContractLintIssue>(issues));
    }

    public String skillKey() { return skillKey; }
    public String sourcePath() { return sourcePath; }
    public SkillContract contract() { return contract; }
    public List<SkillContractLintIssue> issues() { return issues; }
    public boolean passed() { return issues.isEmpty() && contract != null; }

    public int missingCount() {
        return count("missing");
    }

    public int invalidCount() {
        return count("invalid");
    }

    public int forbiddenCount() {
        return count("forbidden");
    }

    private int count(String category) {
        int total = 0;
        for (SkillContractLintIssue issue : issues) {
            if (category.equals(issue.category())) {
                total++;
            }
        }
        return total;
    }

    private String value(String raw) {
        return raw == null ? "" : raw;
    }
}
