package com.devharnesskit.dhk.repository.bdd;

import com.devharnesskit.dhk.model.bdd.BddStep;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class BddStepRepository {
    public long insert(Connection connection, BddStep step) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO bdd_step(scenario_key, step_order, step_type, step_text, normalized_text, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, step.scenarioKey());
            statement.setInt(2, step.stepOrder());
            statement.setString(3, step.stepType());
            statement.setString(4, step.stepText());
            statement.setString(5, step.normalizedText());
            statement.setString(6, step.createdAt());
            statement.executeUpdate();
            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                if (!resultSet.next()) {
                    throw new SQLException("No generated key returned for bdd step insert");
                }
                return resultSet.getLong(1);
            }
        }
    }

    public List<BddStep> listByScenario(Connection connection, String scenarioKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM bdd_step WHERE scenario_key = ? ORDER BY step_order, id")) {
            statement.setString(1, scenarioKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<BddStep> results = new ArrayList<BddStep>();
                while (resultSet.next()) {
                    results.add(map(resultSet));
                }
                return results;
            }
        }
    }

    private BddStep map(ResultSet resultSet) throws SQLException {
        return new BddStep(
                resultSet.getLong("id"),
                resultSet.getString("scenario_key"),
                resultSet.getInt("step_order"),
                resultSet.getString("step_type"),
                resultSet.getString("step_text"),
                resultSet.getString("normalized_text"),
                resultSet.getString("created_at"));
    }
}
