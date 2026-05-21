package com.devharnesskit.dhk.db;

import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class DbConnectionFactory {
    public Connection open(Path projectRoot) throws SQLException {
        Path dbPath = PathUtil.memoryDb(projectRoot);
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toString());
        boolean configured = false;
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 3000");
            configured = true;
            return connection;
        } finally {
            if (!configured) {
                connection.close();
            }
        }
    }
}
