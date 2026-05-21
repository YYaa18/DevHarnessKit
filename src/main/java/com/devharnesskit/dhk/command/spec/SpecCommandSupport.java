package com.devharnesskit.dhk.command.spec;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.spec.SpecChange;
import com.devharnesskit.dhk.repository.ProjectRepository;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.service.SensitiveDataGuard;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

final class SpecCommandSupport {
    private SpecCommandSupport() {
    }

    static Path projectRoot(Args args, CommandContext context) {
        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        PathUtil.createMemoryDirectories(projectRoot);
        return projectRoot;
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

    static boolean rejectSensitive(CommandContext context, SensitiveDataGuard guard,
                                   String location, String... values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (value != null && value.length() > 0) {
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append(value);
            }
        }
        List<String> matches = guard.findMatches(builder.toString());
        if (matches.isEmpty()) {
            return false;
        }
        context.err().println("Sensitive data rejected in " + location + ": " + matches);
        return true;
    }

    static boolean isKeyAllowed(String key) {
        return key != null && key.matches("[A-Za-z0-9._-]+");
    }

    static boolean belongsToProject(SpecChange change, Project project) {
        return change != null && project.projectKey().equals(change.projectKey());
    }
}
