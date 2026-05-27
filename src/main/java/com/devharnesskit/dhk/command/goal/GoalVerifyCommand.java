package com.devharnesskit.dhk.command.goal;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.model.goal.GoalCheck;
import com.devharnesskit.dhk.model.goal.GoalEvaluation;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.service.goal.GoalOrchestrator;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public final class GoalVerifyCommand implements Command {
    private final GoalOrchestrator orchestrator = new GoalOrchestrator();

    public int run(CommandContext context, Args args) {
        Path projectRoot = GoalCommandSupport.projectRoot(args, context);
        try {
            String level = level(args);
            GoalRun goal = GoalCommandSupport.goal(orchestrator, context, args, projectRoot);
            String[] selectedChecks = checksForLevel(context, projectRoot, goal.goalKey(), level);
            List<GoalCheck> checks = runSelectedChecks(context, projectRoot, goal.goalKey(), selectedChecks);
            GoalEvaluation evaluation = orchestrator.evaluate(context, projectRoot, goal.goalKey());
            ReleaseChecks releaseChecks = "release".equals(level) ? runReleaseChecks(projectRoot) : ReleaseChecks.none();
            if (JsonOutput.enabled(args)) {
                printJson(context, projectRoot, goal, checks, evaluation, level, selectedChecks, releaseChecks);
            } else {
                printText(context, projectRoot, goal, checks, evaluation, level, selectedChecks, releaseChecks);
            }
            return ExitCodes.SUCCESS;
        } catch (IllegalArgumentException ex) {
            context.err().println(ex.getMessage());
            return ExitCodes.VALIDATION_ERROR;
        } catch (Exception ex) {
            context.err().println("ERROR goal verify failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private void printText(CommandContext context, Path projectRoot, GoalRun goal, List<GoalCheck> checks,
                           GoalEvaluation evaluation, String level, String[] selectedChecks,
                           ReleaseChecks releaseChecks) {
        List<BlockerDetail> blockers = blockerDetails(goal, checks, evaluation);
        context.out().println("goal_key: " + goal.goalKey());
        context.out().println("level: " + level);
        context.out().println("check_scope: " + ("standard".equals(level) ? "all_required" : level));
        context.out().println("selected_checks:");
        printArray(context, selectedChecks);
        context.out().println("decision: " + evaluation.decision());
        context.out().println("ready_to_complete: " + evaluation.readyToComplete());
        context.out().println("checks:");
        for (GoalCheck check : checks) {
            context.out().println("  - " + check.checkKey() + ": " + check.status()
                    + " - " + check.resultSummary());
        }
        context.out().println("failed_checks:");
        printStringList(context, failedChecks(checks));
        context.out().println("freshness_status: " + freshnessStatus(evaluation));
        context.out().println("missing:");
        printArray(context, evaluation.missing());
        context.out().println("completion_blockers:");
        printArray(context, evaluation.missing());
        context.out().println("blocker_summary: " + blockerSummary(blockers));
        context.out().println("blocker_categories:");
        printArray(context, blockerCategories(blockers));
        context.out().println("blocker_details:");
        printBlockers(context, blockers);
        context.out().println("stale_checks:");
        printArray(context, evaluation.staleChecks());
        printReleaseChecks(context, releaseChecks);
        context.out().println("next_action: " + evaluation.nextAction());
        context.out().println("next_command: " + evaluation.nextCommand());
        context.out().println("context_path: " + PathUtil.goalContext(projectRoot));
    }

    private void printJson(CommandContext context, Path projectRoot, GoalRun goal, List<GoalCheck> checks,
                           GoalEvaluation evaluation, String level, String[] selectedChecks,
                           ReleaseChecks releaseChecks) {
        List<String> rawChecks = new ArrayList<String>();
        for (GoalCheck check : checks) {
            rawChecks.add(JsonOutput.object(
                    JsonOutput.stringField("check_key", check.checkKey()),
                    JsonOutput.stringField("status", check.status()),
                    JsonOutput.numberField("step_count_at_check", check.stepCountAtCheck()),
                    JsonOutput.stringField("workspace_fingerprint", check.workspaceFingerprint()),
                    JsonOutput.stringField("context_fingerprint", check.contextFingerprint()),
                    JsonOutput.stringField("check_fingerprint", check.checkFingerprint()),
                    JsonOutput.stringField("result_summary", check.resultSummary()),
                    JsonOutput.stringField("evidence_path", check.evidencePath())
            ).trim());
        }
        List<BlockerDetail> blockers = blockerDetails(goal, checks, evaluation);
        context.out().print(JsonOutput.object(
                JsonOutput.stringField("command", "goal verify"),
                JsonOutput.stringField("goal_key", goal.goalKey()),
                JsonOutput.stringField("level", level),
                JsonOutput.stringField("check_scope", "standard".equals(level) ? "all_required" : level),
                JsonOutput.rawField("selected_checks", JsonOutput.stringArray(selectedChecks)),
                JsonOutput.stringField("decision", evaluation.decision()),
                JsonOutput.booleanField("ready_to_complete", evaluation.readyToComplete()),
                JsonOutput.numberField("check_count", checks.size()),
                JsonOutput.rawField("checks", JsonOutput.array(rawChecks)),
                JsonOutput.numberField("failed_count", failedChecks(checks).length),
                JsonOutput.rawField("failed_checks", JsonOutput.stringArray(failedChecks(checks))),
                JsonOutput.stringField("freshness_status", freshnessStatus(evaluation)),
                JsonOutput.numberField("missing_count", evaluation.missing().length),
                JsonOutput.rawField("missing", JsonOutput.stringArray(evaluation.missing())),
                JsonOutput.numberField("completion_blocker_count", evaluation.missing().length),
                JsonOutput.rawField("completion_blockers", JsonOutput.stringArray(evaluation.missing())),
                JsonOutput.stringField("blocker_summary", blockerSummary(blockers)),
                JsonOutput.rawField("blocker_categories", JsonOutput.stringArray(blockerCategories(blockers))),
                JsonOutput.rawField("blocker_details", JsonOutput.array(blockerJson(blockers))),
                JsonOutput.numberField("stale_count", evaluation.staleChecks().length),
                JsonOutput.rawField("stale_checks", JsonOutput.stringArray(evaluation.staleChecks())),
                JsonOutput.rawField("release_checks", releaseChecks.json()),
                JsonOutput.stringField("next_action", evaluation.nextAction()),
                JsonOutput.stringField("next_command", evaluation.nextCommand()),
                JsonOutput.stringField("context_path", PathUtil.goalContext(projectRoot).toString())
        ));
    }

    private String level(Args args) {
        String level = args.option("level", "standard").trim().toLowerCase(java.util.Locale.ROOT);
        if (level.length() == 0) {
            return "standard";
        }
        if ("fast".equals(level) || "standard".equals(level) || "release".equals(level)) {
            return level;
        }
        throw new IllegalArgumentException("Unsupported goal verify level: " + level);
    }

    private String[] checksForLevel(CommandContext context, Path projectRoot, String goalKey, String level)
            throws Exception {
        String[] required = orchestrator.requiredChecks(context, projectRoot, goalKey);
        if ("standard".equals(level) || "release".equals(level)) {
            return required;
        }
        Set<String> selected = new LinkedHashSet<String>();
        for (String check : required) {
            if ("sensitive".equals(check)
                    || "think-before-coding".equals(check)
                    || "goal-driven".equals(check)
                    || "simplicity".equals(check)
                    || "surgical-change".equals(check)
                    || "graph".equals(check)
                    || "impact".equals(check)
                    || "architecture".equals(check)
                    || "bdd".equals(check)) {
                selected.add(check);
            }
        }
        if (selected.isEmpty()) {
            selected.add("sensitive");
        }
        return selected.toArray(new String[selected.size()]);
    }

    private List<GoalCheck> runSelectedChecks(CommandContext context, Path projectRoot, String goalKey,
                                              String[] selectedChecks) throws Exception {
        List<GoalCheck> checks = new ArrayList<GoalCheck>();
        for (String check : selectedChecks) {
            checks.addAll(orchestrator.runCheck(context, projectRoot, goalKey, check, false));
        }
        return checks;
    }

    private ReleaseChecks runReleaseChecks(Path projectRoot) {
        ReleaseChecks release = new ReleaseChecks();
        release.enabled = true;
        release.packageStatus = runPackage(projectRoot);
        release.artifactPassport = java.nio.file.Files.isRegularFile(PathUtil.artifactPassport(projectRoot))
                ? "present" : "missing_until_goal_complete";
        release.exportContract = java.nio.file.Files.isRegularFile(PathUtil.goalContext(projectRoot))
                ? "present" : "missing_goal_context";
        return release;
    }

    private String runPackage(Path projectRoot) {
        try {
            Process process = new ProcessBuilder("mvn", "-q", "-DskipTests", "package")
                    .directory(projectRoot.toFile())
                    .redirectErrorStream(true)
                    .start();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            InputStream stream = process.getInputStream();
            long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(180);
            byte[] buffer = new byte[4096];
            while (process.isAlive() && System.currentTimeMillis() < deadline) {
                drain(stream, output, buffer);
                Thread.sleep(20L);
            }
            if (process.isAlive()) {
                process.destroy();
                if (!process.waitFor(2, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
                drain(stream, output, buffer);
                return "failed: package timed out";
            }
            drain(stream, output, buffer);
            return process.waitFor() == 0 ? "passed" : "failed";
        } catch (Exception ex) {
            return "failed: " + ex.getMessage();
        }
    }

    private void drain(InputStream stream, ByteArrayOutputStream output, byte[] buffer) throws Exception {
        while (stream.available() > 0 && output.size() < 8192) {
            int read = stream.read(buffer, 0, Math.min(buffer.length, 8192 - output.size()));
            if (read < 0) {
                return;
            }
            output.write(buffer, 0, read);
        }
    }

    private void printStringList(CommandContext context, String[] values) {
        printArray(context, values);
    }

    private void printArray(CommandContext context, String[] values) {
        if (values.length == 0) {
            context.out().println("  - none");
            return;
        }
        for (String value : values) {
            context.out().println("  - " + value);
        }
    }

    private String[] failedChecks(List<GoalCheck> checks) {
        List<String> failed = new ArrayList<String>();
        for (GoalCheck check : checks) {
            if ("failed".equals(check.status())) {
                failed.add(check.checkKey() + ": " + check.resultSummary());
            }
        }
        return failed.toArray(new String[failed.size()]);
    }

    private String freshnessStatus(GoalEvaluation evaluation) {
        return evaluation.staleChecks().length == 0 ? "fresh" : "stale";
    }

    private List<BlockerDetail> blockerDetails(GoalRun goal, List<GoalCheck> checks, GoalEvaluation evaluation) {
        List<BlockerDetail> blockers = new ArrayList<BlockerDetail>();
        for (String missing : evaluation.missing()) {
            String checkKey = checkKey(missing);
            GoalCheck check = checkKey.length() == 0 ? null : findCheck(checks, checkKey);
            String status = check == null ? statusFromMissing(missing) : check.status();
            blockers.add(new BlockerDetail(category(missing, checkKey, check, evaluation),
                    checkKey, status, missing, explanation(missing, checkKey, check),
                    nextCommand(goal, missing, checkKey)));
        }
        return blockers;
    }

    private String blockerSummary(List<BlockerDetail> blockers) {
        if (blockers.isEmpty()) {
            return "none";
        }
        return blockers.get(0).category + ": " + blockers.get(0).message;
    }

    private String[] blockerCategories(List<BlockerDetail> blockers) {
        Set<String> categories = new LinkedHashSet<String>();
        for (BlockerDetail blocker : blockers) {
            categories.add(blocker.category);
        }
        return categories.toArray(new String[categories.size()]);
    }

    private List<String> blockerJson(List<BlockerDetail> blockers) {
        List<String> raw = new ArrayList<String>();
        for (BlockerDetail blocker : blockers) {
            raw.add(JsonOutput.object(
                    JsonOutput.stringField("category", blocker.category),
                    JsonOutput.stringField("check_key", blocker.checkKey),
                    JsonOutput.stringField("status", blocker.status),
                    JsonOutput.stringField("message", blocker.message),
                    JsonOutput.stringField("explanation", blocker.explanation),
                    JsonOutput.stringField("next_command", blocker.nextCommand)
            ).trim());
        }
        return raw;
    }

    private void printBlockers(CommandContext context, List<BlockerDetail> blockers) {
        if (blockers.isEmpty()) {
            context.out().println("  - none");
            return;
        }
        for (BlockerDetail blocker : blockers) {
            context.out().println("  - category: " + blocker.category);
            if (blocker.checkKey.length() > 0) {
                context.out().println("    check_key: " + blocker.checkKey);
            }
            if (blocker.status.length() > 0) {
                context.out().println("    status: " + blocker.status);
            }
            context.out().println("    message: " + blocker.message);
            context.out().println("    explanation: " + blocker.explanation);
            context.out().println("    next_command: " + blocker.nextCommand);
        }
    }

    private String category(String missing, String checkKey, GoalCheck check, GoalEvaluation evaluation) {
        String lower = missing.toLowerCase(java.util.Locale.ROOT);
        if (missing.startsWith("goal steps incomplete")) {
            return "incomplete_goal_steps";
        }
        if (missing.startsWith("context export is not ready")) {
            return "context_export_not_ready";
        }
        if (missing.startsWith("goal status is ")) {
            return "goal_status_blocked";
        }
        if (lower.contains(" is stale") || contains(evaluation.staleChecks(), checkKey)) {
            return "stale_check";
        }
        if (checkKey.length() > 0 && "pending".equals(statusFromMissing(missing))) {
            return "pending_check";
        }
        String summary = check == null ? "" : check.resultSummary().toLowerCase(java.util.Locale.ROOT);
        if ("workflow".equals(checkKey) && summary.contains("pending hard gates")) {
            return "pending_workflow_gate";
        }
        if ("spec".equals(checkKey)
                && (lower.contains("spec is skipped")
                || summary.contains("no spec change")
                || summary.contains("spec required")
                || summary.contains("spec has no tasks")
                || summary.contains("spec has no acceptance"))) {
            return "missing_spec";
        }
        if (lower.contains("evidence missing") || lower.contains("missing required")
                || lower.contains("lacks ") || lower.contains("scope_justification")) {
            return "missing_evidence";
        }
        if ("skipped".equals(statusFromMissing(missing)) || (check != null && "skipped".equals(check.status()))) {
            return "skipped_required_check";
        }
        if ("failed".equals(statusFromMissing(missing)) || (check != null && "failed".equals(check.status()))) {
            return "failed_check";
        }
        return "goal_blocker";
    }

    private String explanation(String missing, String checkKey, GoalCheck check) {
        if (missing.startsWith("goal steps incomplete")) {
            return "Run goal next, finish the current action, and record goal step evidence.";
        }
        if (missing.startsWith("context export is not ready")) {
            return "Resume the goal so DevHarnessKit can regenerate GOAL_CONTEXT before continuing.";
        }
        if ("workflow".equals(checkKey) && check != null
                && check.resultSummary().toLowerCase(java.util.Locale.ROOT).contains("pending hard gates")) {
            return "A hard workflow gate is still open; record mapped goal evidence or run the relevant workflow check path.";
        }
        if ("spec".equals(checkKey) && check != null
                && check.resultSummary().toLowerCase(java.util.Locale.ROOT).contains("no spec change")) {
            return "This profile requires spec evidence, but the goal has no spec change bound.";
        }
        if (missing.contains(" is stale")) {
            return "A required check was recorded before the latest goal step or workspace state; rerun that check.";
        }
        if (missing.contains(" is pending")) {
            return "A required check has not been recorded yet.";
        }
        if (missing.contains(" is skipped")) {
            return "This profile does not accept skipped status for the required check.";
        }
        if (missing.contains("evidence missing") || missing.contains("lacks ")) {
            return "Add structured goal step evidence for the requested key, then rerun goal verify.";
        }
        if (checkKey.length() > 0) {
            return "The required check is not accepted by the active goal check policy.";
        }
        return "Resolve the blocker shown in the message, then rerun goal verify.";
    }

    private String nextCommand(GoalRun goal, String missing, String checkKey) {
        if (missing.startsWith("goal steps incomplete")) {
            return "dhk goal next --goal " + goal.goalKey();
        }
        if (missing.startsWith("context export is not ready")) {
            return "dhk goal resume --goal " + goal.goalKey();
        }
        if (checkKey.length() > 0) {
            return "dhk goal check --goal " + goal.goalKey() + " --check " + checkKey;
        }
        return "dhk goal status --goal " + goal.goalKey();
    }

    private GoalCheck findCheck(List<GoalCheck> checks, String checkKey) {
        for (GoalCheck check : checks) {
            if (checkKey.equals(check.checkKey())) {
                return check;
            }
        }
        return null;
    }

    private String checkKey(String missing) {
        if (!missing.startsWith("check ")) {
            return "";
        }
        int marker = missing.indexOf(" is ", 6);
        if (marker <= 6) {
            return "";
        }
        return missing.substring(6, marker);
    }

    private String statusFromMissing(String missing) {
        int marker = missing.indexOf(" is ");
        if (marker < 0) {
            return "";
        }
        int start = marker + 4;
        int end = missing.indexOf(':', start);
        int semicolon = missing.indexOf(';', start);
        if (end < 0 || (semicolon >= 0 && semicolon < end)) {
            end = semicolon;
        }
        if (end < 0) {
            end = missing.length();
        }
        return missing.substring(start, end).trim();
    }

    private boolean contains(String[] values, String value) {
        if (value.length() == 0) {
            return false;
        }
        for (String candidate : values) {
            if (value.equals(candidate)) {
                return true;
            }
        }
        return false;
    }

    private void printReleaseChecks(CommandContext context, ReleaseChecks releaseChecks) {
        if (!releaseChecks.enabled) {
            return;
        }
        context.out().println("release_checks:");
        context.out().println("  - package: " + releaseChecks.packageStatus);
        context.out().println("  - artifact_passport: " + releaseChecks.artifactPassport);
        context.out().println("  - export_contract: " + releaseChecks.exportContract);
    }

    private static final class ReleaseChecks {
        private boolean enabled;
        private String packageStatus;
        private String artifactPassport;
        private String exportContract;

        private static ReleaseChecks none() {
            return new ReleaseChecks();
        }

        private ReleaseChecks() {
            this.enabled = false;
            this.packageStatus = "";
            this.artifactPassport = "";
            this.exportContract = "";
        }

        private String json() {
            if (!enabled) {
                return JsonOutput.object(JsonOutput.booleanField("enabled", false));
            }
            return JsonOutput.object(
                    JsonOutput.booleanField("enabled", true),
                    JsonOutput.stringField("package", packageStatus),
                    JsonOutput.stringField("artifact_passport", artifactPassport),
                    JsonOutput.stringField("export_contract", exportContract)
            );
        }
    }

    private static final class BlockerDetail {
        private final String category;
        private final String checkKey;
        private final String status;
        private final String message;
        private final String explanation;
        private final String nextCommand;

        private BlockerDetail(String category, String checkKey, String status, String message,
                              String explanation, String nextCommand) {
            this.category = category == null ? "" : category;
            this.checkKey = checkKey == null ? "" : checkKey;
            this.status = status == null ? "" : status;
            this.message = message == null ? "" : message;
            this.explanation = explanation == null ? "" : explanation;
            this.nextCommand = nextCommand == null ? "" : nextCommand;
        }
    }
}
