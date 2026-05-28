package com.devharnesskit.dhk.service.brief;

import com.devharnesskit.dhk.model.brief.AgentBrief;
import com.devharnesskit.dhk.model.brief.BriefRequest;
import com.devharnesskit.dhk.model.brief.BriefResult;
import com.devharnesskit.dhk.model.brief.HarnessCommand;
import com.devharnesskit.dhk.model.brief.ModeAdvice;
import com.devharnesskit.dhk.model.brief.WorkBrief;
import com.devharnesskit.dhk.model.config.DevHarnessConfig;
import com.devharnesskit.dhk.model.goal.GoalPlan;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.model.knowledge.KnowledgeSnippet;
import com.devharnesskit.dhk.model.knowledge.ProfessionalKnowledgeContext;
import com.devharnesskit.dhk.service.config.DevHarnessConfigService;
import com.devharnesskit.dhk.service.knowledge.ProfessionalKnowledgeService;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class BriefService {
    private final DevHarnessConfigService configService;
    private final ModeAdvisor advisor;
    private final WorkBriefRenderer workRenderer;
    private final AgentBriefRenderer agentRenderer;
    private final BriefLifecycleService lifecycleService;
    private final ProfessionalKnowledgeService knowledgeService;

    public BriefService() {
        this(new DevHarnessConfigService(), new ModeAdvisor(),
                new WorkBriefRenderer(), new AgentBriefRenderer(), new BriefLifecycleService(),
                new ProfessionalKnowledgeService());
    }

    BriefService(DevHarnessConfigService configService, ModeAdvisor advisor,
                 WorkBriefRenderer workRenderer, AgentBriefRenderer agentRenderer,
                 BriefLifecycleService lifecycleService,
                 ProfessionalKnowledgeService knowledgeService) {
        this.configService = configService;
        this.advisor = advisor;
        this.workRenderer = workRenderer;
        this.agentRenderer = agentRenderer;
        this.lifecycleService = lifecycleService;
        this.knowledgeService = knowledgeService;
    }

    public BriefResult prepare(BriefRequest request, boolean writeFiles) throws Exception {
        return prepare(request, null, null, writeFiles);
    }

    public BriefResult prepare(BriefRequest request, GoalRun goal, GoalPlan plan,
                               boolean writeFiles) throws Exception {
        DevHarnessConfig config = loadConfig(request);
        ModeAdvice advice = advisor.advise(request, config);
        String briefId = "brief-" + stableKey(request.task(), request.module(), advice.modeId());
        String recommendationId = "rec-" + stableKey(request.task(), advice.modeId(), advice.profileKey());
        ProfessionalKnowledgeContext knowledge = knowledgeService.forBrief(request, advice);
        WorkBrief workBrief = workBrief(request, advice, briefId, recommendationId, knowledge);
        AgentBrief agentBrief = agentBrief(request, advice, config, workBrief, goal, plan, knowledge);
        Path workPath = PathUtil.workBrief(request.projectRoot());
        Path agentPath = PathUtil.agentBrief(request.projectRoot());
        if (writeFiles) {
            Files.createDirectories(PathUtil.devharnessBriefsDirectory(request.projectRoot()));
            Files.write(workPath, workRenderer.render(workBrief).getBytes("UTF-8"));
            Files.write(agentPath, agentRenderer.render(agentBrief).getBytes("UTF-8"));
            lifecycleService.recordPreWorkInteraction(request.projectRoot(), workBrief);
        }
        return new BriefResult(workBrief, agentBrief, workPath, agentPath);
    }

    public String renderWorkBrief(WorkBrief brief) {
        return workRenderer.render(brief);
    }

    public String renderAgentBrief(AgentBrief brief) {
        return agentRenderer.render(brief);
    }

    private DevHarnessConfig loadConfig(BriefRequest request) throws Exception {
        if (request.projectRoot() != null && configService.hasConfig(request.projectRoot())) {
            return configService.load(request.projectRoot());
        }
        String preset = request.preset().length() == 0 ? "springboot-manual-ide-test" : request.preset();
        java.util.Map<String, String> values = configService.presetValues(preset);
        if (request.graphOverride().length() > 0) {
            values.put("verification.graph.mode", request.graphOverride());
            values.put("verification.graph.required", Boolean.toString("required".equals(request.graphOverride())));
            values.put("verification.graph.fresh_snapshot_required", Boolean.toString("required".equals(request.graphOverride())));
            values.put("verification.graph.impact_map_required", Boolean.toString("required".equals(request.graphOverride())));
        }
        return new DevHarnessConfig(values);
    }

    private WorkBrief workBrief(BriefRequest request, ModeAdvice advice,
                                String briefId, String recommendationId,
                                ProfessionalKnowledgeContext knowledge) {
        return new WorkBrief(briefId, recommendationId, advice.modeId(), taskSummary(request),
                advice.recommendation(), advice.confidence(), advice.why(), advice.riskFlags(),
                expectedWork(advice), willNotDo(advice), userChoices(),
                advice.confirmationRequired(), advice.confirmationReason(), advice.safeToStart(),
                advice.riskScore(), professionalNotes(knowledge));
    }

    private AgentBrief agentBrief(BriefRequest request, ModeAdvice advice, DevHarnessConfig config,
                                  WorkBrief workBrief, GoalRun goal, GoalPlan plan,
                                  ProfessionalKnowledgeContext knowledge) {
        String goalKey = goal == null ? "" : goal.goalKey();
        String currentAction = plan == null
                ? (workBrief.confirmationRequired() || !workBrief.safeToStart() ? "wait_for_user_answer" : "")
                : plan.currentAction();
        String[] requiredEvidence = plan == null ? defaultRequiredEvidence(advice) : plan.requiredEvidence();
        return new AgentBrief(workBrief.briefId(), workBrief.recommendationId(),
                stableKey(request.task(), request.module(), request.target()), goalKey,
                advice.modeId(), advice.profileKey(), currentAction,
                allowedActions(advice), forbiddenActions(), requiredEvidence,
                advice.softEscalationRules(), advice.hardEscalationRules(),
                config.compileMode(), config.testMode(), config.graphMode(),
                "manual".equals(config.compileMode()) || "manual".equals(config.testMode()),
                PathUtil.workBrief(request.projectRoot()).toString(),
                advice.confirmationRequired(), harnessCommands(request, advice, goalKey), knowledge);
    }

    private String[] professionalNotes(ProfessionalKnowledgeContext knowledge) {
        java.util.List<String> notes = new java.util.ArrayList<String>();
        for (KnowledgeSnippet snippet : knowledge.snippets()) {
            String[] lines = snippet.summaryLines();
            if (lines.length > 0) {
                notes.add(snippet.domain() + ": " + lines[0]);
            }
        }
        return notes.toArray(new String[notes.size()]);
    }

    private String[] allowedActions(ModeAdvice advice) {
        if (ModeAdvice.ANALYZE_ONLY.equals(advice.recommendation()) || ModeAdvice.ASK.equals(advice.recommendation())) {
            return new String[]{"explain_work_brief", "ask_user_confirmation"};
        }
        return new String[]{"read_work_brief", "read_agent_brief", "run_goal_next", "perform_current_action_only",
                "record_goal_step", "verify_before_complete"};
    }

    private String[] forbiddenActions() {
        return new String[]{"show_harness_commands_to_user", "skip_goal_context",
                "complete_without_verify", "bypass_hard_escalation"};
    }

    private String[] defaultRequiredEvidence(ModeAdvice advice) {
        if (ModeAdvice.PATCH.equals(advice.recommendation())) {
            return new String[]{"changed_files", "diff_stat", "risk_flags", "verification"};
        }
        if (ModeAdvice.STRICT.equals(advice.recommendation())) {
            return new String[]{"impacted_files", "risk_points", "verification_plan", "manual_evidence_if_required"};
        }
        return new String[]{"goal_understanding", "impacted_files", "verification_plan"};
    }

    private HarnessCommand[] harnessCommands(BriefRequest request, ModeAdvice advice, String goalKey) {
        if (ModeAdvice.ANALYZE_ONLY.equals(advice.recommendation()) || ModeAdvice.ASK.equals(advice.recommendation())) {
            return new HarnessCommand[0];
        }
        if (goalKey.length() == 0) {
            return new HarnessCommand[]{
                    new HarnessCommand("quickstart_start",
                            new String[]{"quickstart", "--project-root", request.projectRoot().toString(),
                                    "--task", request.task(), "--module", request.module(),
                                    "--mode", advice.modeId(), "--profile", advice.profileKey()},
                            "after_user_confirmation", false, true)
            };
        }
        return new HarnessCommand[]{
                new HarnessCommand("goal_next",
                        new String[]{"goal", "next", "--goal", goalKey}, "before_each_step", false, true),
                new HarnessCommand("goal_step",
                        new String[]{"goal", "step", "--goal", goalKey, "--auto", "--summary", "<summary>"},
                        "after_each_step", false, true),
                new HarnessCommand("goal_verify",
                        new String[]{"goal", "verify", "--goal", goalKey}, "before_completion", false, true),
                new HarnessCommand("goal_complete",
                        new String[]{"goal", "complete", "--goal", goalKey}, "only_when_ready", false, true)
        };
    }

    private String taskSummary(BriefRequest request) {
        String module = request.module().length() == 0 ? "global" : request.module();
        return "任务：" + request.task() + "\n模块：" + module;
    }

    private String[] expectedWork(ModeAdvice advice) {
        if (ModeAdvice.PATCH.equals(advice.recommendation())) {
            return new String[]{"读取最小相关文件", "做小范围代码修改", "记录变更文件和验证证据"};
        }
        if (ModeAdvice.STANDARD.equals(advice.recommendation())) {
            return new String[]{"梳理影响面", "制定简短变更计划", "实现并验证"};
        }
        if (ModeAdvice.STRICT.equals(advice.recommendation())) {
            return new String[]{"完整影响面分析", "保留审计证据", "必要时要求人工确认"};
        }
        if (ModeAdvice.ANALYZE_ONLY.equals(advice.recommendation())) {
            return new String[]{"只输出建议与风险，不开始开发"};
        }
        return new String[]{"先补充任务边界，再决定流程"};
    }

    private String[] willNotDo(ModeAdvice advice) {
        return new String[]{"不会把底层 Harness 命令作为普通用户主体验",
                "不会在未确认高风险边界前扩大修改范围",
                ModeAdvice.PATCH.equals(advice.recommendation())
                        ? "轻流程不会跳过敏感检查和验证证据" : "不会把分析结果当作完成证明"};
    }

    private String[] userChoices() {
        return new String[]{"按建议继续", "改用 patch 轻流程", "改用 standard 标准流程",
                "改用 strict 完整审计流程", "只分析不开始"};
    }

    private String stableKey(String... parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.trim().length() == 0) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('-');
            }
            builder.append(part.trim().toLowerCase(Locale.ROOT));
        }
        String normalized = builder.toString().replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (normalized.length() == 0) {
            normalized = "task";
        }
        return normalized.length() > 48 ? normalized.substring(0, 48).replaceAll("-+$", "") : normalized;
    }
}
