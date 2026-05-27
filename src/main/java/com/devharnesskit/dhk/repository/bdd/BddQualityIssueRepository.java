package com.devharnesskit.dhk.repository.bdd;

import com.devharnesskit.dhk.model.bdd.BddQualityIssue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class BddQualityIssueRepository {
    public long insert(Connection connection, BddQualityIssue issue) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO bdd_quality_issue(project_key, feature_key, scenario_key, issue_type, "
                        + "severity, status, message, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, issue.projectKey());
            statement.setString(2, issue.featureKey());
            statement.setString(3, issue.scenarioKey());
            statement.setString(4, issue.issueType());
            statement.setString(5, issue.severity());
            statement.setString(6, issue.status());
            statement.setString(7, issue.message());
            statement.setString(8, issue.createdAt());
            statement.executeUpdate();
            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                if (!resultSet.next()) {
                    throw new SQLException("No generated key returned for bdd quality issue insert");
                }
                return resultSet.getLong(1);
            }
        }
    }

    public void deleteOpenByProject(Connection connection, String projectKey, String featureKey) throws SQLException {
        String sql = featureKey == null || featureKey.length() == 0
                ? "DELETE FROM bdd_quality_issue WHERE project_key = ? AND status = 'open'"
                : "DELETE FROM bdd_quality_issue WHERE project_key = ? AND feature_key = ? AND status = 'open'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, projectKey);
            if (featureKey != null && featureKey.length() > 0) {
                statement.setString(2, featureKey);
            }
            statement.executeUpdate();
        }
    }

    public List<BddQualityIssue> listOpenByProject(Connection connection, String projectKey, String featureKey)
            throws SQLException {
        String sql = featureKey == null || featureKey.length() == 0
                ? "SELECT * FROM bdd_quality_issue WHERE project_key = ? AND status = 'open' "
                + "ORDER BY severity DESC, scenario_key, id"
                : "SELECT * FROM bdd_quality_issue WHERE project_key = ? AND feature_key = ? AND status = 'open' "
                + "ORDER BY severity DESC, scenario_key, id";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, projectKey);
            if (featureKey != null && featureKey.length() > 0) {
                statement.setString(2, featureKey);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                List<BddQualityIssue> results = new ArrayList<BddQualityIssue>();
                while (resultSet.next()) {
                    results.add(map(resultSet));
                }
                return results;
            }
        }
    }

    private BddQualityIssue map(ResultSet resultSet) throws SQLException {
        return new BddQualityIssue(
                resultSet.getLong("id"),
                resultSet.getString("project_key"),
                resultSet.getString("feature_key"),
                resultSet.getString("scenario_key"),
                resultSet.getString("issue_type"),
                resultSet.getString("severity"),
                resultSet.getString("status"),
                resultSet.getString("message"),
                resultSet.getString("created_at"));
    }
}
