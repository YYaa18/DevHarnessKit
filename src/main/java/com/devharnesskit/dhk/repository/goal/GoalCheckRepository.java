package com.devharnesskit.dhk.repository.goal;

import com.devharnesskit.dhk.model.goal.GoalCheck;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class GoalCheckRepository {
    public long upsert(Connection connection, GoalCheck check) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE goal_check SET check_type = ?, required = ?, step_count_at_check = ?, command = ?, status = ?, "
                        + "result_summary = ?, evidence_path = ?, checked_at = ?, updated_at = ? "
                        + "WHERE goal_key = ? AND check_key = ?")) {
            update.setString(1, check.checkType());
            update.setInt(2, check.required() ? 1 : 0);
            update.setInt(3, check.stepCountAtCheck());
            update.setString(4, check.command());
            update.setString(5, check.status());
            update.setString(6, check.resultSummary());
            update.setString(7, check.evidencePath());
            update.setString(8, check.checkedAt());
            update.setString(9, check.updatedAt());
            update.setString(10, check.goalKey());
            update.setString(11, check.checkKey());
            if (update.executeUpdate() > 0) {
                GoalCheck existing = find(connection, check.goalKey(), check.checkKey());
                return existing == null ? 0L : existing.id();
            }
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO goal_check(goal_key, check_key, check_type, required, step_count_at_check, command, status, "
                        + "result_summary, evidence_path, checked_at, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            insert.setString(1, check.goalKey());
            insert.setString(2, check.checkKey());
            insert.setString(3, check.checkType());
            insert.setInt(4, check.required() ? 1 : 0);
            insert.setInt(5, check.stepCountAtCheck());
            insert.setString(6, check.command());
            insert.setString(7, check.status());
            insert.setString(8, check.resultSummary());
            insert.setString(9, check.evidencePath());
            insert.setString(10, check.checkedAt());
            insert.setString(11, check.createdAt());
            insert.setString(12, check.updatedAt());
            insert.executeUpdate();
            try (ResultSet resultSet = insert.getGeneratedKeys()) {
                if (!resultSet.next()) {
                    throw new SQLException("No generated key returned for goal check insert");
                }
                return resultSet.getLong(1);
            }
        }
    }

    public GoalCheck find(Connection connection, String goalKey, String checkKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM goal_check WHERE goal_key = ? AND check_key = ?")) {
            statement.setString(1, goalKey);
            statement.setString(2, checkKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    public List<GoalCheck> listByGoal(Connection connection, String goalKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM goal_check WHERE goal_key = ? ORDER BY check_key")) {
            statement.setString(1, goalKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<GoalCheck> results = new ArrayList<GoalCheck>();
                while (resultSet.next()) {
                    results.add(map(resultSet));
                }
                return results;
            }
        }
    }

    private GoalCheck map(ResultSet resultSet) throws SQLException {
        return new GoalCheck(
                resultSet.getLong("id"),
                resultSet.getString("goal_key"),
                resultSet.getString("check_key"),
                resultSet.getString("check_type"),
                resultSet.getInt("required") == 1,
                resultSet.getInt("step_count_at_check"),
                resultSet.getString("command"),
                resultSet.getString("status"),
                resultSet.getString("result_summary"),
                resultSet.getString("evidence_path"),
                resultSet.getString("checked_at"),
                resultSet.getString("created_at"),
                resultSet.getString("updated_at"));
    }
}
