package com.devharnesskit.dhk.model.goal;

public final class GoalAcceptanceMapping {
    public static final String SOURCE_CHECKS = "checks";
    public static final String SOURCE_TEST = "test";
    public static final String SOURCE_EVIDENCE = "evidence";
    public static final String SOURCE_MANUAL = "manual";

    private final String acceptanceKey;
    private final String description;
    private final String expectedResult;
    private final String source;
    private final String[] requiredChecks;
    private final String evidenceKey;

    public GoalAcceptanceMapping(String acceptanceKey, String description, String expectedResult,
                                 String source, String[] requiredChecks, String evidenceKey) {
        this.acceptanceKey = value(acceptanceKey);
        this.description = defaultText(description, this.acceptanceKey.replace('_', ' '));
        this.expectedResult = defaultText(expectedResult, "Acceptance source " + normalizeSource(source) + " is satisfied");
        this.source = normalizeSource(source);
        this.requiredChecks = requiredChecks == null ? new String[0] : requiredChecks;
        this.evidenceKey = value(evidenceKey);
    }

    public String acceptanceKey() { return acceptanceKey; }
    public String description() { return description; }
    public String expectedResult() { return expectedResult; }
    public String source() { return source; }
    public String[] requiredChecks() { return requiredChecks; }
    public String evidenceKey() { return evidenceKey; }

    private static String value(String value) {
        return value == null ? "" : value.trim();
    }

    private static String defaultText(String value, String defaultValue) {
        String text = value(value);
        return text.length() == 0 ? defaultValue : text;
    }

    private static String normalizeSource(String source) {
        String normalized = value(source);
        if (SOURCE_CHECKS.equals(normalized) || SOURCE_TEST.equals(normalized)
                || SOURCE_EVIDENCE.equals(normalized) || SOURCE_MANUAL.equals(normalized)) {
            return normalized;
        }
        return SOURCE_MANUAL;
    }
}
