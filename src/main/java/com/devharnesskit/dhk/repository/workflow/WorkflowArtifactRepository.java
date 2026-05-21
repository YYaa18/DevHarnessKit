package com.devharnesskit.dhk.repository.workflow;

import com.devharnesskit.dhk.model.workflow.WorkflowArtifact;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class WorkflowArtifactRepository {
    public long insert(Connection connection, WorkflowArtifact artifact) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO workflow_artifact(project_key, run_key, artifact_type, title, file_path, "
                        + "content_hash, status, produced_by_phase, summary, tags, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, artifact.projectKey());
            statement.setString(2, artifact.runKey());
            statement.setString(3, artifact.artifactType());
            statement.setString(4, artifact.title());
            statement.setString(5, artifact.filePath());
            statement.setString(6, artifact.contentHash());
            statement.setString(7, artifact.status());
            statement.setString(8, artifact.producedByPhase());
            statement.setString(9, artifact.summary());
            statement.setString(10, artifact.tags());
            statement.setString(11, artifact.createdAt());
            statement.setString(12, artifact.updatedAt());
            statement.executeUpdate();
            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                if (!resultSet.next()) {
                    throw new SQLException("No generated key returned for workflow artifact insert");
                }
                return resultSet.getLong(1);
            }
        }
    }

    public List<WorkflowArtifact> listByRun(Connection connection, String runKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM workflow_artifact WHERE run_key = ? ORDER BY id")) {
            statement.setString(1, runKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<WorkflowArtifact> results = new ArrayList<WorkflowArtifact>();
                while (resultSet.next()) {
                    results.add(map(resultSet));
                }
                return results;
            }
        }
    }

    public int countByRun(Connection connection, String runKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM workflow_artifact WHERE run_key = ?")) {
            statement.setString(1, runKey);
            return singleInt(statement);
        }
    }

    private int singleInt(PreparedStatement statement) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
    }

    private WorkflowArtifact map(ResultSet resultSet) throws SQLException {
        return new WorkflowArtifact(
                resultSet.getLong("id"),
                resultSet.getString("project_key"),
                resultSet.getString("run_key"),
                resultSet.getString("artifact_type"),
                resultSet.getString("title"),
                resultSet.getString("file_path"),
                resultSet.getString("content_hash"),
                resultSet.getString("status"),
                resultSet.getString("produced_by_phase"),
                resultSet.getString("summary"),
                resultSet.getString("tags"),
                resultSet.getString("created_at"),
                resultSet.getString("updated_at"));
    }
}
