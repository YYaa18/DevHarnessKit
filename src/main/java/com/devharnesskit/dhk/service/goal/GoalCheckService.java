package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.goal.GoalCheck;
import com.devharnesskit.dhk.model.goal.GoalProfile;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.model.spec.SpecAcceptance;
import com.devharnesskit.dhk.model.spec.SpecTask;
import com.devharnesskit.dhk.model.workflow.WorkflowGateRun;
import com.devharnesskit.dhk.model.workflow.WorkflowRun;
import com.devharnesskit.dhk.repository.goal.GoalCheckRepository;
import com.devharnesskit.dhk.repository.spec.SpecAcceptanceRepository;
import com.devharnesskit.dhk.repository.spec.SpecTaskRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowGateRunRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowRunRepository;
import com.devharnesskit.dhk.service.SensitiveDataGuard;
import com.devharnesskit.dhk.util.PathUtil;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class GoalCheckService {
    public static final String[] REQUIRED_CHECKS = GoalCheckPolicy.DEFAULT_REQUIRED_CHECKS;
    static final int MAX_COMMAND_LOG_BYTES = 256 * 1024;

    private final GoalCheckRepository checkRepository;
    private final SpecTaskRepository taskRepository;
    private final SpecAcceptanceRepository acceptanceRepository;
    private final WorkflowRunRepository workflowRunRepository;
    private final WorkflowGateRunRepository gateRunRepository;
    private final SensitiveDataGuard sensitiveDataGuard;
    private final GoalCheckPolicyService policyService;
    private final GoalProfileService profileService;
    private final WorkspaceFingerprintService fingerprintService;

    public GoalCheckService() {
        this(new GoalCheckRepository(), new SpecTaskRepository(), new SpecAcceptanceRepository(),
                new WorkflowRunRepository(), new WorkflowGateRunRepository(), new SensitiveDataGuard(),
                new GoalCheckPolicyService(), new GoalProfileService(), new WorkspaceFingerprintService());
    }

    GoalCheckService(GoalCheckRepository checkRepository, SpecTaskRepository taskRepository,
                     SpecAcceptanceRepository acceptanceRepository,
                     WorkflowRunRepository workflowRunRepository,
                     WorkflowGateRunRepository gateRunRepository,
                     SensitiveDataGuard sensitiveDataGuard,
                     GoalCheckPolicyService policyService,
                     GoalProfileService profileService,
                     WorkspaceFingerprintService fingerprintService) {
        this.checkRepository = checkRepository;
        this.taskRepository = taskRepository;
        this.acceptanceRepository = acceptanceRepository;
        this.workflowRunRepository = workflowRunRepository;
        this.gateRunRepository = gateRunRepository;
        this.sensitiveDataGuard = sensitiveDataGuard;
        this.policyService = policyService;
        this.profileService = profileService;
        this.fingerprintService = fingerprintService;
    }

    public GoalCheck run(Connection connection, Path projectRoot, GoalRun goal,
                         String checkKey, String now) throws Exception {
        return run(connection, projectRoot, goal, checkKey, now, policyService.load(projectRoot));
    }

    public GoalCheck run(Connection connection, Path projectRoot, GoalRun goal,
                         String checkKey, String now, GoalCheckPolicy policy) throws Exception {
        GoalProfile profile = profileService.find(projectRoot, goal.profileKey());
        if ("compile".equals(checkKey)) {
            return runMavenCheck(connection, projectRoot, goal, checkKey, policy.compileCommand(), now);
        }
        if ("test".equals(checkKey)) {
            return runMavenCheck(connection, projectRoot, goal, checkKey, policy.testCommand(), now);
        }
        if ("sensitive".equals(checkKey)) {
            return runSensitiveCheck(connection, projectRoot, goal, now);
        }
        if ("spec".equals(checkKey)) {
            return runSpecCheck(connection, projectRoot, goal, profile, now);
        }
        if ("workflow".equals(checkKey)) {
            return runWorkflowCheck(connection, projectRoot, goal, now, policy, profile);
        }
        throw new IllegalArgumentException("Unknown goal check: " + checkKey);
    }

    public List<GoalCheck> runAll(Connection connection, Path projectRoot, GoalRun goal,
                                  String now) throws Exception {
        GoalCheckPolicy policy = policyService.load(projectRoot);
        List<GoalCheck> results = new ArrayList<GoalCheck>();
        for (String check : policy.requiredChecks(profileService.find(projectRoot, goal.profileKey()))) {
            results.add(run(connection, projectRoot, goal, check, now, policy));
        }
        return results;
    }

    private GoalCheck runMavenCheck(Connection connection, Path projectRoot, GoalRun goal,
                                    String checkKey, String[] command, String now) throws Exception {
        Path log = logPath(projectRoot, goal, checkKey);
        String commandText = join(command);
        if (!Files.isRegularFile(projectRoot.resolve("pom.xml"))) {
            String summary = "pom.xml not found; " + checkKey + " check skipped";
            writeLog(log, summary + "\n");
            return save(connection, projectRoot, goal, checkKey, "command", commandText, "skipped", summary, log, now);
        }
        CommandResult result = execute(projectRoot, command, 120);
        writeLog(log, result.output());
        String status = result.exitCode() == 0 ? "passed" : "failed";
        String summary = checkKey + " exit_code=" + result.exitCode()
                + " duration_ms=" + result.durationMs()
                + " output_truncated=" + result.truncated();
        return save(connection, projectRoot, goal, checkKey, "command", commandText, status, summary, log, now);
    }

    private GoalCheck runSensitiveCheck(Connection connection, Path projectRoot, GoalRun goal,
                                        String now) throws Exception {
        Path log = logPath(projectRoot, goal, "sensitive");
        Path[] files = new Path[]{
                PathUtil.currentContext(projectRoot),
                PathUtil.goalContext(projectRoot),
                PathUtil.workflowContext(projectRoot),
                PathUtil.specContext(projectRoot)
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
        writeLog(log, output.toString());
        String status = failures.isEmpty() ? "passed" : "failed";
        String summary = failures.isEmpty()
                ? "sensitive scan passed; files_scanned=" + scanned
                : "sensitive scan failed: " + failures;
        return save(connection, projectRoot, goal, "sensitive", "sensitive", "", status, summary, log, now);
    }

    private GoalCheck runSpecCheck(Connection connection, Path projectRoot, GoalRun goal, GoalProfile profile,
                                   String now) throws Exception {
        Path log = null;
        if (goal.specChangeKey().length() == 0) {
            if (profile != null && profile.specRequired()) {
                return save(connection, projectRoot, goal, "spec", "spec", "", "failed",
                        "spec required but goal has no spec change", log, now);
            }
            return save(connection, projectRoot, goal, "spec", "spec", "", "skipped",
                    "goal has no spec change", log, now);
        }
        List<String> missing = new ArrayList<String>();
        List<SpecTask> tasks = taskRepository.listByChange(connection, goal.specChangeKey());
        List<SpecAcceptance> acceptances = acceptanceRepository.listByChange(connection, goal.specChangeKey());
        if (profile != null && profile.specRequired() && profile.specRequireNonEmptyTasks() && tasks.isEmpty()) {
            missing.add("spec has no tasks");
        }
        if (profile != null && profile.specRequired() && profile.specRequireNonEmptyAcceptance()
                && acceptances.isEmpty()) {
            missing.add("spec has no acceptance criteria");
        }
        for (SpecTask task : tasks) {
            if (!"done".equals(task.status()) && !"skipped".equals(task.status())) {
                missing.add("task " + task.taskKey() + " is " + task.status());
            }
        }
        for (SpecAcceptance acceptance : acceptances) {
            if (!"passed".equals(acceptance.status()) && !"waived".equals(acceptance.status())) {
                missing.add("acceptance " + acceptance.acceptanceKey() + " is " + acceptance.status());
            }
        }
        String status = missing.isEmpty() ? "passed" : "failed";
        String summary = missing.isEmpty()
                ? "spec tasks and acceptance are closed"
                : "spec incomplete: " + missing;
        return save(connection, projectRoot, goal, "spec", "spec", "", status, summary, log, now);
    }

    private GoalCheck runWorkflowCheck(Connection connection, Path projectRoot, GoalRun goal, String now,
                                       GoalCheckPolicy policy, GoalProfile profile) throws Exception {
        if (goal.workflowRunKey().length() == 0) {
            return save(connection, projectRoot, goal, "workflow", "workflow", "", "skipped",
                    "goal has no workflow run", null, now);
        }
        WorkflowRun run = workflowRunRepository.findByKey(connection, goal.workflowRunKey());
        if (run == null) {
            return save(connection, projectRoot, goal, "workflow", "workflow", "", "failed",
                    "workflow run not found", null, now);
        }
        if ("blocked".equals(run.status()) || "failed".equals(run.status()) || "abandoned".equals(run.status())) {
            return save(connection, projectRoot, goal, "workflow", "workflow", "", "failed",
                    "workflow run status is " + run.status(), null, now);
        }
        int failedHard = 0;
        int pendingHard = 0;
        int pendingCompletionHard = 0;
        for (WorkflowGateRun gate : gateRunRepository.listByRun(connection, run.runKey())) {
            if ("hard".equals(gate.severity()) && "failed".equals(gate.status())) {
                failedHard++;
            }
            if ("hard".equals(gate.severity()) && "pending".equals(gate.status())) {
                if (profile != null && profile.completionRequireCheckpoint()
                        && "checkpoint_created".equals(gate.gateKey())) {
                    pendingCompletionHard++;
                    continue;
                }
                pendingHard++;
            }
        }
        if (failedHard > 0) {
            return save(connection, projectRoot, goal, "workflow", "workflow", "", "failed",
                    "workflow has failed hard gates: " + failedHard, null, now);
        }
        if (policy.failPendingHardGates(profile) && pendingHard > 0) {
            return save(connection, projectRoot, goal, "workflow", "workflow", "", "failed",
                    "workflow has pending hard gates: " + pendingHard, null, now);
        }
        return save(connection, projectRoot, goal, "workflow", "workflow", "", "passed",
                "workflow run is active; pending_hard_gates=" + pendingHard
                        + " pending_completion_gates=" + pendingCompletionHard, null, now);
    }

    private GoalCheck save(Connection connection, Path projectRoot, GoalRun goal, String checkKey, String checkType,
                           String command, String status, String summary, Path evidencePath,
                           String now) throws Exception {
        String evidence = evidencePath == null ? "" : evidencePath.toString();
        String workspaceFingerprint = fingerprintService.workspaceFingerprint(projectRoot);
        String contextFingerprint = fingerprintService.contextFingerprint(projectRoot);
        String checkFingerprint = fingerprintService.checkFingerprint(checkKey, status, summary, evidence,
                workspaceFingerprint, contextFingerprint);
        GoalCheck check = new GoalCheck(0L, goal.goalKey(), checkKey, checkType, true, goal.stepCount(),
                command, status, summary, evidence, workspaceFingerprint, contextFingerprint,
                checkFingerprint, now, now, now);
        checkRepository.upsert(connection, check);
        return checkRepository.find(connection, goal.goalKey(), checkKey);
    }

    private Path logPath(Path projectRoot, GoalRun goal, String checkKey) {
        return PathUtil.goalCheckArtifactsDirectory(projectRoot, goal.goalKey()).resolve(checkKey + ".log");
    }

    private void writeLog(Path path, String content) throws Exception {
        Files.createDirectories(path.getParent());
        Files.write(path, content.getBytes("UTF-8"));
    }

    CommandResult execute(Path projectRoot, String[] command, int timeoutSeconds) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(command);
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
        return new CommandResult(exitCode, commandLog(projectRoot, command, startedAt,
                finishedAt, durationMs, timedOut, output), durationMs, timedOut, output.truncated());
    }

    private String commandLog(Path projectRoot, String[] command, Instant startedAt, Instant finishedAt,
                              long durationMs, boolean timedOut, BoundedOutput output) throws Exception {
        StringBuilder builder = new StringBuilder();
        builder.append("command: ").append(join(command)).append('\n');
        builder.append("working_directory: ").append(projectRoot.toAbsolutePath().normalize()).append('\n');
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

    private String join(String[] command) {
        StringBuilder builder = new StringBuilder();
        for (String part : command) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(part);
        }
        return builder.toString();
    }

    static final class CommandResult {
        private final int exitCode;
        private final String output;
        private final long durationMs;
        private final boolean timedOut;
        private final boolean truncated;

        private CommandResult(int exitCode, String output, long durationMs,
                              boolean timedOut, boolean truncated) {
            this.exitCode = exitCode;
            this.output = output == null ? "" : output;
            this.durationMs = durationMs;
            this.timedOut = timedOut;
            this.truncated = truncated;
        }

        int exitCode() { return exitCode; }
        String output() { return output; }
        long durationMs() { return durationMs; }
        boolean timedOut() { return timedOut; }
        boolean truncated() { return truncated; }
    }

    private static final class BoundedOutput {
        private final byte[] buffer;
        private int size;
        private boolean truncated;

        private BoundedOutput(int maxBytes) {
            this.buffer = new byte[maxBytes];
        }

        synchronized void write(byte[] source, int offset, int length) {
            if (length <= 0) {
                return;
            }
            if (source == null) {
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
