package com.devharnesskit.dhk.util;

import com.devharnesskit.dhk.cli.Args;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void locatesMemoryPaths() {
        Path root = tempDir.resolve("demo");

        assertEquals(root.resolve(".agents/memory"), PathUtil.memoryDirectory(root));
        assertEquals(root.resolve(".agents/memory/exports"), PathUtil.exportsDirectory(root));
        assertEquals(root.resolve(".agents/memory/project.json"), PathUtil.projectJson(root));
        assertEquals(root.resolve(".agents/memory/memory.db"), PathUtil.memoryDb(root));
    }

    @Test
    void createsMemoryAndExportDirectories() {
        Path root = tempDir.resolve("demo");

        PathUtil.createMemoryDirectories(root);

        assertTrue(Files.isDirectory(root.resolve(".agents/memory")));
        assertTrue(Files.isDirectory(root.resolve(".agents/memory/exports")));
    }
}
