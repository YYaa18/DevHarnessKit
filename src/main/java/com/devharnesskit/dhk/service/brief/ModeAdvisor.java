package com.devharnesskit.dhk.service.brief;

import com.devharnesskit.dhk.model.brief.BriefRequest;
import com.devharnesskit.dhk.model.brief.ModeAdvice;
import com.devharnesskit.dhk.model.config.DevHarnessConfig;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class ModeAdvisor {
    public ModeAdvice advise(BriefRequest request, DevHarnessConfig config) {
        String requested = normalizeMode(request.requestedMode());
        Scores scores = score(request, config);
        String recommendation = recommendation(requested, scores);
        boolean confirmation = confirmationRequired(request, recommendation, scores, config);
        boolean safeToStart = !(ModeAdvice.ASK.equals(recommendation) || ModeAdvice.ANALYZE_ONLY.equals(recommendation));
        String reason = confirmation ? confirmationReason(request, recommendation, scores, config) : "none";
        String profile = profileFor(recommendation, request.profileKey(), config);
        return new ModeAdvice(recommendation, confidence(recommendation, scores), scores.total(),
                scores.security, scores.db, scores.config, scores.crossModule, scores.testGap, scores.graphScope,
                scores.riskFlags(), why(recommendation, scores, config),
                softEscalationRules(), hardEscalationRules(), confirmation, reason, safeToStart, profile);
    }

    private Scores score(BriefRequest request, DevHarnessConfig config) {
        String text = (request.task() + " " + request.module() + " " + request.target()).toLowerCase(Locale.ROOT);
        Scores scores = new Scores();
        if (containsAny(text, "auth", "permission", "security", "encrypt", "token",
                "权限", "鉴权", "认证", "安全", "加密")) {
            scores.security = 30;
            scores.flags.add("security");
        }
        if (containsAny(text, "payment", "pay", "settlement", "amount", "balance",
                "支付", "资金", "账务", "余额", "结算", "风控")) {
            scores.security = Math.max(scores.security, 35);
            scores.flags.add("financial_risk");
        }
        if (containsAny(text, "sql", "schema", "migration", "ddl", "table", "index",
                "数据库", "表结构", "索引", "迁移")) {
            scores.db = 25;
            scores.flags.add("database");
        }
        if (containsAny(text, "config", "yaml", "properties", "pom.xml", "build.gradle",
                "配置", "依赖", "网关")) {
            scores.config = 20;
            scores.flags.add("configuration");
        }
        if (containsAny(text, "cross", "integration", "gateway", "多个模块", "跨模块", "联调")) {
            scores.crossModule = 20;
            scores.flags.add("cross_module");
        }
        if ("manual".equals(config.testMode()) || "disabled".equals(config.testMode())) {
            scores.testGap = 20;
            scores.flags.add("manual_or_limited_tests");
        }
        if ("required".equals(config.graphMode())) {
            scores.graphScope = 15;
            scores.flags.add("graph_required");
        } else if ("advisory".equals(config.graphMode())) {
            scores.graphScope = 5;
            scores.flags.add("graph_advisory");
        }
        if (request.task().length() < 6 || containsAny(text, "看看", "处理一下", "优化一下", "不确定", "maybe", "unknown")) {
            scores.ambiguous = true;
            scores.flags.add("task_unclear");
        }
        if (containsAny(text, "新增", "new api", "new endpoint", "add endpoint", "controller", "接口")) {
            scores.newSurface = true;
            scores.flags.add("new_api_surface");
        }
        if (containsAny(text, "小改", "修复", "fix", "bug", "逻辑", "调整", "change logic")) {
            scores.patchLike = true;
        }
        return scores;
    }

    private String recommendation(String requested, Scores scores) {
        if (ModeAdvice.ANALYZE_ONLY.equals(requested) || ModeAdvice.ASK.equals(requested)
                || ModeAdvice.PATCH.equals(requested) || ModeAdvice.STANDARD.equals(requested)
                || ModeAdvice.STRICT.equals(requested)) {
            return requested;
        }
        if (scores.ambiguous) {
            return ModeAdvice.ASK;
        }
        if (scores.hardRisk()) {
            return ModeAdvice.STRICT;
        }
        if (scores.total() >= 45 || scores.newSurface) {
            return ModeAdvice.STANDARD;
        }
        if (scores.patchLike || scores.total() < 25) {
            return ModeAdvice.PATCH;
        }
        return ModeAdvice.STANDARD;
    }

    private boolean confirmationRequired(BriefRequest request, String recommendation, Scores scores,
                                         DevHarnessConfig config) {
        return ModeAdvice.STRICT.equals(recommendation)
                || ModeAdvice.ASK.equals(recommendation)
                || ModeAdvice.ANALYZE_ONLY.equals(recommendation)
                || scores.hardRisk()
                || config.preWorkConfirmationRequired()
                || userConfirmationRequested(request)
                || "manual".equals(config.testMode())
                || "manual".equals(config.compileMode());
    }

    private String confirmationReason(BriefRequest request, String recommendation, Scores scores,
                                      DevHarnessConfig config) {
        if (ModeAdvice.ASK.equals(recommendation)) {
            return "任务描述不够明确，需要先确认边界。";
        }
        if (ModeAdvice.ANALYZE_ONLY.equals(recommendation)) {
            return "当前模式只分析，不自动开始开发。";
        }
        if (scores.hardRisk()) {
            return "命中安全、权限、支付、数据库结构或受保护配置等高风险信号。";
        }
        if (config.preWorkConfirmationRequired()) {
            return config.preWorkConfirmationReason();
        }
        if (userConfirmationRequested(request)) {
            return "任务要求分步或确认后推进，需要等待用户确认。";
        }
        if ("manual".equals(config.testMode()) || "manual".equals(config.compileMode())) {
            return "项目配置要求人工或 IDE 验证证据。";
        }
        return "none";
    }

    private boolean userConfirmationRequested(BriefRequest request) {
        return explicitConfirmationRequested(request) || stepByStepDelivery(request);
    }

    private boolean explicitConfirmationRequested(BriefRequest request) {
        String text = (request.task() + " " + request.module() + " " + request.target()).toLowerCase(Locale.ROOT);
        return containsAny(text, "wait for confirmation", "ask for confirmation", "after confirmation",
                "explicit approval", "user approval", "do not continue", "pause after", "stop after",
                "用户确认", "向用户确认", "等待确认", "等我确认", "确认后", "明确确认", "先确认",
                "人工确认", "不要继续", "先停", "停下来");
    }

    private boolean stepByStepDelivery(BriefRequest request) {
        String text = (request.task() + " " + request.module() + " " + request.target()).toLowerCase(Locale.ROOT);
        return containsAny(text, "step-by-step", "step by step", "l1", "l2", "l3",
                "逐步", "分步", "步骤", "每一步", "一步步");
    }

    private String profileFor(String recommendation, String requestedProfile, DevHarnessConfig config) {
        if (ModeAdvice.PATCH.equals(recommendation)) {
            return "java-api-patch";
        }
        if (ModeAdvice.STRICT.equals(recommendation) && ("required".equals(config.graphMode())
                || "advisory".equals(config.graphMode()))) {
            return "java-api-change-with-graph";
        }
        if (requestedProfile != null && requestedProfile.trim().length() > 0) {
            return requestedProfile.trim();
        }
        return "java-api-change";
    }

    private String confidence(String recommendation, Scores scores) {
        if (ModeAdvice.ASK.equals(recommendation)) {
            return "low";
        }
        if (scores.hardRisk() || (ModeAdvice.PATCH.equals(recommendation) && scores.total() < 20)) {
            return "high";
        }
        return "medium";
    }

    private String[] why(String recommendation, Scores scores, DevHarnessConfig config) {
        ArrayList<String> why = new ArrayList<String>();
        if (ModeAdvice.PATCH.equals(recommendation)) {
            why.add("影响面看起来较小，适合轻流程先处理。");
        } else if (ModeAdvice.STANDARD.equals(recommendation)) {
            why.add("任务涉及新增或中等影响面，建议使用标准流程。");
        } else if (ModeAdvice.STRICT.equals(recommendation)) {
            why.add("任务包含高风险信号，建议使用完整审计流程。");
        } else if (ModeAdvice.ANALYZE_ONLY.equals(recommendation)) {
            why.add("当前只生成分析建议，不启动 goal。");
        } else {
            why.add("任务边界不够清晰，建议先确认再启动。");
        }
        if ("manual".equals(config.testMode()) || "manual".equals(config.compileMode())) {
            why.add("项目验证模式需要人工或 IDE 证据，Agent 不应自行运行完整 Maven 测试。");
        }
        if (scores.graphScope > 0) {
            why.add("项目配置包含 Graph 影响面分析信号。");
        }
        return why.toArray(new String[why.size()]);
    }

    private String[] softEscalationRules() {
        return new String[]{
                "改动文件数扩大时建议从 patch 升级为 standard",
                "影响跨模块调用时建议从 standard 升级为 strict"
        };
    }

    private String[] hardEscalationRules() {
        return new String[]{
                "触碰权限、安全、支付、资金、schema 或受保护配置时不得以 patch 完成",
                "缺少必须的人工验证证据时不得完成"
        };
    }

    private String normalizeMode(String mode) {
        String normalized = mode == null ? "" : mode.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        if (normalized.length() == 0 || "recommend".equals(normalized) || "auto".equals(normalized)) {
            return "recommend";
        }
        if ("full".equals(normalized)) {
            return ModeAdvice.STRICT;
        }
        return normalized;
    }

    private boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static final class Scores {
        private int security;
        private int db;
        private int config;
        private int crossModule;
        private int testGap;
        private int graphScope;
        private boolean ambiguous;
        private boolean newSurface;
        private boolean patchLike;
        private final Set<String> flags = new LinkedHashSet<String>();

        private int total() {
            return security + db + config + crossModule + testGap + graphScope;
        }

        private boolean hardRisk() {
            return security >= 30 || db >= 25 || config >= 20;
        }

        private String[] riskFlags() {
            return flags.toArray(new String[flags.size()]);
        }
    }
}
