package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.goal.GoalProfile;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.model.workflow.WorkflowEvent;
import com.devharnesskit.dhk.model.workflow.WorkflowGateRun;
import com.devharnesskit.dhk.model.workflow.WorkflowPhaseRun;
import com.devharnesskit.dhk.model.workflow.WorkflowRun;
import com.devharnesskit.dhk.repository.workflow.WorkflowEventRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowGateRunRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowPhaseRunRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowRunRepository;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;

final class GoalWorkflowSyncSupport {
    private final WorkflowRunRepository workflowRunRepository;
    private final WorkflowPhaseRunRepository phaseRunRepository;
    private final WorkflowGateRunRepository gateRunRepository;
    private final WorkflowEventRepository workflowEventRepository;

    GoalWorkflowSyncSupport(WorkflowRunRepository workflowRunRepository,
                            WorkflowPhaseRunRepository phaseRunRepository,
                            WorkflowGateRunRepository gateRunRepository,
                            WorkflowEventRepository workflowEventRepository) {
        this.workflowRunRepository = workflowRunRepository;
        this.phaseRunRepository = phaseRunRepository;
        this.gateRunRepository = gateRunRepository;
        this.workflowEventRepository = workflowEventRepository;
    }

    void passContextExport(Connection connection, Path projectRoot, Project project,
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

    void passUserApproval(Connection connection, Project project, GoalRun goal, GoalProfile profile,
                          String now) throws Exception {
        passGate(connection, project, goal, "user_approval_before_implementation",
                "Goal implementation step recorded under ordered protocol",
                "goal_step=implement_minimal_change", now);
        passPhase(connection, project, goal, "user_approval",
                "Approval/rationale satisfied by ordered goal protocol",
                "goal_step=implement_minimal_change", now, profile);
    }

    void passCheckpoint(Connection connection, Project project, GoalRun goal,
                        GoalProfile profile, long checkpointId, String now) throws Exception {
        if (goal.workflowRunKey().length() == 0) {
            return;
        }
        passGate(connection, project, goal, "checkpoint_created", "Completion checkpoint created",
                "checkpoint_id=" + checkpointId, now);
        passPhase(connection, project, goal, "create_checkpoint", "Completion checkpoint created",
                "checkpoint_id=" + checkpointId, now, profile);
    }

    void passOptionalFinalWorkflowPhases(Connection connection, Project project,
                                         GoalRun goal, GoalProfile profile, String now) throws Exception {
        if (goal.workflowRunKey().length() == 0) {
            return;
        }
        passPhase(connection, project, goal, "suggest_memory_updates",
                "No automatic memory confirmation; suggestions remain optional",
                "goal_complete", now, profile);
    }

    boolean passGate(Connection connection, Project project, GoalRun goal, String gateKey,
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

    boolean passPhase(Connection connection, Project project, GoalRun goal, String phaseKey,
                      String summary, String evidence, String now) throws Exception {
        return passPhase(connection, project, goal, phaseKey, summary, evidence, now, null);
    }

    boolean passPhase(Connection connection, Project project, GoalRun goal, String phaseKey,
                      String summary, String evidence, String now, GoalProfile profile) throws Exception {
        if (phaseKey == null || phaseKey.length() == 0 || goal.workflowRunKey().length() == 0) {
            return false;
        }
        WorkflowPhaseRun phase = phaseRunRepository.find(connection, goal.workflowRunKey(), phaseKey);
        if (phase == null || "passed".equals(phase.status()) || "failed".equals(phase.status())
                || "blocked".equals(phase.status())) {
            return false;
        }
        WorkflowPhaseRun blocker = priorIncompletePhase(connection, goal, profile, phase);
        if (blocker != null) {
            workflowEventRepository.insert(connection, new WorkflowEvent(0L, project.projectKey(),
                    goal.workflowRunKey(), "custom", phaseKey, "", "warn",
                    "Phase sync blocked by pending earlier phase: " + blocker.phaseKey(), summary, now));
            return false;
        }
        phaseRunRepository.updateStatus(connection, goal.workflowRunKey(), phaseKey, "passed",
                summary, evidence, now, now);
        workflowEventRepository.insert(connection, new WorkflowEvent(0L, project.projectKey(),
                goal.workflowRunKey(), "phase_completed", phaseKey, "", "info",
                "Phase passed by goal sync: " + phaseKey, summary, now));
        return true;
    }

    void refreshProgress(Connection connection, String workflowRunKey, String now) throws Exception {
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

    boolean completeUnmappedWorkflow(Connection connection, Project project, GoalRun goal,
                                     GoalProfile profile, long checkpointId, String now) throws Exception {
        if (goal.workflowRunKey().length() == 0 || profile == null || !profile.actionMappings().isEmpty()) {
            return false;
        }
        String evidence = "goal_complete checkpoint_id=" + checkpointId;
        for (WorkflowGateRun gate : gateRunRepository.listByRun(connection, goal.workflowRunKey())) {
            if ("passed".equals(gate.status()) || "waived".equals(gate.status()) || "failed".equals(gate.status())) {
                continue;
            }
            gateRunRepository.updateStatus(connection, gate.id(), "passed",
                    "Satisfied by completed lightweight goal protocol", "", evidence, now, now);
            workflowEventRepository.insert(connection, new WorkflowEvent(0L, project.projectKey(),
                    goal.workflowRunKey(), "gate_checked", gate.phaseKey(), gate.gateKey(), "info",
                    "Gate passed by lightweight goal completion: " + gate.gateKey(),
                    "Satisfied by completed lightweight goal protocol", now));
        }

        String lastPhaseKey = "";
        for (WorkflowPhaseRun phase : phaseRunRepository.listByRun(connection, goal.workflowRunKey())) {
            lastPhaseKey = phase.phaseKey();
            if ("passed".equals(phase.status()) || "failed".equals(phase.status())
                    || "blocked".equals(phase.status())) {
                continue;
            }
            phaseRunRepository.updateStatus(connection, goal.workflowRunKey(), phase.phaseKey(), "passed",
                    "Satisfied by completed lightweight goal protocol", evidence, now, now);
            workflowEventRepository.insert(connection, new WorkflowEvent(0L, project.projectKey(),
                    goal.workflowRunKey(), "phase_completed", phase.phaseKey(), "", "info",
                    "Phase passed by lightweight goal completion: " + phase.phaseKey(),
                    "Satisfied by completed lightweight goal protocol", now));
        }
        workflowRunRepository.complete(connection, goal.workflowRunKey(), lastPhaseKey, now, now);
        workflowEventRepository.insert(connection, new WorkflowEvent(0L, project.projectKey(),
                goal.workflowRunKey(), "run_completed", lastPhaseKey, "", "info",
                "Workflow completed by lightweight goal completion", evidence, now));
        return true;
    }

    private WorkflowPhaseRun priorIncompletePhase(Connection connection, GoalRun goal, GoalProfile profile,
                                                 WorkflowPhaseRun target) throws Exception {
        if (profile == null || !profile.strictWorkflowPhaseOrder()) {
            return null;
        }
        List<WorkflowPhaseRun> phases = phaseRunRepository.listByRun(connection, goal.workflowRunKey());
        for (WorkflowPhaseRun phase : phases) {
            if (phase.phaseOrder() >= target.phaseOrder()) {
                continue;
            }
            if (!"passed".equals(phase.status())) {
                return phase;
            }
        }
        return null;
    }
}
