package com.devharnesskit.dhk.repository.checkpoint;

import com.devharnesskit.dhk.model.checkpoint.HumanCheckpoint;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class HumanCheckpointRepository {
    public long insert(Connection connection, HumanCheckpoint checkpoint) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO human_checkpoint(goal_key, checkpoint_type, reason, status, requested_by, "
                        + "requested_at, approver, approved_at, decision_reason, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, checkpoint.goalKey());
            statement.setString(2, checkpoint.checkpointType());
            statement.setString(3, checkpoint.reason());
            statement.setString(4, checkpoint.status());
            statement.setString(5, checkpoint.requestedBy());
            statement.setString(6, checkpoint.requestedAt());
            statement.setString(7, checkpoint.approver());
            statement.setString(8, checkpoint.approvedAt());
            statement.setString(9, checkpoint.decisionReason());
            statement.setString(10, checkpoint.createdAt());
            statement.setString(11, checkpoint.updatedAt());
            statement.executeUpdate();
            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                if (!resultSet.next()) {
                    throw new SQLException("No generated key returned for human checkpoint insert");
                }
                return resultSet.getLong(1);
            }
        }
    }

    public HumanCheckpoint find(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM human_checkpoint WHERE id = ?")) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    public List<HumanCheckpoint> listByGoal(Connection connection, String goalKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM human_checkpoint WHERE goal_key = ? ORDER BY id")) {
            statement.setString(1, goalKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<HumanCheckpoint> results = new ArrayList<HumanCheckpoint>();
                while (resultSet.next()) {
                    results.add(map(resultSet));
                }
                return results;
            }
        }
    }

    public boolean hasApproved(Connection connection, String goalKey, String checkpointType) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM human_checkpoint WHERE goal_key = ? AND checkpoint_type = ? "
                        + "AND status = 'approved' LIMIT 1")) {
            statement.setString(1, goalKey);
            statement.setString(2, checkpointType);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public void approve(Connection connection, long id, String approver, String approvedAt,
                        String decisionReason) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE human_checkpoint SET status = 'approved', approver = ?, approved_at = ?, "
                        + "decision_reason = ?, updated_at = ? WHERE id = ?")) {
            statement.setString(1, approver);
            statement.setString(2, approvedAt);
            statement.setString(3, decisionReason);
            statement.setString(4, approvedAt);
            statement.setLong(5, id);
            statement.executeUpdate();
        }
    }

    private HumanCheckpoint map(ResultSet resultSet) throws SQLException {
        return new HumanCheckpoint(
                resultSet.getLong("id"),
                resultSet.getString("goal_key"),
                resultSet.getString("checkpoint_type"),
                resultSet.getString("reason"),
                resultSet.getString("status"),
                resultSet.getString("requested_by"),
                resultSet.getString("requested_at"),
                resultSet.getString("approver"),
                resultSet.getString("approved_at"),
                resultSet.getString("decision_reason"),
                resultSet.getString("created_at"),
                resultSet.getString("updated_at"));
    }
}
