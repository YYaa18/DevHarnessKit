package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.db.TransactionTemplate;
import com.devharnesskit.dhk.export.GoalSummaryRenderer;
import com.devharnesskit.dhk.model.Checkpoint;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.goal.GoalArtifact;
import com.devharnesskit.dhk.model.goal.GoalCheck;
import com.devharnesskit.dhk.model.goal.GoalEvaluation;
import com.devharnesskit.dhk.model.goal.GoalEvent;
import com.devharnesskit.dhk.model.goal.GoalPlan;
import com.devharnesskit.dhk.model.goal.GoalProfile;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.model.goal.GoalStep;
import com.devharnesskit.dhk.model.spec.SpecChange;
import com.devharnesskit.dhk.model.workflow.WorkflowRun;
import com.devharnesskit.dhk.model.workflow.WorkflowTemplate;
import com.devharnesskit.dhk.repository.ProjectRepository;
import com.devharnesskit.dhk.repository.CheckpointRepository;
import com.devharnesskit.dhk.repository.goal.GoalArtifactRepository;
import com.devharnesskit.dhk.repository.goal.GoalCheckRepository;
import com.devharnesskit.dhk.repository.goal.GoalEventRepository;
import com.devharnesskit.dhk.repository.goal.GoalRunRepository;
import com.devharnesskit.dhk.repository.goal.GoalStepRepository;
import com.devharnesskit.dhk.repository.spec.SpecChangeRepository;
import com.devharnesskit.dhk.repository.spec.SpecDocumentRepository;
import com.devharnesskit.dhk.repository.spec.SpecEventRepository;
import com.devharnesskit.dhk.repository.spec.WorkflowSpecBindingRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowEventRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowGateRunRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowGateTemplateRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowPhaseRunRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowPhaseTemplateRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowRunRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowTemplateRepository;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.service.SensitiveDataGuard;
import com.devharnesskit.dhk.service.spec.SpecService;
import com.devharnesskit.dhk.service.workflow.WorkflowSeedService;
import com.devharnesskit.dhk.service.workflow.WorkflowStartService;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public final class GoalOrchestrator {
    private final DbConnectionFactory connectionFactory = new DbConnectionFactory();
    private final MigrationRunner migrationRunner = new MigrationRunner();
    private final ProjectService projectService = new ProjectService();
    private final ProjectRepository projectRepository = new ProjectRepository();
    private final WorkflowTemplateRepository workflowTemplateRepository = new WorkflowTemplateRepository();
    private final WorkflowSeedService workflowSeedService = new WorkflowSeedService(
            workflowTemplateRepository, new WorkflowPhaseTemplateRepository(),
            new WorkflowGateTemplateRepository());
    private final WorkflowRunRepository workflowRunRepository = new WorkflowRunRepository();
    private final WorkflowStartService workflowStartService = new WorkflowStartService(
            new WorkflowPhaseTemplateRepository(), new WorkflowGateTemplateRepository(),
            workflowRunRepository, new WorkflowPhaseRunRepository(), new WorkflowGateRunRepository(),
            new WorkflowEventRepository());
    private final SpecChangeRepository specChangeRepository = new SpecChangeRepository();
    private final SpecService specService = new SpecService(specChangeRepository,
            new SpecDocumentRepository(), new SpecEventRepository(), new WorkflowSpecBindingRepository());
    private final GoalRunRepository goalRunRepository = new GoalRunRepository();
    private final GoalStepRepository goalStepRepository = new GoalStepRepository();
    private final GoalEventRepository goalEventRepository = new GoalEventRepository();
    private final GoalCheckRepository goalCheckRepository = new GoalCheckRepository();
    private final GoalArtifactRepository goalArtifactRepository = new GoalArtifactRepository();
    private final CheckpointRepository checkpointRepository = new CheckpointRepository();
    private final GoalProfileService profileService = new GoalProfileService();
    private final GoalPlanner planner = new GoalPlanner();
    private final GoalKeyGenerator keyGenerator = new GoalKeyGenerator();
    private final GoalContextService contextService = new GoalContextService();
    private final GoalCheckService checkService = new GoalCheckService();
    private final GoalCheckPolicyService checkPolicyService = new GoalCheckPolicyService();
    private final GoalCompletionEvaluator completionEvaluator = new GoalCompletionEvaluator();
    private final GoalSummaryRenderer summaryRenderer = new GoalSummaryRenderer();
    private final SensitiveDataGuard sensitiveDataGuard = new SensitiveDataGuard();
    private final TransactionTemplate transactionTemplate = new TransactionTemplate();

    public GoalStartResult start(CommandContext context, Path projectRoot, String profileKey,
                                 String task, String module, String mode, String condition)
            throws Exception {
        final GoalProfile profile = requireProfile(projectRoot, profileKey);
        final String selectedMode = mode.length() == 0 || "auto".equals(mode) ? profile.defaultMode() : mode;
        rejectSensitive("goal start", profileKey, task, module, selectedMode, condition);
        PathUtil.createMemoryDirectories(projectRoot);
        try (Connection connection = connectionFactory.open(projectRoot)) {
            final Project project = projectService.ensureProject(projectRoot, context.clock());
            migrationRunner.migrate(connection, context.clock());
            projectRepository.upsert(connection, project);
            workflowSeedService.seed(connection, context.clock().now().toString());
            final WorkflowTemplate template = workflowTemplateRepository.findByKey(connection, profile.workflowKey());
            if (template == null) {
                throw new IllegalStateException("Workflow template not found for profile: " + profile.profileKey());
            }
            final String now = context.clock().now().toString();
            GoalStartTransaction result = transactionTemplate.execute(connection,
                    new TransactionTemplate.Work<GoalStartTransaction>() {
                        public GoalStartTransaction execute() throws Exception {
                            WorkflowRun workflowRun = workflowStartService.start(connection, project.projectKey(),
                                    template, task, condition, module, selectedMode, context.clock().now());
                            SpecChange specChange = null;
                            if (profile.specRequired()) {
                                String changeKey = keyGenerator.uniqueChangeKey(connection, specChangeRepository,
                                        module, task, context.clock().now());
                                specChange = specService.createChange(connection, project, changeKey, task,
                                        condition, module, selectedMode, "normal", now);
                                specService.bindWorkflow(connection, specChange, workflowRun, "implements", now);
                            }
                            String goalKey = keyGenerator.uniqueGoalKey(connection, goalRunRepository,
                                    module, context.clock().now());
                            String firstAction = profile.actions().length == 0 ? "" : profile.actions()[0];
                            GoalRun goal = new GoalRun(goalKey, project.projectKey(), workflowRun.runKey(),
                                    specChange == null ? "" : specChange.changeKey(), profile.profileKey(),
                                    task, module, selectedMode, condition, "context_exporting", firstAction,
                                    30, 0, now, now, "");
                            goalRunRepository.insert(connection, goal);
                            goalEventRepository.insert(connection, new GoalEvent(0L, goalKey, "goal_started",
                                    "info", "Goal started", profile.profileKey(), now));
                            return new GoalStartTransaction(goal, workflowRun, specChange);
                        }
                    });
            Path contextPath = exportGoalContext(connection, projectRoot, project, result.goal(),
                    result.workflowRun(), result.specChange(), "context_ready", now);
            GoalRun readyGoal = goalRunRepository.findByKey(connection, result.goal().goalKey());
            return new GoalStartResult(readyGoal, result.workflowRun(), result.specChange(), contextPath);
        }
    }

    public GoalRun find(CommandContext context, Path projectRoot, String goalKey) throws Exception {
        try (Connection connection = connectionFactory.open(projectRoot)) {
            migrationRunner.migrate(connection, context.clock());
            GoalRun goal = goalRunRepository.findByKey(connection, goalKey);
            if (goal == null) {
                throw new IllegalStateException("Goal not found: " + goalKey);
            }
            return goal;
        }
    }

    public GoalRun latestOpen(CommandContext context, Path projectRoot) throws Exception {
        try (Connection connection = connectionFactory.open(projectRoot)) {
            Project project = projectService.readProject(PathUtil.projectJson(projectRoot));
            migrationRunner.migrate(connection, context.clock());
            GoalRun goal = goalRunRepository.latestOpen(connection, project.projectKey());
            if (goal == null) {
                throw new IllegalStateException("No open goal found");
            }
            return goal;
        }
    }

    public GoalPlan plan(GoalRun goal) {
        return planner.plan(goal, requireProfile(goal.profileKey()));
    }

    public GoalPlan plan(Path projectRoot, GoalRun goal) {
        return planner.plan(goal, requireProfile(projectRoot, goal.profileKey()));
    }

    public GoalStepResult step(CommandContext context, Path projectRoot, String goalKey,
                               String summary, String changedFiles, String evidence) throws Exception {
        rejectSensitive("goal step", summary, changedFiles, evidence);
        try (Connection connection = connectionFactory.open(projectRoot)) {
            migrationRunner.migrate(connection, context.clock());
            GoalRun goal = goalRunRepository.findByKey(connection, goalKey);
            if (goal == null) {
                throw new IllegalStateException("Goal not found: " + goalKey);
            }
            if (isContextExportIncomplete(goal.status())) {
                throw new IllegalStateException("Goal context export is incomplete; run dhk goal resume --goal "
                        + goal.goalKey());
            }
            GoalProfile profile = requireProfile(projectRoot, goal.profileKey());
            GoalPlan plan = planner.plan(goal, profile);
            validateStepEvidence(plan, summary, changedFiles, evidence);
            String now = context.clock().now().toString();
            int nextIndex = goal.stepCount() + 1;
            String action = goal.currentAction();
            long stepId = goalStepRepository.insert(connection, new GoalStep(0L, goal.goalKey(), nextIndex,
                    action, summary, changedFiles, evidence, "accepted", now));
            String nextAction = planner.nextAction(profile, action);
            String status = planner.statusForAction(nextAction);
            goalRunRepository.updateProgress(connection, goal.goalKey(), "context_exporting", nextAction, nextIndex, now);
            goalEventRepository.insert(connection, new GoalEvent(0L, goal.goalKey(), "goal_step_recorded",
                    "info", "Goal step recorded", action, now));
            GoalRun updated = goalRunRepository.findByKey(connection, goal.goalKey());
            Project project = projectService.readProject(PathUtil.projectJson(projectRoot));
            WorkflowRun workflowRun = workflowRunRepository.findByKey(connection, updated.workflowRunKey());
            SpecChange specChange = updated.specChangeKey().length() == 0
                    ? null : specChangeRepository.findByKey(connection, updated.specChangeKey());
            Path contextPath = exportGoalContext(connection, projectRoot, project, updated,
                    workflowRun, specChange, status, now);
            GoalRun readyGoal = goalRunRepository.findByKey(connection, goal.goalKey());
            return new GoalStepResult(stepId, readyGoal, contextPath);
        }
    }

    public Path export(CommandContext context, Path projectRoot, String goalKey) throws Exception {
        try (Connection connection = connectionFactory.open(projectRoot)) {
            migrationRunner.migrate(connection, context.clock());
            GoalRun goal = goalRunRepository.findByKey(connection, goalKey);
            if (goal == null) {
                throw new IllegalStateException("Goal not found: " + goalKey);
            }
            Project project = projectService.readProject(PathUtil.projectJson(projectRoot));
            WorkflowRun workflowRun = workflowRunRepository.findByKey(connection, goal.workflowRunKey());
            SpecChange specChange = goal.specChangeKey().length() == 0
                    ? null : specChangeRepository.findByKey(connection, goal.specChangeKey());
            String now = context.clock().now().toString();
            return exportGoalContext(connection, projectRoot, project, goal, workflowRun, specChange,
                    targetStatusAfterExport(goal), now);
        }
    }

    public List<GoalStep> steps(CommandContext context, Path projectRoot, String goalKey) throws Exception {
        try (Connection connection = connectionFactory.open(projectRoot)) {
            migrationRunner.migrate(connection, context.clock());
            return goalStepRepository.listByGoal(connection, goalKey);
        }
    }

    public List<GoalCheck> runCheck(CommandContext context, Path projectRoot, String goalKey,
                                    String checkKey, boolean all) throws Exception {
        try (Connection connection = connectionFactory.open(projectRoot)) {
            migrationRunner.migrate(connection, context.clock());
            GoalRun goal = goalRunRepository.findByKey(connection, goalKey);
            if (goal == null) {
                throw new IllegalStateException("Goal not found: " + goalKey);
            }
            String now = context.clock().now().toString();
            List<GoalCheck> checks = all
                    ? checkService.runAll(connection, projectRoot, goal, now)
                    : single(checkService.run(connection, projectRoot, goal, checkKey, now));
            for (GoalCheck check : checks) {
                if (check.evidencePath().length() > 0) {
                    goalArtifactRepository.insert(connection, new GoalArtifact(0L, goal.goalKey(),
                            "check_log", check.checkKey() + " check", check.evidencePath(), "",
                            check.resultSummary(), now));
                }
            }
            goalEventRepository.insert(connection, new GoalEvent(0L, goal.goalKey(), "goal_check_recorded",
                    "info", all ? "All goal checks recorded" : "Goal check recorded", all ? "all" : checkKey, now));
            return checks;
        }
    }

    public GoalEvaluation evaluate(CommandContext context, Path projectRoot, String goalKey) throws Exception {
        try (Connection connection = connectionFactory.open(projectRoot)) {
            migrationRunner.migrate(connection, context.clock());
            GoalRun goal = goalRunRepository.findByKey(connection, goalKey);
            if (goal == null) {
                throw new IllegalStateException("Goal not found: " + goalKey);
            }
            return evaluate(connection, projectRoot, goal);
        }
    }

    public GoalCompleteResult complete(CommandContext context, Path projectRoot, String goalKey) throws Exception {
        try (Connection connection = connectionFactory.open(projectRoot)) {
            migrationRunner.migrate(connection, context.clock());
            GoalRun goal = goalRunRepository.findByKey(connection, goalKey);
            if (goal == null) {
                throw new IllegalStateException("Goal not found: " + goalKey);
            }
            GoalEvaluation evaluation = evaluate(connection, projectRoot, goal);
            if (!evaluation.readyToComplete()) {
                throw new GoalNotReadyException(evaluation);
            }
            final String now = context.clock().now().toString();
            final List<GoalStep> steps = goalStepRepository.listByGoal(connection, goal.goalKey());
            final List<GoalCheck> checks = goalCheckRepository.listByGoal(connection, goal.goalKey());
            final Project project = projectService.readProject(PathUtil.projectJson(projectRoot));
            GoalCompleteResult result = transactionTemplate.execute(connection,
                    new TransactionTemplate.Work<GoalCompleteResult>() {
                        public GoalCompleteResult execute() throws Exception {
                            long checkpointId = checkpointRepository.insert(connection, new Checkpoint(0L,
                                    project.projectKey(), goal.taskName(), goal.moduleName(),
                                    "Goal completed: " + goal.taskName(), changedFiles(steps),
                                    "none", "goal checks accepted", PathUtil.GOAL_SUMMARY, now));
                            String summary = summaryRenderer.render(goal, steps, checks, checkpointId, now);
                            summary = sensitiveDataGuard.redact(summary);
                            if (sensitiveDataGuard.containsSensitiveData(summary)) {
                                throw new IllegalStateException("Sensitive data rejected during goal summary export: "
                                        + sensitiveDataGuard.findMatches(summary));
                            }
                            Path summaryPath = PathUtil.goalSummary(projectRoot);
                            Files.createDirectories(summaryPath.getParent());
                            Files.write(summaryPath, summary.getBytes("UTF-8"));
                            goalArtifactRepository.insert(connection, new GoalArtifact(0L, goal.goalKey(),
                                    "goal_summary", "GOAL_SUMMARY.md", summaryPath.toString(), "",
                                    "Goal summary exported", now));
                            goalArtifactRepository.insert(connection, new GoalArtifact(0L, goal.goalKey(),
                                    "checkpoint", "Completion checkpoint", "", "",
                                    "checkpoint_id=" + checkpointId, now));
                            goalRunRepository.complete(connection, goal.goalKey(), now, now);
                            goalEventRepository.insert(connection, new GoalEvent(0L, goal.goalKey(), "goal_completed",
                                    "info", "Goal completed", "checkpoint_id=" + checkpointId, now));
                            GoalRun completed = goalRunRepository.findByKey(connection, goal.goalKey());
                            return new GoalCompleteResult(completed, checkpointId, summaryPath);
                        }
                    });
            WorkflowRun workflowRun = workflowRunRepository.findByKey(connection, result.goal().workflowRunKey());
            SpecChange specChange = result.goal().specChangeKey().length() == 0
                    ? null : specChangeRepository.findByKey(connection, result.goal().specChangeKey());
            contextService.export(connection, projectRoot, project, result.goal(), workflowRun, specChange, now);
            return result;
        }
    }

    private Path exportGoalContext(Connection connection, Path projectRoot, Project project, GoalRun goal,
                                   WorkflowRun workflowRun, SpecChange specChange, String targetStatus,
                                   String now) throws Exception {
        goalRunRepository.updateStatus(connection, goal.goalKey(), "context_exporting", now);
        GoalRun exportGoal = withStatus(goalRunRepository.findByKey(connection, goal.goalKey()), targetStatus, now);
        try {
            Path contextPath = contextService.export(connection, projectRoot, project, exportGoal,
                    workflowRun, specChange, now);
            goalRunRepository.updateStatus(connection, goal.goalKey(), targetStatus, now);
            goalEventRepository.insert(connection, new GoalEvent(0L, goal.goalKey(), "goal_context_exported",
                    "info", "Goal context exported", targetStatus, now));
            return contextPath;
        } catch (Exception ex) {
            goalRunRepository.updateStatus(connection, goal.goalKey(), "context_export_failed", now);
            goalEventRepository.insert(connection, new GoalEvent(0L, goal.goalKey(), "goal_context_export_failed",
                    "error", "Goal context export failed", ex.getMessage(), now));
            throw ex;
        }
    }

    private String targetStatusAfterExport(GoalRun goal) {
        if (isContextExportIncomplete(goal.status())) {
            if (goal.stepCount() <= 0) {
                return "context_ready";
            }
            return planner.statusForAction(goal.currentAction());
        }
        return goal.status();
    }

    private boolean isContextExportIncomplete(String status) {
        return "context_export_failed".equals(status) || "context_exporting".equals(status);
    }

    private GoalRun withStatus(GoalRun goal, String status, String now) {
        return new GoalRun(goal.goalKey(), goal.projectKey(), goal.workflowRunKey(), goal.specChangeKey(),
                goal.profileKey(), goal.taskName(), goal.moduleName(), goal.mode(), goal.conditionText(),
                status, goal.currentAction(), goal.maxSteps(), goal.stepCount(), goal.createdAt(), now,
                goal.completedAt());
    }

    private GoalEvaluation evaluate(Connection connection, Path projectRoot, GoalRun goal) throws Exception {
        return completionEvaluator.evaluate(goal, goalCheckRepository.listByGoal(connection, goal.goalKey()),
                checkPolicyService.load(projectRoot), requireProfile(projectRoot, goal.profileKey()),
                goalStepRepository.listByGoal(connection, goal.goalKey()));
    }

    private List<GoalCheck> single(GoalCheck check) {
        List<GoalCheck> checks = new ArrayList<GoalCheck>();
        checks.add(check);
        return checks;
    }

    private String changedFiles(List<GoalStep> steps) {
        StringBuilder builder = new StringBuilder();
        for (GoalStep step : steps) {
            if (step.changedFiles().length() == 0) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(step.changedFiles());
        }
        return builder.length() == 0 ? "none" : builder.toString();
    }

    private void validateStepEvidence(GoalPlan plan, String summary, String changedFiles, String evidence) {
        List<String> missing = new ArrayList<String>();
        String evidenceText = evidence == null ? "" : evidence.trim();
        String lowerEvidence = evidenceText.toLowerCase(java.util.Locale.ROOT);
        String lowerChangedFiles = changedFiles == null ? "" : changedFiles.trim().toLowerCase(java.util.Locale.ROOT);
        for (String required : plan.requiredEvidence()) {
            String key = required == null ? "" : required.trim();
            if (key.length() == 0) {
                continue;
            }
            if ("summary".equals(key)) {
                if (summary == null || summary.trim().length() == 0) {
                    missing.add(key);
                }
                continue;
            }
            if ("evidence".equals(key)) {
                if (evidenceText.length() == 0) {
                    missing.add(key);
                }
                continue;
            }
            if ("changed_files".equals(key)) {
                if (lowerChangedFiles.length() == 0 && !containsEvidenceKey(lowerEvidence, key)) {
                    missing.add(key);
                }
                continue;
            }
            if ("implementation_summary".equals(key)) {
                if ((summary == null || summary.trim().length() == 0) && !containsEvidenceKey(lowerEvidence, key)) {
                    missing.add(key);
                }
                continue;
            }
            if (!containsEvidenceKey(lowerEvidence, key)) {
                missing.add(key);
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Goal step evidence missing required items: " + missing);
        }
    }

    private boolean containsEvidenceKey(String lowerEvidence, String key) {
        String normalized = key.toLowerCase(java.util.Locale.ROOT).replace('-', '_').replace(' ', '_');
        return lowerEvidence.contains(normalized);
    }

    private GoalProfile requireProfile(String profileKey) {
        GoalProfile profile = profileService.find(profileKey);
        if (profile == null) {
            throw new IllegalArgumentException("Unknown goal profile: " + profileKey);
        }
        return profile;
    }

    private GoalProfile requireProfile(Path projectRoot, String profileKey) {
        GoalProfile profile = profileService.find(projectRoot, profileKey);
        if (profile == null) {
            throw new IllegalArgumentException("Unknown goal profile: " + profileKey);
        }
        return profile;
    }

    private void rejectSensitive(String location, String... values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (value != null && value.length() > 0) {
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append(value);
            }
        }
        if (sensitiveDataGuard.containsSensitiveData(builder.toString())) {
            throw new IllegalArgumentException("Sensitive data rejected in " + location + ": "
                    + sensitiveDataGuard.findMatches(builder.toString()));
        }
    }

    private static final class GoalStartTransaction {
        private final GoalRun goal;
        private final WorkflowRun workflowRun;
        private final SpecChange specChange;

        private GoalStartTransaction(GoalRun goal, WorkflowRun workflowRun, SpecChange specChange) {
            this.goal = goal;
            this.workflowRun = workflowRun;
            this.specChange = specChange;
        }

        private GoalRun goal() { return goal; }
        private WorkflowRun workflowRun() { return workflowRun; }
        private SpecChange specChange() { return specChange; }
    }

    public static final class GoalStartResult {
        private final GoalRun goal;
        private final WorkflowRun workflowRun;
        private final SpecChange specChange;
        private final Path contextPath;

        private GoalStartResult(GoalRun goal, WorkflowRun workflowRun, SpecChange specChange, Path contextPath) {
            this.goal = goal;
            this.workflowRun = workflowRun;
            this.specChange = specChange;
            this.contextPath = contextPath;
        }

        public GoalRun goal() { return goal; }
        public WorkflowRun workflowRun() { return workflowRun; }
        public SpecChange specChange() { return specChange; }
        public Path contextPath() { return contextPath; }
    }

    public static final class GoalStepResult {
        private final long stepId;
        private final GoalRun goal;
        private final Path contextPath;

        private GoalStepResult(long stepId, GoalRun goal, Path contextPath) {
            this.stepId = stepId;
            this.goal = goal;
            this.contextPath = contextPath;
        }

        public long stepId() { return stepId; }
        public GoalRun goal() { return goal; }
        public Path contextPath() { return contextPath; }
    }

    public static final class GoalCompleteResult {
        private final GoalRun goal;
        private final long checkpointId;
        private final Path summaryPath;

        private GoalCompleteResult(GoalRun goal, long checkpointId, Path summaryPath) {
            this.goal = goal;
            this.checkpointId = checkpointId;
            this.summaryPath = summaryPath;
        }

        public GoalRun goal() { return goal; }
        public long checkpointId() { return checkpointId; }
        public Path summaryPath() { return summaryPath; }
    }

    public static final class GoalNotReadyException extends Exception {
        private final GoalEvaluation evaluation;

        private GoalNotReadyException(GoalEvaluation evaluation) {
            super("Goal is not ready to complete");
            this.evaluation = evaluation;
        }

        public GoalEvaluation evaluation() { return evaluation; }
    }
}
