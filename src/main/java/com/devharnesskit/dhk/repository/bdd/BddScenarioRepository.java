package com.devharnesskit.dhk.repository.bdd;

import com.devharnesskit.dhk.model.bdd.BddScenario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class BddScenarioRepository {
    public void insert(Connection connection, BddScenario scenario) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO bdd_scenario(scenario_key, feature_key, project_key, title, description, "
                        + "scenario_type, priority, status, tags, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            bind(statement, scenario);
            statement.executeUpdate();
        }
    }

    public BddScenario findByKey(Connection connection, String scenarioKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM bdd_scenario WHERE scenario_key = ?")) {
            statement.setString(1, scenarioKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    public List<BddScenario> listByProject(Connection connection, String projectKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM bdd_scenario WHERE project_key = ? ORDER BY feature_key, scenario_key")) {
            statement.setString(1, projectKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAll(resultSet);
            }
        }
    }

    public List<BddScenario> listByFeature(Connection connection, String featureKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM bdd_scenario WHERE feature_key = ? ORDER BY scenario_key")) {
            statement.setString(1, featureKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAll(resultSet);
            }
        }
    }

    private List<BddScenario> mapAll(ResultSet resultSet) throws SQLException {
        List<BddScenario> results = new ArrayList<BddScenario>();
        while (resultSet.next()) {
            results.add(map(resultSet));
        }
        return results;
    }

    private void bind(PreparedStatement statement, BddScenario scenario) throws SQLException {
        statement.setString(1, scenario.scenarioKey());
        statement.setString(2, scenario.featureKey());
        statement.setString(3, scenario.projectKey());
        statement.setString(4, scenario.title());
        statement.setString(5, scenario.description());
        statement.setString(6, scenario.scenarioType());
        statement.setString(7, scenario.priority());
        statement.setString(8, scenario.status());
        statement.setString(9, scenario.tags());
        statement.setString(10, scenario.createdAt());
        statement.setString(11, scenario.updatedAt());
    }

    private BddScenario map(ResultSet resultSet) throws SQLException {
        return new BddScenario(
                resultSet.getString("scenario_key"),
                resultSet.getString("feature_key"),
                resultSet.getString("project_key"),
                resultSet.getString("title"),
                resultSet.getString("description"),
                resultSet.getString("scenario_type"),
                resultSet.getString("priority"),
                resultSet.getString("status"),
                resultSet.getString("tags"),
                resultSet.getString("created_at"),
                resultSet.getString("updated_at"));
    }
}
