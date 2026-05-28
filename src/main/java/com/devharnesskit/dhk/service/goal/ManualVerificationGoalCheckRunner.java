package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.goal.GoalCheck;
import com.devharnesskit.dhk.model.goal.GoalStep;
import com.devharnesskit.dhk.model.brief.InteractionRequest;
import com.devharnesskit.dhk.repository.goal.GoalStepRepository;
import com.devharnesskit.dhk.repository.brief.InteractionRequestRepository;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class ManualVerificationGoalCheckRunner extends AbstractGoalCheckRunner {
    private final GoalCheckRecorder recorder;
    private final GoalStepRepository stepRepository;
    private final InteractionRequestRepository interactionRepository;
    private final GoalCheckCommandExecutor commandExecutor;
    private final String scopeKey;

    ManualVerificationGoalCheckRunner(String key, GoalCheckRecorder recorder,
                                      GoalStepRepository stepRepository,
                                      InteractionRequestRepository interactionRepository,
                                      GoalCheckCommandExecutor commandExecutor,
                                      String scopeKey) {
        super(key);
        this.recorder = recorder;
        this.stepRepository = stepRepository;
        this.interactionRepository = interactionRepository;
        this.commandExecutor = commandExecutor;
        this.scopeKey = scopeKey;
    }

    public GoalCheck run(GoalCheckContext context) throws Exception {
        Path log = recorder.logPath(context.projectRoot(), context.goal(), key());
        List<GoalStep> steps = stepRepository.listByGoal(context.connection(), context.goal().goalKey());
        List<String> failures = new ArrayList<String>();
        String manualEvidenceStatus = GoalCheckSupport.latestEvidenceValue(steps, "manual_evidence_status");
        String scope = GoalCheckSupport.latestEvidenceValue(steps, scopeKey);
        String manualEvidencePath = GoalCheckSupport.latestEvidenceValue(steps, "manual_evidence_path");
        String tester = GoalCheckSupport.latestEvidenceValue(steps, "tester");
        String riskIfNotRun = GoalCheckSupport.latestEvidenceValue(steps, "risk_if_not_run");

        StringBuilder output = new StringBuilder();
        output.append("check_key: ").append(key()).append('\n');
        output.append("manual_evidence_status: ")
                .append(GoalCheckSupport.empty(manualEvidenceStatus, "none")).append('\n');
        output.append(scopeKey).append(": ").append(GoalCheckSupport.empty(scope, "none")).append('\n');
        output.append("manual_evidence_path: ")
                .append(GoalCheckSupport.empty(manualEvidencePath, "none")).append('\n');
        output.append("tester: ").append(GoalCheckSupport.empty(tester, "none")).append('\n');
        output.append("risk_if_not_run: ").append(GoalCheckSupport.empty(riskIfNotRun, "none")).append('\n');

        if (!"passed".equalsIgnoreCase(manualEvidenceStatus)) {
            failures.add("manual evidence is not passed: manual_evidence_status="
                    + GoalCheckSupport.empty(manualEvidenceStatus, "none"));
        }
        if (scope.length() == 0) {
            failures.add(scopeKey + " is required");
        }
        if (!GoalCheckSupport.artifactExists(context.projectRoot(), manualEvidencePath)) {
            failures.add("manual evidence artifact missing: manual_evidence_path="
                    + GoalCheckSupport.empty(manualEvidencePath, "none"));
        }

        if (!failures.isEmpty()) {
            GoalCheck interactionCheck = runAnsweredInteraction(context, log, output);
            if (interactionCheck != null) {
                return interactionCheck;
            }
        }

        recorder.writeLog(log, output.toString());
        String status = failures.isEmpty() ? "passed" : "failed";
        String summary = failures.isEmpty()
                ? key() + " evidence passed; " + scopeKey + "=" + scope
                + " manual_evidence_path=" + manualEvidencePath
                : key() + " manual evidence required: " + failures;
        return recorder.save(context.connection(), context.projectRoot(), context.goal(), key(), "manual",
                "", status, summary, log, context.now());
    }

    private GoalCheck runAnsweredInteraction(GoalCheckContext context, Path log, StringBuilder output)
            throws Exception {
        InteractionRequest request = manualVerificationRequest(context);
        if (request == null || !"answered".equals(request.status())) {
            return null;
        }
        Map<String, String> fields = parseFields(request.answer());
        String decision = fields.get("decision");
        if ("try_auto".equals(decision)) {
            return runAutoFallback(context, log, output);
        }
        if ("manual_passed".equals(decision)) {
            return runManualPassedAnswer(context, log, output, fields);
        }
        if ("waive_verification".equals(decision)) {
            return runWaiverAnswer(context, log, output, fields);
        }
        return null;
    }

    private GoalCheck runAutoFallback(GoalCheckContext context, Path log, StringBuilder output) throws Exception {
        String[] command = "manual-compile".equals(key()) ? context.policy().compileCommand()
                : context.policy().testCommand();
        GoalCheckCommandResult result = commandExecutor.execute(context.projectRoot(), command, 120);
        output.append("decision: try_auto\n");
        output.append("auto_command: ").append(GoalCheckSupport.join(command)).append('\n');
        output.append("auto_exit_code: ").append(result.exitCode()).append('\n');
        output.append("duration_ms: ").append(result.durationMs()).append('\n');
        output.append("output_truncated: ").append(result.truncated()).append('\n');
        output.append("\n<auto-output>\n").append(result.output()).append("\n</auto-output>\n");
        recorder.writeLog(log, output.toString());
        String status = result.exitCode() == 0 ? "passed" : "failed";
        String summary = key() + " user-selected auto fallback exit_code=" + result.exitCode()
                + " duration_ms=" + result.durationMs() + " output_truncated=" + result.truncated();
        return recorder.save(context.connection(), context.projectRoot(), context.goal(), key(), "manual-auto",
                GoalCheckSupport.join(command), status, summary, log, context.now());
    }

    private GoalCheck runManualPassedAnswer(GoalCheckContext context, Path log, StringBuilder output,
                                            Map<String, String> fields) throws Exception {
        List<String> failures = new ArrayList<String>();
        String scope = value(fields.get(scopeKey));
        String path = value(fields.get("manual_evidence_path"));
        String tester = value(fields.get("tester"));
        output.append("decision: manual_passed\n");
        output.append(scopeKey).append(": ").append(GoalCheckSupport.empty(scope, "none")).append('\n');
        output.append("manual_evidence_path: ").append(GoalCheckSupport.empty(path, "none")).append('\n');
        output.append("tester: ").append(GoalCheckSupport.empty(tester, "none")).append('\n');
        if (scope.length() == 0) {
            failures.add(scopeKey + " is required");
        }
        if (!GoalCheckSupport.artifactExists(context.projectRoot(), path)) {
            failures.add("manual evidence artifact missing: manual_evidence_path="
                    + GoalCheckSupport.empty(path, "none"));
        }
        recorder.writeLog(log, output.toString());
        String status = failures.isEmpty() ? "passed" : "failed";
        String summary = failures.isEmpty()
                ? key() + " user-confirmed manual evidence passed; " + scopeKey + "=" + scope
                + " manual_evidence_path=" + path
                : key() + " manual verification answer incomplete: " + failures;
        return recorder.save(context.connection(), context.projectRoot(), context.goal(), key(), "manual",
                "", status, summary, log, context.now());
    }

    private GoalCheck runWaiverAnswer(GoalCheckContext context, Path log, StringBuilder output,
                                      Map<String, String> fields) throws Exception {
        List<String> failures = new ArrayList<String>();
        String reason = value(fields.get("waive_reason"));
        String approver = value(fields.get("approver"));
        String riskScope = value(fields.get("risk_scope"));
        String rollbackPlan = value(fields.get("rollback_plan"));
        output.append("decision: waive_verification\n");
        output.append("waive_reason: ").append(GoalCheckSupport.empty(reason, "none")).append('\n');
        output.append("approver: ").append(GoalCheckSupport.empty(approver, "none")).append('\n');
        output.append("risk_scope: ").append(GoalCheckSupport.empty(riskScope, "none")).append('\n');
        output.append("rollback_plan: ").append(GoalCheckSupport.empty(rollbackPlan, "none")).append('\n');
        if (reason.length() == 0) {
            failures.add("waive_reason is required");
        }
        if (approver.length() == 0) {
            failures.add("approver is required");
        }
        if (riskScope.length() == 0) {
            failures.add("risk_scope is required");
        }
        if (!GoalCheckSupport.artifactExists(context.projectRoot(), rollbackPlan)) {
            failures.add("rollback plan artifact missing: rollback_plan="
                    + GoalCheckSupport.empty(rollbackPlan, "none"));
        }
        recorder.writeLog(log, output.toString());
        String status = failures.isEmpty() ? "waived" : "failed";
        String summary = failures.isEmpty()
                ? key() + " verification waived with approval; approver=" + approver
                + " rollback_plan=" + rollbackPlan
                : key() + " verification waiver incomplete: " + failures;
        return recorder.save(context.connection(), context.projectRoot(), context.goal(), key(), "manual-waiver",
                "", status, summary, log, context.now());
    }

    private InteractionRequest manualVerificationRequest(GoalCheckContext context) throws Exception {
        String requestId = "interaction-" + context.goal().goalKey() + "-" + key();
        for (InteractionRequest request : interactionRepository.list(context.connection())) {
            if (requestId.equals(request.requestId())) {
                return request;
            }
        }
        return null;
    }

    private Map<String, String> parseFields(String answer) {
        Map<String, String> fields = new LinkedHashMap<String, String>();
        if (answer == null) {
            return fields;
        }
        String[] parts = answer.split("[;\\n\\r]+");
        for (String part : parts) {
            int equals = part.indexOf('=');
            if (equals <= 0) {
                continue;
            }
            String key = part.substring(0, equals).trim().toLowerCase(Locale.ROOT);
            String value = part.substring(equals + 1).trim();
            if (key.length() > 0) {
                fields.put(key, value);
            }
        }
        return fields;
    }

    private String value(String value) {
        return value == null ? "" : value.trim();
    }
}
