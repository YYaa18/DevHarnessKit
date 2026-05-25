package com.devharnesskit.dhk.repository.goal;

import com.devharnesskit.dhk.model.goal.GoalEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class GoalEventRepository {
    public long insert(Connection connection, GoalEvent event) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO goal_event(goal_key, event_type, level, message, data, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, event.goalKey());
            statement.setString(2, event.eventType());
            statement.setString(3, event.level());
            statement.setString(4, event.message());
            statement.setString(5, event.data());
            statement.setString(6, event.createdAt());
            statement.executeUpdate();
            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                if (!resultSet.next()) {
                    throw new SQLException("No generated key returned for goal event insert");
                }
                return resultSet.getLong(1);
            }
        }
    }
}
