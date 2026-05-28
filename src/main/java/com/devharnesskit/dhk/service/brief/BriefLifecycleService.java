package com.devharnesskit.dhk.service.brief;

import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.MigrationRunner;
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
import com.devharnesskit.dhk.repository.ProjectRepository;
import com.devharnesskit.dhk.repository.brief.GrowthLessonRepository;
import com.devharnesskit.dhk.repository.brief.InteractionRequestRepository;
import com.devharnesskit.dhk.repository.brief.KnowledgeCandidateRepository;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.service.SensitiveDataGuard;
import com.devharnesskit.dhk.util.Clock;
import com.devharnesskit.dhk.util.PathUtil;
import com.devharnesskit.dhk.util.SystemClock;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class BriefLifecycleService {
    private final DbConnectionFactory connectionFactory;
    private final MigrationRunner migrationRunner;
    private final ProjectService projectService;
    private final ProjectRepository projectRepository;
    private final MemoryRepository memoryRepository;
    private final FtsRepository ftsRepository;
    private final KnowledgeCandidateRepository candidateRepository;
    private final InteractionRequestRepository interactionRepository;
    private final GrowthLessonRepository growthLessonRepository;
    private final BriefLifecycleCompatibilityStoreWriter compatibilityStoreWriter;
    private final ManualVerificationInteractionSupport manualVerificationInteractionSupport;
    private final ProfessionalKnowledgeCandidateFactory knowledgeCandidateFactory;
    private final KnowledgeCandidatesBriefRenderer knowledgeBriefRenderer;
    private final TransactionTemplate transactionTemplate;
    private final SensitiveDataGuard sensitiveDataGuard;
    private final Clock systemClock = new SystemClock();

    public BriefLifecycleService() {
        this(new DbConnectionFactory(), new MigrationRunner(), new ProjectService(), new ProjectRepository(),
                new MemoryRepository(), new FtsRepository(), new KnowledgeCandidateRepository(),
                new InteractionRequestRepository(), new GrowthLessonRepository(), new BriefLifecycleCompatibilityStoreWriter(),
                new ManualVerificationInteractionSupport(), new ProfessionalKnowledgeCandidateFactory(),
                new KnowledgeCandidatesBriefRenderer(), new TransactionTemplate(), new SensitiveDataGuard());
    }

    BriefLifecycleService(DbConnectionFactory connectionFactory, MigrationRunner migrationRunner,
                          ProjectService projectService, ProjectRepository projectRepository, MemoryRepository memoryRepository,
                          FtsRepository ftsRepository, KnowledgeCandidateRepository candidateRepository,
                          InteractionRequestRepository interactionRepository, GrowthLessonRepository growthLessonRepository,
                          BriefLifecycleCompatibilityStoreWriter compatibilityStoreWriter,
                          ManualVerificationInteractionSupport manualVerificationInteractionSupport,
                          ProfessionalKnowledgeCandidateFactory knowledgeCandidateFactory,
                          KnowledgeCandidatesBriefRenderer knowledgeBriefRenderer,
                          TransactionTemplate transactionTemplate, SensitiveDataGuard sensitiveDataGuard) {
        this.connectionFactory = connectionFactory;
        this.migrationRunner = migrationRunner;
        this.projectService = projectService;
        this.projectRepository = projectRepository;
        this.memoryRepository = memoryRepository;
        this.ftsRepository = ftsRepository;
        this.candidateRepository = candidateRepository;
        this.interactionRepository = interactionRepository;
        this.growthLessonRepository = growthLessonRepository;
        this.compatibilityStoreWriter = compatibilityStoreWriter;
        this.manualVerificationInteractionSupport = manualVerificationInteractionSupport;
        this.knowledgeCandidateFactory = knowledgeCandidateFactory;
        this.knowledgeBriefRenderer = knowledgeBriefRenderer;
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
                if (("manual_evidence".equals(request.type()) || "manual_verification".equals(request.type()))
                        && "verify".equals(currentAction)) {
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

    public InteractionRequest answerInteraction(Path projectRoot, String requestId, String answer,
                                                String evidencePath, String scope, String tester,
                                                String reason, String approver, String riskScope,
                                                String rollbackPlan) throws Exception {
        InteractionRequest request = findInteraction(projectRoot, requestId);
        if (request != null && "manual_verification".equals(request.type())) {
            return answerInteraction(projectRoot, requestId, manualVerificationInteractionSupport.answer(request, answer,
                    evidencePath, scope, tester, reason, approver, riskScope, rollbackPlan));
        }
        return answerInteraction(projectRoot, requestId, answer);
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
        recordVerifyInteractions(projectRoot, goal, checks, evaluation);
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
        for (KnowledgeCandidate candidate : knowledgeCandidateFactory.candidates(projectRoot, goal)) {
            if (findCandidate(candidates, candidate.candidateId()) == null) {
                candidates.add(candidate);
            }
        }
        saveCandidates(projectRoot, candidates);
        return candidates;
    }

    public List<KnowledgeCandidate> loadCandidates(Path projectRoot) {
        try {
            return withLifecycleStore(projectRoot, new LifecycleStoreWork<List<KnowledgeCandidate>>() {
                public List<KnowledgeCandidate> execute(Connection connection, Project project, String now)
                        throws Exception {
                    return candidateRepository.list(connection);
                }
            });
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load knowledge candidates from SQLite: " + ex.getMessage(), ex);
        }
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
        try {
            return withLifecycleStore(projectRoot, new LifecycleStoreWork<List<GrowthLesson>>() {
                public List<GrowthLesson> execute(Connection connection, Project project, String now)
                        throws Exception {
                    return growthLessonRepository.list(connection);
                }
            });
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load growth lessons from SQLite: " + ex.getMessage(), ex);
        }
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
        Path path = PathUtil.knowledgeCandidatesBrief(projectRoot);
        Files.write(path, knowledgeBriefRenderer.render(loadCandidates(projectRoot)).getBytes("UTF-8"));
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

    private void recordVerifyInteractions(Path projectRoot, GoalRun goal, List<GoalCheck> checks,
                                          GoalEvaluation evaluation) throws Exception {
        boolean manualVerificationCreated = false;
        for (GoalCheck check : checks) {
            if (manualVerificationInteractionSupport.isFailedManualVerification(check)) {
                upsertInteraction(projectRoot, manualVerificationInteractionSupport.request(goal, check));
                manualVerificationCreated = true;
            }
        }
        if (manualVerificationCreated) {
            writeInteractionsBrief(projectRoot);
            return;
        }
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
        try {
            return withLifecycleStore(projectRoot, new LifecycleStoreWork<List<InteractionRequest>>() {
                public List<InteractionRequest> execute(Connection connection, Project project, String now)
                        throws Exception {
                    return interactionRepository.list(connection);
                }
            });
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load interaction requests from SQLite: " + ex.getMessage(), ex);
        }
    }

    private void saveInteractions(Path projectRoot, List<InteractionRequest> requests) throws Exception {
        withLifecycleStore(projectRoot, new LifecycleStoreWork<Void>() {
            public Void execute(Connection connection, Project project, String now) throws Exception {
                for (InteractionRequest request : requests) {
                    interactionRepository.upsert(connection, project.projectKey(), request, now);
                }
                return null;
            }
        });
        compatibilityStoreWriter.writeInteractions(projectRoot, requests);
    }

    private void saveCandidates(Path projectRoot, List<KnowledgeCandidate> candidates) throws Exception {
        withLifecycleStore(projectRoot, new LifecycleStoreWork<Void>() {
            public Void execute(Connection connection, Project project, String now) throws Exception {
                for (KnowledgeCandidate candidate : candidates) {
                    candidateRepository.upsert(connection, project.projectKey(), candidate, now);
                }
                return null;
            }
        });
        compatibilityStoreWriter.writeCandidates(projectRoot, candidates);
    }

    private void saveGrowthLessons(Path projectRoot, List<GrowthLesson> lessons) throws Exception {
        withLifecycleStore(projectRoot, new LifecycleStoreWork<Void>() {
            public Void execute(Connection connection, Project project, String now) throws Exception {
                for (GrowthLesson lesson : lessons) {
                    growthLessonRepository.upsert(connection, project.projectKey(), lesson, now);
                }
                return null;
            }
        });
        compatibilityStoreWriter.writeGrowthLessons(projectRoot, lessons);
    }

    private <T> T withLifecycleStore(Path projectRoot, LifecycleStoreWork<T> work) throws Exception {
        PathUtil.createMemoryDirectories(projectRoot);
        try (Connection connection = connectionFactory.open(projectRoot)) {
            migrationRunner.migrate(connection, systemClock);
            Project project = projectService.ensureProject(projectRoot, systemClock);
            projectRepository.upsert(connection, project);
            return work.execute(connection, project, systemClock.now().toString());
        }
    }

    private interface LifecycleStoreWork<T> {
        T execute(Connection connection, Project project, String now) throws Exception;
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
}
