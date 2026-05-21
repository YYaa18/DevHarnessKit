package com.devharnesskit.dhk.util;

import com.devharnesskit.dhk.cli.Args;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PathUtil {
    public static final String AGENTS_DIRECTORY = ".agents";
    public static final String MEMORY_DIRECTORY = "memory";
    public static final String EXPORTS_DIRECTORY = "exports";
    public static final String PROJECT_JSON = "project.json";
    public static final String MEMORY_DB = "memory.db";
    public static final String PROJECT_INDEX = "PROJECT_INDEX.md";
    public static final String CURRENT_CONTEXT = "CURRENT_CONTEXT.md";
    public static final String RECOVERY_CONTEXT = "RECOVERY_CONTEXT.md";
    public static final String WORKFLOW_CONTEXT = "WORKFLOW_CONTEXT.md";

    private PathUtil() {
    }

    public static Path resolveProjectRoot(Args args, Path workingDirectory) {
        String configuredRoot = args.option("project-root", "");
        if (configuredRoot.length() == 0) {
            return workingDirectory.toAbsolutePath().normalize();
        }
        return resolvePath(configuredRoot, workingDirectory);
    }

    public static Path resolvePath(String value, Path workingDirectory) {
        Path path = workingDirectory.getFileSystem().getPath(value);
        if (!path.isAbsolute()) {
            path = workingDirectory.resolve(path);
        }
        return path.toAbsolutePath().normalize();
    }

    public static Path memoryDirectory(Path projectRoot) {
        return projectRoot.resolve(AGENTS_DIRECTORY).resolve(MEMORY_DIRECTORY);
    }

    public static Path exportsDirectory(Path projectRoot) {
        return memoryDirectory(projectRoot).resolve(EXPORTS_DIRECTORY);
    }

    public static Path projectJson(Path projectRoot) {
        return memoryDirectory(projectRoot).resolve(PROJECT_JSON);
    }

    public static Path memoryDb(Path projectRoot) {
        return memoryDirectory(projectRoot).resolve(MEMORY_DB);
    }

    public static Path projectIndex(Path projectRoot) {
        return exportsDirectory(projectRoot).resolve(PROJECT_INDEX);
    }

    public static Path currentContext(Path projectRoot) {
        return exportsDirectory(projectRoot).resolve(CURRENT_CONTEXT);
    }

    public static Path recoveryContext(Path projectRoot) {
        return exportsDirectory(projectRoot).resolve(RECOVERY_CONTEXT);
    }

    public static Path workflowContext(Path projectRoot) {
        return exportsDirectory(projectRoot).resolve(WORKFLOW_CONTEXT);
    }

    public static void createMemoryDirectories(Path projectRoot) {
        try {
            Files.createDirectories(exportsDirectory(projectRoot));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to create memory directories: " + ex.getMessage(), ex);
        }
    }
}
