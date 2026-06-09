package com.devharnesskit.dhk.context.artifact;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class ContextArtifactRepository {
    public void upsert(Connection connection, ContextArtifact artifact) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT OR REPLACE INTO context_artifact(project_key, goal_key, artifact_key, source_type, "
                        + "source_path, original_sha256, original_text, compressed_text, retained_spans_json, "
                        + "omitted_lines, token_before, token_after, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, artifact.projectKey());
            statement.setString(2, artifact.goalKey());
            statement.setString(3, artifact.artifactKey());
            statement.setString(4, artifact.sourceType());
            statement.setString(5, artifact.sourcePath());
            statement.setString(6, artifact.originalSha256());
            statement.setString(7, artifact.originalText());
            statement.setString(8, artifact.compressedText());
            statement.setString(9, artifact.retainedSpansJson());
            statement.setInt(10, artifact.omittedLines());
            statement.setInt(11, artifact.tokenBefore());
            statement.setInt(12, artifact.tokenAfter());
            statement.setString(13, artifact.createdAt());
            statement.executeUpdate();
        }
    }

    public ContextArtifact findByKey(Connection connection, String projectKey, String artifactKey)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM context_artifact WHERE project_key = ? AND artifact_key = ?")) {
            statement.setString(1, projectKey);
            statement.setString(2, artifactKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    public List<ContextArtifact> list(Connection connection, String projectKey, String goalKey, int limit)
            throws SQLException {
        return list(connection, projectKey, goalKey, "", limit);
    }

    public List<ContextArtifact> list(Connection connection, String projectKey, String goalKey, String sourceType,
                                      int limit) throws SQLException {
        return list(connection, projectKey, goalKey, sourceType, limit, 0);
    }

    public List<ContextArtifact> list(Connection connection, String projectKey, String goalKey, String sourceType,
                                      int limit, int offset) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM context_artifact WHERE project_key = ?");
        if (goalKey != null && goalKey.length() > 0) {
            sql.append(" AND goal_key = ?");
        }
        if (sourceType != null && sourceType.length() > 0) {
            sql.append(" AND source_type = ?");
        }
        sql.append(" ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?");
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            statement.setString(index++, projectKey);
            if (goalKey != null && goalKey.length() > 0) {
                statement.setString(index++, goalKey);
            }
            if (sourceType != null && sourceType.length() > 0) {
                statement.setString(index++, sourceType);
            }
            statement.setInt(index++, Math.max(1, limit));
            statement.setInt(index, Math.max(0, offset));
            List<ContextArtifact> results = new ArrayList<ContextArtifact>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    results.add(map(resultSet));
                }
            }
            return results;
        }
    }

    public ContextArtifactStats stats(Connection connection, String projectKey, String goalKey)
            throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) AS artifact_count, COALESCE(SUM(token_before), 0) AS token_before, "
                + "COALESCE(SUM(token_after), 0) AS token_after, "
                + "COALESCE(SUM(omitted_lines), 0) AS omitted_lines "
                + "FROM context_artifact WHERE project_key = ?");
        if (goalKey != null && goalKey.length() > 0) {
            sql.append(" AND goal_key = ?");
        }
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            statement.setString(1, projectKey);
            if (goalKey != null && goalKey.length() > 0) {
                statement.setString(2, goalKey);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return new ContextArtifactStats(0, 0L, 0L, 0L);
                }
                return new ContextArtifactStats(resultSet.getInt("artifact_count"),
                        resultSet.getLong("token_before"),
                        resultSet.getLong("token_after"),
                        resultSet.getLong("omitted_lines"),
                        statsBySourceType(connection, projectKey, goalKey));
            }
        }
    }

    public List<ContextArtifactStats.SourceTypeStats> statsBySourceType(Connection connection, String projectKey,
                                                                        String goalKey) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT source_type, COUNT(*) AS artifact_count, ")
                .append("COALESCE(SUM(token_before), 0) AS token_before, ")
                .append("COALESCE(SUM(token_after), 0) AS token_after, ")
                .append("COALESCE(SUM(omitted_lines), 0) AS omitted_lines, ")
                .append("COALESCE(SUM(CASE WHEN compressed_text LIKE '%degraded: true%' THEN 1 ELSE 0 END), 0) ")
                .append("AS degraded_count ")
                .append("FROM context_artifact WHERE project_key = ?");
        if (goalKey != null && goalKey.length() > 0) {
            sql.append(" AND goal_key = ?");
        }
        sql.append(" GROUP BY source_type ORDER BY artifact_count DESC, source_type ASC");
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            statement.setString(1, projectKey);
            if (goalKey != null && goalKey.length() > 0) {
                statement.setString(2, goalKey);
            }
            List<ContextArtifactStats.SourceTypeStats> results =
                    new ArrayList<ContextArtifactStats.SourceTypeStats>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    results.add(new ContextArtifactStats.SourceTypeStats(
                            resultSet.getString("source_type"),
                            resultSet.getInt("artifact_count"),
                            resultSet.getLong("token_before"),
                            resultSet.getLong("token_after"),
                            resultSet.getLong("omitted_lines"),
                            resultSet.getInt("degraded_count")));
                }
            }
            return results;
        }
    }

    private ContextArtifact map(ResultSet resultSet) throws SQLException {
        return new ContextArtifact(
                resultSet.getLong("id"),
                resultSet.getString("project_key"),
                resultSet.getString("goal_key"),
                resultSet.getString("artifact_key"),
                resultSet.getString("source_type"),
                resultSet.getString("source_path"),
                resultSet.getString("original_sha256"),
                resultSet.getString("original_text"),
                resultSet.getString("compressed_text"),
                resultSet.getString("retained_spans_json"),
                resultSet.getInt("omitted_lines"),
                resultSet.getInt("token_before"),
                resultSet.getInt("token_after"),
                resultSet.getString("created_at"));
    }
}
