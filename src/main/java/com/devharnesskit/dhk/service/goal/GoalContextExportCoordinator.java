package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.goal.GoalEvent;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.model.spec.SpecChange;
import com.devharnesskit.dhk.model.workflow.WorkflowRun;
import com.devharnesskit.dhk.repository.goal.GoalEventRepository;
import com.devharnesskit.dhk.repository.goal.GoalRunRepository;

import java.nio.file.Path;
import java.sql.Connection;

final class GoalContextExportCoordinator {
    private final GoalRunRepository goalRunRepository;
    private final GoalEventRepository goalEventRepository;
    private final GoalPlanner planner;
    private final GoalContextService contextService;

    GoalContextExportCoordinator(GoalRunRepository goalRunRepository,
                                 GoalEventRepository goalEventRepository,
                                 GoalPlanner planner,
                                 GoalContextService contextService) {
        this.goalRunRepository = goalRunRepository;
        this.goalEventRepository = goalEventRepository;
        this.planner = planner;
        this.contextService = contextService;
    }

    Path export(Connection connection, Path projectRoot, Project project, GoalRun goal,
                WorkflowRun workflowRun, SpecChange specChange, String targetStatus,
                String now) throws Exception {
        goalRunRepository.updateStatus(connection, goal.goalKey(), "context_exporting", now);
        GoalRun exportGoal = withStatus(goalRunRepository.findByKey(connection, goal.goalKey()),
                targetStatus, now);
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

    String targetStatusAfterExport(GoalRun goal) {
        if (isIncomplete(goal.status())) {
            if (goal.stepCount() <= 0) {
                return "context_ready";
            }
            return planner.statusForAction(goal.currentAction());
        }
        return goal.status();
    }

    boolean isIncomplete(String status) {
        return "context_export_failed".equals(status) || "context_exporting".equals(status);
    }

    private GoalRun withStatus(GoalRun goal, String status, String now) {
        return new GoalRun(goal.goalKey(), goal.projectKey(), goal.workflowRunKey(), goal.specChangeKey(),
                goal.externalRef(), goal.profileKey(), goal.taskName(), goal.moduleName(), goal.mode(),
                goal.conditionText(), status, goal.currentAction(), goal.maxSteps(), goal.stepCount(),
                goal.createdAt(), now, goal.completedAt());
    }
}
