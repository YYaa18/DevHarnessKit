package com.devharnesskit.dhk.service.goal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class GoalCheckRunnerRegistry {
    private final Map<String, GoalCheckRunner> runners;

    private GoalCheckRunnerRegistry(Map<String, GoalCheckRunner> runners) {
        this.runners = Collections.unmodifiableMap(new LinkedHashMap<String, GoalCheckRunner>(runners));
    }

    public static GoalCheckRunnerRegistry of(GoalCheckRunner... runnerList) {
        Map<String, GoalCheckRunner> runners = new LinkedHashMap<String, GoalCheckRunner>();
        for (GoalCheckRunner runner : runnerList) {
            register(runners, runner);
        }
        return new GoalCheckRunnerRegistry(runners);
    }

    public GoalCheckRunner find(String key) {
        return runners.get(key);
    }

    public Set<String> keys() {
        return runners.keySet();
    }

    private static void register(Map<String, GoalCheckRunner> runners, GoalCheckRunner runner) {
        runners.put(runner.key(), runner);
    }
}
