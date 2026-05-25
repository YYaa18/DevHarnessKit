package com.devharnesskit.dhk.repository.goal;

import com.devharnesskit.dhk.model.goal.GoalArtifact;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class GoalArtifactRepository {
    public long insert(Connection connection, GoalArtifact artifact) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO goal_artifact(goal_key, artifact_type, title, file_path, content_hash, summary, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, artifact.goalKey());
            statement.setString(2, artifact.artifactType());
            statement.setString(3, artifact.title());
            statement.setString(4, artifact.filePath());
            statement.setString(5, artifact.contentHash());
            statement.setString(6, artifact.summary());
            statement.setString(7, artifact.createdAt());
            statement.executeUpdate();
            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                if (!resultSet.next()) {
                    throw new SQLException("No generated key returned for goal artifact insert");
                }
                return resultSet.getLong(1);
            }
        }
    }

    public List<GoalArtifact> listByGoal(Connection connection, String goalKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM goal_artifact WHERE goal_key = ? ORDER BY id")) {
            statement.setString(1, goalKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<GoalArtifact> results = new ArrayList<GoalArtifact>();
                while (resultSet.next()) {
                    results.add(map(resultSet));
                }
                return results;
            }
        }
    }

    private GoalArtifact map(ResultSet resultSet) throws SQLException {
        return new GoalArtifact(
                resultSet.getLong("id"),
                resultSet.getString("goal_key"),
                resultSet.getString("artifact_type"),
                resultSet.getString("title"),
                resultSet.getString("file_path"),
                resultSet.getString("content_hash"),
                resultSet.getString("summary"),
                resultSet.getString("created_at"));
    }
}
