package com.devharnesskit.dhk.repository.bdd;

import com.devharnesskit.dhk.model.bdd.BddFeature;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class BddFeatureRepository {
    public void insert(Connection connection, BddFeature feature) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO bdd_feature(feature_key, project_key, module_name, title, description, tags, "
                        + "status, source_kind, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            bind(statement, feature);
            statement.executeUpdate();
        }
    }

    public BddFeature findByKey(Connection connection, String featureKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM bdd_feature WHERE feature_key = ?")) {
            statement.setString(1, featureKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    public List<BddFeature> listByProject(Connection connection, String projectKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM bdd_feature WHERE project_key = ? ORDER BY module_name, feature_key")) {
            statement.setString(1, projectKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<BddFeature> results = new ArrayList<BddFeature>();
                while (resultSet.next()) {
                    results.add(map(resultSet));
                }
                return results;
            }
        }
    }

    private void bind(PreparedStatement statement, BddFeature feature) throws SQLException {
        statement.setString(1, feature.featureKey());
        statement.setString(2, feature.projectKey());
        statement.setString(3, feature.moduleName());
        statement.setString(4, feature.title());
        statement.setString(5, feature.description());
        statement.setString(6, feature.tags());
        statement.setString(7, feature.status());
        statement.setString(8, feature.sourceKind());
        statement.setString(9, feature.createdAt());
        statement.setString(10, feature.updatedAt());
    }

    private BddFeature map(ResultSet resultSet) throws SQLException {
        return new BddFeature(
                resultSet.getString("feature_key"),
                resultSet.getString("project_key"),
                resultSet.getString("module_name"),
                resultSet.getString("title"),
                resultSet.getString("description"),
                resultSet.getString("tags"),
                resultSet.getString("status"),
                resultSet.getString("source_kind"),
                resultSet.getString("created_at"),
                resultSet.getString("updated_at"));
    }
}
