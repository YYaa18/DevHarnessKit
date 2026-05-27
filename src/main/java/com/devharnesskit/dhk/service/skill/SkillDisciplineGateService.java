package com.devharnesskit.dhk.service.skill;

import com.devharnesskit.dhk.model.goal.GoalProfile;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.model.goal.GoalStep;
import com.devharnesskit.dhk.model.skill.SkillDisciplineGateResult;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SkillDisciplineGateService {
    public SkillDisciplineGateResult thinkBeforeCoding(GoalRun goal, GoalProfile profile,
                                                       List<GoalStep> steps) {
        List<String> failures = new ArrayList<String>();
        StringBuilder output = new StringBuilder();
        GoalStep plan = firstStep(steps, "create_change_plan");
        GoalStep implementation = firstActionContaining(steps, "implement");
        GoalStep inspect = firstStep(steps, "inspect_existing_code");
        GoalStep preCoding = inspect == null && !profileHasAction(profile, "inspect_existing_code")
                ? firstPreCodingStep(steps, implementation) : inspect;

        output.append("goal_key: ").append(goal.goalKey()).append('\n');
        output.append("profile: ").append(goal.profileKey()).append('\n');
        output.append("inspect_step: ").append(inspect == null ? "missing" : inspect.stepIndex()).append('\n');
        output.append("pre_coding_step: ")
                .append(preCoding == null ? "missing" : preCoding.stepIndex()).append('\n');
        output.append("plan_step: ").append(plan == null ? "missing" : plan.stepIndex()).append('\n');
        output.append("implementation_step: ")
                .append(implementation == null ? "missing" : implementation.stepIndex()).append('\n');

        if (preCoding == null) {
            failures.add("inspect_existing_code step missing before coding");
        }
        if (implementation != null && preCoding != null && preCoding.stepIndex() > implementation.stepIndex()) {
            failures.add("pre-coding evidence was recorded after implementation");
        }
        if (implementation != null && plan != null && plan.stepIndex() > implementation.stepIndex()) {
            failures.add("create_change_plan was recorded after implementation");
        }

        String goalUnderstanding = firstEvidenceValue(steps, implementation,
                new String[]{"goal_understanding", "task_understanding", "requirements_summary"});
        String assumptions = firstEvidenceValue(steps, implementation, new String[]{"assumptions"});
        String questions = firstEvidenceValue(steps, implementation,
                new String[]{"questions", "open_questions", "pending"});
        String tradeoff = firstEvidenceValue(steps, implementation,
                new String[]{"tradeoff", "tradeoffs", "risk_points"});

        output.append("goal_understanding: ").append(empty(goalUnderstanding, "missing")).append('\n');
        output.append("assumptions: ").append(empty(assumptions, "missing")).append('\n');
        output.append("questions_or_pending: ").append(empty(questions, "missing")).append('\n');
        output.append("tradeoff_or_risk_points: ").append(empty(tradeoff, "missing")).append('\n');

        if (goalUnderstanding.length() == 0) {
            failures.add("goal understanding evidence missing; add goal_understanding=<summary> before implementation");
        }
        if (assumptions.length() == 0 && questions.length() == 0) {
            failures.add("assumptions or pending/questions evidence missing before implementation");
        }

        return result("think-before-coding", failures, output);
    }

    public SkillDisciplineGateResult goalDriven(GoalRun goal, GoalProfile profile, List<GoalStep> steps) {
        List<String> failures = new ArrayList<String>();
        StringBuilder output = new StringBuilder();
        output.append("goal_key: ").append(goal.goalKey()).append('\n');
        output.append("profile: ").append(goal.profileKey()).append('\n');
        output.append("status: ").append(goal.status()).append('\n');
        output.append("current_action: ").append(goal.currentAction()).append('\n');
        output.append("step_count: ").append(steps == null ? 0 : steps.size()).append('\n');

        if (goal.goalKey().length() == 0) {
            failures.add("goal key is missing");
        }
        String[] actions = profile == null ? new String[0] : profile.actions();
        if (actions.length == 0) {
            failures.add("goal profile has no action contract");
        }
        if (steps == null || steps.isEmpty()) {
            failures.add("no goal steps recorded");
        } else {
            int expectedIndex = 0;
            String previousAction = "";
            for (GoalStep step : steps) {
                ActionMatch match = matchAction(actions, expectedIndex, previousAction, step.actionKey());
                String expected = match.expectedAction;
                output.append("- step ").append(step.stepIndex()).append(": ")
                        .append(step.actionKey()).append(" expected=")
                        .append(expected.length() == 0 ? "none" : expected).append('\n');
                if (!match.accepted) {
                    failures.add(match.failure(step));
                } else {
                    if (match.skippedActions.length() > 0) {
                        failures.add("step " + step.stepIndex() + " skipped expected action(s): "
                                + match.skippedActions);
                    }
                    expectedIndex = match.nextExpectedIndex;
                    previousAction = step.actionKey();
                }
                if (step.summary().length() == 0 || step.evidence().length() == 0) {
                    failures.add("step " + step.stepIndex() + " is missing summary or evidence");
                }
            }
        }

        return result("goal-driven", failures, output);
    }

    private ActionMatch matchAction(String[] actions, int expectedIndex, String previousAction, String actionKey) {
        if (actions.length == 0 || actionKey == null || actionKey.length() == 0) {
            return ActionMatch.rejected("", "has no action contract match");
        }
        if (previousAction.length() > 0 && previousAction.equals(actionKey)) {
            return ActionMatch.accepted(previousAction, expectedIndex, expectedIndex, "");
        }
        if (expectedIndex < actions.length && actions[expectedIndex].equals(actionKey)) {
            return ActionMatch.accepted(actions[expectedIndex], expectedIndex + 1, expectedIndex, "");
        }
        for (int i = expectedIndex + 1; i < actions.length; i++) {
            if (actions[i].equals(actionKey)) {
                return ActionMatch.accepted(actions[i], i + 1, expectedIndex,
                        join(actions, expectedIndex, i));
            }
        }
        return ActionMatch.rejected(expectedIndex < actions.length ? actions[expectedIndex] : "",
                expectedIndex >= actions.length
                        ? "exceeds profile action contract"
                        : "action mismatch: expected " + actions[expectedIndex] + " but was " + actionKey);
    }

    private String join(String[] values, int startInclusive, int endExclusive) {
        StringBuilder builder = new StringBuilder();
        for (int i = startInclusive; i < endExclusive; i++) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(values[i]);
        }
        return builder.toString();
    }

    public SkillDisciplineGateResult simplicity(GoalRun goal, GoalProfile profile, List<GoalStep> steps) {
        List<String> failures = new ArrayList<String>();
        StringBuilder output = new StringBuilder();
        List<String> changedFiles = changedFiles(steps);
        int maxChangedFiles = profile == null ? 6 : Math.min(profile.legacyMaxChangedFiles(), 6);
        String scopeJustification = evidenceValue(steps, "scope_justification");
        String simplicityJustification = evidenceValue(steps, "simplicity_justification");
        List<String> abstractionFiles = abstractionFiles(changedFiles);

        output.append("goal_key: ").append(goal.goalKey()).append('\n');
        output.append("changed_files_count: ").append(changedFiles.size()).append('\n');
        output.append("max_changed_files_without_scope_evidence: ").append(maxChangedFiles).append('\n');
        output.append("scope_justification: ").append(empty(scopeJustification, "missing")).append('\n');
        output.append("simplicity_justification: ").append(empty(simplicityJustification, "missing")).append('\n');
        output.append("abstraction_risk_files: ").append(abstractionFiles).append('\n');

        if (changedFiles.size() > maxChangedFiles && scopeJustification.length() == 0) {
            failures.add("large change set lacks scope_justification: changed_files=" + changedFiles.size()
                    + " max=" + maxChangedFiles);
        }
        if (!abstractionFiles.isEmpty() && simplicityJustification.length() == 0) {
            failures.add("abstraction risk lacks simplicity_justification: " + abstractionFiles);
        }

        return result("simplicity", failures, output);
    }

    public SkillDisciplineGateResult surgicalChange(GoalRun goal, GoalProfile profile, List<GoalStep> steps,
                                                    String[] protectedGlobs) {
        List<String> failures = new ArrayList<String>();
        StringBuilder output = new StringBuilder();
        List<String> changedFiles = changedFiles(steps);
        int maxChangedFiles = profile == null ? 6 : Math.min(profile.legacyMaxChangedFiles(), 6);
        String impactedFiles = evidenceValue(steps, "impacted_files");
        String impactEvidence = firstNonEmpty(evidenceValue(steps, "impact_map"),
                evidenceValue(steps, "impact_evidence"));
        String scopeJustification = evidenceValue(steps, "scope_justification");
        String protectedConfirmation = firstNonEmpty(evidenceValue(steps, "protected_file_confirmation"),
                evidenceValue(steps, "manual_evidence_status"));
        List<String> protectedChangedFiles = protectedChangedFiles(changedFiles, protectedGlobs);

        output.append("goal_key: ").append(goal.goalKey()).append('\n');
        output.append("changed_files: ").append(changedFiles).append('\n');
        output.append("changed_files_count: ").append(changedFiles.size()).append('\n');
        output.append("impacted_files: ").append(empty(impactedFiles, "missing")).append('\n');
        output.append("impact_evidence: ").append(empty(impactEvidence, "missing")).append('\n');
        output.append("scope_justification: ").append(empty(scopeJustification, "missing")).append('\n');
        output.append("protected_changed_files: ").append(protectedChangedFiles).append('\n');
        output.append("protected_file_confirmation: ").append(empty(protectedConfirmation, "missing")).append('\n');

        if (changedFiles.size() > maxChangedFiles
                && impactedFiles.length() == 0
                && impactEvidence.length() == 0
                && scopeJustification.length() == 0) {
            failures.add("large change set lacks impacted_files, impact_evidence, or scope_justification: changed_files="
                    + changedFiles.size() + " max=" + maxChangedFiles);
        }
        if (!protectedChangedFiles.isEmpty() && !isApproved(protectedConfirmation)) {
            failures.add("protected files changed without manual evidence: " + protectedChangedFiles);
        }

        return result("surgical-change", failures, output);
    }

    private SkillDisciplineGateResult result(String gateKey, List<String> failures, StringBuilder output) {
        String status = failures.isEmpty() ? "passed" : "failed";
        String summary = failures.isEmpty()
                ? gateKey + " gate passed"
                : gateKey + " gate failed: " + failures;
        output.append("gate_status: ").append(status).append('\n');
        output.append("summary: ").append(summary).append('\n');
        return new SkillDisciplineGateResult(gateKey, status, summary, output.toString());
    }

    private GoalStep firstStep(List<GoalStep> steps, String actionKey) {
        if (steps == null) {
            return null;
        }
        for (GoalStep step : steps) {
            if (actionKey.equals(step.actionKey())) {
                return step;
            }
        }
        return null;
    }

    private GoalStep firstActionContaining(List<GoalStep> steps, String text) {
        if (steps == null) {
            return null;
        }
        for (GoalStep step : steps) {
            if (step.actionKey().indexOf(text) >= 0) {
                return step;
            }
        }
        return null;
    }

    private GoalStep firstPreCodingStep(List<GoalStep> steps, GoalStep implementation) {
        if (steps == null) {
            return null;
        }
        for (GoalStep step : steps) {
            if (implementation != null && step.stepIndex() >= implementation.stepIndex()) {
                break;
            }
            if (step.actionKey().indexOf("verify") >= 0) {
                continue;
            }
            if (step.actionKey().indexOf("implement") >= 0) {
                continue;
            }
            return step;
        }
        return null;
    }

    private boolean profileHasAction(GoalProfile profile, String actionKey) {
        if (profile == null || actionKey == null) {
            return false;
        }
        String[] actions = profile.actions();
        for (String action : actions) {
            if (actionKey.equals(action)) {
                return true;
            }
        }
        return false;
    }

    private String firstEvidenceValue(List<GoalStep> steps, GoalStep beforeStep, String[] keys) {
        if (steps == null) {
            return "";
        }
        for (GoalStep step : steps) {
            if (beforeStep != null && step.stepIndex() >= beforeStep.stepIndex()) {
                break;
            }
            for (String key : keys) {
                String value = evidenceValue(step.evidence(), key);
                if (value.length() > 0) {
                    return value;
                }
            }
        }
        return "";
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

    private String evidenceValue(List<GoalStep> steps, String key) {
        if (steps == null) {
            return "";
        }
        String value = "";
        for (GoalStep step : steps) {
            String candidate = evidenceValue(step.evidence(), key);
            if (candidate.length() > 0) {
                value = candidate;
            }
        }
        return value;
    }

    private List<String> changedFiles(List<GoalStep> steps) {
        Set<String> files = new LinkedHashSet<String>();
        if (steps == null) {
            return new ArrayList<String>();
        }
        for (GoalStep step : steps) {
            addFiles(files, step.changedFiles());
            addFiles(files, evidenceValue(step.evidence(), "changed_files"));
        }
        return new ArrayList<String>(files);
    }

    private void addFiles(Set<String> files, String raw) {
        if (raw == null || raw.trim().length() == 0 || "none".equalsIgnoreCase(raw.trim())) {
            return;
        }
        String normalized = raw.replace('\n', ',').replace(';', ',');
        String[] parts = normalized.split(",");
        for (String part : parts) {
            String file = part.trim();
            if (file.length() > 0) {
                files.add(file.replace('\\', '/'));
            }
        }
    }

    private List<String> abstractionFiles(List<String> changedFiles) {
        List<String> result = new ArrayList<String>();
        for (String file : changedFiles) {
            String lower = file.toLowerCase();
            if (lower.indexOf("abstract") >= 0
                    || lower.indexOf("factory") >= 0
                    || lower.indexOf("strategy") >= 0
                    || lower.indexOf("registry") >= 0
                    || lower.indexOf("provider") >= 0
                    || lower.indexOf("adapter") >= 0
                    || lower.indexOf("/base") >= 0) {
                result.add(file);
            }
        }
        return result;
    }

    private List<String> protectedChangedFiles(List<String> changedFiles, String[] protectedGlobs) {
        List<String> result = new ArrayList<String>();
        for (String file : changedFiles) {
            if (matchesAny(file, protectedGlobs)) {
                result.add(file);
            }
        }
        return result;
    }

    private boolean matchesAny(String value, String[] globs) {
        if (globs == null) {
            return false;
        }
        for (String glob : globs) {
            if (globMatches(normalize(value), normalize(glob))) {
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

    private String firstNonEmpty(String first, String second) {
        if (first != null && first.length() > 0) {
            return first;
        }
        return second == null ? "" : second;
    }

    private boolean isApproved(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        return "approved".equals(normalized)
                || "confirmed".equals(normalized)
                || "passed".equals(normalized);
    }

    private String empty(String value, String fallback) {
        return value == null || value.length() == 0 ? fallback : value;
    }

    private static final class ActionMatch {
        private final boolean accepted;
        private final String expectedAction;
        private final int nextExpectedIndex;
        private final String skippedActions;
        private final String failure;

        private ActionMatch(boolean accepted, String expectedAction, int nextExpectedIndex,
                            String skippedActions, String failure) {
            this.accepted = accepted;
            this.expectedAction = expectedAction == null ? "" : expectedAction;
            this.nextExpectedIndex = nextExpectedIndex;
            this.skippedActions = skippedActions == null ? "" : skippedActions;
            this.failure = failure == null ? "" : failure;
        }

        private static ActionMatch accepted(String expectedAction, int nextExpectedIndex,
                                            int previousExpectedIndex, String skippedActions) {
            return new ActionMatch(true, expectedAction, Math.max(nextExpectedIndex, previousExpectedIndex),
                    skippedActions, "");
        }

        private static ActionMatch rejected(String expectedAction, String failure) {
            return new ActionMatch(false, expectedAction, 0, "", failure);
        }

        private String failure(GoalStep step) {
            return "step " + step.stepIndex() + " " + failure;
        }
    }
}
