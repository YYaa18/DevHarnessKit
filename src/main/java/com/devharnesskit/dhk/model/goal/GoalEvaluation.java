package com.devharnesskit.dhk.model.goal;

public final class GoalEvaluation {
    private final String decision;
    private final String[] missing;
    private final String[] staleChecks;
    private final String nextAction;
    private final String nextCommand;

    public GoalEvaluation(String decision, String[] missing, String nextAction, String nextCommand) {
        this(decision, missing, new String[0], nextAction, nextCommand);
    }

    public GoalEvaluation(String decision, String[] missing, String[] staleChecks,
                          String nextAction, String nextCommand) {
        this.decision = decision == null ? "" : decision;
        this.missing = missing == null ? new String[0] : missing;
        this.staleChecks = staleChecks == null ? new String[0] : staleChecks;
        this.nextAction = nextAction == null ? "" : nextAction;
        this.nextCommand = nextCommand == null ? "" : nextCommand;
    }

    public String decision() { return decision; }
    public String[] missing() { return missing; }
    public String[] staleChecks() { return staleChecks; }
    public String nextAction() { return nextAction; }
    public String nextCommand() { return nextCommand; }

    public boolean readyToComplete() {
        return "ready_to_complete".equals(decision);
    }
}
