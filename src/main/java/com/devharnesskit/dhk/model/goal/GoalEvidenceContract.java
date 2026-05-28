package com.devharnesskit.dhk.model.goal;

import java.util.ArrayList;
import java.util.List;

public final class GoalEvidenceContract {
    private static final String[] BASE_STRUCTURED_FIELDS = new String[]{
            "--read-files",
            "--changed-files",
            "--tests-run",
            "--compile-result",
            "--risks",
            "--pending"
    };

    private final String goalKey;
    private final String currentAction;
    private final String[] requiredEvidence;
    private final String[] structuredEvidenceFields;
    private final String exampleEvidence;
    private final String exampleCommand;

    private GoalEvidenceContract(String goalKey, String currentAction, String[] requiredEvidence,
                                 String[] structuredEvidenceFields, String exampleEvidence,
                                 String exampleCommand) {
        this.goalKey = value(goalKey);
        this.currentAction = value(currentAction);
        this.requiredEvidence = copy(requiredEvidence);
        this.structuredEvidenceFields = copy(structuredEvidenceFields);
        this.exampleEvidence = value(exampleEvidence);
        this.exampleCommand = value(exampleCommand);
    }

    public static GoalEvidenceContract from(GoalRun goal, GoalPlan plan) {
        String goalKey = goal == null ? "" : goal.goalKey();
        String currentAction = plan == null ? "" : plan.currentAction();
        String[] required = plan == null ? new String[0] : plan.requiredEvidence();
        List<String> fields = new ArrayList<String>();
        for (String field : BASE_STRUCTURED_FIELDS) {
            fields.add(field);
        }
        for (String evidence : required) {
            String key = value(evidence);
            if (key.length() > 0) {
                fields.add("--field " + key + "=<value>");
            }
        }
        String exampleEvidence = exampleEvidence(required);
        String exampleCommand = "dhk goal step --goal " + (goalKey.length() == 0 ? "<goal-key>" : goalKey)
                + " --summary \"<summary>\"";
        if (exampleEvidence.length() > 0) {
            exampleCommand += " --evidence \"" + exampleEvidence + "\"";
        } else {
            exampleCommand += " --evidence \"<evidence>\"";
        }
        return new GoalEvidenceContract(goalKey, currentAction, required,
                fields.toArray(new String[fields.size()]), exampleEvidence, exampleCommand);
    }

    public String goalKey() { return goalKey; }
    public String currentAction() { return currentAction; }
    public String[] requiredEvidence() { return copy(requiredEvidence); }
    public String[] structuredEvidenceFields() { return copy(structuredEvidenceFields); }
    public String exampleEvidence() { return exampleEvidence; }
    public String exampleCommand() { return exampleCommand; }

    private static String exampleEvidence(String[] required) {
        StringBuilder builder = new StringBuilder();
        for (String evidence : required == null ? new String[0] : required) {
            String key = value(evidence);
            if (key.length() == 0) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append("; ");
            }
            builder.append(key).append("=<value>");
        }
        return builder.toString();
    }

    private static String value(String value) {
        return value == null ? "" : value.trim();
    }

    private static String[] copy(String[] values) {
        if (values == null) {
            return new String[0];
        }
        String[] copy = new String[values.length];
        System.arraycopy(values, 0, copy, 0, values.length);
        return copy;
    }
}
