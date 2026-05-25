package com.devharnesskit.dhk.repository.goal;

import com.devharnesskit.dhk.model.goal.GoalStep;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class GoalStepRepository {
    public long insert(Connection connection, GoalStep step) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO goal_step(goal_key, step_index, action_key, summary, changed_files, "
                        + "evidence, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, step.goalKey());
            statement.setInt(2, step.stepIndex());
            statement.setString(3, step.actionKey());
            statement.setString(4, step.summary());
            statement.setString(5, step.changedFiles());
            statement.setString(6, step.evidence());
            statement.setString(7, step.status());
            statement.setString(8, step.createdAt());
            statement.executeUpdate();
            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                if (!resultSet.next()) {
                    throw new SQLException("No generated key returned for goal step insert");
                }
                return resultSet.getLong(1);
            }
        }
    }

    public GoalStep latest(Connection connection, String goalKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM goal_step WHERE goal_key = ? ORDER BY step_index DESC, id DESC LIMIT 1")) {
            statement.setString(1, goalKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    public List<GoalStep> listByGoal(Connection connection, String goalKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM goal_step WHERE goal_key = ? ORDER BY step_index, id")) {
            statement.setString(1, goalKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<GoalStep> results = new ArrayList<GoalStep>();
                while (resultSet.next()) {
                    results.add(map(resultSet));
                }
                return results;
            }
        }
    }

    private GoalStep map(ResultSet resultSet) throws SQLException {
        return new GoalStep(
                resultSet.getLong("id"),
                resultSet.getString("goal_key"),
                resultSet.getInt("step_index"),
                resultSet.getString("action_key"),
                resultSet.getString("summary"),
                resultSet.getString("changed_files"),
                resultSet.getString("evidence"),
                resultSet.getString("status"),
                resultSet.getString("created_at"));
    }
}
