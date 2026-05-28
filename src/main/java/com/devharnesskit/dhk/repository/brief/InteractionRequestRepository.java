package com.devharnesskit.dhk.repository.brief;

import com.devharnesskit.dhk.model.brief.InteractionRequest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class InteractionRequestRepository {
    public void upsert(Connection connection, String projectKey, InteractionRequest request,
                       String now) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO interaction_request(request_id, project_key, goal_key, phase, "
                        + "interaction_type, priority, question, why, choices, default_choice, "
                        + "blocks_progress, status, answer, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                        + "ON CONFLICT(request_id) DO UPDATE SET "
                        + "project_key = excluded.project_key, "
                        + "goal_key = excluded.goal_key, "
                        + "phase = excluded.phase, "
                        + "interaction_type = excluded.interaction_type, "
                        + "priority = excluded.priority, "
                        + "question = excluded.question, "
                        + "why = excluded.why, "
                        + "choices = excluded.choices, "
                        + "default_choice = excluded.default_choice, "
                        + "blocks_progress = excluded.blocks_progress, "
                        + "status = excluded.status, "
                        + "answer = excluded.answer, "
                        + "updated_at = excluded.updated_at")) {
            statement.setString(1, request.requestId());
            statement.setString(2, projectKey);
            statement.setString(3, request.goalKey());
            statement.setString(4, request.phase());
            statement.setString(5, request.type());
            statement.setString(6, request.priority());
            statement.setString(7, request.question());
            statement.setString(8, request.why());
            statement.setString(9, request.choices());
            statement.setString(10, request.defaultChoice());
            statement.setInt(11, request.blocksProgress() ? 1 : 0);
            statement.setString(12, request.status());
            statement.setString(13, request.answer());
            statement.setString(14, now);
            statement.setString(15, now);
            statement.executeUpdate();
        }
    }

    public List<InteractionRequest> list(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM interaction_request ORDER BY updated_at DESC, request_id ASC");
             ResultSet resultSet = statement.executeQuery()) {
            List<InteractionRequest> result = new ArrayList<InteractionRequest>();
            while (resultSet.next()) {
                result.add(map(resultSet));
            }
            return result;
        }
    }

    private InteractionRequest map(ResultSet resultSet) throws SQLException {
        return new InteractionRequest(
                resultSet.getString("request_id"),
                resultSet.getString("goal_key"),
                resultSet.getString("phase"),
                resultSet.getString("interaction_type"),
                resultSet.getString("priority"),
                resultSet.getString("question"),
                resultSet.getString("why"),
                resultSet.getString("choices"),
                resultSet.getString("default_choice"),
                resultSet.getInt("blocks_progress") == 1,
                resultSet.getString("status"),
                resultSet.getString("answer"));
    }
}
