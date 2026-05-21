package com.devharnesskit.dhk.command.memory;

import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

final class MemoryCommandSupport {
    private MemoryCommandSupport() {
    }

    static Project requireInitializedProject(CommandContext context, Path projectRoot, ProjectService projectService,
                                             Connection connection) throws SQLException {
        Path projectJson = PathUtil.projectJson(projectRoot);
        if (!Files.isRegularFile(projectJson)) {
            context.err().println("Memory project is not initialized: missing " + projectJson);
            return null;
        }
        if (MigrationRunner.currentSchemaVersion(connection) < MigrationRunner.V1) {
            context.err().println("Memory database is not initialized: schema v1 missing");
            return null;
        }
        return projectService.readProject(projectJson);
    }
}
