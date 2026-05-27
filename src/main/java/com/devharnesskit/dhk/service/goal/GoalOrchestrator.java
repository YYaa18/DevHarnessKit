package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.db.TransactionTemplate;
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
import com.devharnesskit.dhk.repository.graph.GoalGraphBindingRepository;
import com.devharnesskit.dhk.repository.graph.GraphRepository;
import com.devharnesskit.dhk.repository.spec.SpecChangeRepository;
import com.devharnesskit.dhk.repository.spec.SpecDocumentRepository;
import com.devharnesskit.dhk.repository.spec.SpecEventRepository;
import com.devharnesskit.dhk.repository.spec.WorkflowSpecBindingRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowArtifactRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowCheckpointBindingRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowEventRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowGateRunRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowGateTemplateRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowPhaseRunRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowPhaseTemplateRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowRunRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowTemplateRepository;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.service.SensitiveDataGuard;
import com.devharnesskit.dhk.service.checkpoint.HumanCheckpointService;
import com.devharnesskit.dhk.service.policy.DevHarnessPolicyService;
import com.devharnesskit.dhk.service.policy.PolicyHookService;
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
    private final GraphRepository graphRepository = new GraphRepository();
    private final GoalGraphBindingRepository goalGraphBindingRepository = new GoalGraphBindingRepository();
    private final CheckpointRepository checkpointRepository = new CheckpointRepository();
    private final WorkflowArtifactRepository workflowArtifactRepository = new WorkflowArtifactRepository();
    private final WorkflowCheckpointBindingRepository workflowCheckpointBindingRepository =
            new WorkflowCheckpointBindingRepository();
    private final GoalProfileService profileService = new GoalProfileService();
    private final GoalPlanner planner = new GoalPlanner();
    private final GoalKeyGenerator keyGenerator = new GoalKeyGenerator();
    private final GoalContextService contextService = new GoalContextService();
    private final GoalContextExportCoordinator contextExportCoordinator =
            new GoalContextExportCoordinator(goalRunRepository, goalEventRepository, planner, contextService);
    private final GoalStepEvidenceValidator stepEvidenceValidator = new GoalStepEvidenceValidator();
    private final GoalCheckService checkService = new GoalCheckService();
    private final GoalCheckPolicyService checkPolicyService = new GoalCheckPolicyService();
    private final GoalCompletionEvaluator completionEvaluator = new GoalCompletionEvaluator();
    private final GoalIntegrityGateService integrityGateService = new GoalIntegrityGateService();
    private final GoalActionSyncService actionSyncService = new GoalActionSyncService();
    private final WorkspaceFingerprintService fingerprintService = new WorkspaceFingerprintService();
    private final SensitiveDataGuard sensitiveDataGuard = new SensitiveDataGuard();
    private final GoalCompletionArtifactService completionArtifactService =
            new GoalCompletionArtifactService(goalArtifactRepository, graphRepository,
                    goalGraphBindingRepository, checkpointRepository, workflowArtifactRepository,
                    workflowCheckpointBindingRepository, new com.devharnesskit.dhk.export.GoalSummaryRenderer(),
                    new com.devharnesskit.dhk.export.ArtifactPassportRenderer(), sensitiveDataGuard);
    private final PolicyHookService policyHookService = new PolicyHookService();
    private final DevHarnessPolicyService devHarnessPolicyService = new DevHarnessPolicyService();
    private final HumanCheckpointService humanCheckpointService = new HumanCheckpointService();
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
            Path contextPath = contextExportCoordinator.export(connection, projectRoot, project, result.goal(),
                    result.workflowRun(), result.specChange(), "context_ready", now);
            GoalRun exportedGoal = goalRunRepository.findByKey(connection, result.goal().goalKey());
            actionSyncService.syncAfterContextExport(connection, projectRoot, project, exportedGoal, profile, now);
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

    public GoalRun findOpenByIdentity(CommandContext context, Path projectRoot, String profileKey,
                                      String task, String module) throws Exception {
        if (!Files.isRegularFile(PathUtil.memoryDb(projectRoot)) || !Files.isRegularFile(PathUtil.projectJson(projectRoot))) {
            return null;
        }
        try (Connection connection = connectionFactory.open(projectRoot)) {
            Project project = projectService.readProject(PathUtil.projectJson(projectRoot));
            migrationRunner.migrate(connection, context.clock());
            return goalRunRepository.findOpenByIdentity(connection, project.projectKey(), profileKey, module, task);
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
            if (contextExportCoordinator.isIncomplete(goal.status())) {
                throw new IllegalStateException("Goal context export is incomplete; run dhk goal resume --goal "
                        + goal.goalKey());
            }
            GoalProfile profile = requireProfile(projectRoot, goal.profileKey());
            GoalPlan plan = planner.plan(goal, profile);
            stepEvidenceValidator.validate(plan, summary, changedFiles, evidence);
            String now = context.clock().now().toString();
            int nextIndex = goal.stepCount() + 1;
            String action = goal.currentAction();
            GoalStep recordedStep = new GoalStep(0L, goal.goalKey(), nextIndex,
                    action, summary, changedFiles, evidence, "accepted", now);
            long stepId = goalStepRepository.insert(connection, recordedStep);
            recordedStep = new GoalStep(stepId, recordedStep.goalKey(), recordedStep.stepIndex(),
                    recordedStep.actionKey(), recordedStep.summary(), recordedStep.changedFiles(),
                    recordedStep.evidence(), recordedStep.status(), recordedStep.createdAt());
            String nextAction = planner.nextAction(profile, action);
            String status = planner.statusForAction(nextAction);
            goalRunRepository.updateProgress(connection, goal.goalKey(), "context_exporting", nextAction, nextIndex, now);
            goalEventRepository.insert(connection, new GoalEvent(0L, goal.goalKey(), "goal_step_recorded",
                    "info", "Goal step recorded", action, now));
            GoalRun updated = goalRunRepository.findByKey(connection, goal.goalKey());
            Project project = projectService.readProject(PathUtil.projectJson(projectRoot));
            actionSyncService.syncAfterStep(connection, projectRoot, project, updated, profile, recordedStep, now);
            WorkflowRun workflowRun = workflowRunRepository.findByKey(connection, updated.workflowRunKey());
            SpecChange specChange = updated.specChangeKey().length() == 0
                    ? null : specChangeRepository.findByKey(connection, updated.specChangeKey());
            Path contextPath = contextExportCoordinator.export(connection, projectRoot, project, updated,
                    workflowRun, specChange, status, now);
            GoalRun readyGoal = goalRunRepository.findByKey(connection, goal.goalKey());
            return new GoalStepResult(stepId, readyGoal, contextPath);
        }
    }

    public GoalStepValidationResult validateStep(CommandContext context, Path projectRoot, String goalKey,
                                                 String summary, String changedFiles, String evidence)
            throws Exception {
        rejectSensitive("goal step", summary, changedFiles, evidence);
        try (Connection connection = connectionFactory.open(projectRoot)) {
            migrationRunner.migrate(connection, context.clock());
            GoalRun goal = goalRunRepository.findByKey(connection, goalKey);
            if (goal == null) {
                throw new IllegalStateException("Goal not found: " + goalKey);
            }
            if (contextExportCoordinator.isIncomplete(goal.status())) {
                throw new IllegalStateException("Goal context export is incomplete; run dhk goal resume --goal "
                        + goal.goalKey());
            }
            GoalProfile profile = requireProfile(projectRoot, goal.profileKey());
            GoalPlan plan = planner.plan(goal, profile);
            stepEvidenceValidator.validate(plan, summary, changedFiles, evidence);
            return new GoalStepValidationResult(goal, plan);
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
            return contextExportCoordinator.export(connection, projectRoot, project, goal, workflowRun, specChange,
                    contextExportCoordinator.targetStatusAfterExport(goal), now);
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
            GoalProfile profile = requireProfile(projectRoot, goal.profileKey());
            GoalCheckPolicy policy = checkPolicyService.load(projectRoot);
            Project project = projectService.readProject(PathUtil.projectJson(projectRoot));
            List<GoalCheck> checks;
            if (all) {
                checks = runAllChecks(connection, projectRoot, project, goal, profile, policy, now);
            } else {
                List<GoalCheck> before = new ArrayList<GoalCheck>();
                actionSyncService.syncBeforeCheck(connection, projectRoot, project, goal, profile, policy,
                        checkKey, before, now);
                checks = single(checkService.run(connection, projectRoot, goal, checkKey, now, policy));
            }
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

    public String[] requiredChecks(CommandContext context, Path projectRoot, String goalKey) throws Exception {
        try (Connection connection = connectionFactory.open(projectRoot)) {
            migrationRunner.migrate(connection, context.clock());
            GoalRun goal = goalRunRepository.findByKey(connection, goalKey);
            if (goal == null) {
                throw new IllegalStateException("Goal not found: " + goalKey);
            }
            GoalProfile profile = requireProfile(projectRoot, goal.profileKey());
            return checkPolicyService.load(projectRoot).requiredChecks(profile);
        }
    }

    public GoalEvaluation evaluate(CommandContext context, Path projectRoot, String goalKey) throws Exception {
        try (Connection connection = connectionFactory.open(projectRoot)) {
            migrationRunner.migrate(connection, context.clock());
            GoalRun goal = goalRunRepository.findByKey(connection, goalKey);
            if (goal == null) {
                throw new IllegalStateException("Goal not found: " + goalKey);
            }
            return evaluate(connection, projectRoot, goal, context.clock().now().toString());
        }
    }

    public GoalAuditResult audit(CommandContext context, Path projectRoot, String goalKey) throws Exception {
        try (Connection connection = connectionFactory.open(projectRoot)) {
            migrationRunner.migrate(connection, context.clock());
            GoalRun goal = goalRunRepository.findByKey(connection, goalKey);
            if (goal == null) {
                throw new IllegalStateException("Goal not found: " + goalKey);
            }
            List<GoalStep> steps = goalStepRepository.listByGoal(connection, goal.goalKey());
            List<GoalCheck> checks = goalCheckRepository.listByGoal(connection, goal.goalKey());
            List<GoalArtifact> artifacts = goalArtifactRepository.listByGoal(connection, goal.goalKey());
            GoalProfile profile = requireProfile(projectRoot, goal.profileKey());
            GoalCheckPolicy policy = checkPolicyService.load(projectRoot);
            GoalEvaluation evaluation = completionEvaluator.evaluate(goal, checks, policy, profile, steps,
                    fingerprintService.workspaceFingerprint(projectRoot),
                    fingerprintService.contextFingerprint(projectRoot));
            GoalEvaluation integrityEvaluation = integrityGateService.applyPreComplete(goal, evaluation,
                    policy, profile, steps, checks);
            GoalEvaluation finalEvaluation = humanCheckpointService.applyCompletionGate(connection, goal,
                    integrityEvaluation, devHarnessPolicyService.load(projectRoot));
            return new GoalAuditResult(goal, steps, checks, artifacts, finalEvaluation,
                    Files.isRegularFile(PathUtil.artifactPassport(projectRoot)));
        }
    }

    public GoalCompleteResult complete(CommandContext context, Path projectRoot, String goalKey) throws Exception {
        try (Connection connection = connectionFactory.open(projectRoot)) {
            migrationRunner.migrate(connection, context.clock());
            GoalRun goal = goalRunRepository.findByKey(connection, goalKey);
            if (goal == null) {
                throw new IllegalStateException("Goal not found: " + goalKey);
            }
            final String now = context.clock().now().toString();
            GoalEvaluation evaluation = evaluate(connection, projectRoot, goal, now);
            if (!evaluation.readyToComplete()) {
                throw new GoalNotReadyException(evaluation);
            }
            final List<GoalStep> steps = goalStepRepository.listByGoal(connection, goal.goalKey());
            final List<GoalCheck> checks = goalCheckRepository.listByGoal(connection, goal.goalKey());
            final Project project = projectService.readProject(PathUtil.projectJson(projectRoot));
            final GoalProfile profile = requireProfile(projectRoot, goal.profileKey());
            final GoalCheckPolicy policy = checkPolicyService.load(projectRoot);
            GoalEvaluation preIntegrity = integrityGateService.applyPreComplete(goal, evaluation,
                    policy, profile, steps, checks);
            if (!preIntegrity.readyToComplete()) {
                throw new GoalNotReadyException(preIntegrity);
            }
            policyHookService.requireGoalCompleteAllowed(projectRoot, steps);
            GoalCompleteResult result = transactionTemplate.execute(connection,
                    new TransactionTemplate.Work<GoalCompleteResult>() {
                        public GoalCompleteResult execute() throws Exception {
                            GoalCompletionArtifacts artifacts = completionArtifactService.create(connection,
                                    projectRoot, project, goal, profile, steps, checks,
                                    stepEvidenceValidator.changedFiles(steps), now);
                            Path passportPath = PathUtil.artifactPassport(projectRoot);
                            GoalEvaluation finalIntegrity = integrityGateService.finalComplete(goal,
                                    artifacts.summaryPath(), passportPath,
                                    goalArtifactRepository.listByGoal(connection, goal.goalKey()));
                            if (!finalIntegrity.readyToComplete()) {
                                throw new GoalNotReadyException(finalIntegrity);
                            }
                            actionSyncService.syncOnComplete(connection, project, goal, profile, policy,
                                    checks, artifacts.checkpointId(), now);
                            completionArtifactService.bindWorkflowCompletion(connection, project, goal,
                                    artifacts.checkpointId(), artifacts.summaryPath(), now);
                            goalRunRepository.complete(connection, goal.goalKey(), now, now);
                            goalEventRepository.insert(connection, new GoalEvent(0L, goal.goalKey(), "goal_completed",
                                    "info", "Goal completed", "checkpoint_id=" + artifacts.checkpointId(), now));
                            GoalRun completed = goalRunRepository.findByKey(connection, goal.goalKey());
                            return new GoalCompleteResult(completed, artifacts.checkpointId(), artifacts.summaryPath());
                        }
                    });
            WorkflowRun workflowRun = workflowRunRepository.findByKey(connection, result.goal().workflowRunKey());
            SpecChange specChange = result.goal().specChangeKey().length() == 0
                    ? null : specChangeRepository.findByKey(connection, result.goal().specChangeKey());
            contextService.export(connection, projectRoot, project, result.goal(), workflowRun, specChange, now);
            return result;
        }
    }

    private GoalEvaluation evaluate(Connection connection, Path projectRoot, GoalRun goal,
                                    String now) throws Exception {
        Project project = projectService.readProject(PathUtil.projectJson(projectRoot));
        GoalProfile profile = requireProfile(projectRoot, goal.profileKey());
        GoalCheckPolicy policy = checkPolicyService.load(projectRoot);
        actionSyncService.syncBeforeEvaluate(connection, projectRoot, project, goal, profile, policy, now);
        List<GoalCheck> checks = goalCheckRepository.listByGoal(connection, goal.goalKey());
        List<GoalStep> steps = goalStepRepository.listByGoal(connection, goal.goalKey());
        GoalEvaluation evaluation = completionEvaluator.evaluate(goal, checks,
                policy, profile, steps,
                fingerprintService.workspaceFingerprint(projectRoot),
                fingerprintService.contextFingerprint(projectRoot));
        GoalEvaluation integrityEvaluation = integrityGateService.applyPreComplete(goal, evaluation,
                policy, profile, steps, checks);
        return humanCheckpointService.applyCompletionGate(connection, goal, integrityEvaluation,
                devHarnessPolicyService.load(projectRoot));
    }

    private List<GoalCheck> runAllChecks(Connection connection, Path projectRoot, Project project,
                                         GoalRun goal, GoalProfile profile, GoalCheckPolicy policy,
                                         String now) throws Exception {
        List<GoalCheck> checks = new ArrayList<GoalCheck>();
        for (String key : policy.requiredChecks(profile)) {
            actionSyncService.syncBeforeCheck(connection, projectRoot, project, goal, profile, policy,
                    key, checks, now);
            checks.add(checkService.run(connection, projectRoot, goal, key, now, policy));
        }
        return checks;
    }

    private List<GoalCheck> single(GoalCheck check) {
        List<GoalCheck> checks = new ArrayList<GoalCheck>();
        checks.add(check);
        return checks;
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

    public static final class GoalStepValidationResult {
        private final GoalRun goal;
        private final GoalPlan plan;

        private GoalStepValidationResult(GoalRun goal, GoalPlan plan) {
            this.goal = goal;
            this.plan = plan;
        }

        public GoalRun goal() { return goal; }
        public GoalPlan plan() { return plan; }
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

    public static final class GoalAuditResult {
        private final GoalRun goal;
        private final List<GoalStep> steps;
        private final List<GoalCheck> checks;
        private final List<GoalArtifact> artifacts;
        private final GoalEvaluation evaluation;
        private final boolean artifactPassportPresent;

        private GoalAuditResult(GoalRun goal, List<GoalStep> steps, List<GoalCheck> checks,
                                List<GoalArtifact> artifacts, GoalEvaluation evaluation,
                                boolean artifactPassportPresent) {
            this.goal = goal;
            this.steps = steps;
            this.checks = checks;
            this.artifacts = artifacts;
            this.evaluation = evaluation;
            this.artifactPassportPresent = artifactPassportPresent;
        }

        public GoalRun goal() { return goal; }
        public List<GoalStep> steps() { return steps; }
        public List<GoalCheck> checks() { return checks; }
        public List<GoalArtifact> artifacts() { return artifacts; }
        public GoalEvaluation evaluation() { return evaluation; }
        public boolean artifactPassportPresent() { return artifactPassportPresent; }
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
