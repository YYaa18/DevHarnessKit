package com.devharnesskit.dhk.service.goal;

public final class GoalCheckPolicy {
    public static final String[] DEFAULT_REQUIRED_CHECKS =
            new String[]{"compile", "test", "sensitive", "spec", "workflow"};

    private final String[] requiredChecks;
    private final String[] compileCommand;
    private final String[] testCommand;
    private final boolean failPendingHardGates;

    public GoalCheckPolicy(String[] requiredChecks, String[] compileCommand,
                           String[] testCommand, boolean failPendingHardGates) {
        this.requiredChecks = requiredChecks == null || requiredChecks.length == 0
                ? DEFAULT_REQUIRED_CHECKS
                : requiredChecks;
        this.compileCommand = compileCommand == null || compileCommand.length == 0
                ? new String[]{"mvn", "-q", "-DskipTests", "compile"}
                : compileCommand;
        this.testCommand = testCommand == null || testCommand.length == 0
                ? new String[]{"mvn", "-q", "test"}
                : testCommand;
        this.failPendingHardGates = failPendingHardGates;
    }

    public static GoalCheckPolicy defaults() {
        return new GoalCheckPolicy(DEFAULT_REQUIRED_CHECKS,
                new String[]{"mvn", "-q", "-DskipTests", "compile"},
                new String[]{"mvn", "-q", "test"}, false);
    }

    public String[] requiredChecks() {
        return requiredChecks;
    }

    public String[] compileCommand() {
        return compileCommand;
    }

    public String[] testCommand() {
        return testCommand;
    }

    public boolean failPendingHardGates() {
        return failPendingHardGates;
    }
}
