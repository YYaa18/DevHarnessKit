package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.goal.GoalCheck;
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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class GoalCheckService {
    public static final String[] REQUIRED_CHECKS = GoalCheckPolicy.DEFAULT_REQUIRED_CHECKS;

    private final GoalCheckRepository checkRepository;
    private final SpecTaskRepository taskRepository;
    private final SpecAcceptanceRepository acceptanceRepository;
    private final WorkflowRunRepository workflowRunRepository;
    private final WorkflowGateRunRepository gateRunRepository;
    private final SensitiveDataGuard sensitiveDataGuard;
    private final GoalCheckPolicyService policyService;

    public GoalCheckService() {
        this(new GoalCheckRepository(), new SpecTaskRepository(), new SpecAcceptanceRepository(),
                new WorkflowRunRepository(), new WorkflowGateRunRepository(), new SensitiveDataGuard(),
                new GoalCheckPolicyService());
    }

    GoalCheckService(GoalCheckRepository checkRepository, SpecTaskRepository taskRepository,
                     SpecAcceptanceRepository acceptanceRepository,
                     WorkflowRunRepository workflowRunRepository,
                     WorkflowGateRunRepository gateRunRepository,
                     SensitiveDataGuard sensitiveDataGuard,
                     GoalCheckPolicyService policyService) {
        this.checkRepository = checkRepository;
        this.taskRepository = taskRepository;
        this.acceptanceRepository = acceptanceRepository;
        this.workflowRunRepository = workflowRunRepository;
        this.gateRunRepository = gateRunRepository;
        this.sensitiveDataGuard = sensitiveDataGuard;
        this.policyService = policyService;
    }

    public GoalCheck run(Connection connection, Path projectRoot, GoalRun goal,
                         String checkKey, String now) throws Exception {
        return run(connection, projectRoot, goal, checkKey, now, policyService.load(projectRoot));
    }

    public GoalCheck run(Connection connection, Path projectRoot, GoalRun goal,
                         String checkKey, String now, GoalCheckPolicy policy) throws Exception {
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
            return runSpecCheck(connection, goal, now);
        }
        if ("workflow".equals(checkKey)) {
            return runWorkflowCheck(connection, goal, now, policy);
        }
        throw new IllegalArgumentException("Unknown goal check: " + checkKey);
    }

    public List<GoalCheck> runAll(Connection connection, Path projectRoot, GoalRun goal,
                                  String now) throws Exception {
        GoalCheckPolicy policy = policyService.load(projectRoot);
        List<GoalCheck> results = new ArrayList<GoalCheck>();
        for (String check : policy.requiredChecks()) {
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
            return save(connection, goal, checkKey, "command", commandText, "skipped", summary, log, now);
        }
        CommandResult result = execute(projectRoot, command, 120);
        writeLog(log, result.output());
        String status = result.exitCode() == 0 ? "passed" : "failed";
        String summary = checkKey + " exit_code=" + result.exitCode();
        return save(connection, goal, checkKey, "command", commandText, status, summary, log, now);
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
        return save(connection, goal, "sensitive", "sensitive", "", status, summary, log, now);
    }

    private GoalCheck runSpecCheck(Connection connection, GoalRun goal, String now) throws Exception {
        Path log = null;
        if (goal.specChangeKey().length() == 0) {
            return save(connection, goal, "spec", "spec", "", "skipped",
                    "goal has no spec change", log, now);
        }
        List<String> missing = new ArrayList<String>();
        for (SpecTask task : taskRepository.listByChange(connection, goal.specChangeKey())) {
            if (!"done".equals(task.status()) && !"skipped".equals(task.status())) {
                missing.add("task " + task.taskKey() + " is " + task.status());
            }
        }
        for (SpecAcceptance acceptance : acceptanceRepository.listByChange(connection, goal.specChangeKey())) {
            if (!"passed".equals(acceptance.status()) && !"waived".equals(acceptance.status())) {
                missing.add("acceptance " + acceptance.acceptanceKey() + " is " + acceptance.status());
            }
        }
        String status = missing.isEmpty() ? "passed" : "failed";
        String summary = missing.isEmpty()
                ? "spec tasks and acceptance are closed or empty"
                : "spec incomplete: " + missing;
        return save(connection, goal, "spec", "spec", "", status, summary, log, now);
    }

    private GoalCheck runWorkflowCheck(Connection connection, GoalRun goal, String now,
                                      GoalCheckPolicy policy) throws Exception {
        if (goal.workflowRunKey().length() == 0) {
            return save(connection, goal, "workflow", "workflow", "", "skipped",
                    "goal has no workflow run", null, now);
        }
        WorkflowRun run = workflowRunRepository.findByKey(connection, goal.workflowRunKey());
        if (run == null) {
            return save(connection, goal, "workflow", "workflow", "", "failed",
                    "workflow run not found", null, now);
        }
        if ("blocked".equals(run.status()) || "failed".equals(run.status()) || "abandoned".equals(run.status())) {
            return save(connection, goal, "workflow", "workflow", "", "failed",
                    "workflow run status is " + run.status(), null, now);
        }
        int failedHard = 0;
        int pendingHard = 0;
        for (WorkflowGateRun gate : gateRunRepository.listByRun(connection, run.runKey())) {
            if ("hard".equals(gate.severity()) && "failed".equals(gate.status())) {
                failedHard++;
            }
            if ("hard".equals(gate.severity()) && "pending".equals(gate.status())) {
                pendingHard++;
            }
        }
        if (failedHard > 0) {
            return save(connection, goal, "workflow", "workflow", "", "failed",
                    "workflow has failed hard gates: " + failedHard, null, now);
        }
        if (policy.failPendingHardGates() && pendingHard > 0) {
            return save(connection, goal, "workflow", "workflow", "", "failed",
                    "workflow has pending hard gates: " + pendingHard, null, now);
        }
        return save(connection, goal, "workflow", "workflow", "", "passed",
                "workflow run is active; pending_hard_gates=" + pendingHard, null, now);
    }

    private GoalCheck save(Connection connection, GoalRun goal, String checkKey, String checkType,
                           String command, String status, String summary, Path evidencePath,
                           String now) throws Exception {
        GoalCheck check = new GoalCheck(0L, goal.goalKey(), checkKey, checkType, true,
                command, status, summary, evidencePath == null ? "" : evidencePath.toString(),
                now, now, now);
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

    private CommandResult execute(Path projectRoot, String[] command, int timeoutSeconds) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(projectRoot.toFile());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        InputStream input = process.getInputStream();
        byte[] buffer = new byte[4096];
        long deadline = timeoutSeconds <= 0
                ? Long.MAX_VALUE
                : System.currentTimeMillis() + (timeoutSeconds * 1000L);
        while (true) {
            drain(input, buffer, output);
            if (process.waitFor(100, TimeUnit.MILLISECONDS)) {
                break;
            }
            if (System.currentTimeMillis() >= deadline) {
                process.destroy();
                output.write("\nCHECK TIMEOUT\n".getBytes("UTF-8"));
                return new CommandResult(124, output.toString("UTF-8"));
            }
        }
        drain(input, buffer, output);
        return new CommandResult(process.exitValue(), output.toString("UTF-8"));
    }

    private void drain(InputStream input, byte[] buffer, ByteArrayOutputStream output) throws Exception {
        while (input.available() > 0) {
            int read = input.read(buffer);
            if (read < 0) {
                break;
            }
            output.write(buffer, 0, read);
        }
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

    private static final class CommandResult {
        private final int exitCode;
        private final String output;

        private CommandResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output == null ? "" : output;
        }

        private int exitCode() { return exitCode; }
        private String output() { return output; }
    }
}
