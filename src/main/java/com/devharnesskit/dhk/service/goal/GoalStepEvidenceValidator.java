package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.goal.GoalPlan;
import com.devharnesskit.dhk.model.goal.GoalStep;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class GoalStepEvidenceValidator {
    void validate(GoalPlan plan, String summary, String changedFiles, String evidence) {
        List<String> missing = new ArrayList<String>();
        String evidenceText = evidence == null ? "" : evidence.trim();
        String lowerEvidence = evidenceText.toLowerCase(Locale.ROOT);
        String lowerChangedFiles = changedFiles == null ? "" : changedFiles.trim().toLowerCase(Locale.ROOT);
        for (String required : plan.requiredEvidence()) {
            String key = required == null ? "" : required.trim();
            if (key.length() == 0) {
                continue;
            }
            if ("summary".equals(key)) {
                if (summary == null || summary.trim().length() == 0) {
                    missing.add(key);
                }
                continue;
            }
            if ("evidence".equals(key)) {
                if (evidenceText.length() == 0) {
                    missing.add(key);
                }
                continue;
            }
            if ("changed_files".equals(key)) {
                if (lowerChangedFiles.length() == 0 && !containsEvidenceKey(lowerEvidence, key)) {
                    missing.add(key);
                }
                continue;
            }
            if ("implementation_summary".equals(key)) {
                if ((summary == null || summary.trim().length() == 0) && !containsEvidenceKey(lowerEvidence, key)) {
                    missing.add(key);
                }
                continue;
            }
            if (!containsEvidenceKey(lowerEvidence, key)) {
                missing.add(key);
            }
        }
        if (!missing.isEmpty()) {
            throw new GoalStepEvidenceException(missing.toArray(new String[missing.size()]), plan);
        }
    }

    String changedFiles(List<GoalStep> steps) {
        StringBuilder builder = new StringBuilder();
        for (GoalStep step : steps) {
            if (step.changedFiles().length() == 0) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(step.changedFiles());
        }
        return builder.length() == 0 ? "none" : builder.toString();
    }

    private boolean containsEvidenceKey(String lowerEvidence, String key) {
        String normalized = key.toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (lowerEvidence.contains(normalized)) {
            return true;
        }
        if (normalized.startsWith("existing_")) {
            return lowerEvidence.contains("read_files=");
        }
        if ("impacted_files".equals(normalized)) {
            return lowerEvidence.contains("changed_files=") || lowerEvidence.contains("read_files=");
        }
        if ("risk_points".equals(normalized)) {
            return lowerEvidence.contains("risks=");
        }
        if ("test_result".equals(normalized)) {
            return lowerEvidence.contains("tests_run=");
        }
        return false;
    }
}
