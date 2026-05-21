package com.devharnesskit.dhk.repository;

import com.devharnesskit.dhk.model.Project;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class ProjectRepository {
    public Project findByKey(Connection connection, String projectKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT project_key, project_name, root_path, project_type, language, framework, "
                        + "database_type, created_at, updated_at FROM project WHERE project_key = ?")) {
            statement.setString(1, projectKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return mapProject(resultSet);
            }
        }
    }

    public void upsert(Connection connection, Project project) throws SQLException {
        int updated;
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE project SET project_name = ?, root_path = ?, project_type = ?, language = ?, "
                        + "framework = ?, database_type = ?, updated_at = ? WHERE project_key = ?")) {
            statement.setString(1, project.projectName());
            statement.setString(2, project.rootPath());
            statement.setString(3, project.projectType());
            statement.setString(4, project.language());
            statement.setString(5, project.framework());
            statement.setString(6, project.databaseType());
            statement.setString(7, project.updatedAt());
            statement.setString(8, project.projectKey());
            updated = statement.executeUpdate();
        }
        if (updated > 0) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO project(project_key, project_name, root_path, project_type, language, framework, "
                        + "database_type, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, project.projectKey());
            statement.setString(2, project.projectName());
            statement.setString(3, project.rootPath());
            statement.setString(4, project.projectType());
            statement.setString(5, project.language());
            statement.setString(6, project.framework());
            statement.setString(7, project.databaseType());
            statement.setString(8, project.createdAt());
            statement.setString(9, project.updatedAt());
            statement.executeUpdate();
        }
    }

    private Project mapProject(ResultSet resultSet) throws SQLException {
        return new Project(
                resultSet.getString("project_key"),
                resultSet.getString("project_name"),
                resultSet.getString("root_path"),
                resultSet.getString("project_type"),
                resultSet.getString("language"),
                resultSet.getString("framework"),
                resultSet.getString("database_type"),
                resultSet.getString("created_at"),
                resultSet.getString("updated_at")
        );
    }
}
