package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.goal.GoalCheck;

import java.nio.file.Files;
import java.nio.file.Path;

final class MavenGoalCheckRunner extends AbstractGoalCheckRunner {
    private final GoalCheckRecorder recorder;
    private final GoalCheckCommandExecutor executor;

    MavenGoalCheckRunner(String key, GoalCheckRecorder recorder, GoalCheckCommandExecutor executor) {
        super(key);
        this.recorder = recorder;
        this.executor = executor;
    }

    public GoalCheck run(GoalCheckContext context) throws Exception {
        Path log = recorder.logPath(context.projectRoot(), context.goal(), key());
        String[] command = "compile".equals(key()) ? context.policy().compileCommand()
                : context.policy().testCommand();
        String commandText = GoalCheckSupport.join(command);
        if (!Files.isRegularFile(context.projectRoot().resolve("pom.xml"))) {
            String summary = "pom.xml not found; " + key() + " check skipped";
            recorder.writeLog(log, summary + "\n");
            return recorder.save(context.connection(), context.projectRoot(), context.goal(), key(), "command",
                    commandText, "skipped", summary, log, context.now());
        }
        GoalCheckCommandResult result = executor.execute(context.projectRoot(), command, 120);
        recorder.writeLog(log, result.output());
        String status = result.exitCode() == 0 ? "passed" : "failed";
        String summary = key() + " exit_code=" + result.exitCode()
                + " duration_ms=" + result.durationMs()
                + " output_truncated=" + result.truncated();
        return recorder.save(context.connection(), context.projectRoot(), context.goal(), key(), "command",
                commandText, status, summary, log, context.now());
    }
}
