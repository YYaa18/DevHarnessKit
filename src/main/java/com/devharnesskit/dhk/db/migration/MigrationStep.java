package com.devharnesskit.dhk.db.migration;

import com.devharnesskit.dhk.util.Clock;

import java.sql.Connection;
import java.sql.SQLException;

public interface MigrationStep {
    int version();

    String description();

    void apply(Connection connection, Clock clock) throws SQLException;
}
