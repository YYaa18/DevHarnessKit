package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.goal.GoalCheck;
import com.devharnesskit.dhk.model.graph.GraphArchitectureCheckResult;
import com.devharnesskit.dhk.service.graph.GraphArchitectureCheckService;

import java.nio.file.Path;

final class ArchitectureGoalCheckRunner extends AbstractGoalCheckRunner {
    private final GoalCheckRecorder recorder;
    private final GraphArchitectureCheckService architectureCheckService;

    ArchitectureGoalCheckRunner(GoalCheckRecorder recorder,
                                GraphArchitectureCheckService architectureCheckService) {
        super("architecture");
        this.recorder = recorder;
        this.architectureCheckService = architectureCheckService;
    }

    public GoalCheck run(GoalCheckContext context) throws Exception {
        Path log = recorder.logPath(context.projectRoot(), context.goal(), key());
        GraphArchitectureCheckResult result = architectureCheckService.check(context.connection(),
                context.projectRoot());
        recorder.writeLog(log, result.output());
        return recorder.save(context.connection(), context.projectRoot(), context.goal(), key(), "architecture",
                "", result.status(), result.summary(), log, context.now());
    }
}
