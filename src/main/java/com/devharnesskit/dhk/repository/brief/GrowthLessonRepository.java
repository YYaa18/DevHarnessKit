package com.devharnesskit.dhk.repository.brief;

import com.devharnesskit.dhk.model.brief.GrowthLesson;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class GrowthLessonRepository {
    public void upsert(Connection connection, String projectKey, GrowthLesson lesson,
                       String now) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO growth_lesson(lesson_id, project_key, source_candidate_id, title, "
                        + "summary, status, advisory_only, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) "
                        + "ON CONFLICT(lesson_id) DO UPDATE SET "
                        + "project_key = excluded.project_key, "
                        + "source_candidate_id = excluded.source_candidate_id, "
                        + "title = excluded.title, "
                        + "summary = excluded.summary, "
                        + "status = excluded.status, "
                        + "advisory_only = excluded.advisory_only, "
                        + "updated_at = excluded.updated_at")) {
            statement.setString(1, lesson.lessonId());
            statement.setString(2, projectKey);
            statement.setString(3, lesson.sourceCandidateId());
            statement.setString(4, lesson.title());
            statement.setString(5, lesson.summary());
            statement.setString(6, lesson.status());
            statement.setInt(7, lesson.advisoryOnly() ? 1 : 0);
            statement.setString(8, now);
            statement.setString(9, now);
            statement.executeUpdate();
        }
    }

    public List<GrowthLesson> list(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM growth_lesson ORDER BY updated_at DESC, lesson_id ASC");
             ResultSet resultSet = statement.executeQuery()) {
            List<GrowthLesson> result = new ArrayList<GrowthLesson>();
            while (resultSet.next()) {
                result.add(map(resultSet));
            }
            return result;
        }
    }

    private GrowthLesson map(ResultSet resultSet) throws SQLException {
        return new GrowthLesson(
                resultSet.getString("lesson_id"),
                resultSet.getString("source_candidate_id"),
                resultSet.getString("title"),
                resultSet.getString("summary"),
                resultSet.getString("status"),
                resultSet.getInt("advisory_only") == 1);
    }
}
