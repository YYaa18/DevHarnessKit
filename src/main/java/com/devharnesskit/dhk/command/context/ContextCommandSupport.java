package com.devharnesskit.dhk.command.context;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.repository.ProjectRepository;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;

final class ContextCommandSupport {
    private ContextCommandSupport() {
    }

    static Path projectRoot(Args args, CommandContext context) {
        return PathUtil.resolveProjectRoot(args, context.workingDirectory());
    }

    static Project requireProject(CommandContext context, Path projectRoot, Connection connection)
            throws Exception {
        Path projectJson = PathUtil.projectJson(projectRoot);
        if (!requireProjectJson(context, projectRoot)) {
            return null;
        }
        new MigrationRunner().migrate(connection, context.clock());
        Project project = new ProjectService().readProject(projectJson);
        new ProjectRepository().upsert(connection, project);
        return project;
    }

    static boolean requireProjectJson(CommandContext context, Path projectRoot) {
        Path projectJson = PathUtil.projectJson(projectRoot);
        if (Files.isRegularFile(projectJson)) {
            return true;
        }
        context.err().println("Context artifacts are not initialized: missing " + projectJson);
        context.err().println("Run `dhk memory init` first.");
        return false;
    }

    static int parseLimit(CommandContext context, String rawValue, String example) {
        try {
            int value = Integer.parseInt(rawValue);
            if (value < 1) {
                context.err().println("Invalid limit: " + rawValue + ". Limit must be a positive integer.");
                context.err().println("Example: " + example);
                return -1;
            }
            return Math.min(value, 200);
        } catch (NumberFormatException ex) {
            context.err().println("Invalid limit: " + rawValue + ". Limit must be a positive integer.");
            context.err().println("Example: " + example);
            return -1;
        }
    }

    static int parseNonNegativeInt(CommandContext context, String rawValue, String example) {
        try {
            int value = Integer.parseInt(rawValue);
            if (value < 0) {
                context.err().println("Invalid value: " + rawValue + ". Value must be zero or a positive integer.");
                context.err().println("Example: " + example);
                return -1;
            }
            return value;
        } catch (NumberFormatException ex) {
            context.err().println("Invalid value: " + rawValue + ". Value must be zero or a positive integer.");
            context.err().println("Example: " + example);
            return -1;
        }
    }
}
