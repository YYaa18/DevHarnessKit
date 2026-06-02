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
    void gitWorkspaceFingerprintIgnoresUntrackedGeneratedHarnessArtifacts() throws Exception {
        // Regression: in a git repo WITHOUT a .gitignore for .agents/, generated harness
        // artifacts (graph snapshots/exports, memory db, bdd exports) appear as untracked
        // content. They must not change the workspace fingerprint, otherwise a freshly
        // created graph snapshot self-invalidates on the very next command.
        WorkspaceFingerprintService service = new WorkspaceFingerprintService();
        Path root = tempDir.resolve("git-untracked-workspace");
        Path source = root.resolve("src/main/java/demo/App.java");
        Files.createDirectories(source.getParent());
        Files.write(source, "class App {}\n".getBytes("UTF-8"));
        org.junit.jupiter.api.Assumptions.assumeTrue(runGit(root, "init"));

        String initial = service.workspaceFingerprint(root);
        assertTrue(initial.startsWith("git:"));

        // Generated artifacts under the excluded harness directories (all untracked).
        writeFile(root, ".agents/graph/snapshots/graph-snap-1.json", "{\"nodes\":13}\n");
        writeFile(root, ".agents/graph/exports/GRAPH_SNAPSHOT.json", "{\"snapshot\":\"a\"}\n");
        writeFile(root, ".agents/graph/exports/IMPACT_MAP.md", "# IMPACT_MAP\n");
        writeFile(root, ".agents/graph/cache/query-cache.json", "{}\n");
        writeFile(root, ".agents/memory/memory.db", "binary-ish\n");
        writeFile(root, ".agents/bdd/exports/BDD_EVIDENCE.md", "# BDD_EVIDENCE\n");
        writeFile(root, ".agents/devharness/briefs/WORK_BRIEF.md", "# Work Brief\n");
        assertEquals(initial, service.workspaceFingerprint(root));

        // Re-indexing rewrites the same generated artifacts with new content; still no change.
        writeFile(root, ".agents/graph/snapshots/graph-snap-2.json", "{\"nodes\":14}\n");
        writeFile(root, ".agents/graph/exports/GRAPH_SNAPSHOT.json", "{\"snapshot\":\"b\"}\n");
        assertEquals(initial, service.workspaceFingerprint(root));

        // A real (untracked) source change must still move the fingerprint.
        Files.write(source, "class App { int v = 2; }\n".getBytes("UTF-8"));
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

    private void writeFile(Path root, String relativePath, String content) throws Exception {
        Path file = root.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes("UTF-8"));
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
