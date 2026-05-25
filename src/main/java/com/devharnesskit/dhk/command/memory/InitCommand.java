package com.devharnesskit.dhk.command.memory;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.MigrationResult;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.repository.ProjectRepository;
import com.devharnesskit.dhk.service.ProjectIndexRenderer;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.util.PathUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

public final class InitCommand implements Command {
    private final DbConnectionFactory connectionFactory;
    private final MigrationRunner migrationRunner;
    private final ProjectService projectService;
    private final ProjectRepository projectRepository;
    private final ProjectIndexRenderer projectIndexRenderer;

    public InitCommand() {
        this(new DbConnectionFactory(), new MigrationRunner(), new ProjectService(),
                new ProjectRepository(), new ProjectIndexRenderer());
    }

    InitCommand(DbConnectionFactory connectionFactory, MigrationRunner migrationRunner,
                ProjectService projectService, ProjectRepository projectRepository,
                ProjectIndexRenderer projectIndexRenderer) {
        this.connectionFactory = connectionFactory;
        this.migrationRunner = migrationRunner;
        this.projectService = projectService;
        this.projectRepository = projectRepository;
        this.projectIndexRenderer = projectIndexRenderer;
    }

    public int run(CommandContext context, Args args) {
        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        try {
            PathUtil.createMemoryDirectories(projectRoot);
            Project project = projectService.ensureProject(projectRoot, context.clock());
            MigrationResult migrationResult;
            try (Connection connection = connectionFactory.open(projectRoot)) {
                migrationResult = migrationRunner.migrate(connection, context.clock());
                projectRepository.upsert(connection, project);
            }
            writeProjectIndex(projectRoot, project);

            context.out().println("memory init complete");
            context.out().println("project_key: " + project.projectKey());
            context.out().println("memory_db: " + PathUtil.memoryDb(projectRoot));
            context.out().println("exports_dir: " + PathUtil.exportsDirectory(projectRoot));
            context.out().println("sensitive_policy: " + PathUtil.sensitivePolicy(projectRoot));
            if (migrationResult.backupPath().length() > 0) {
                context.out().println("migration_backup: " + migrationResult.backupPath());
            }
            context.out().println("fts: " + (migrationResult.ftsAvailable() ? "available" : "fallback"));
            if (!migrationResult.ftsAvailable() && migrationResult.ftsError().length() > 0) {
                context.out().println("fts_reason: " + migrationResult.ftsError());
            }
            return ExitCodes.SUCCESS;
        } catch (SQLException ex) {
            context.err().println("ERROR sqlite init failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        } catch (RuntimeException ex) {
            context.err().println("ERROR memory init failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private void writeProjectIndex(Path projectRoot, Project project) {
        Path index = PathUtil.projectIndex(projectRoot);
        try {
            Files.write(index, projectIndexRenderer.render(project).getBytes("UTF-8"));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write PROJECT_INDEX.md: " + ex.getMessage(), ex);
        }
    }
}
