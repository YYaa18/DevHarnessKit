package com.devharnesskit.dhk.service.skill;

import com.devharnesskit.dhk.model.goal.GoalCheck;
import com.devharnesskit.dhk.model.goal.GoalStep;
import com.devharnesskit.dhk.model.skill.SkillQualityScore;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SkillQualityScoreService {
    private static final String[] DISCIPLINE_GATES = new String[]{
            "think-before-coding", "goal-driven", "simplicity", "surgical-change"
    };
    private static final String[][] EVIDENCE_GROUPS = new String[][]{
            {"goal_understanding", "task_understanding", "requirements_summary"},
            {"assumptions", "pending", "questions", "open_questions"},
            {"impacted_files"},
            {"risk_points", "risks"},
            {"verification_plan"},
            {"implementation_summary"},
            {"test_result", "tests_run"},
            {"compile_result"},
            {"sensitive_result"},
            {"rollback_plan", "rollback_quality"}
    };

    public SkillQualityScore score(String goalKey, List<GoalStep> steps, List<GoalCheck> checks) {
        int gatePassRate = gatePassRate(checks);
        int evidenceCompleteness = evidenceCompleteness(steps);
        int rollbackQuality = rollbackQuality(steps);
        int score = Math.round((gatePassRate * 0.4f) + (evidenceCompleteness * 0.4f)
                + (rollbackQuality * 0.2f));
        String summary = "gate_pass_rate=" + gatePassRate + " evidence_completeness="
                + evidenceCompleteness + " rollback_quality=" + rollbackQuality;
        return new SkillQualityScore(goalKey, score, gatePassRate, evidenceCompleteness,
                rollbackQuality, summary);
    }

    private int gatePassRate(List<GoalCheck> checks) {
        if (checks == null || checks.isEmpty()) {
            return 0;
        }
        int observed = 0;
        int passed = 0;
        for (String gate : DISCIPLINE_GATES) {
            GoalCheck check = find(checks, gate);
            if (check == null) {
                continue;
            }
            observed++;
            if ("passed".equals(check.status())) {
                passed++;
            }
        }
        if (observed == 0) {
            return 0;
        }
        return percent(passed, observed);
    }

    private int evidenceCompleteness(List<GoalStep> steps) {
        if (steps == null || steps.isEmpty()) {
            return 0;
        }
        int present = 0;
        for (String[] group : EVIDENCE_GROUPS) {
            if (hasAnyEvidence(steps, group)) {
                present++;
            }
        }
        return percent(present, EVIDENCE_GROUPS.length);
    }

    private int rollbackQuality(List<GoalStep> steps) {
        if (hasAnyEvidence(steps, new String[]{"rollback_quality"})) {
            String value = evidenceValue(steps, "rollback_quality").toLowerCase();
            if (value.indexOf("verified") >= 0 || value.indexOf("complete") >= 0) {
                return 100;
            }
            return 80;
        }
        if (hasAnyEvidence(steps, new String[]{"rollback_plan"})) {
            String value = evidenceValue(steps, "rollback_plan");
            return value.length() >= 12 ? 100 : 60;
        }
        return 0;
    }

    private boolean hasAnyEvidence(List<GoalStep> steps, String[] keys) {
        for (String key : keys) {
            if (evidenceValue(steps, key).length() > 0) {
                return true;
            }
        }
        return false;
    }

    private String evidenceValue(List<GoalStep> steps, String key) {
        String value = "";
        if (steps == null) {
            return value;
        }
        for (GoalStep step : steps) {
            String candidate = evidenceValue(step.evidence(), key);
            if (candidate.length() > 0) {
                value = candidate;
            }
        }
        return value;
    }

    private String evidenceValue(String evidence, String key) {
        Pattern pattern = Pattern.compile("(?i)(?:^|[;\\n\\r])\\s*" + Pattern.quote(key)
                + "\\s*=\\s*([^;\\n\\r]+)");
        Matcher matcher = pattern.matcher(evidence == null ? "" : evidence);
        String value = "";
        while (matcher.find()) {
            value = matcher.group(1).trim();
        }
        return value;
    }

    private GoalCheck find(List<GoalCheck> checks, String checkKey) {
        for (GoalCheck check : checks) {
            if (checkKey.equals(check.checkKey())) {
                return check;
            }
        }
        return null;
    }

    private int percent(int numerator, int denominator) {
        if (denominator <= 0) {
            return 0;
        }
        return Math.round((numerator * 100f) / denominator);
    }
}
