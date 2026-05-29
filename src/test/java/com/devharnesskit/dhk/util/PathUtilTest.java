package com.devharnesskit.dhk.util;

import com.devharnesskit.dhk.cli.Args;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PathUtilTest {
    @TempDir
    Path tempDir;

    @Test
    void defaultsProjectRootToWorkingDirectory() {
        Args args = Args.parse(new String[]{"memory", "init"});

        Path root = PathUtil.resolveProjectRoot(args, tempDir);

        assertEquals(tempDir.toAbsolutePath().normalize(), root);
    }

    @Test
    void resolvesRelativeProjectRootAgainstWorkingDirectory() {
        Args args = Args.parse(new String[]{"memory", "init", "--project-root", "path with spaces"});

        Path root = PathUtil.resolveProjectRoot(args, tempDir);

        assertEquals(tempDir.resolve("path with spaces").toAbsolutePath().normalize(), root);
    }

    @Test
    void displayPathUsesForwardSlashes() {
        String display = PathUtil.displayPath(tempDir.resolve("C:\\demo\\contract.json"));

        assertFalse(display.contains("\\"));
        assertTrue(display.endsWith("C:/demo/contract.json"));
    }

    @Test
    void locatesMemoryPaths() {
        Path root = tempDir.resolve("demo");

        assertEquals(root.resolve(".agents/memory"), PathUtil.memoryDirectory(root));
        assertEquals(root.resolve(".agents/devharness"), PathUtil.devharnessDirectory(root));
        assertEquals(root.resolve(".agents/devharness/policy.json"), PathUtil.devharnessPolicy(root));
        assertEquals(root.resolve(".agents/devharness/sensitive-policy.json"), PathUtil.sensitivePolicy(root));
        assertEquals(root.resolve(".agents/devharness/goal-check-policy.json"), PathUtil.goalCheckPolicy(root));
        assertEquals(root.resolve(".agents/devharness/goal-profiles/java-api-change.json"), PathUtil.goalProfile(root, "java-api-change"));
        assertEquals(root.resolve(".agents/memory/exports"), PathUtil.exportsDirectory(root));
        assertEquals(root.resolve(".agents/memory/artifacts"), PathUtil.artifactsDirectory(root));
        assertEquals(root.resolve(".agents/memory/artifacts/goals/goal-1/checks"), PathUtil.goalCheckArtifactsDirectory(root, "goal-1"));
        assertEquals(root.resolve(".agents/memory/project.json"), PathUtil.projectJson(root));
        assertEquals(root.resolve(".agents/memory/memory.db"), PathUtil.memoryDb(root));
        assertEquals(root.resolve(".agents/memory/exports/GOAL_SUMMARY.md"), PathUtil.goalSummary(root));
    }

    @Test
    void createsMemoryAndExportDirectories() {
        Path root = tempDir.resolve("demo");

        PathUtil.createMemoryDirectories(root);

        assertTrue(Files.isDirectory(root.resolve(".agents/memory")));
        assertTrue(Files.isDirectory(root.resolve(".agents/devharness")));
        assertTrue(Files.isDirectory(root.resolve(".agents/memory/exports")));
        assertTrue(Files.isDirectory(root.resolve(".agents/memory/artifacts")));
    }
}
