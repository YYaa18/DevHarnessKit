package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.goal.GoalPlan;

public final class GoalStepEvidenceException extends IllegalArgumentException {
    private final String[] missing;
    private final GoalPlan plan;

    GoalStepEvidenceException(String[] missing, GoalPlan plan) {
        super("Goal step evidence missing required items: " + join(missing));
        this.missing = copy(missing);
        this.plan = plan;
    }

    public String[] missing() {
        return copy(missing);
    }

    public GoalPlan plan() {
        return plan;
    }

    private static String join(String[] values) {
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(values[i]);
        }
        builder.append(']');
        return builder.toString();
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
