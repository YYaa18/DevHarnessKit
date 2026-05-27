package com.devharnesskit.dhk.db;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class MigrationSchema {
    private MigrationSchema() {
    }

    public static int currentSchemaVersion(Connection connection) throws SQLException {
        if (!hasTable(connection, "schema_version")) {
            return 0;
        }
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT MAX(version) FROM schema_version")) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
            return 0;
        }
    }

    public static boolean hasTable(Connection connection, String tableName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT name FROM sqlite_master WHERE type IN ('table', 'virtual table') AND name = ?")) {
            statement.setString(1, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    static Path databasePath(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA database_list")) {
            while (resultSet.next()) {
                String name = resultSet.getString("name");
                if ("main".equals(name)) {
                    String file = resultSet.getString("file");
                    if (file != null && file.length() > 0) {
                        return Paths.get(file);
                    }
                }
            }
            return null;
        }
    }
}
