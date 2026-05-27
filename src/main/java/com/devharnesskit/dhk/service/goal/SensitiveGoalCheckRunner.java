package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.goal.GoalCheck;
import com.devharnesskit.dhk.service.SensitiveDataGuard;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class SensitiveGoalCheckRunner extends AbstractGoalCheckRunner {
    private final GoalCheckRecorder recorder;
    private final SensitiveDataGuard sensitiveDataGuard;

    SensitiveGoalCheckRunner(GoalCheckRecorder recorder, SensitiveDataGuard sensitiveDataGuard) {
        super("sensitive");
        this.recorder = recorder;
        this.sensitiveDataGuard = sensitiveDataGuard;
    }

    public GoalCheck run(GoalCheckContext context) throws Exception {
        Path log = recorder.logPath(context.projectRoot(), context.goal(), key());
        Path[] files = new Path[]{
                PathUtil.currentContext(context.projectRoot()),
                PathUtil.goalContext(context.projectRoot()),
                PathUtil.workflowContext(context.projectRoot()),
                PathUtil.specContext(context.projectRoot())
        };
        StringBuilder output = new StringBuilder();
        List<String> failures = new ArrayList<String>();
        int scanned = 0;
        for (Path file : files) {
            if (!Files.isRegularFile(file)) {
                continue;
            }
            scanned++;
            String original = new String(Files.readAllBytes(file), "UTF-8");
            List<String> matches = sensitiveDataGuard.findMatches(original);
            output.append(file).append(": ").append(matches.isEmpty() ? "ok" : matches.toString()).append('\n');
            if (!matches.isEmpty()) {
                failures.add(file.getFileName() + " " + matches);
            }
        }
        if (scanned == 0) {
            output.append("no context files found\n");
        }
        recorder.writeLog(log, output.toString());
        String status = failures.isEmpty() ? "passed" : "failed";
        String summary = failures.isEmpty()
                ? "sensitive scan passed; files_scanned=" + scanned
                : "sensitive scan failed: " + failures;
        return recorder.save(context.connection(), context.projectRoot(), context.goal(), key(), "sensitive",
                "", status, summary, log, context.now());
    }
}
