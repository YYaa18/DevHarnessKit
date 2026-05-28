package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.util.PathUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WorkspaceFingerprintServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void fallbackWorkspaceFingerprintIgnoresGeneratedStateButChangesForSourceFiles() throws Exception {
        WorkspaceFingerprintService service = new WorkspaceFingerprintService();
        Path root = tempDir.resolve("workspace");
        Path source = root.resolve("src/main/java/demo/App.java");
        Files.createDirectories(source.getParent());
        Files.write(source, "class App {}\n".getBytes("UTF-8"));

        String initial = service.workspaceFingerprint(root);
        assertTrue(initial.startsWith("fallback:"));

        Files.createDirectories(root.resolve("target"));
        Files.write(root.resolve("target/generated.txt"), "generated\n".getBytes("UTF-8"));
        Files.createDirectories(PathUtil.exportsDirectory(root));
        Files.write(PathUtil.currentContext(root), "# generated context\n".getBytes("UTF-8"));
        Files.createDirectories(root.resolve(".agents/devharness"));
        Files.write(root.resolve(".agents/devharness/config.json"),
                "{\"verification.compile.mode\":\"manual\"}\n".getBytes("UTF-8"));

        assertEquals(initial, service.workspaceFingerprint(root));

        Files.write(root.resolve(".agents/devharness/config.json"),
                "{\"verification.compile.mode\":\"auto\"}\n".getBytes("UTF-8"));
        assertEquals(initial, service.workspaceFingerprint(root));

        Files.write(source, "class App { int version = 2; }\n".getBytes("UTF-8"));
        String changed = service.workspaceFingerprint(root);
        assertTrue(changed.startsWith("fallback:"));
        assertNotEquals(initial, changed);
    }

    @Test
    void gitWorkspaceFingerprintIgnoresDevharnessConfigButChangesForSourceFiles() throws Exception {
        WorkspaceFingerprintService service = new WorkspaceFingerprintService();
        Path root = tempDir.resolve("git-workspace");
        Path source = root.resolve("src/main/java/demo/App.java");
        Files.createDirectories(source.getParent());
        Files.write(source, "class App {}\n".getBytes("UTF-8"));
        org.junit.jupiter.api.Assumptions.assumeTrue(runGit(root, "init"));

        String initial = service.workspaceFingerprint(root);
        assertTrue(initial.startsWith("git:"));

        Files.createDirectories(root.resolve(".agents/devharness"));
        Files.write(root.resolve(".agents/devharness/config.json"),
                "{\"verification.test.mode\":\"manual\"}\n".getBytes("UTF-8"));
        assertEquals(initial, service.workspaceFingerprint(root));

        Files.write(root.resolve(".agents/devharness/config.json"),
                "{\"verification.test.mode\":\"auto\"}\n".getBytes("UTF-8"));
        assertEquals(initial, service.workspaceFingerprint(root));

        Files.write(source, "class App { int version = 2; }\n".getBytes("UTF-8"));
        assertNotEquals(initial, service.workspaceFingerprint(root));
    }

    @Test
    void contextFingerprintChangesWithGeneratedGoalContext() throws Exception {
        WorkspaceFingerprintService service = new WorkspaceFingerprintService();
        Path root = tempDir.resolve("workspace");
        Files.createDirectories(PathUtil.exportsDirectory(root));

        String empty = service.contextFingerprint(root);
        assertTrue(empty.startsWith("context:"));

        Files.write(PathUtil.goalContext(root), "# GOAL_CONTEXT\nfirst\n".getBytes("UTF-8"));
        String first = service.contextFingerprint(root);
        assertNotEquals(empty, first);

        Files.write(PathUtil.goalContext(root), "# GOAL_CONTEXT\nsecond\n".getBytes("UTF-8"));
        String second = service.contextFingerprint(root);
        assertNotEquals(first, second);
    }

    @Test
    void checkFingerprintIncludesCheckMetadataAndHandlesNulls() {
        WorkspaceFingerprintService service = new WorkspaceFingerprintService();

        String baseline = service.checkFingerprint("compile", "passed", "ok", null,
                "workspace-a", "context-a");
        String same = service.checkFingerprint("compile", "passed", "ok", null,
                "workspace-a", "context-a");
        String changed = service.checkFingerprint("compile", "failed", "compile failed", "compile.log",
                "workspace-a", "context-a");

        assertTrue(baseline.startsWith("check:"));
        assertEquals(baseline, same);
        assertNotEquals(baseline, changed);
    }

    private boolean runGit(Path root, String... args) throws Exception {
        String[] command = new String[args.length + 1];
        command[0] = "git";
        System.arraycopy(args, 0, command, 1, args.length);
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(root.toFile());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        return process.waitFor() == 0;
    }
}
