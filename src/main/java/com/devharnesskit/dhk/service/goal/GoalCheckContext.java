package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.goal.GoalProfile;
import com.devharnesskit.dhk.model.goal.GoalRun;

import java.nio.file.Path;
import java.sql.Connection;

public final class GoalCheckContext {
    private final Connection connection;
    private final Path projectRoot;
    private final GoalRun goal;
    private final String checkKey;
    private final String now;
    private final GoalCheckPolicy policy;
    private final GoalProfile profile;

    public GoalCheckContext(Connection connection, Path projectRoot, GoalRun goal, String checkKey,
                            String now, GoalCheckPolicy policy, GoalProfile profile) {
        this.connection = connection;
        this.projectRoot = projectRoot;
        this.goal = goal;
        this.checkKey = checkKey == null ? "" : checkKey;
        this.now = now == null ? "" : now;
        this.policy = policy;
        this.profile = profile;
    }

    public Connection connection() { return connection; }
    public Path projectRoot() { return projectRoot; }
    public GoalRun goal() { return goal; }
    public String checkKey() { return checkKey; }
    public String now() { return now; }
    public GoalCheckPolicy policy() { return policy; }
    public GoalProfile profile() { return profile; }
}
