package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.goal.GoalAcceptanceMapping;
import com.devharnesskit.dhk.model.goal.GoalActionMapping;
import com.devharnesskit.dhk.model.goal.GoalProfile;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.model.goal.GoalStep;
import com.devharnesskit.dhk.model.spec.SpecAcceptance;
import com.devharnesskit.dhk.model.spec.SpecChange;
import com.devharnesskit.dhk.model.spec.SpecEvent;
import com.devharnesskit.dhk.model.spec.SpecTask;
import com.devharnesskit.dhk.repository.spec.SpecAcceptanceRepository;
import com.devharnesskit.dhk.repository.spec.SpecChangeRepository;
import com.devharnesskit.dhk.repository.spec.SpecEventRepository;
import com.devharnesskit.dhk.repository.spec.SpecTaskRepository;
import com.devharnesskit.dhk.service.spec.SpecAcceptanceService;
import com.devharnesskit.dhk.service.spec.SpecTaskService;

import java.sql.Connection;
import java.util.List;

final class GoalSpecSyncSupport {
    private static final String GOAL_ACCEPTANCE = "goal_checks_pass";

    private final SpecChangeRepository specChangeRepository;
    private final SpecTaskRepository taskRepository;
    private final SpecAcceptanceRepository acceptanceRepository;
    private final SpecEventRepository specEventRepository;
    private final SpecTaskService taskService;
    private final SpecAcceptanceService acceptanceService;

    GoalSpecSyncSupport(SpecChangeRepository specChangeRepository,
                        SpecTaskRepository taskRepository,
                        SpecAcceptanceRepository acceptanceRepository,
                        SpecEventRepository specEventRepository,
                        SpecTaskService taskService,
                        SpecAcceptanceService acceptanceService) {
        this.specChangeRepository = specChangeRepository;
        this.taskRepository = taskRepository;
        this.acceptanceRepository = acceptanceRepository;
        this.specEventRepository = specEventRepository;
        this.taskService = taskService;
        this.acceptanceService = acceptanceService;
    }

    void syncTaskForStep(Connection connection, Project project, GoalRun goal,
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

    void ensureScaffold(Connection connection, Project project, GoalRun goal,
                        GoalProfile profile, String now) throws Exception {
        SpecChange change = specChange(connection, goal);
        if (change == null || profile == null || !profile.specRequired() || !hasManagedSpec(profile)) {
            return;
        }
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
        }
        for (GoalAcceptanceMapping mapping : profile.acceptanceMappings().values()) {
            if (acceptanceRepository.findByKey(connection, change.changeKey(), mapping.acceptanceKey()) == null) {
                acceptanceService.addAcceptance(connection, change, mapping.acceptanceKey(),
                        mapping.description(), mapping.expectedResult(), now);
            }
        }
        if (hasAutoAcceptance(profile)
                && acceptanceRepository.findByKey(connection, change.changeKey(), GOAL_ACCEPTANCE) == null) {
            acceptanceService.addAcceptance(connection, change, GOAL_ACCEPTANCE,
                    "Required goal checks are accepted",
                    "compile/test/sensitive/workflow checks are accepted by policy", now);
        }
    }

    void verifyChange(Connection connection, GoalRun goal, GoalProfile profile, String now) throws Exception {
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

    SpecChange specChange(Connection connection, GoalRun goal) throws Exception {
        if (goal.specChangeKey().length() == 0) {
            return null;
        }
        return specChangeRepository.findByKey(connection, goal.specChangeKey());
    }

    boolean hasManagedAcceptance(GoalProfile profile) {
        return profile != null && (!profile.acceptanceMappings().isEmpty() || hasAutoAcceptance(profile));
    }

    boolean hasAutoAcceptance(GoalProfile profile) {
        if (profile == null) {
            return false;
        }
        for (GoalActionMapping mapping : profile.actionMappings().values()) {
            if (GoalActionMapping.ACCEPTANCE_CHECKS.equals(mapping.acceptanceSource())
                    || "auto_pass".equals(mapping.specAcceptanceUpdate())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasManagedSpec(GoalProfile profile) {
        if (profile == null) {
            return false;
        }
        if (!profile.acceptanceMappings().isEmpty()) {
            return true;
        }
        for (GoalActionMapping mapping : profile.actionMappings().values()) {
            if (mapping.specTask().length() > 0 || mapping.specAcceptanceUpdate().length() > 0) {
                return true;
            }
        }
        return false;
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
