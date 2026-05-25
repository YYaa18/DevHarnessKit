package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.export.CurrentContextRenderer;
import com.devharnesskit.dhk.export.GoalContextRenderer;
import com.devharnesskit.dhk.export.SpecContextRenderer;
import com.devharnesskit.dhk.export.WorkflowContextRenderer;
import com.devharnesskit.dhk.model.Checkpoint;
import com.devharnesskit.dhk.model.MemoryItem;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.goal.GoalPlan;
import com.devharnesskit.dhk.model.goal.GoalProfile;
import com.devharnesskit.dhk.model.goal.GoalRun;
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

    public GoalContextService() {
        this(new ExportSelectionService(new MemoryRepository()), new CheckpointRepository(),
                new WorkflowExportService(new WorkflowPhaseRunRepository(), new WorkflowGateRunRepository(),
                        new WorkflowPhaseTemplateRepository(), new WorkflowContextRenderer()),
                new SpecExportService(new SpecDocumentRepository(), new SpecTaskRepository(),
                        new SpecAcceptanceRepository(), new WorkflowSpecBindingRepository(),
                        new SpecContextRenderer()),
                new CurrentContextRenderer(), new GoalContextRenderer(), new GoalProfileService(),
                new GoalPlanner(), new GoalCheckPolicyService(), new GoalCompletionEvaluator(),
                new GoalCheckRepository(), new GoalStepRepository(), new SensitiveDataGuard());
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
                       SensitiveDataGuard sensitiveDataGuard) {
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
    }

    public Path export(Connection connection, Path projectRoot, Project project, GoalRun goal,
                       WorkflowRun workflowRun, SpecChange specChange, String generatedAt)
            throws Exception {
        Files.createDirectories(PathUtil.exportsDirectory(projectRoot));

        String workflowContext = workflowRun == null ? "" : workflowExportService.render(connection, workflowRun, generatedAt);
        workflowContext = write(PathUtil.workflowContext(projectRoot), workflowContext);

        String inlineWorkflow = workflowRun == null ? "" : workflowExportService.renderInline(connection, workflowRun);
        String specContext = "";
        String inlineSpec = "";
        if (specChange != null) {
            specContext = specExportService.renderFull(connection, specChange, generatedAt);
            specContext = write(PathUtil.specContext(projectRoot), specContext);
            inlineSpec = specExportService.renderInline(connection, specChange);
        }

        List<MemoryItem> memory = exportSelectionService.select(connection, project.projectKey(),
                goal.moduleName(), goal.mode(), goal.taskName(), goal.profileKey(), 30);
        Checkpoint checkpoint = checkpointRepository.latest(connection, project.projectKey(), goal.moduleName());
        String current = currentContextRenderer.render(project, goal.taskName(), goal.moduleName(),
                goal.mode(), goal.profileKey(), generatedAt, memory, checkpoint, inlineWorkflow, inlineSpec);
        write(PathUtil.currentContext(projectRoot), current);

        GoalProfile profile = profileService.find(projectRoot, goal.profileKey());
        GoalPlan plan = planner.plan(goal, profile);
        GoalCheckPolicy policy = checkPolicyService.load(projectRoot);
        String[] completionBlockers = completionEvaluator.evaluate(goal,
                goalCheckRepository.listByGoal(connection, goal.goalKey()), policy, profile,
                goalStepRepository.listByGoal(connection, goal.goalKey())).missing();
        String goalContext = goalContextRenderer.render(goal, plan, policy.requiredChecks(),
                completionBlockers, generatedAt);
        return writePath(PathUtil.goalContext(projectRoot), goalContext);
    }

    private String write(Path path, String text) throws Exception {
        writePath(path, text);
        return sensitiveDataGuard.redact(text);
    }

    private Path writePath(Path path, String text) throws Exception {
        String output = sensitiveDataGuard.redact(text);
        if (sensitiveDataGuard.containsSensitiveData(output)) {
            throw new IllegalStateException("Sensitive data rejected during goal context export: "
                    + sensitiveDataGuard.findMatches(output));
        }
        Files.createDirectories(path.getParent());
        Files.write(path, output.getBytes("UTF-8"));
        return path;
    }
}
