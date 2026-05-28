package com.devharnesskit.dhk.service.brief;

import com.devharnesskit.dhk.model.brief.InteractionRequest;
import com.devharnesskit.dhk.model.goal.GoalCheck;
import com.devharnesskit.dhk.model.goal.GoalRun;

import java.util.Locale;

final class ManualVerificationInteractionSupport {
    boolean isFailedManualVerification(GoalCheck check) {
        return check != null
                && ("manual-compile".equals(check.checkKey()) || "manual-test".equals(check.checkKey()))
                && "failed".equals(check.status());
    }

    InteractionRequest request(GoalRun goal, GoalCheck check) {
        String checkKey = check.checkKey();
        boolean compile = "manual-compile".equals(checkKey);
        String noun = compile ? "编译验证" : "测试验证";
        String autoCommand = compile ? "mvn compile" : "mvn test";
        return new InteractionRequest(
                "interaction-" + goal.goalKey() + "-" + checkKey,
                goal.goalKey(),
                "verify",
                "manual_verification",
                "blocking",
                noun + "需要确认，请选择处理方式。",
                check.resultSummary(),
                "manual_passed:我已在 IDE 或本地工具中手动" + (compile ? "编译" : "测试") + "通过"
                        + "|try_auto:尝试自动运行 " + autoCommand
                        + "|waive_verification:本次不验证" + (compile ? "编译" : "测试") + "并记录豁免原因",
                "manual_passed",
                true,
                "open",
                "");
    }

    String answer(InteractionRequest request, String answer, String evidencePath, String scope, String tester,
                  String reason, String approver, String riskScope, String rollbackPlan) {
        String decision = normalizeDecision(answer);
        if ("manual_passed".equals(decision)) {
            if (value(evidencePath).length() == 0) {
                throw new IllegalArgumentException("manual verification answer requires --evidence-path");
            }
            String scopeKey = request.requestId().contains("manual-test") ? "test_scope" : "compile_scope";
            return "decision=manual_passed; manual_evidence_status=passed; "
                    + scopeKey + "=" + defaultValue(scope, "manual verification confirmed by user") + "; "
                    + "manual_evidence_path=" + value(evidencePath) + "; tester=" + defaultValue(tester, "user");
        }
        if ("try_auto".equals(decision)) {
            return "decision=try_auto";
        }
        if ("waive_verification".equals(decision)) {
            if (value(reason).length() == 0 || value(approver).length() == 0
                    || value(riskScope).length() == 0 || value(rollbackPlan).length() == 0) {
                throw new IllegalArgumentException("verification waiver requires --reason, --approver, "
                        + "--risk-scope and --rollback-plan");
            }
            return "decision=waive_verification; waive_reason=" + value(reason)
                    + "; approver=" + value(approver)
                    + "; risk_scope=" + value(riskScope)
                    + "; rollback_plan=" + value(rollbackPlan);
        }
        throw new IllegalArgumentException("Invalid manual verification choice: " + answer
                + ". Valid choices: manual_passed, try_auto, waive_verification");
    }

    private String normalizeDecision(String answer) {
        String normalized = value(answer).toLowerCase(Locale.ROOT);
        if (normalized.contains("try_auto") || normalized.contains("自动") || normalized.contains("mvn")) {
            return "try_auto";
        }
        if (normalized.contains("waive") || normalized.contains("豁免") || normalized.contains("不验证")) {
            return "waive_verification";
        }
        if (normalized.contains("manual_passed") || normalized.contains("手动")
                || normalized.contains("ide") || normalized.contains("通过")) {
            return "manual_passed";
        }
        return normalized;
    }

    private String defaultValue(String value, String fallback) {
        String text = value(value);
        return text.length() == 0 ? fallback : text;
    }

    private String value(String value) {
        return value == null ? "" : value.trim();
    }
}
