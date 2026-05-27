package com.devharnesskit.dhk.model.skill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SkillAuditResult {
    private final String skillKey;
    private final String sourcePath;
    private final String sourceHash;
    private final List<SkillAuditIssue> issues;

    public SkillAuditResult(String skillKey, String sourcePath, String sourceHash,
                            List<SkillAuditIssue> issues) {
        this.skillKey = value(skillKey);
        this.sourcePath = value(sourcePath);
        this.sourceHash = value(sourceHash);
        this.issues = immutable(issues);
    }

    public String skillKey() { return skillKey; }
    public String sourcePath() { return sourcePath; }
    public String sourceHash() { return sourceHash; }
    public List<SkillAuditIssue> issues() { return issues; }

    public String decision() {
        return issues.isEmpty() ? "passed" : "review_required";
    }

    public boolean passed() {
        return issues.isEmpty();
    }

    public int issueCount() {
        return issues.size();
    }

    public int countBySeverity(String severity) {
        int count = 0;
        for (SkillAuditIssue issue : issues) {
            if (severity.equals(issue.severity())) {
                count++;
            }
        }
        return count;
    }

    private List<SkillAuditIssue> immutable(List<SkillAuditIssue> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<SkillAuditIssue>(values));
    }

    private String value(String raw) {
        return raw == null ? "" : raw;
    }
}
