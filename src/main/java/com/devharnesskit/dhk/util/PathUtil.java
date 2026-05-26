package com.devharnesskit.dhk.util;

import com.devharnesskit.dhk.cli.Args;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PathUtil {
    public static final String AGENTS_DIRECTORY = ".agents";
    public static final String DEVHARNESS_DIRECTORY = "devharness";
    public static final String MEMORY_DIRECTORY = "memory";
    public static final String GRAPH_DIRECTORY = "graph";
    public static final String EXPORTS_DIRECTORY = "exports";
    public static final String ARTIFACTS_DIRECTORY = "artifacts";
    public static final String BACKUPS_DIRECTORY = "backups";
    public static final String SNAPSHOTS_DIRECTORY = "snapshots";
    public static final String CACHE_DIRECTORY = "cache";
    public static final String PROJECT_JSON = "project.json";
    public static final String MEMORY_DB = "memory.db";
    public static final String PROJECT_INDEX = "PROJECT_INDEX.md";
    public static final String GRAPH_CONFIG_JSON = "config.json";
    public static final String GRAPH_INDEX_REPORT = "GRAPH_INDEX_REPORT.md";
    public static final String IMPACT_MAP = "IMPACT_MAP.md";
    public static final String CURRENT_CONTEXT = "CURRENT_CONTEXT.md";
    public static final String RECOVERY_CONTEXT = "RECOVERY_CONTEXT.md";
    public static final String WORKFLOW_CONTEXT = "WORKFLOW_CONTEXT.md";
    public static final String SPEC_CONTEXT = "SPEC_CONTEXT.md";
    public static final String GOAL_CONTEXT = "GOAL_CONTEXT.md";
    public static final String GOAL_SUMMARY = "GOAL_SUMMARY.md";
    public static final String POLICY_JSON = "policy.json";
    public static final String SENSITIVE_POLICY_JSON = "sensitive-policy.json";
    public static final String GOAL_PROFILES_DIRECTORY = "goal-profiles";
    public static final String GOAL_CHECK_POLICY_JSON = "goal-check-policy.json";

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

    public static Path devharnessDirectory(Path projectRoot) {
        return projectRoot.resolve(AGENTS_DIRECTORY).resolve(DEVHARNESS_DIRECTORY);
    }

    public static Path graphDirectory(Path projectRoot) {
        return projectRoot.resolve(AGENTS_DIRECTORY).resolve(GRAPH_DIRECTORY);
    }

    public static Path graphConfig(Path projectRoot) {
        return graphDirectory(projectRoot).resolve(GRAPH_CONFIG_JSON);
    }

    public static Path graphExportsDirectory(Path projectRoot) {
        return graphDirectory(projectRoot).resolve(EXPORTS_DIRECTORY);
    }

    public static Path graphSnapshotsDirectory(Path projectRoot) {
        return graphDirectory(projectRoot).resolve(SNAPSHOTS_DIRECTORY);
    }

    public static Path graphCacheDirectory(Path projectRoot) {
        return graphDirectory(projectRoot).resolve(CACHE_DIRECTORY);
    }

    public static Path graphIndexReport(Path projectRoot) {
        return graphExportsDirectory(projectRoot).resolve(GRAPH_INDEX_REPORT);
    }

    public static Path graphImpactMap(Path projectRoot) {
        return graphExportsDirectory(projectRoot).resolve(IMPACT_MAP);
    }

    public static Path sensitivePolicy(Path projectRoot) {
        return devharnessDirectory(projectRoot).resolve(SENSITIVE_POLICY_JSON);
    }

    public static Path devharnessPolicy(Path projectRoot) {
        return devharnessDirectory(projectRoot).resolve(POLICY_JSON);
    }

    public static Path goalProfilesDirectory(Path projectRoot) {
        return devharnessDirectory(projectRoot).resolve(GOAL_PROFILES_DIRECTORY);
    }

    public static Path goalProfile(Path projectRoot, String profileKey) {
        return goalProfilesDirectory(projectRoot).resolve(profileKey + ".json");
    }

    public static Path goalCheckPolicy(Path projectRoot) {
        return devharnessDirectory(projectRoot).resolve(GOAL_CHECK_POLICY_JSON);
    }

    public static Path exportsDirectory(Path projectRoot) {
        return memoryDirectory(projectRoot).resolve(EXPORTS_DIRECTORY);
    }

    public static Path artifactsDirectory(Path projectRoot) {
        return memoryDirectory(projectRoot).resolve(ARTIFACTS_DIRECTORY);
    }

    public static Path goalArtifactsDirectory(Path projectRoot, String goalKey) {
        return artifactsDirectory(projectRoot).resolve("goals").resolve(goalKey);
    }

    public static Path goalCheckArtifactsDirectory(Path projectRoot, String goalKey) {
        return goalArtifactsDirectory(projectRoot, goalKey).resolve("checks");
    }

    public static Path backupsDirectory(Path projectRoot) {
        return memoryDirectory(projectRoot).resolve(BACKUPS_DIRECTORY);
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

    public static Path specContext(Path projectRoot) {
        return exportsDirectory(projectRoot).resolve(SPEC_CONTEXT);
    }

    public static Path goalContext(Path projectRoot) {
        return exportsDirectory(projectRoot).resolve(GOAL_CONTEXT);
    }

    public static Path goalSummary(Path projectRoot) {
        return exportsDirectory(projectRoot).resolve(GOAL_SUMMARY);
    }

    public static void createMemoryDirectories(Path projectRoot) {
        try {
            Files.createDirectories(devharnessDirectory(projectRoot));
            Files.createDirectories(exportsDirectory(projectRoot));
            Files.createDirectories(artifactsDirectory(projectRoot));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to create memory directories: " + ex.getMessage(), ex);
        }
    }

    public static void createGraphDirectories(Path projectRoot) {
        try {
            Files.createDirectories(graphDirectory(projectRoot));
            Files.createDirectories(graphExportsDirectory(projectRoot));
            Files.createDirectories(graphSnapshotsDirectory(projectRoot));
            Files.createDirectories(graphCacheDirectory(projectRoot));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to create graph directories: " + ex.getMessage(), ex);
        }
    }
}
