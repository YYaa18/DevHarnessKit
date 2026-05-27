package com.devharnesskit.dhk.service.goal;

abstract class AbstractGoalCheckRunner implements GoalCheckRunner {
    private final String key;

    AbstractGoalCheckRunner(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
