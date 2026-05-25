package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.goal.GoalActionMapping;
import com.devharnesskit.dhk.model.goal.GoalCheck;
import com.devharnesskit.dhk.model.goal.GoalProfile;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.model.goal.GoalStep;
import com.devharnesskit.dhk.model.spec.SpecAcceptance;
import com.devharnesskit.dhk.model.spec.SpecChange;
import com.devharnesskit.dhk.model.spec.SpecEvent;
import com.devharnesskit.dhk.model.spec.SpecTask;
import com.devharnesskit.dhk.model.workflow.WorkflowEvent;
import com.devharnesskit.dhk.model.workflow.WorkflowGateRun;
import com.devharnesskit.dhk.model.workflow.WorkflowPhaseRun;
import com.devharnesskit.dhk.model.workflow.WorkflowRun;
import com.devharnesskit.dhk.repository.goal.GoalCheckRepository;
import com.devharnesskit.dhk.repository.goal.GoalStepRepository;
import com.devharnesskit.dhk.repository.spec.SpecAcceptanceRepository;
import com.devharnesskit.dhk.repository.spec.SpecChangeRepository;
import com.devharnesskit.dhk.repository.spec.SpecEventRepository;
import com.devharnesskit.dhk.repository.spec.SpecTaskRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowEventRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowGateRunRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowPhaseRunRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowRunRepository;
import com.devharnesskit.dhk.service.spec.SpecAcceptanceService;
import com.devharnesskit.dhk.service.spec.SpecTaskService;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GoalActionSyncService {
    private static final String GOAL_ACCEPTANCE = "goal_checks_pass";

    private final GoalStepRepository stepRepository;
    private final GoalCheckRepository checkRepository;
    private final WorkflowRunRepository workflowRunRepository;
    private final WorkflowPhaseRunRepository phaseRunRepository;
    private final WorkflowGateRunRepository gateRunRepository;
    private final WorkflowEventRepository workflowEventRepository;
    private final SpecChangeRepository specChangeRepository;
    private final SpecTaskRepository taskRepository;
    private final SpecAcceptanceRepository acceptanceRepository;
    private final SpecEventRepository specEventRepository;
    private final SpecTaskService taskService;
    private final SpecAcceptanceService acceptanceService;

    public GoalActionSyncService() {
        this(new GoalStepRepository(), new GoalCheckRepository(), new WorkflowRunRepository(),
                new WorkflowPhaseRunRepository(), new WorkflowGateRunRepository(), new WorkflowEventRepository(),
                new SpecChangeRepository(), new SpecTaskRepository(), new SpecAcceptanceRepository(),
                new SpecEventRepository());
    }

    GoalActionSyncService(GoalStepRepository stepRepository, GoalCheckRepository checkRepository,
                          WorkflowRunRepository workflowRunRepository,
                          WorkflowPhaseRunRepository phaseRunRepository,
                          WorkflowGateRunRepository gateRunRepository,
                          WorkflowEventRepository workflowEventRepository,
                          SpecChangeRepository specChangeRepository,
                          SpecTaskRepository taskRepository,
                          SpecAcceptanceRepository acceptanceRepository,
                          SpecEventRepository specEventRepository) {
        this.stepRepository = stepRepository;
        this.checkRepository = checkRepository;
        this.workflowRunRepository = workflowRunRepository;
        this.phaseRunRepository = phaseRunRepository;
        this.gateRunRepository = gateRunRepository;
        this.workflowEventRepository = workflowEventRepository;
        this.specChangeRepository = specChangeRepository;
        this.taskRepository = taskRepository;
        this.acceptanceRepository = acceptanceRepository;
        this.specEventRepository = specEventRepository;
        this.taskService = new SpecTaskService(taskRepository, specEventRepository);
        this.acceptanceService = new SpecAcceptanceService(acceptanceRepository, specEventRepository);
    }

    public void syncAfterContextExport(Connection connection, Path projectRoot, Project project,
                                       GoalRun goal, GoalProfile profile, String now) throws Exception {
        if (goal.workflowRunKey().length() == 0) {
            return;
        }
        passContextExport(connection, projectRoot, project, goal, now);
        ensureSpecScaffold(connection, project, goal, profile, now);
        refreshWorkflowProgress(connection, goal.workflowRunKey(), now);
    }

    public void syncAfterStep(Connection connection, Path projectRoot, Project project, GoalRun goal,
                              GoalProfile profile, GoalStep step, String now) throws Exception {
        syncRecordedSteps(connection, projectRoot, project, goal, profile, now);
        syncSpecTaskForStep(connection, project, goal, profile, step, now);
    }

    public void syncBeforeCheck(Connection connection, Path projectRoot, Project project,
                                GoalRun goal, GoalProfile profile, GoalCheckPolicy policy,
                                String checkKey, List<GoalCheck> currentRunChecks,
                                String now) throws Exception {
        syncRecordedSteps(connection, projectRoot, project, goal, profile, now);
        if ("workflow".equals(checkKey)) {
            syncWorkflowVerification(connection, project, goal, profile, policy, currentRunChecks, now);
        }
        if ("spec".equals(checkKey)) {
            syncSpecAcceptance(connection, project, goal, profile, policy, currentRunChecks, now);
        }
    }

    public void syncBeforeEvaluate(Connection connection, Path projectRoot, Project project,
                                   GoalRun goal, GoalProfile profile, GoalCheckPolicy policy,
                                   String now) throws Exception {
        List<GoalCheck> checks = checkRepository.listByGoal(connection, goal.goalKey());
        syncRecordedSteps(connection, projectRoot, project, goal, profile, now);
        syncWorkflowVerification(connection, project, goal, profile, policy, checks, now);
        syncSpecAcceptance(connection, project, goal, profile, policy, checks, now);
    }

    public void syncOnComplete(Connection connection, Project project, GoalRun goal, GoalProfile profile,
                               GoalCheckPolicy policy, List<GoalCheck> checks, long checkpointId,
                               String now) throws Exception {
        passCheckpoint(connection, project, goal, checkpointId, now);
        passOptionalFinalWorkflowPhases(connection, project, goal, now);
        refreshWorkflowProgress(connection, goal.workflowRunKey(), now);
        verifySpecChange(connection, goal, profile, now);
    }

    private void syncRecordedSteps(Connection connection, Path projectRoot, Project project,
                                   GoalRun goal, GoalProfile profile, String now) throws Exception {
        if (goal.workflowRunKey().length() > 0) {
            passContextExport(connection, projectRoot, project, goal, now);
        }
        ensureSpecScaffold(connection, project, goal, profile, now);
        List<GoalStep> steps = stepRepository.listByGoal(connection, goal.goalKey());
        for (GoalStep step : steps) {
            syncWorkflowForStep(connection, project, goal, profile, step, now);
            syncSpecTaskForStep(connection, project, goal, profile, step, now);
        }
        if (hasImplementationStep(steps)) {
            passUserApproval(connection, project, goal, now);
        }
        refreshWorkflowProgress(connection, goal.workflowRunKey(), now);
    }

    private void syncWorkflowForStep(Connection connection, Project project, GoalRun goal,
                                     GoalProfile profile, GoalStep step, String now) throws Exception {
        if (goal.workflowRunKey().length() == 0 || profile == null) {
            return;
        }
        GoalActionMapping mapping = profile.actionMapping(step.actionKey());
        if (mapping == null) {
            return;
        }
        for (String gateKey : mapping.requiredGates()) {
            passGate(connection, project, goal, gateKey, "Passed by goal action evidence",
                    "goal_step=" + step.id() + " action=" + step.actionKey(), now);
        }
        if (mapping.workflowPhase().length() > 0 && !"verify_tests".equals(mapping.workflowPhase())
                && !"verify_compile".equals(mapping.workflowPhase())) {
            passPhase(connection, project, goal, mapping.workflowPhase(), "Completed by goal action",
                    "goal_step=" + step.id() + " action=" + step.actionKey(), now);
        }
    }

    private void syncSpecTaskForStep(Connection connection, Project project, GoalRun goal,
                                     GoalProfile profile, GoalStep step, String now) throws Exception {
        SpecChange change = specChange(connection, goal);
        if (change == null || profile == null) {
            return;
        }
        GoalActionMapping mapping = profile.actionMapping(step.actionKey());
        if (mapping == null || mapping.specTask().length() == 0) {
            return;
        }
        SpecTask task = taskRepository.findByKey(connection, change.changeKey(), mapping.specTask());
        if (task == null || "done".equals(task.status()) || "skipped".equals(task.status())) {
            return;
        }
        taskService.updateTask(connection, change, task, "done",
                "Completed by goal step " + step.stepIndex() + " (" + step.actionKey() + ")", now);
    }

    private void syncWorkflowVerification(Connection connection, Project project, GoalRun goal,
                                          GoalProfile profile, GoalCheckPolicy policy,
                                          List<GoalCheck> currentRunChecks, String now) throws Exception {
        if (goal.workflowRunKey().length() == 0) {
            return;
        }
        Map<String, GoalCheck> checks = checksByKey(connection, goal.goalKey(), currentRunChecks);
        GoalCheck compile = checks.get("compile");
        if (isFreshAccepted(compile, goal, policy, profile)) {
            passPhase(connection, project, goal, "verify_compile", "Compile check passed",
                    checkEvidence(compile), now);
        }
        GoalCheck test = checks.get("test");
        if (isFreshAccepted(test, goal, policy, profile)) {
            passGate(connection, project, goal, "tests_recorded", "Test check accepted",
                    checkEvidence(test), now);
            passPhase(connection, project, goal, "verify_tests", "Test check accepted",
                    checkEvidence(test), now);
        }
        refreshWorkflowProgress(connection, goal.workflowRunKey(), now);
    }

    private void syncSpecAcceptance(Connection connection, Project project, GoalRun goal,
                                    GoalProfile profile, GoalCheckPolicy policy,
                                    List<GoalCheck> currentRunChecks, String now) throws Exception {
        SpecChange change = specChange(connection, goal);
        if (change == null || profile == null || !hasAutoAcceptance(profile)) {
            return;
        }
        Map<String, GoalCheck> checks = checksByKey(connection, goal.goalKey(), currentRunChecks);
        String[] required = policy.requiredChecks(profile);
        List<String> missing = new ArrayList<String>();
        for (String checkKey : required) {
            if ("spec".equals(checkKey)) {
                continue;
            }
            GoalCheck check = checks.get(checkKey);
            if (!isFreshAccepted(check, goal, policy, profile)) {
                missing.add(checkKey);
            }
        }
        if (!missing.isEmpty()) {
            return;
        }
        SpecAcceptance acceptance = acceptanceRepository.findByKey(connection, change.changeKey(), GOAL_ACCEPTANCE);
        if (acceptance == null || "passed".equals(acceptance.status()) || "waived".equals(acceptance.status())) {
            return;
        }
        acceptanceService.updateAcceptance(connection, change, acceptance, "passed",
                "All non-spec goal checks accepted at step " + goal.stepCount(), now);
    }

    private void ensureSpecScaffold(Connection connection, Project project, GoalRun goal,
                                    GoalProfile profile, String now) throws Exception {
        SpecChange change = specChange(connection, goal);
        if (change == null || profile == null || !profile.specRequired() || !hasManagedSpec(profile)) {
            return;
        }
        int index = 1;
        for (String action : profile.actions()) {
            GoalActionMapping mapping = profile.actionMapping(action);
            if (mapping == null || mapping.specTask().length() == 0) {
                continue;
            }
            if (taskRepository.findByKey(connection, change.changeKey(), mapping.specTask()) == null) {
                taskService.addTask(connection, change, mapping.specTask(),
                        titleForAction(action), "Automatically tracked by goal action mapping.",
                        mapping.workflowPhase(), now);
            }
            index++;
        }
        if (hasAutoAcceptance(profile)
                && acceptanceRepository.findByKey(connection, change.changeKey(), GOAL_ACCEPTANCE) == null) {
            acceptanceService.addAcceptance(connection, change, GOAL_ACCEPTANCE,
                    "Required goal checks are accepted",
                    "compile/test/sensitive/workflow checks are accepted by policy", now);
        }
    }

    private void passContextExport(Connection connection, Path projectRoot, Project project,
                                   GoalRun goal, String now) throws Exception {
        if (goal.workflowRunKey().length() == 0) {
            return;
        }
        if (Files.isRegularFile(PathUtil.currentContext(projectRoot))
                || Files.isRegularFile(PathUtil.goalContext(projectRoot))) {
            passGate(connection, project, goal, "current_context_exists",
                    "Context export exists", "goal_context_export", now);
        }
        passGate(connection, project, goal, "confirmed_memory_only",
                "Goal context uses exported confirmed memory boundary", "goal_context_export", now);
        passPhase(connection, project, goal, "export_context", "Context exported",
                "goal_context_export", now);
    }

    private void passUserApproval(Connection connection, Project project, GoalRun goal, String now) throws Exception {
        passGate(connection, project, goal, "user_approval_before_implementation",
                "Goal implementation step recorded under ordered protocol",
                "goal_step=implement_minimal_change", now);
        passPhase(connection, project, goal, "user_approval",
                "Approval/rationale satisfied by ordered goal protocol",
                "goal_step=implement_minimal_change", now);
    }

    private void passCheckpoint(Connection connection, Project project, GoalRun goal,
                                long checkpointId, String now) throws Exception {
        if (goal.workflowRunKey().length() == 0) {
            return;
        }
        passGate(connection, project, goal, "checkpoint_created", "Completion checkpoint created",
                "checkpoint_id=" + checkpointId, now);
        passPhase(connection, project, goal, "create_checkpoint", "Completion checkpoint created",
                "checkpoint_id=" + checkpointId, now);
    }

    private void passOptionalFinalWorkflowPhases(Connection connection, Project project,
                                                 GoalRun goal, String now) throws Exception {
        if (goal.workflowRunKey().length() == 0) {
            return;
        }
        passPhase(connection, project, goal, "suggest_memory_updates",
                "No automatic memory confirmation; suggestions remain optional",
                "goal_complete", now);
    }

    private boolean passGate(Connection connection, Project project, GoalRun goal, String gateKey,
                             String summary, String evidence, String now) throws Exception {
        if (gateKey == null || gateKey.length() == 0 || goal.workflowRunKey().length() == 0) {
            return false;
        }
        List<WorkflowGateRun> gates = gateRunRepository.listByRunGate(connection, goal.workflowRunKey(), gateKey);
        boolean changed = false;
        for (WorkflowGateRun gate : gates) {
            if ("passed".equals(gate.status()) || "waived".equals(gate.status()) || "failed".equals(gate.status())) {
                continue;
            }
            gateRunRepository.updateStatus(connection, gate.id(), "passed", summary, "", evidence, now, now);
            workflowEventRepository.insert(connection, new WorkflowEvent(0L, project.projectKey(),
                    goal.workflowRunKey(), "gate_checked", gate.phaseKey(), gate.gateKey(), "info",
                    "Gate passed by goal sync: " + gate.gateKey(), summary, now));
            changed = true;
        }
        return changed;
    }

    private boolean passPhase(Connection connection, Project project, GoalRun goal, String phaseKey,
                              String summary, String evidence, String now) throws Exception {
        if (phaseKey == null || phaseKey.length() == 0 || goal.workflowRunKey().length() == 0) {
            return false;
        }
        WorkflowPhaseRun phase = phaseRunRepository.find(connection, goal.workflowRunKey(), phaseKey);
        if (phase == null || "passed".equals(phase.status()) || "failed".equals(phase.status())
                || "blocked".equals(phase.status())) {
            return false;
        }
        phaseRunRepository.updateStatus(connection, goal.workflowRunKey(), phaseKey, "passed",
                summary, evidence, now, now);
        workflowEventRepository.insert(connection, new WorkflowEvent(0L, project.projectKey(),
                goal.workflowRunKey(), "phase_completed", phaseKey, "", "info",
                "Phase passed by goal sync: " + phaseKey, summary, now));
        return true;
    }

    private void refreshWorkflowProgress(Connection connection, String workflowRunKey, String now) throws Exception {
        if (workflowRunKey == null || workflowRunKey.length() == 0) {
            return;
        }
        WorkflowRun run = workflowRunRepository.findByKey(connection, workflowRunKey);
        if (run == null || "failed".equals(run.status()) || "abandoned".equals(run.status())
                || "blocked".equals(run.status())) {
            return;
        }
        List<WorkflowPhaseRun> phases = phaseRunRepository.listByRun(connection, workflowRunKey);
        for (WorkflowPhaseRun phase : phases) {
            if (!"passed".equals(phase.status())) {
                workflowRunRepository.updateCurrentPhase(connection, workflowRunKey, "running",
                        phase.phaseKey(), now);
                return;
            }
        }
        workflowRunRepository.updateStatus(connection, workflowRunKey, "completed", now, now);
    }

    private void verifySpecChange(Connection connection, GoalRun goal, GoalProfile profile,
                                  String now) throws Exception {
        SpecChange change = specChange(connection, goal);
        if (change == null || profile == null || !profile.specRequired()) {
            return;
        }
        List<SpecTask> tasks = taskRepository.listByChange(connection, change.changeKey());
        List<SpecAcceptance> acceptances = acceptanceRepository.listByChange(connection, change.changeKey());
        if (tasks.isEmpty() || acceptances.isEmpty()) {
            return;
        }
        for (SpecTask task : tasks) {
            if (!"done".equals(task.status()) && !"skipped".equals(task.status())) {
                return;
            }
        }
        for (SpecAcceptance acceptance : acceptances) {
            if (!"passed".equals(acceptance.status()) && !"waived".equals(acceptance.status())) {
                return;
            }
        }
        specChangeRepository.updateStatus(connection, change.changeKey(), "verified", now);
        specEventRepository.insert(connection, new SpecEvent(0L, change.projectKey(), change.changeKey(),
                "change_verified", "info", "Spec change verified by goal complete",
                goal.goalKey(), now));
    }

    private Map<String, GoalCheck> checksByKey(Connection connection, String goalKey,
                                               List<GoalCheck> currentRunChecks) throws Exception {
        Map<String, GoalCheck> result = new LinkedHashMap<String, GoalCheck>();
        for (GoalCheck check : checkRepository.listByGoal(connection, goalKey)) {
            result.put(check.checkKey(), check);
        }
        if (currentRunChecks != null) {
            for (GoalCheck check : currentRunChecks) {
                result.put(check.checkKey(), check);
            }
        }
        return result;
    }

    private boolean isFreshAccepted(GoalCheck check, GoalRun goal, GoalCheckPolicy policy, GoalProfile profile) {
        return check != null
                && check.stepCountAtCheck() >= goal.stepCount()
                && policy.accepts(check.checkKey(), check.status(), profile);
    }

    private boolean hasImplementationStep(List<GoalStep> steps) {
        for (GoalStep step : steps) {
            if (step.actionKey().indexOf("implement") >= 0 || step.actionKey().indexOf("apply") >= 0) {
                return true;
            }
        }
        return false;
    }

    private boolean hasManagedSpec(GoalProfile profile) {
        if (profile == null) {
            return false;
        }
        for (GoalActionMapping mapping : profile.actionMappings().values()) {
            if (mapping.specTask().length() > 0 || mapping.specAcceptanceUpdate().length() > 0) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAutoAcceptance(GoalProfile profile) {
        if (profile == null) {
            return false;
        }
        for (GoalActionMapping mapping : profile.actionMappings().values()) {
            if ("auto_pass".equals(mapping.specAcceptanceUpdate())) {
                return true;
            }
        }
        return false;
    }

    private SpecChange specChange(Connection connection, GoalRun goal) throws Exception {
        if (goal.specChangeKey().length() == 0) {
            return null;
        }
        return specChangeRepository.findByKey(connection, goal.specChangeKey());
    }

    private String checkEvidence(GoalCheck check) {
        if (check == null) {
            return "";
        }
        if (check.evidencePath().length() > 0) {
            return check.checkKey() + " evidence: " + check.evidencePath();
        }
        return check.checkKey() + " status=" + check.status();
    }

    private String titleForAction(String action) {
        if ("inspect_existing_code".equals(action)) {
            return "Inspect existing code";
        }
        if ("create_change_plan".equals(action)) {
            return "Create change plan";
        }
        if ("implement_minimal_change".equals(action)) {
            return "Implement minimal change";
        }
        if ("verify".equals(action)) {
            return "Verify goal checks";
        }
        return action.replace('_', ' ');
    }
}
