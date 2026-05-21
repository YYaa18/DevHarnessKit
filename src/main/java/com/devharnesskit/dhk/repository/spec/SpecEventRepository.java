package com.devharnesskit.dhk.repository.spec;

import com.devharnesskit.dhk.model.spec.SpecEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class SpecEventRepository {
    public void insert(Connection connection, SpecEvent event) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO spec_event(project_key, change_key, event_type, level, message, data, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, event.projectKey());
            statement.setString(2, event.changeKey());
            statement.setString(3, event.eventType());
            statement.setString(4, event.level());
            statement.setString(5, event.message());
            statement.setString(6, event.data());
            statement.setString(7, event.createdAt());
            statement.executeUpdate();
        }
    }
}
