package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.export.CurrentContextRenderer;
import com.devharnesskit.dhk.export.GoalContextRenderer;
import com.devharnesskit.dhk.export.SpecContextRenderer;
import com.devharnesskit.dhk.export.WorkflowContextRenderer;
import com.devharnesskit.dhk.model.Checkpoint;
import com.devharnesskit.dhk.model.checkpoint.HumanCheckpoint;
import com.devharnesskit.dhk.model.MemoryItem;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.goal.GoalEvaluation;
import com.devharnesskit.dhk.model.goal.GoalCheck;
import com.devharnesskit.dhk.model.goal.GoalPlan;
import com.devharnesskit.dhk.model.goal.GoalProfile;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.model.config.DevHarnessConfig;
import com.devharnesskit.dhk.model.spec.SpecChange;
import com.devharnesskit.dhk.model.workflow.WorkflowRun;
import com.devharnesskit.dhk.repository.CheckpointRepository;
import com.devharnesskit.dhk.repository.MemoryRepository;
import com.devharnesskit.dhk.repository.goal.GoalCheckRepository;
import com.devharnesskit.dhk.repository.goal.GoalStepRepository;
import com.devharnesskit.dhk.repository.spec.SpecAcceptanceRepository;
import com.devharnesskit.dhk.repository.spec.SpecDocumentRepository;
import com.devharnesskit.dhk.repository.spec.SpecTaskRepository;
import com.devharnesskit.dhk.repository.spec.WorkflowSpecBindingRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowGateRunRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowPhaseRunRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowPhaseTemplateRepository;
import com.devharnesskit.dhk.service.ExportSelectionService;
import com.devharnesskit.dhk.service.SensitiveDataGuard;
import com.devharnesskit.dhk.service.checkpoint.HumanCheckpointService;
import com.devharnesskit.dhk.model.policy.DevHarnessPolicy;
import com.devharnesskit.dhk.service.policy.DevHarnessPolicyService;
import com.devharnesskit.dhk.service.policy.PolicyHookService;
import com.devharnesskit.dhk.service.config.DevHarnessConfigService;
import com.devharnesskit.dhk.service.spec.SpecExportService;
import com.devharnesskit.dhk.service.workflow.WorkflowExportService;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;

public final class GoalContextService {
    private final ExportSelectionService exportSelectionService;
    private final CheckpointRepository checkpointRepository;
    private final WorkflowExportService workflowExportService;
    private final SpecExportService specExportService;
    private final CurrentContextRenderer currentContextRenderer;
    private final GoalContextRenderer goalContextRenderer;
    private final GoalProfileService profileService;
    private final GoalPlanner planner;
    private final GoalCheckPolicyService checkPolicyService;
    private final GoalCompletionEvaluator completionEvaluator;
    private final GoalCheckRepository goalCheckRepository;
    private final GoalStepRepository goalStepRepository;
    private final SensitiveDataGuard sensitiveDataGuard;
    private final PolicyHookService policyHookService;
    private final WorkspaceFingerprintService fingerprintService;
    private final GoalGraphStateService graphStateService;
    private final GoalBddStateService bddStateService;
    private final GoalIntegrityGateService integrityGateService;
    private final DevHarnessPolicyService devHarnessPolicyService;
    private final HumanCheckpointService humanCheckpointService;
    private final DevHarnessConfigService devHarnessConfigService = new DevHarnessConfigService();

    public GoalContextService() {
        this(new ExportSelectionService(new MemoryRepository()), new CheckpointRepository(),
                new WorkflowExportService(new WorkflowPhaseRunRepository(), new WorkflowGateRunRepository(),
                        new WorkflowPhaseTemplateRepository(), new WorkflowContextRenderer()),
                new SpecExportService(new SpecDocumentRepository(), new SpecTaskRepository(),
                        new SpecAcceptanceRepository(), new WorkflowSpecBindingRepository(),
                        new SpecContextRenderer()),
                new CurrentContextRenderer(), new GoalContextRenderer(), new GoalProfileService(),
                new GoalPlanner(), new GoalCheckPolicyService(), new GoalCompletionEvaluator(),
                new GoalCheckRepository(), new GoalStepRepository(), new SensitiveDataGuard(),
                new PolicyHookService(), new WorkspaceFingerprintService(), new GoalGraphStateService(),
                new GoalBddStateService(), new GoalIntegrityGateService(), new DevHarnessPolicyService(),
                new HumanCheckpointService());
    }

    GoalContextService(ExportSelectionService exportSelectionService,
                       CheckpointRepository checkpointRepository,
                       WorkflowExportService workflowExportService,
                       SpecExportService specExportService,
                       CurrentContextRenderer currentContextRenderer,
                       GoalContextRenderer goalContextRenderer,
                       GoalProfileService profileService,
                       GoalPlanner planner,
                       GoalCheckPolicyService checkPolicyService,
                       GoalCompletionEvaluator completionEvaluator,
                       GoalCheckRepository goalCheckRepository,
                       GoalStepRepository goalStepRepository,
                       SensitiveDataGuard sensitiveDataGuard,
                       PolicyHookService policyHookService,
                       WorkspaceFingerprintService fingerprintService,
                       GoalGraphStateService graphStateService,
                       GoalBddStateService bddStateService,
                       GoalIntegrityGateService integrityGateService,
                       DevHarnessPolicyService devHarnessPolicyService,
                       HumanCheckpointService humanCheckpointService) {
        this.exportSelectionService = exportSelectionService;
        this.checkpointRepository = checkpointRepository;
        this.workflowExportService = workflowExportService;
        this.specExportService = specExportService;
        this.currentContextRenderer = currentContextRenderer;
        this.goalContextRenderer = goalContextRenderer;
        this.profileService = profileService;
        this.planner = planner;
        this.checkPolicyService = checkPolicyService;
        this.completionEvaluator = completionEvaluator;
        this.goalCheckRepository = goalCheckRepository;
        this.goalStepRepository = goalStepRepository;
        this.sensitiveDataGuard = sensitiveDataGuard;
        this.policyHookService = policyHookService;
        this.fingerprintService = fingerprintService;
        this.graphStateService = graphStateService;
        this.bddStateService = bddStateService;
        this.integrityGateService = integrityGateService;
        this.devHarnessPolicyService = devHarnessPolicyService;
        this.humanCheckpointService = humanCheckpointService;
    }

    public Path export(Connection connection, Path projectRoot, Project project, GoalRun goal,
                       WorkflowRun workflowRun, SpecChange specChange, String generatedAt)
            throws Exception {
        Files.createDirectories(PathUtil.exportsDirectory(projectRoot));

        String workflowContext = workflowRun == null ? "" : workflowExportService.render(connection, workflowRun, generatedAt);
        workflowContext = write(projectRoot, PathUtil.workflowContext(projectRoot), workflowContext);

        String inlineWorkflow = workflowRun == null ? "" : workflowExportService.renderInline(connection, workflowRun);
        String specContext = "";
        String inlineSpec = "";
        if (specChange != null) {
            specContext = specExportService.renderFull(connection, specChange, generatedAt);
            specContext = write(projectRoot, PathUtil.specContext(projectRoot), specContext);
            inlineSpec = specExportService.renderInline(connection, specChange);
        }

        List<MemoryItem> memory = exportSelectionService.select(connection, project.projectKey(),
                goal.moduleName(), goal.mode(), goal.taskName(), goal.profileKey(), 30);
        Checkpoint checkpoint = checkpointRepository.latest(connection, project.projectKey(), goal.moduleName());
        String current = currentContextRenderer.render(project, goal.taskName(), goal.moduleName(),
                goal.mode(), goal.profileKey(), generatedAt, memory, checkpoint, inlineWorkflow, inlineSpec);
        write(projectRoot, PathUtil.currentContext(projectRoot), current);

        GoalProfile profile = profileService.find(projectRoot, goal.profileKey());
        GoalPlan plan = planner.plan(goal, profile);
        GoalCheckPolicy policy = checkPolicyService.load(projectRoot);
        DevHarnessConfig verificationConfig = devHarnessConfigService.load(projectRoot);
        DevHarnessPolicy devPolicy = devHarnessPolicyService.load(projectRoot);
        String[] requiredChecks = policy.requiredChecks(profile);
        List<GoalCheck> checks = goalCheckRepository.listByGoal(connection, goal.goalKey());
        List<com.devharnesskit.dhk.model.goal.GoalStep> steps = goalStepRepository.listByGoal(connection,
                goal.goalKey());
        GoalEvaluation evaluation = completionEvaluator.evaluate(goal,
                checks, policy, profile, steps,
                fingerprintService.workspaceFingerprint(projectRoot),
                fingerprintService.contextFingerprint(projectRoot));
        evaluation = integrityGateService.applyPreComplete(goal, evaluation, policy, profile, steps, checks);
        evaluation = humanCheckpointService.applyCompletionGate(connection, goal, evaluation, devPolicy);
        String goalContext = goalContextRenderer.render(goal, plan, requiredChecks,
                evaluation.missing(), evaluation.staleChecks(), freshnessStatus(evaluation), generatedAt,
                graphStateService.inspect(projectRoot, profile, plan),
                bddStateService.inspect(connection, projectRoot, project, goal, profile),
                disciplineGateStatus(requiredChecks, checks),
                humanCheckpointStatus(connection, goal, devPolicy),
                verificationConfig);
        return writePath(projectRoot, PathUtil.goalContext(projectRoot), goalContext);
    }

    private String[] humanCheckpointStatus(Connection connection, GoalRun goal, DevHarnessPolicy policy)
            throws Exception {
        if (policy == null || !policy.humanCheckpointRequired()) {
            return new String[0];
        }
        String type = policy.humanCheckpointType();
        List<HumanCheckpoint> checkpoints = humanCheckpointService.list(connection, goal.goalKey());
        boolean approved = false;
        long approvedId = 0L;
        for (HumanCheckpoint checkpoint : checkpoints) {
            if (type.equals(checkpoint.checkpointType()) && "approved".equals(checkpoint.status())) {
                approved = true;
                approvedId = checkpoint.id();
            }
        }
        java.util.List<String> result = new java.util.ArrayList<String>();
        result.add("human_checkpoint_required: true");
        result.add("type: " + type);
        result.add("status: " + (approved ? "approved" : "missing_approved"));
        if (approved) {
            result.add("approved_checkpoint_id: " + approvedId);
        } else {
            result.add("next_command: dhk checkpoint request --goal " + goal.goalKey()
                    + " --type " + type + " --reason \"<reason>\"");
        }
        return result.toArray(new String[result.size()]);
    }

    private String[] disciplineGateStatus(String[] requiredChecks, List<GoalCheck> checks) {
        java.util.List<String> result = new java.util.ArrayList<String>();
        for (String gate : new String[]{"think-before-coding", "goal-driven", "simplicity", "surgical-change"}) {
            if (!contains(requiredChecks, gate)) {
                continue;
            }
            GoalCheck check = find(checks, gate);
            result.add(gate + ": " + (check == null ? "pending" : check.status()));
        }
        return result.toArray(new String[result.size()]);
    }

    private boolean contains(String[] values, String target) {
        if (values == null) {
            return false;
        }
        for (String value : values) {
            if (target.equals(value)) {
                return true;
            }
        }
        return false;
    }

    private GoalCheck find(List<GoalCheck> checks, String key) {
        if (checks == null) {
            return null;
        }
        for (GoalCheck check : checks) {
            if (key.equals(check.checkKey())) {
                return check;
            }
        }
        return null;
    }

    private String write(Path projectRoot, Path path, String text) throws Exception {
        writePath(projectRoot, path, text);
        return sensitiveDataGuard.redact(text);
    }

    private Path writePath(Path projectRoot, Path path, String text) throws Exception {
        String output = sensitiveDataGuard.redact(text);
        List<String> matches = sensitiveDataGuard.findMatches(output);
        policyHookService.requireContextExportAllowed(projectRoot, path, output, matches);
        if (!matches.isEmpty()) {
            throw new IllegalStateException("Sensitive data rejected during goal context export: "
                    + matches);
        }
        Files.createDirectories(path.getParent());
        Files.write(path, output.getBytes("UTF-8"));
        return path;
    }

    private String freshnessStatus(GoalEvaluation evaluation) {
        return evaluation.staleChecks().length == 0 ? "fresh" : "stale";
    }
}
