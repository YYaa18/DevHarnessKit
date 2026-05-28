package com.devharnesskit.dhk.db;

import com.devharnesskit.dhk.db.migration.DefaultMigrationStepCatalog;
import com.devharnesskit.dhk.db.migration.MigrationStep;
import com.devharnesskit.dhk.util.Clock;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MigrationRunner {
    public static final int V1 = 1;
    public static final int V2 = 2;
    public static final int V3 = 3;
    public static final int V4 = 4;
    public static final int V5 = 5;
    public static final int V6 = 6;
    public static final int V7 = 7;
    public static final int V8 = 8;
    public static final int V9 = 9;
    public static final int V10 = 10;
    public static final int V11 = 11;
    public static final int V12 = 12;
    public static final int V13 = 13;
    public static final int V14 = 14;

    private final List<MigrationStep> steps;
    private final MigrationBackupCoordinator backupCoordinator;

    public MigrationRunner() {
        this(DefaultMigrationStepCatalog.steps());
    }

    MigrationRunner(List<MigrationStep> steps) {
        this(steps, new MigrationBackupCoordinator());
    }

    MigrationRunner(List<MigrationStep> steps, MigrationBackupCoordinator backupCoordinator) {
        this.steps = Collections.unmodifiableList(new ArrayList<MigrationStep>(steps));
        this.backupCoordinator = backupCoordinator;
    }

    public MigrationResult migrate(Connection connection, Clock clock) throws SQLException {
        String backupPath = backupCoordinator.backupBeforeUpgrade(connection, clock, targetSchemaVersion());
        boolean originalAutoCommit = connection.getAutoCommit();
        if (originalAutoCommit) {
            connection.setAutoCommit(false);
        }
        try {
            for (MigrationStep step : steps) {
                step.apply(connection, clock);
            }
            if (originalAutoCommit) {
                connection.commit();
            }
        } catch (SQLException ex) {
            if (originalAutoCommit) {
                connection.rollback();
            }
            throw ex;
        } catch (RuntimeException ex) {
            if (originalAutoCommit) {
                connection.rollback();
            }
            throw ex;
        } finally {
            if (originalAutoCommit) {
                connection.setAutoCommit(true);
            }
        }

        boolean ftsAvailable = true;
        String ftsError = "";
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE VIRTUAL TABLE IF NOT EXISTS memory_fts USING fts5(title, content, tags)");
        } catch (SQLException ex) {
            ftsAvailable = false;
            ftsError = ex.getMessage();
        }
        return new MigrationResult(currentSchemaVersion(connection), ftsAvailable, ftsError, backupPath);
    }

    private int targetSchemaVersion() {
        int target = 0;
        for (MigrationStep step : steps) {
            if (step.version() > target) {
                target = step.version();
            }
        }
        return target;
    }

    public static int currentSchemaVersion(Connection connection) throws SQLException {
        return MigrationSchema.currentSchemaVersion(connection);
    }

    public static boolean hasTable(Connection connection, String tableName) throws SQLException {
        return MigrationSchema.hasTable(connection, tableName);
    }
}
