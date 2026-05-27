package com.devharnesskit.dhk.util;

import com.devharnesskit.dhk.cli.Args;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PathUtil {
    public static final String AGENTS_DIRECTORY = ".agents";
    public static final String DEVHARNESS_DIRECTORY = "devharness";
    public static final String SKILLS_DIRECTORY = "skills";
    public static final String MEMORY_DIRECTORY = "memory";
    public static final String GRAPH_DIRECTORY = "graph";
    public static final String BDD_DIRECTORY = "bdd";
    public static final String FEATURES_DIRECTORY = "features";
    public static final String EVIDENCE_DIRECTORY = "evidence";
    public static final String EXPORTS_DIRECTORY = "exports";
    public static final String ARTIFACTS_DIRECTORY = "artifacts";
    public static final String BACKUPS_DIRECTORY = "backups";
    public static final String SNAPSHOTS_DIRECTORY = "snapshots";
    public static final String CACHE_DIRECTORY = "cache";
    public static final String PROJECT_JSON = "project.json";
    public static final String MEMORY_DB = "memory.db";
    public static final String PROJECT_INDEX = "PROJECT_INDEX.md";
    public static final String GRAPH_CONFIG_JSON = "config.json";
    public static final String GRAPH_ARCHITECTURE_JSON = "architecture.json";
    public static final String GRAPH_INDEX_REPORT = "GRAPH_INDEX_REPORT.md";
    public static final String IMPACT_MAP = "IMPACT_MAP.md";
    public static final String GRAPH_CONTEXT = "GRAPH_CONTEXT.md";
    public static final String GRAPH_SNAPSHOT_JSON = "GRAPH_SNAPSHOT.json";
    public static final String BDD_CONTEXT = "BDD_CONTEXT.md";
    public static final String BDD_EVIDENCE = "BDD_EVIDENCE.md";
    public static final String BDD_COVERAGE = "BDD_COVERAGE.md";
    public static final String SCENARIO_IMPACT_MAP = "SCENARIO_IMPACT_MAP.md";
    public static final String CURRENT_CONTEXT = "CURRENT_CONTEXT.md";
    public static final String RECOVERY_CONTEXT = "RECOVERY_CONTEXT.md";
    public static final String WORKFLOW_CONTEXT = "WORKFLOW_CONTEXT.md";
    public static final String SPEC_CONTEXT = "SPEC_CONTEXT.md";
    public static final String GOAL_CONTEXT = "GOAL_CONTEXT.md";
    public static final String GOAL_SUMMARY = "GOAL_SUMMARY.md";
    public static final String ARTIFACT_PASSPORT_JSON = "ARTIFACT_PASSPORT.json";
    public static final String POLICY_JSON = "policy.json";
    public static final String DEVHARNESS_CONFIG_JSON = "config.json";
    public static final String SENSITIVE_POLICY_JSON = "sensitive-policy.json";
    public static final String GOAL_PROFILES_DIRECTORY = "goal-profiles";
    public static final String GOAL_CHECK_POLICY_JSON = "goal-check-policy.json";
    public static final String CONTRACT_JSON = "contract.json";

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

    public static Path bddDirectory(Path projectRoot) {
        return projectRoot.resolve(AGENTS_DIRECTORY).resolve(BDD_DIRECTORY);
    }

    public static Path skillsDirectory(Path projectRoot) {
        return projectRoot.resolve(AGENTS_DIRECTORY).resolve(SKILLS_DIRECTORY);
    }

    public static Path skillDirectory(Path projectRoot, String skillKey) {
        return skillsDirectory(projectRoot).resolve(skillKey);
    }

    public static Path skillContract(Path projectRoot, String skillKey) {
        return skillDirectory(projectRoot, skillKey).resolve(CONTRACT_JSON);
    }

    public static Path graphConfig(Path projectRoot) {
        return graphDirectory(projectRoot).resolve(GRAPH_CONFIG_JSON);
    }

    public static Path graphArchitectureConfig(Path projectRoot) {
        return graphDirectory(projectRoot).resolve(GRAPH_ARCHITECTURE_JSON);
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

    public static Path graphContext(Path projectRoot) {
        return graphExportsDirectory(projectRoot).resolve(GRAPH_CONTEXT);
    }

    public static Path graphSnapshotJson(Path projectRoot) {
        return graphExportsDirectory(projectRoot).resolve(GRAPH_SNAPSHOT_JSON);
    }

    public static Path bddFeaturesDirectory(Path projectRoot) {
        return bddDirectory(projectRoot).resolve(FEATURES_DIRECTORY);
    }

    public static Path bddEvidenceDirectory(Path projectRoot) {
        return bddDirectory(projectRoot).resolve(EVIDENCE_DIRECTORY);
    }

    public static Path bddExportsDirectory(Path projectRoot) {
        return bddDirectory(projectRoot).resolve(EXPORTS_DIRECTORY);
    }

    public static Path bddContext(Path projectRoot) {
        return bddExportsDirectory(projectRoot).resolve(BDD_CONTEXT);
    }

    public static Path bddEvidence(Path projectRoot) {
        return bddExportsDirectory(projectRoot).resolve(BDD_EVIDENCE);
    }

    public static Path bddCoverage(Path projectRoot) {
        return bddExportsDirectory(projectRoot).resolve(BDD_COVERAGE);
    }

    public static Path scenarioImpactMap(Path projectRoot) {
        return bddExportsDirectory(projectRoot).resolve(SCENARIO_IMPACT_MAP);
    }

    public static Path sensitivePolicy(Path projectRoot) {
        return devharnessDirectory(projectRoot).resolve(SENSITIVE_POLICY_JSON);
    }

    public static Path devharnessPolicy(Path projectRoot) {
        return devharnessDirectory(projectRoot).resolve(POLICY_JSON);
    }

    public static Path devharnessConfig(Path projectRoot) {
        return devharnessDirectory(projectRoot).resolve(DEVHARNESS_CONFIG_JSON);
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

    public static Path artifactPassport(Path projectRoot) {
        return exportsDirectory(projectRoot).resolve(ARTIFACT_PASSPORT_JSON);
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

    public static void createBddDirectories(Path projectRoot) {
        try {
            Files.createDirectories(bddDirectory(projectRoot));
            Files.createDirectories(bddFeaturesDirectory(projectRoot));
            Files.createDirectories(bddEvidenceDirectory(projectRoot));
            Files.createDirectories(bddExportsDirectory(projectRoot));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to create BDD directories: " + ex.getMessage(), ex);
        }
    }

    public static void createSkillDirectories(Path projectRoot) {
        try {
            Files.createDirectories(skillsDirectory(projectRoot));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to create skill directories: " + ex.getMessage(), ex);
        }
    }
}
