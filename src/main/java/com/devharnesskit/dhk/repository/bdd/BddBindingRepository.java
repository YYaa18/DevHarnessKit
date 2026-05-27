package com.devharnesskit.dhk.repository.bdd;

import com.devharnesskit.dhk.model.bdd.BddBinding;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class BddBindingRepository {
    public BddBinding insertOrFind(Connection connection, BddBinding binding) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT OR IGNORE INTO bdd_binding(scenario_key, binding_type, binding_key, relation, metadata, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, binding.scenarioKey());
            statement.setString(2, binding.bindingType());
            statement.setString(3, binding.bindingKey());
            statement.setString(4, binding.relation());
            statement.setString(5, binding.metadata());
            statement.setString(6, binding.createdAt());
            statement.executeUpdate();
        }
        BddBinding existing = findUnique(connection, binding.scenarioKey(), binding.bindingType(),
                binding.bindingKey(), binding.relation());
        if (existing == null) {
            throw new SQLException("BDD binding was not saved: " + binding.scenarioKey());
        }
        return existing;
    }

    public List<BddBinding> listByScenario(Connection connection, String scenarioKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM bdd_binding WHERE scenario_key = ? ORDER BY binding_type, binding_key, relation, id")) {
            statement.setString(1, scenarioKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<BddBinding> results = new ArrayList<BddBinding>();
                while (resultSet.next()) {
                    results.add(map(resultSet));
                }
                return results;
            }
        }
    }

    public List<BddBinding> listByBinding(Connection connection, String bindingType, String bindingKey)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM bdd_binding WHERE binding_type = ? AND binding_key = ? "
                        + "ORDER BY scenario_key, relation, id")) {
            statement.setString(1, bindingType);
            statement.setString(2, bindingKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<BddBinding> results = new ArrayList<BddBinding>();
                while (resultSet.next()) {
                    results.add(map(resultSet));
                }
                return results;
            }
        }
    }

    private BddBinding findUnique(Connection connection, String scenarioKey, String bindingType,
                                  String bindingKey, String relation) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM bdd_binding WHERE scenario_key = ? AND binding_type = ? "
                        + "AND binding_key = ? AND relation = ?")) {
            statement.setString(1, scenarioKey);
            statement.setString(2, bindingType);
            statement.setString(3, bindingKey);
            statement.setString(4, relation);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    private BddBinding map(ResultSet resultSet) throws SQLException {
        return new BddBinding(
                resultSet.getLong("id"),
                resultSet.getString("scenario_key"),
                resultSet.getString("binding_type"),
                resultSet.getString("binding_key"),
                resultSet.getString("relation"),
                resultSet.getString("metadata"),
                resultSet.getString("created_at"));
    }
}
