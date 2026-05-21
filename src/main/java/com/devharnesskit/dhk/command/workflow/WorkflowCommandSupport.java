package com.devharnesskit.dhk.command.workflow;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.repository.ProjectRepository;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

final class WorkflowCommandSupport {
    private WorkflowCommandSupport() {
    }

    static Path projectRoot(Args args, CommandContext context) {
        return PathUtil.resolveProjectRoot(args, context.workingDirectory());
    }

    static Project ensureProject(CommandContext context, Path projectRoot, Connection connection,
                                 ProjectService projectService, ProjectRepository projectRepository,
                                 MigrationRunner migrationRunner) throws SQLException {
        PathUtil.createMemoryDirectories(projectRoot);
        Project project = projectService.ensureProject(projectRoot, context.clock());
        migrationRunner.migrate(connection, context.clock());
        projectRepository.upsert(connection, project);
        return project;
    }

    static boolean isModeAllowed(String mode) {
        return "auto".equals(mode) || "api".equals(mode) || "mvc".equals(mode) || "mixed".equals(mode)
                || "sql".equals(mode) || "debug".equals(mode) || "review".equals(mode);
    }
}
