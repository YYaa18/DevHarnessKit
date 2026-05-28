package com.devharnesskit.dhk.service.brief;

import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.TransactionTemplate;
import com.devharnesskit.dhk.model.MemoryItem;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.brief.GrowthLesson;
import com.devharnesskit.dhk.model.brief.InteractionRequest;
import com.devharnesskit.dhk.model.brief.KnowledgeCandidate;
import com.devharnesskit.dhk.model.brief.WorkBrief;
import com.devharnesskit.dhk.model.goal.GoalCheck;
import com.devharnesskit.dhk.model.goal.GoalEvaluation;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.repository.FtsRepository;
import com.devharnesskit.dhk.repository.MemoryRepository;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.service.SensitiveDataGuard;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class BriefLifecycleService {
    private final DbConnectionFactory connectionFactory;
    private final ProjectService projectService;
    private final MemoryRepository memoryRepository;
    private final FtsRepository ftsRepository;
    private final TransactionTemplate transactionTemplate;
    private final SensitiveDataGuard sensitiveDataGuard;

    public BriefLifecycleService() {
        this(new DbConnectionFactory(), new ProjectService(), new MemoryRepository(),
                new FtsRepository(), new TransactionTemplate(), new SensitiveDataGuard());
    }

    BriefLifecycleService(DbConnectionFactory connectionFactory, ProjectService projectService,
                          MemoryRepository memoryRepository, FtsRepository ftsRepository,
                          TransactionTemplate transactionTemplate, SensitiveDataGuard sensitiveDataGuard) {
        this.connectionFactory = connectionFactory;
        this.projectService = projectService;
        this.memoryRepository = memoryRepository;
        this.ftsRepository = ftsRepository;
        this.transactionTemplate = transactionTemplate;
        this.sensitiveDataGuard = sensitiveDataGuard;
    }

    public void recordPreWorkInteraction(Path projectRoot, WorkBrief brief) throws Exception {
        if (!brief.confirmationRequired() && brief.safeToStart()) {
            return;
        }
        String type = "ask".equals(brief.recommendation()) ? "clarification" : interactionType(brief);
        String question = "ask".equals(brief.recommendation())
                ? "请先确认任务边界，再让 Agent 开始。"
                : "当前任务需要用户确认后再继续执行。";
        InteractionRequest request = new InteractionRequest(
                "interaction-" + brief.briefId(),
                "",
                "pre_work",
                type,
                "blocking",
                question,
                brief.confirmationReason(),
                join(brief.userChoices(), "|"),
                "按建议继续",
                true,
                "open",
                "");
        upsertInteraction(projectRoot, request);
        writeInteractionsBrief(projectRoot);
    }

    public void requireNoBlockingInteraction(Path projectRoot, String goalKey, String currentAction) {
        for (InteractionRequest request : loadInteractions(projectRoot)) {
            if (request.openBlockingFor(goalKey)) {
                if ("manual_evidence".equals(request.type()) && "verify".equals(currentAction)) {
                    continue;
                }
                throw new BlockingInteractionException(request.requestId(), request.question(), request.choices());
            }
        }
    }

    public InteractionRequest answerInteraction(Path projectRoot, String requestId, String answer) throws Exception {
        List<InteractionRequest> requests = loadInteractions(projectRoot);
        List<InteractionRequest> updated = new ArrayList<InteractionRequest>();
        InteractionRequest answered = null;
        for (InteractionRequest request : requests) {
            if (request.requestId().equals(requestId)) {
                answered = request.answered(answer);
                updated.add(answered);
            } else {
                updated.add(request);
            }
        }
        if (answered == null) {
            throw new IllegalArgumentException("Interaction request not found: " + requestId);
        }
        saveInteractions(projectRoot, updated);
        writeInteractionsBrief(projectRoot);
        updateAgentBriefAfterAnswer(projectRoot);
        return answered;
    }

    public InteractionRequest findInteraction(Path projectRoot, String requestId) {
        for (InteractionRequest request : loadInteractions(projectRoot)) {
            if (request.requestId().equals(requestId)) {
                return request;
            }
        }
        return null;
    }

    public Path writeProgressBrief(Path projectRoot, GoalRun goal, long stepId, String summary) throws Exception {
        Files.createDirectories(PathUtil.devharnessBriefsDirectory(projectRoot));
        StringBuilder builder = new StringBuilder();
        builder.append("# Progress Brief\n\n");
        builder.append("- goal_key: ").append(goal.goalKey()).append('\n');
        builder.append("- status: ").append(goal.status()).append('\n');
        builder.append("- current_action: ").append(goal.currentAction()).append('\n');
        builder.append("- step_id: ").append(stepId).append('\n');
        builder.append("- step_number: ").append(goal.stepCount()).append("\n\n");
        builder.append("## 已完成\n\n");
        builder.append("- ").append(summary == null || summary.trim().length() == 0
                ? "已记录当前步骤证据。" : summary.trim()).append("\n\n");
        builder.append("## 下一步\n\n");
        builder.append("- 继续按当前 Goal Context 执行下一步，必要时先处理 blocking interaction。\n\n");
        appendOpenInteractions(builder, projectRoot, goal.goalKey());
        Path path = PathUtil.progressBrief(projectRoot);
        Files.write(path, builder.toString().getBytes("UTF-8"));
        return path;
    }

    public Path writeVerifyBrief(Path projectRoot, GoalRun goal, List<GoalCheck> checks,
                                 GoalEvaluation evaluation) throws Exception {
        Files.createDirectories(PathUtil.devharnessBriefsDirectory(projectRoot));
        StringBuilder builder = new StringBuilder();
        builder.append("# Verify Brief\n\n");
        builder.append("- goal_key: ").append(goal.goalKey()).append('\n');
        builder.append("- decision: ").append(evaluation.decision()).append('\n');
        builder.append("- ready_to_complete: ").append(evaluation.readyToComplete()).append("\n\n");
        builder.append("## 验证结果\n\n");
        for (GoalCheck check : checks) {
            builder.append("- ").append(check.checkKey()).append(": ")
                    .append(check.status()).append(" - ").append(check.resultSummary()).append('\n');
        }
        if (checks.isEmpty()) {
            builder.append("- 暂无检查结果。\n");
        }
        builder.append("\n## 还差什么\n\n");
        if (evaluation.missing().length == 0) {
            builder.append("- 没有阻塞项，可以完成。\n");
        } else {
            for (String missing : evaluation.missing()) {
                builder.append("- ").append(userFriendlyBlocker(missing)).append('\n');
            }
        }
        builder.append("\n## 下一步\n\n");
        builder.append("- ").append(evaluation.nextCommand()).append('\n');
        recordVerifyInteraction(projectRoot, goal, evaluation);
        Path path = PathUtil.verifyBrief(projectRoot);
        Files.write(path, builder.toString().getBytes("UTF-8"));
        return path;
    }

    public Path writeCompletionBrief(Path projectRoot, GoalRun goal, long checkpointId, Path summaryPath)
            throws Exception {
        Files.createDirectories(PathUtil.devharnessBriefsDirectory(projectRoot));
        ensureDefaultKnowledgeCandidates(projectRoot, goal);
        StringBuilder builder = new StringBuilder();
        builder.append("# Completion Brief\n\n");
        builder.append("- goal_key: ").append(goal.goalKey()).append('\n');
        builder.append("- status: ").append(goal.status()).append('\n');
        builder.append("- checkpoint_id: ").append(checkpointId).append('\n');
        builder.append("- summary_path: ").append(summaryPath).append('\n');
        builder.append("- knowledge_candidates_path: ").append(PathUtil.knowledgeCandidatesBrief(projectRoot)).append("\n\n");
        builder.append("## 完成内容\n\n");
        builder.append("- 本次 goal 已完成，详细审计见 GOAL_SUMMARY.md 与 ARTIFACT_PASSPORT.json。\n\n");
        builder.append("## 知识沉淀\n\n");
        builder.append("- 已生成候选知识，用户确认前不会进入 confirmed memory。\n");
        Path path = PathUtil.completionBrief(projectRoot);
        Files.write(path, builder.toString().getBytes("UTF-8"));
        writeKnowledgeBrief(projectRoot);
        return path;
    }

    public List<KnowledgeCandidate> ensureDefaultKnowledgeCandidates(Path projectRoot, GoalRun goal) throws Exception {
        List<KnowledgeCandidate> candidates = loadCandidates(projectRoot);
        if (findCandidate(candidates, "kc-" + goal.goalKey() + "-verification") == null) {
            candidates.add(new KnowledgeCandidate("kc-" + goal.goalKey() + "-verification", goal.goalKey(),
                    "verification_rule_candidate",
                    "本次任务的验证方式",
                    "记录本次 goal 使用的验证路径，可作为未来类似任务的人工或自动验证参考。",
                    "goal:" + goal.goalKey(), "project_memory", "medium", true, "passed", "draft"));
        }
        if (findCandidate(candidates, "kc-" + goal.goalKey() + "-growth") == null) {
            candidates.add(new KnowledgeCandidate("kc-" + goal.goalKey() + "-growth", goal.goalKey(),
                    "growth_lesson_candidate",
                    "本次任务的执行经验",
                    "保留本次任务的风险识别、轻重流程选择和验证经验，供未来 Work Brief advisory 使用。",
                    "goal:" + goal.goalKey(), "growth", "medium", true, "passed", "draft"));
        }
        saveCandidates(projectRoot, candidates);
        return candidates;
    }

    public List<KnowledgeCandidate> loadCandidates(Path projectRoot) {
        List<KnowledgeCandidate> result = new ArrayList<KnowledgeCandidate>();
        for (String[] row : readRows(PathUtil.knowledgeCandidatesStore(projectRoot))) {
            if (row.length < 11) {
                continue;
            }
            result.add(new KnowledgeCandidate(row[0], row[1], row[2], row[3], row[4], row[5],
                    row[6], row[7], Boolean.parseBoolean(row[8]), row[9], row[10]));
        }
        return result;
    }

    public KnowledgeCandidate rejectCandidate(Path projectRoot, String candidateId) throws Exception {
        List<KnowledgeCandidate> candidates = loadCandidates(projectRoot);
        KnowledgeCandidate selected = null;
        List<KnowledgeCandidate> updated = new ArrayList<KnowledgeCandidate>();
        for (KnowledgeCandidate candidate : candidates) {
            if (candidate.candidateId().equals(candidateId)) {
                selected = candidate.status("rejected");
                updated.add(selected);
            } else {
                updated.add(candidate);
            }
        }
        if (selected == null) {
            throw new IllegalArgumentException("Knowledge candidate not found: " + candidateId);
        }
        saveCandidates(projectRoot, updated);
        writeKnowledgeBrief(projectRoot);
        return selected;
    }

    public KnowledgeCandidate confirmCandidate(CommandContext context, Path projectRoot, String candidateId,
                                               String destination) throws Exception {
        List<KnowledgeCandidate> candidates = loadCandidates(projectRoot);
        KnowledgeCandidate selected = null;
        List<KnowledgeCandidate> updated = new ArrayList<KnowledgeCandidate>();
        for (KnowledgeCandidate candidate : candidates) {
            if (candidate.candidateId().equals(candidateId)) {
                selected = candidate.destination(destination.length() == 0
                        ? candidate.suggestedDestination() : destination);
                if (!"passed".equals(selected.sensitiveScanStatus())) {
                    throw new IllegalArgumentException("Knowledge candidate sensitive scan is not passed: "
                            + selected.sensitiveScanStatus());
                }
                updated.add(selected.status("confirmed"));
            } else {
                updated.add(candidate);
            }
        }
        if (selected == null) {
            throw new IllegalArgumentException("Knowledge candidate not found: " + candidateId);
        }
        if ("growth".equals(selected.suggestedDestination())) {
            createGrowthDraft(projectRoot, selected);
        } else {
            createMemoryDraft(context, projectRoot, selected);
        }
        saveCandidates(projectRoot, updated);
        writeKnowledgeBrief(projectRoot);
        return selected.status("confirmed");
    }

    public List<GrowthLesson> loadGrowthLessons(Path projectRoot) {
        List<GrowthLesson> result = new ArrayList<GrowthLesson>();
        for (String[] row : readRows(PathUtil.growthLessonsStore(projectRoot))) {
            if (row.length < 6) {
                continue;
            }
            result.add(new GrowthLesson(row[0], row[1], row[2], row[3], row[4], Boolean.parseBoolean(row[5])));
        }
        return result;
    }

    public GrowthLesson confirmGrowth(Path projectRoot, String lessonId) throws Exception {
        List<GrowthLesson> lessons = loadGrowthLessons(projectRoot);
        GrowthLesson selected = null;
        List<GrowthLesson> updated = new ArrayList<GrowthLesson>();
        for (GrowthLesson lesson : lessons) {
            if (lesson.lessonId().equals(lessonId)) {
                selected = lesson.status("confirmed");
                updated.add(selected);
            } else {
                updated.add(lesson);
            }
        }
        if (selected == null) {
            throw new IllegalArgumentException("Growth lesson not found: " + lessonId);
        }
        saveGrowthLessons(projectRoot, updated);
        return selected;
    }

    public Path exportGrowth(Path projectRoot) throws Exception {
        Files.createDirectories(PathUtil.devharnessBriefsDirectory(projectRoot));
        StringBuilder builder = new StringBuilder();
        builder.append("# Growth Context\n\n");
        builder.append("advisory_only: true\n\n");
        for (GrowthLesson lesson : loadGrowthLessons(projectRoot)) {
            if ("rejected".equals(lesson.status())) {
                continue;
            }
            builder.append("## ").append(lesson.title()).append("\n\n");
            builder.append("- lesson_id: ").append(lesson.lessonId()).append('\n');
            builder.append("- status: ").append(lesson.status()).append('\n');
            builder.append("- advisory_only: ").append(lesson.advisoryOnly()).append("\n\n");
            builder.append(lesson.summary()).append("\n\n");
        }
        Path path = PathUtil.growthContext(projectRoot);
        Files.write(path, builder.toString().getBytes("UTF-8"));
        return path;
    }

    public Path writeKnowledgeBrief(Path projectRoot) throws Exception {
        Files.createDirectories(PathUtil.devharnessBriefsDirectory(projectRoot));
        StringBuilder builder = new StringBuilder();
        builder.append("# Knowledge Candidates\n\n");
        builder.append("用户确认前，候选知识不会进入 confirmed memory。\n\n");
        for (KnowledgeCandidate candidate : loadCandidates(projectRoot)) {
            if ("rejected".equals(candidate.status())) {
                continue;
            }
            builder.append("## ").append(candidate.title()).append("\n\n");
            builder.append("- candidate_id: ").append(candidate.candidateId()).append('\n');
            builder.append("- goal_key: ").append(candidate.goalKey()).append('\n');
            builder.append("- type: ").append(candidate.type()).append('\n');
            builder.append("- suggested_destination: ").append(candidate.suggestedDestination()).append('\n');
            builder.append("- confidence: ").append(candidate.confidence()).append('\n');
            builder.append("- requires_confirmation: ").append(candidate.requiresConfirmation()).append('\n');
            builder.append("- sensitive_scan_status: ").append(candidate.sensitiveScanStatus()).append('\n');
            builder.append("- status: ").append(candidate.status()).append("\n\n");
            builder.append(candidate.summary()).append("\n\n");
        }
        Path path = PathUtil.knowledgeCandidatesBrief(projectRoot);
        Files.write(path, builder.toString().getBytes("UTF-8"));
        return path;
    }

    private void createMemoryDraft(CommandContext context, Path projectRoot, KnowledgeCandidate candidate)
            throws Exception {
        if (!Files.isRegularFile(PathUtil.memoryDb(projectRoot))) {
            throw new IllegalArgumentException("memory db is not initialized; run memory init first");
        }
        List<String> matches = sensitiveDataGuard.findMatches(candidate.title() + "\n" + candidate.summary());
        if (!matches.isEmpty()) {
            throw new IllegalArgumentException("Sensitive data rejected for memory draft: " + matches);
        }
        try (Connection connection = connectionFactory.open(projectRoot)) {
            Project project = projectService.ensureProject(projectRoot, context.clock());
            final String now = context.clock().now().toString();
            final MemoryItem item = new MemoryItem(0L, project.projectKey(), "global",
                    memoryType(candidate.type()), "project", candidate.title(), candidate.summary(),
                    "knowledge-candidate", "draft", 60, "knowledge_candidate", "", "",
                    candidate.evidenceRefs(), "candidate_id=" + candidate.candidateId(),
                    "", "", now, now, "", 0);
            transactionTemplate.execute(connection, new TransactionTemplate.Work<Long>() {
                public Long execute() throws Exception {
                    long id = memoryRepository.insert(connection, item);
                    MemoryItem inserted = memoryRepository.findById(connection, item.projectKey(), id);
                    ftsRepository.sync(connection, inserted);
                    return Long.valueOf(id);
                }
            });
        }
    }

    private void createGrowthDraft(Path projectRoot, KnowledgeCandidate candidate) throws Exception {
        List<GrowthLesson> lessons = loadGrowthLessons(projectRoot);
        String lessonId = "growth-" + candidate.candidateId();
        for (GrowthLesson lesson : lessons) {
            if (lesson.lessonId().equals(lessonId)) {
                saveGrowthLessons(projectRoot, lessons);
                return;
            }
        }
        lessons.add(new GrowthLesson(lessonId, candidate.candidateId(), candidate.title(),
                candidate.summary(), "draft", true));
        saveGrowthLessons(projectRoot, lessons);
        exportGrowth(projectRoot);
    }

    private String memoryType(String candidateType) {
        if (candidateType.contains("convention")) {
            return "code_pattern";
        }
        if (candidateType.contains("risk")) {
            return "risk";
        }
        if (candidateType.contains("verification")) {
            return "testing_convention";
        }
        return "project_fact";
    }

    private void upsertInteraction(Path projectRoot, InteractionRequest request) throws Exception {
        List<InteractionRequest> requests = loadInteractions(projectRoot);
        List<InteractionRequest> updated = new ArrayList<InteractionRequest>();
        boolean replaced = false;
        for (InteractionRequest existing : requests) {
            if (existing.requestId().equals(request.requestId())) {
                updated.add(existing.status().length() == 0 || "open".equals(existing.status()) ? request : existing);
                replaced = true;
            } else {
                updated.add(existing);
            }
        }
        if (!replaced) {
            updated.add(request);
        }
        saveInteractions(projectRoot, updated);
    }

    private String interactionType(WorkBrief brief) {
        String reason = brief.confirmationReason().toLowerCase(Locale.ROOT);
        if (reason.contains("风险") || reason.contains("权限") || reason.contains("支付")
                || reason.contains("安全") || reason.contains("数据库")) {
            return "risk_escalation";
        }
        return "confirmation";
    }

    private void recordVerifyInteraction(Path projectRoot, GoalRun goal, GoalEvaluation evaluation) throws Exception {
        for (String missing : evaluation.missing()) {
            String lower = missing.toLowerCase(Locale.ROOT);
            if (lower.contains("manual") || lower.contains("evidence")) {
                upsertInteraction(projectRoot, new InteractionRequest(
                        "interaction-" + goal.goalKey() + "-manual-evidence",
                        goal.goalKey(),
                        "verify",
                        "manual_evidence",
                        "blocking",
                        "请补充人工验证证据后再完成任务。",
                        missing,
                        "已补充证据|暂不完成|切换验证策略",
                        "已补充证据",
                        true,
                        "open",
                        ""));
                writeInteractionsBrief(projectRoot);
                return;
            }
        }
    }

    private List<InteractionRequest> loadInteractions(Path projectRoot) {
        List<InteractionRequest> result = new ArrayList<InteractionRequest>();
        for (String[] row : readRows(PathUtil.interactionRequests(projectRoot))) {
            if (row.length < 12) {
                continue;
            }
            result.add(new InteractionRequest(row[0], row[1], row[2], row[3], row[4], row[5],
                    row[6], row[7], row[8], Boolean.parseBoolean(row[9]), row[10], row[11]));
        }
        return result;
    }

    private void saveInteractions(Path projectRoot, List<InteractionRequest> requests) throws Exception {
        Files.createDirectories(PathUtil.devharnessBriefsDirectory(projectRoot));
        StringBuilder builder = new StringBuilder();
        for (InteractionRequest request : requests) {
            builder.append(escape(request.requestId())).append('\t')
                    .append(escape(request.goalKey())).append('\t')
                    .append(escape(request.phase())).append('\t')
                    .append(escape(request.type())).append('\t')
                    .append(escape(request.priority())).append('\t')
                    .append(escape(request.question())).append('\t')
                    .append(escape(request.why())).append('\t')
                    .append(escape(request.choices())).append('\t')
                    .append(escape(request.defaultChoice())).append('\t')
                    .append(request.blocksProgress()).append('\t')
                    .append(escape(request.status())).append('\t')
                    .append(escape(request.answer())).append('\n');
        }
        Files.write(PathUtil.interactionRequests(projectRoot), builder.toString().getBytes("UTF-8"));
    }

    private void saveCandidates(Path projectRoot, List<KnowledgeCandidate> candidates) throws Exception {
        Files.createDirectories(PathUtil.devharnessBriefsDirectory(projectRoot));
        StringBuilder builder = new StringBuilder();
        for (KnowledgeCandidate candidate : candidates) {
            builder.append(escape(candidate.candidateId())).append('\t')
                    .append(escape(candidate.goalKey())).append('\t')
                    .append(escape(candidate.type())).append('\t')
                    .append(escape(candidate.title())).append('\t')
                    .append(escape(candidate.summary())).append('\t')
                    .append(escape(candidate.evidenceRefs())).append('\t')
                    .append(escape(candidate.suggestedDestination())).append('\t')
                    .append(escape(candidate.confidence())).append('\t')
                    .append(candidate.requiresConfirmation()).append('\t')
                    .append(escape(candidate.sensitiveScanStatus())).append('\t')
                    .append(escape(candidate.status())).append('\n');
        }
        Files.write(PathUtil.knowledgeCandidatesStore(projectRoot), builder.toString().getBytes("UTF-8"));
    }

    private void saveGrowthLessons(Path projectRoot, List<GrowthLesson> lessons) throws Exception {
        Files.createDirectories(PathUtil.devharnessBriefsDirectory(projectRoot));
        StringBuilder builder = new StringBuilder();
        for (GrowthLesson lesson : lessons) {
            builder.append(escape(lesson.lessonId())).append('\t')
                    .append(escape(lesson.sourceCandidateId())).append('\t')
                    .append(escape(lesson.title())).append('\t')
                    .append(escape(lesson.summary())).append('\t')
                    .append(escape(lesson.status())).append('\t')
                    .append(lesson.advisoryOnly()).append('\n');
        }
        Files.write(PathUtil.growthLessonsStore(projectRoot), builder.toString().getBytes("UTF-8"));
    }

    private void writeInteractionsBrief(Path projectRoot) throws Exception {
        Files.createDirectories(PathUtil.devharnessBriefsDirectory(projectRoot));
        StringBuilder builder = new StringBuilder();
        builder.append("# Interaction Requests\n\n");
        for (InteractionRequest request : loadInteractions(projectRoot)) {
            builder.append("- request_id: ").append(request.requestId()).append('\n');
            builder.append("  type: ").append(request.type()).append('\n');
            builder.append("  priority: ").append(request.priority()).append('\n');
            builder.append("  question: ").append(request.question()).append('\n');
            builder.append("  why: ").append(request.why()).append('\n');
            builder.append("  blocks_progress: ").append(request.blocksProgress()).append('\n');
            builder.append("  status: ").append(request.status()).append("\n\n");
        }
        Files.write(PathUtil.devharnessBriefsDirectory(projectRoot).resolve("INTERACTION_REQUESTS.md"),
                builder.toString().getBytes("UTF-8"));
    }

    private void appendOpenInteractions(StringBuilder builder, Path projectRoot, String goalKey) {
        List<InteractionRequest> requests = loadInteractions(projectRoot);
        boolean any = false;
        for (InteractionRequest request : requests) {
            if (request.openBlockingFor(goalKey)) {
                if (!any) {
                    builder.append("## 需要用户处理\n\n");
                    any = true;
                }
                builder.append("- ").append(request.question()).append(" (request_id: ")
                        .append(request.requestId()).append(")\n");
            }
        }
        if (!any) {
            builder.append("## 需要用户处理\n\n- 当前没有 blocking interaction。\n");
        }
    }

    private void updateAgentBriefAfterAnswer(Path projectRoot) throws Exception {
        Path path = PathUtil.agentBrief(projectRoot);
        if (!Files.isRegularFile(path)) {
            return;
        }
        String text = new String(Files.readAllBytes(path), "UTF-8");
        text = text.replace("\"current_action\": \"wait_for_user_answer\"",
                "\"current_action\": \"answered_continue\"");
        Files.write(path, text.getBytes("UTF-8"));
    }

    private String userFriendlyBlocker(String missing) {
        String lower = missing.toLowerCase(Locale.ROOT);
        if (lower.contains("manual") || lower.contains("evidence")) {
            return "需要补充人工验证或结构化证据：" + missing;
        }
        if (lower.contains("stale")) {
            return "检查结果已过期，需要重新验证：" + missing;
        }
        if (lower.contains("pending")) {
            return "还有检查未执行：" + missing;
        }
        return missing;
    }

    private KnowledgeCandidate findCandidate(List<KnowledgeCandidate> candidates, String id) {
        for (KnowledgeCandidate candidate : candidates) {
            if (candidate.candidateId().equals(id)) {
                return candidate;
            }
        }
        return null;
    }

    private List<String[]> readRows(Path path) {
        List<String[]> rows = new ArrayList<String[]>();
        if (!Files.isRegularFile(path)) {
            return rows;
        }
        try {
            for (String line : Files.readAllLines(path)) {
                if (line.trim().length() == 0) {
                    continue;
                }
                String[] raw = line.split("\\t", -1);
                String[] unescaped = new String[raw.length];
                for (int i = 0; i < raw.length; i++) {
                    unescaped[i] = unescape(raw[i]);
                }
                rows.add(unescaped);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to read brief lifecycle store: " + ex.getMessage(), ex);
        }
        return rows;
    }

    private String join(String[] values, String delimiter) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) {
                builder.append(delimiter);
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\")
                .replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r");
    }

    private String unescape(String value) {
        StringBuilder builder = new StringBuilder();
        boolean slash = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (slash) {
                if (ch == 't') {
                    builder.append('\t');
                } else if (ch == 'n') {
                    builder.append('\n');
                } else if (ch == 'r') {
                    builder.append('\r');
                } else {
                    builder.append(ch);
                }
                slash = false;
            } else if (ch == '\\') {
                slash = true;
            } else {
                builder.append(ch);
            }
        }
        if (slash) {
            builder.append('\\');
        }
        return builder.toString();
    }
}
