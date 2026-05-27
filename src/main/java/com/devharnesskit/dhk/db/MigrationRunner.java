package com.devharnesskit.dhk.db;

import com.devharnesskit.dhk.db.migration.MigrationStep;
import com.devharnesskit.dhk.db.migration.V10BddAcceptanceMigration;
import com.devharnesskit.dhk.db.migration.V11SkillContractMigration;
import com.devharnesskit.dhk.db.migration.V12HumanCheckpointMigration;
import com.devharnesskit.dhk.db.migration.V13SkillTrustHardeningMigration;
import com.devharnesskit.dhk.db.migration.V1InitialMemoryMigration;
import com.devharnesskit.dhk.db.migration.V2WorkflowPersistenceMigration;
import com.devharnesskit.dhk.db.migration.V3WorkflowArtifactBindingMigration;
import com.devharnesskit.dhk.db.migration.V4SpecPersistenceMigration;
import com.devharnesskit.dhk.db.migration.V5GoalOrchestrationMigration;
import com.devharnesskit.dhk.db.migration.V6GoalCheckFreshnessMigration;
import com.devharnesskit.dhk.db.migration.V7GoalContextExportFailureMigration;
import com.devharnesskit.dhk.db.migration.V8GoalWorkflowSpecSyncMigration;
import com.devharnesskit.dhk.db.migration.V9GraphLiteMigration;
import com.devharnesskit.dhk.service.MemoryBackupService;
import com.devharnesskit.dhk.util.Clock;
import com.devharnesskit.dhk.util.PathUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
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

    private final List<MigrationStep> steps;

    public MigrationRunner() {
        this(defaultSteps());
    }

    MigrationRunner(List<MigrationStep> steps) {
        this.steps = Collections.unmodifiableList(new ArrayList<MigrationStep>(steps));
    }

    public MigrationResult migrate(Connection connection, Clock clock) throws SQLException {
        String backupPath = backupBeforeUpgrade(connection, clock);
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

    private static List<MigrationStep> defaultSteps() {
        List<MigrationStep> result = new ArrayList<MigrationStep>();
        result.add(new V1InitialMemoryMigration());
        result.add(new V2WorkflowPersistenceMigration());
        result.add(new V3WorkflowArtifactBindingMigration());
        result.add(new V4SpecPersistenceMigration());
        result.add(new V5GoalOrchestrationMigration());
        result.add(new V6GoalCheckFreshnessMigration());
        result.add(new V7GoalContextExportFailureMigration());
        result.add(new V8GoalWorkflowSpecSyncMigration());
        result.add(new V9GraphLiteMigration());
        result.add(new V10BddAcceptanceMigration());
        result.add(new V11SkillContractMigration());
        result.add(new V12HumanCheckpointMigration());
        result.add(new V13SkillTrustHardeningMigration());
        return result;
    }

    private String backupBeforeUpgrade(Connection connection, Clock clock) throws SQLException {
        Path dbPath = databasePath(connection);
        if (dbPath == null || !Files.isRegularFile(dbPath)) {
            return "";
        }
        int currentVersion = currentSchemaVersion(connection);
        if (currentVersion >= V13) {
            return "";
        }
        try {
            if (Files.size(dbPath) <= 0L) {
                return "";
            }
            Path memoryDir = dbPath.getParent();
            if (memoryDir == null) {
                return "";
            }
            Path projectRoot = memoryDir.getParent() == null ? null : memoryDir.getParent().getParent();
            if (projectRoot == null) {
                return "";
            }
            MemoryBackupService backupService = new MemoryBackupService();
            Path out = backupService.defaultBackupPath(projectRoot, clock.now().toString(),
                    "pre-migration-v" + currentVersion + "-to-v" + V13);
            backupService.writeBackup(PathUtil.memoryDirectory(projectRoot), out);
            return out.toString();
        } catch (IOException ex) {
            throw new SQLException("Failed to create pre-migration backup: " + ex.getMessage(), ex);
        }
    }

    private Path databasePath(Connection connection) throws SQLException {
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
        try (java.sql.PreparedStatement statement = connection.prepareStatement(
                "SELECT name FROM sqlite_master WHERE type IN ('table', 'virtual table') AND name = ?")) {
            statement.setString(1, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }
}
