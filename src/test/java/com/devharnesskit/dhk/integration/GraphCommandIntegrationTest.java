package com.devharnesskit.dhk.integration;

import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.CommandRouter;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.util.Clock;
import com.devharnesskit.dhk.util.PathUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GraphCommandIntegrationTest {
    @TempDir
    Path tempDir;

    @Test
    void graphInitAndStatusCreateConfigAndReport() throws Exception {
        Path root = tempDir.resolve("demo");
        write(root, "src/main/java/App.java", "class App {}\n");
        write(root, "src/main/resources/mapper.xml", "<mapper />\n");
        write(root, "src/main/webapp/WEB-INF/view.jsp", "<html />\n");
        write(root, "src/main/resources/application.properties", "app=true\n");
        write(root, "src/main/resources/query.sql", "select 1\n");
        write(root, "src/main/resources/secret.properties", "token=example\n");
        write(root, "target/classes/App.class", "fake\n");

        Harness initHarness = new Harness(tempDir);
        int initExit = new CommandRouter().run(new String[]{"graph", "init", "--project-root", "demo"},
                initHarness.context());

        Harness statusHarness = new Harness(tempDir);
        int statusExit = new CommandRouter().run(new String[]{"graph", "status", "--project-root", "demo"},
                statusHarness.context());

        assertEquals(ExitCodes.SUCCESS, initExit);
        assertEquals(ExitCodes.SUCCESS, statusExit);
        assertTrue(Files.isRegularFile(PathUtil.graphConfig(root)));
        assertTrue(Files.isRegularFile(PathUtil.graphIndexReport(root)));
        assertTrue(initHarness.stdout().contains("graph init complete"));
        assertTrue(statusHarness.stdout().contains("indexed_files: 5"));
        assertTrue(statusHarness.stdout().contains("skipped_files: 1"));

        String report = new String(Files.readAllBytes(PathUtil.graphIndexReport(root)), "UTF-8");
        assertTrue(report.contains("GRAPH_INDEX_REPORT"));
        assertTrue(report.contains("src/main/java/App.java"));
        assertTrue(report.contains("src/main/resources/secret.properties [sensitive_filename]"));
    }

    @Test
    void graphStatusSupportsJson() throws Exception {
        Path root = tempDir.resolve("demo-json");
        write(root, "src/main/java/App.java", "class App {}\n");

        Harness harness = new Harness(tempDir);
        int exit = new CommandRouter().run(new String[]{
                "graph", "status", "--project-root", "demo-json", "--json"
        }, harness.context());

        assertEquals(ExitCodes.SUCCESS, exit);
        assertTrue(harness.stdout().contains("\"command\": \"graph status\""));
        assertTrue(harness.stdout().contains("\"indexed_files\": 1"));
    }

    private void write(Path root, String relativePath, String content) throws Exception {
        Path file = root.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes("UTF-8"));
    }

    private static final class Harness {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();
        private final ByteArrayOutputStream err = new ByteArrayOutputStream();
        private final Path workingDirectory;

        private Harness(Path workingDirectory) {
            this.workingDirectory = workingDirectory;
        }

        CommandContext context() {
            return new CommandContext(
                    workingDirectory,
                    new PrintStream(out),
                    new PrintStream(err),
                    new FixedClock()
            );
        }

        String stdout() {
            return out.toString();
        }
    }

    private static final class FixedClock implements Clock {
        public Instant now() {
            return Instant.parse("2026-05-26T00:00:00Z");
        }
    }
}
