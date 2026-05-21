package com.devharnesskit.dhk.repository.spec;

import com.devharnesskit.dhk.model.spec.SpecTask;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class SpecTaskRepository {
    public long insert(Connection connection, SpecTask task) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO spec_task(change_key, task_key, task_order, title, description, status, "
                        + "phase_key, evidence, created_at, updated_at, completed_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, task);
            statement.executeUpdate();
            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                if (!resultSet.next()) {
                    throw new SQLException("No generated key returned for spec task insert");
                }
                return resultSet.getLong(1);
            }
        }
    }

    public SpecTask findByKey(Connection connection, String changeKey, String taskKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM spec_task WHERE change_key = ? AND task_key = ?")) {
            statement.setString(1, changeKey);
            statement.setString(2, taskKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    public List<SpecTask> listByChange(Connection connection, String changeKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM spec_task WHERE change_key = ? ORDER BY task_order, id")) {
            statement.setString(1, changeKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<SpecTask> results = new ArrayList<SpecTask>();
                while (resultSet.next()) {
                    results.add(map(resultSet));
                }
                return results;
            }
        }
    }

    public int nextOrder(Connection connection, String changeKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COALESCE(MAX(task_order), 0) + 1 FROM spec_task WHERE change_key = ?")) {
            statement.setString(1, changeKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 1;
            }
        }
    }

    public void updateStatus(Connection connection, String changeKey, String taskKey, String status,
                             String evidence, String completedAt, String now) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE spec_task SET status = ?, evidence = ?, completed_at = ?, updated_at = ? "
                        + "WHERE change_key = ? AND task_key = ?")) {
            statement.setString(1, status);
            statement.setString(2, evidence);
            statement.setString(3, completedAt);
            statement.setString(4, now);
            statement.setString(5, changeKey);
            statement.setString(6, taskKey);
            statement.executeUpdate();
        }
    }

    private void bind(PreparedStatement statement, SpecTask task) throws SQLException {
        statement.setString(1, task.changeKey());
        statement.setString(2, task.taskKey());
        statement.setInt(3, task.taskOrder());
        statement.setString(4, task.title());
        statement.setString(5, task.description());
        statement.setString(6, task.status());
        statement.setString(7, task.phaseKey());
        statement.setString(8, task.evidence());
        statement.setString(9, task.createdAt());
        statement.setString(10, task.updatedAt());
        statement.setString(11, task.completedAt());
    }

    private SpecTask map(ResultSet resultSet) throws SQLException {
        return new SpecTask(
                resultSet.getLong("id"),
                resultSet.getString("change_key"),
                resultSet.getString("task_key"),
                resultSet.getInt("task_order"),
                resultSet.getString("title"),
                resultSet.getString("description"),
                resultSet.getString("status"),
                resultSet.getString("phase_key"),
                resultSet.getString("evidence"),
                resultSet.getString("created_at"),
                resultSet.getString("updated_at"),
                resultSet.getString("completed_at"));
    }
}
