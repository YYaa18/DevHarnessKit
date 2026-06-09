package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.context.artifact.ContextArtifact;
import com.devharnesskit.dhk.context.artifact.ContextArtifactService;
import com.devharnesskit.dhk.context.compress.BuildLogCompressor;
import com.devharnesskit.dhk.context.compress.CompressResult;
import com.devharnesskit.dhk.context.compress.TestLogCompressor;
import com.devharnesskit.dhk.model.goal.GoalCheck;
import com.devharnesskit.dhk.util.Clock;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

final class MavenGoalCheckRunner extends AbstractGoalCheckRunner {
    private final GoalCheckRecorder recorder;
    private final GoalCheckCommandExecutor executor;
    private final ContextArtifactService artifactService;

    MavenGoalCheckRunner(String key, GoalCheckRecorder recorder, GoalCheckCommandExecutor executor) {
        this(key, recorder, executor, new ContextArtifactService());
    }

    MavenGoalCheckRunner(String key, GoalCheckRecorder recorder, GoalCheckCommandExecutor executor,
                         ContextArtifactService artifactService) {
        super(key);
        this.recorder = recorder;
        this.executor = executor;
        this.artifactService = artifactService;
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
        CompressResult compressed = compress(commandText, result);
        ContextArtifact artifact = artifactService.persist(context.projectRoot(), context.connection(),
                context.goal().projectKey(), context.goal().goalKey(), "", compressed.sourceType(),
                PathUtil.displayPath(log), result.output(), compressed, new GoalCheckClock(context.now()));
        recorder.writeLog(log, digestWithArtifact(compressed, artifact));
        String status = result.exitCode() == 0 ? "passed" : "failed";
        String summary = key() + " exit_code=" + result.exitCode()
                + " duration_ms=" + result.durationMs()
                + " output_truncated=" + result.truncated()
                + " context_artifact=" + artifact.artifactKey();
        return recorder.save(context.connection(), context.projectRoot(), context.goal(), key(), "command",
                commandText, status, summary, log, context.now());
    }

    private CompressResult compress(String commandText, GoalCheckCommandResult result) {
        if ("test".equals(key())) {
            return new TestLogCompressor().compressTest(commandText, result.exitCode(), result.output());
        }
        return new BuildLogCompressor().compressBuild(commandText, result.exitCode(), result.output());
    }

    private String digestWithArtifact(CompressResult compressed, ContextArtifact artifact) {
        StringBuilder builder = new StringBuilder();
        builder.append(compressed.compressedText());
        if (!compressed.compressedText().endsWith("\n")) {
            builder.append('\n');
        }
        builder.append("artifact_ref: ").append(artifact.artifactKey()).append('\n');
        builder.append("retrieve_command: dhk context retrieve ").append(artifact.artifactKey()).append('\n');
        return builder.toString();
    }

    private static final class GoalCheckClock implements Clock {
        private final Instant now;

        private GoalCheckClock(String rawNow) {
            Instant parsed;
            try {
                parsed = rawNow == null || rawNow.length() == 0 ? Instant.now() : Instant.parse(rawNow);
            } catch (Exception ex) {
                parsed = Instant.now();
            }
            this.now = parsed;
        }

        public Instant now() {
            return now;
        }
    }
}
