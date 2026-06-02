package com.devharnesskit.dhk.command.memory;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.guidance.CommandErrorGuidance;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.service.SensitiveDataGuard;
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

    static int parseLimit(CommandContext context, Args args, String rawValue, String example) {
        try {
            int value = Integer.parseInt(rawValue);
            if (value < 1) {
                CommandErrorGuidance.invalidNumber(context, args, "INVALID_MEMORY_LIMIT",
                        "memory limit", rawValue, "limit must be a positive integer.",
                        example, "README.md#core-path");
                return -1;
            }
            return Math.min(value, 100);
        } catch (NumberFormatException ex) {
            CommandErrorGuidance.invalidNumber(context, args, "INVALID_MEMORY_LIMIT",
                    "memory limit", rawValue, "limit must be a positive integer.",
                    example, "README.md#core-path");
            return -1;
        }
    }

    static long parseId(CommandContext context, Args args, String option, String rawValue, String errorCode,
                        String example) {
        try {
            long value = Long.parseLong(rawValue);
            if (value < 1) {
                CommandErrorGuidance.invalidNumber(context, args, errorCode,
                        option, rawValue, option + " must be a positive integer.",
                        example, "README.md#core-path");
                return -1L;
            }
            return value;
        } catch (NumberFormatException ex) {
            CommandErrorGuidance.invalidNumber(context, args, errorCode,
                    option, rawValue, option + " must be a positive integer.",
                    example, "README.md#core-path");
            return -1L;
        }
    }

    static int parseConfidence(CommandContext context, Args args, String rawValue, String example) {
        try {
            int value = Integer.parseInt(rawValue);
            if (value < 0 || value > 100) {
                CommandErrorGuidance.invalidNumber(context, args, "INVALID_MEMORY_CONFIDENCE",
                        "memory confidence", rawValue, "confidence must be an integer from 0 to 100.",
                        example, "README.md#core-path");
                return -1;
            }
            return value;
        } catch (NumberFormatException ex) {
            CommandErrorGuidance.invalidNumber(context, args, "INVALID_MEMORY_CONFIDENCE",
                    "memory confidence", rawValue, "confidence must be an integer from 0 to 100.",
                    example, "README.md#core-path");
            return -1;
        }
    }

    static boolean rejectSensitive(CommandContext context, String text) {
        java.util.List<String> matches = new SensitiveDataGuard().findMatches(text);
        if (!matches.isEmpty()) {
            context.err().println("Sensitive data rejected: " + matches);
            return true;
        }
        return false;
    }
}
