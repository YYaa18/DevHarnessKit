package com.devharnesskit.dhk.db;

import com.devharnesskit.dhk.service.MemoryBackupService;
import com.devharnesskit.dhk.util.Clock;
import com.devharnesskit.dhk.util.PathUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

final class MigrationBackupCoordinator {
    String backupBeforeUpgrade(Connection connection, Clock clock, int targetVersion) throws SQLException {
        Path dbPath = MigrationSchema.databasePath(connection);
        if (dbPath == null || !Files.isRegularFile(dbPath)) {
            return "";
        }
        int currentVersion = MigrationSchema.currentSchemaVersion(connection);
        if (currentVersion >= targetVersion) {
            return "";
        }
        try {
            if (Files.size(dbPath) <= 0L) {
                return "";
            }
            Path projectRoot = projectRootFromDatabasePath(dbPath);
            if (projectRoot == null) {
                return "";
            }
            MemoryBackupService backupService = new MemoryBackupService();
            Path out = backupService.defaultBackupPath(projectRoot, clock.now().toString(),
                    "pre-migration-v" + currentVersion + "-to-v" + targetVersion);
            backupService.writeBackup(PathUtil.memoryDirectory(projectRoot), out);
            return out.toString();
        } catch (IOException ex) {
            throw new SQLException("Failed to create pre-migration backup: " + ex.getMessage(), ex);
        }
    }

    private Path projectRootFromDatabasePath(Path dbPath) {
        Path memoryDir = dbPath.getParent();
        if (memoryDir == null || memoryDir.getParent() == null) {
            return null;
        }
        return memoryDir.getParent().getParent();
    }
}
