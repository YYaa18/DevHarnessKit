package com.devharnesskit.dhk.model.routine;

public final class RoutineOutcomeSummary {
    private final int totalGoals;
    private final int completedGoals;
    private final int failedGoals;
    private final int abandonedGoals;
    private final int inProgressGoals;

    public RoutineOutcomeSummary(int totalGoals, int completedGoals, int failedGoals,
                                 int abandonedGoals, int inProgressGoals) {
        this.totalGoals = totalGoals;
        this.completedGoals = completedGoals;
        this.failedGoals = failedGoals;
        this.abandonedGoals = abandonedGoals;
        this.inProgressGoals = inProgressGoals;
    }

    public int totalGoals() { return totalGoals; }
    public int completedGoals() { return completedGoals; }
    public int failedGoals() { return failedGoals; }
    public int abandonedGoals() { return abandonedGoals; }
    public int inProgressGoals() { return inProgressGoals; }

    public int completionRateBasisPoints() {
        if (totalGoals <= 0) {
            return 0;
        }
        return (int) Math.round((completedGoals * 10000.0d) / totalGoals);
    }
}
