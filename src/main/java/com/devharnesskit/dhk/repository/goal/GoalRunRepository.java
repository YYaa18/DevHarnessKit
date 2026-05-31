package com.devharnesskit.dhk.repository.goal;

import com.devharnesskit.dhk.model.goal.GoalRun;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class GoalRunRepository {
    public void insert(Connection connection, GoalRun goal) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO goal_run(goal_key, project_key, workflow_run_key, spec_change_key, external_ref, "
                        + "profile_key, task_name, module_name, mode, condition_text, status, "
                        + "current_action, max_steps, step_count, created_at, updated_at, completed_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            bind(statement, goal);
            statement.executeUpdate();
        }
    }

    public GoalRun findByKey(Connection connection, String goalKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM goal_run WHERE goal_key = ?")) {
            statement.setString(1, goalKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    public GoalRun latestOpen(Connection connection, String projectKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM goal_run WHERE project_key = ? "
                        + "AND status NOT IN ('completed', 'abandoned', 'failed') "
                        + "ORDER BY updated_at DESC, created_at DESC, goal_key DESC LIMIT 1")) {
            statement.setString(1, projectKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    public GoalRun findOpenByIdentity(Connection connection, String projectKey, String profileKey,
                                      String moduleName, String taskName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM goal_run WHERE project_key = ? AND profile_key = ? "
                        + "AND module_name = ? AND task_name = ? "
                        + "AND status NOT IN ('completed', 'abandoned', 'failed') "
                        + "ORDER BY updated_at DESC, created_at DESC, goal_key DESC LIMIT 1")) {
            statement.setString(1, projectKey);
            statement.setString(2, profileKey);
            statement.setString(3, moduleName);
            statement.setString(4, taskName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    public void updateProgress(Connection connection, String goalKey, String status,
                               String currentAction, int stepCount, String now) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE goal_run SET status = ?, current_action = ?, step_count = ?, "
                        + "updated_at = ? WHERE goal_key = ?")) {
            statement.setString(1, status);
            statement.setString(2, currentAction);
            statement.setInt(3, stepCount);
            statement.setString(4, now);
            statement.setString(5, goalKey);
            statement.executeUpdate();
        }
    }

    public void updateStatus(Connection connection, String goalKey, String status, String now) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE goal_run SET status = ?, updated_at = ? WHERE goal_key = ?")) {
            statement.setString(1, status);
            statement.setString(2, now);
            statement.setString(3, goalKey);
            statement.executeUpdate();
        }
    }

    public void complete(Connection connection, String goalKey, String completedAt, String now) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE goal_run SET status = 'completed', current_action = 'completed', "
                        + "completed_at = ?, updated_at = ? WHERE goal_key = ?")) {
            statement.setString(1, completedAt);
            statement.setString(2, now);
            statement.setString(3, goalKey);
            statement.executeUpdate();
        }
    }

    private void bind(PreparedStatement statement, GoalRun goal) throws SQLException {
        statement.setString(1, goal.goalKey());
        statement.setString(2, goal.projectKey());
        statement.setString(3, goal.workflowRunKey());
        statement.setString(4, goal.specChangeKey());
        statement.setString(5, goal.externalRef());
        statement.setString(6, goal.profileKey());
        statement.setString(7, goal.taskName());
        statement.setString(8, goal.moduleName());
        statement.setString(9, goal.mode());
        statement.setString(10, goal.conditionText());
        statement.setString(11, goal.status());
        statement.setString(12, goal.currentAction());
        statement.setInt(13, goal.maxSteps());
        statement.setInt(14, goal.stepCount());
        statement.setString(15, goal.createdAt());
        statement.setString(16, goal.updatedAt());
        statement.setString(17, goal.completedAt());
    }

    private GoalRun map(ResultSet resultSet) throws SQLException {
        return new GoalRun(
                resultSet.getString("goal_key"),
                resultSet.getString("project_key"),
                resultSet.getString("workflow_run_key"),
                resultSet.getString("spec_change_key"),
                resultSet.getString("external_ref"),
                resultSet.getString("profile_key"),
                resultSet.getString("task_name"),
                resultSet.getString("module_name"),
                resultSet.getString("mode"),
                resultSet.getString("condition_text"),
                resultSet.getString("status"),
                resultSet.getString("current_action"),
                resultSet.getInt("max_steps"),
                resultSet.getInt("step_count"),
                resultSet.getString("created_at"),
                resultSet.getString("updated_at"),
                resultSet.getString("completed_at"));
    }
}
