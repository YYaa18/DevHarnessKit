package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.util.PathUtil;
import com.devharnesskit.dhk.util.ProcessCommandUtil;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

final class GoalCheckCommandExecutor {
    static final int MAX_COMMAND_LOG_BYTES = 256 * 1024;

    GoalCheckCommandResult execute(Path projectRoot, String[] command, int timeoutSeconds) throws Exception {
        String[] resolvedCommand = ProcessCommandUtil.resolveExecutable(command);
        ProcessBuilder builder = new ProcessBuilder(resolvedCommand);
        builder.directory(projectRoot.toFile());
        builder.redirectErrorStream(true);
        Instant startedAt = Instant.now();
        long startedNanos = System.nanoTime();
        Process process = builder.start();
        final BoundedOutput output = new BoundedOutput(MAX_COMMAND_LOG_BYTES);
        final InputStream input = process.getInputStream();
        Thread reader = new Thread(new Runnable() {
            public void run() {
                byte[] buffer = new byte[4096];
                try {
                    int read;
                    while ((read = input.read(buffer)) >= 0) {
                        output.write(buffer, 0, read);
                    }
                } catch (Exception ex) {
                    output.write("\nOUTPUT READER ERROR: " + ex.getMessage() + "\n");
                }
            }
        }, "dhk-goal-check-output-reader");
        reader.setDaemon(true);
        reader.start();

        boolean timedOut = false;
        boolean finished;
        if (timeoutSeconds <= 0) {
            process.waitFor();
            finished = true;
        } else {
            finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        }
        int exitCode;
        if (!finished) {
            timedOut = true;
            process.destroy();
            if (!process.waitFor(1, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor(1, TimeUnit.SECONDS);
            }
            exitCode = 124;
        } else {
            exitCode = process.exitValue();
        }
        reader.join(1000L);
        Instant finishedAt = Instant.now();
        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos);
        return new GoalCheckCommandResult(exitCode, commandLog(projectRoot, resolvedCommand, startedAt,
                finishedAt, durationMs, timedOut, output), durationMs, timedOut, output.truncated());
    }

    private String commandLog(Path projectRoot, String[] command, Instant startedAt, Instant finishedAt,
                              long durationMs, boolean timedOut, BoundedOutput output) throws Exception {
        StringBuilder builder = new StringBuilder();
        builder.append("command: ").append(GoalCheckSupport.join(command)).append('\n');
        builder.append("working_directory: ").append(PathUtil.displayPath(projectRoot)).append('\n');
        builder.append("started_at: ").append(startedAt.toString()).append('\n');
        builder.append("finished_at: ").append(finishedAt.toString()).append('\n');
        builder.append("duration_ms: ").append(durationMs).append('\n');
        builder.append("timeout: ").append(timedOut).append('\n');
        builder.append("output_truncated: ").append(output.truncated()).append('\n');
        builder.append('\n');
        builder.append(output.text());
        if (timedOut) {
            builder.append("\nCHECK TIMEOUT\n");
        }
        if (output.truncated()) {
            builder.append("\nCOMMAND OUTPUT TRUNCATED after ")
                    .append(MAX_COMMAND_LOG_BYTES).append(" bytes\n");
        }
        return builder.toString();
    }

    private static final class BoundedOutput {
        private final byte[] buffer;
        private int size;
        private boolean truncated;

        private BoundedOutput(int maxBytes) {
            this.buffer = new byte[maxBytes];
        }

        synchronized void write(byte[] source, int offset, int length) {
            if (length <= 0 || source == null) {
                return;
            }
            int remaining = buffer.length - size;
            if (remaining <= 0) {
                truncated = true;
                return;
            }
            int copy = Math.min(remaining, length);
            System.arraycopy(source, offset, buffer, size, copy);
            size += copy;
            if (copy < length) {
                truncated = true;
            }
        }

        void write(String value) {
            if (value == null) {
                return;
            }
            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            write(bytes, 0, bytes.length);
        }

        synchronized String text() throws Exception {
            return new String(buffer, 0, size, "UTF-8");
        }

        synchronized boolean truncated() {
            return truncated;
        }
    }
}
