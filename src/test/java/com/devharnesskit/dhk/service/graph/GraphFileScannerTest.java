package com.devharnesskit.dhk.service.graph;

import com.devharnesskit.dhk.model.graph.GraphConfig;
import com.devharnesskit.dhk.model.graph.GraphFileEntry;
import com.devharnesskit.dhk.model.graph.GraphScanReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GraphFileScannerTest {
    @TempDir
    Path tempDir;

    @Test
    void scansSupportedLanguagesAndSkipsRiskyFiles() throws Exception {
        write("src/main/java/App.java", "class App {}\n");
        write("src/main/resources/mapper.xml", "<mapper />\n");
        write("src/main/webapp/WEB-INF/view.jsp", "<html />\n");
        write("src/main/resources/application.properties", "app=true\n");
        write("src/main/resources/query.sql", "select 1\n");
        write("src/main/resources/secret.properties", "token=example\n");
        write("src/main/java/lib.jar", "fake\n");
        write("target/classes/App.class", "fake\n");
        write("node_modules/pkg/index.js", "fake\n");
        write("src/main/resources/large.sql", "012345678901234567890123456789\n");

        GraphConfig config = new GraphConfig("lite", GraphConfig.defaults().include(),
                GraphConfig.defaults().exclude(), 20, 10, 3, 500, false);

        GraphScanReport report = new GraphFileScanner().scan(tempDir, config);

        assertEquals(5, report.indexedFiles());
        assertTrue(report.languageCounts().containsKey("java"));
        assertTrue(report.languageCounts().containsKey("xml"));
        assertTrue(report.languageCounts().containsKey("jsp"));
        assertTrue(report.languageCounts().containsKey("properties"));
        assertTrue(report.languageCounts().containsKey("sql"));
        assertSkipped(report, "src/main/resources/secret.properties", "sensitive_filename");
        assertSkipped(report, "src/main/java/lib.jar", "excluded");
        assertSkipped(report, "src/main/resources/large.sql", "max_file_bytes");
    }

    @Test
    void enforcesMaxIndexedFiles() throws Exception {
        write("src/main/java/A.java", "class A {}\n");
        write("src/main/java/B.java", "class B {}\n");

        GraphConfig config = new GraphConfig("lite", Arrays.asList("src/main/java/**"),
                GraphConfig.defaults().exclude(), 1024, 1, 3, 500, false);

        GraphScanReport report = new GraphFileScanner().scan(tempDir, config);

        assertEquals(1, report.indexedFiles());
        assertEquals(1, report.skippedFiles());
        assertTrue(hasSkipReason(report, "max_indexed_files"));
    }

    private void write(String relativePath, String content) throws Exception {
        Path file = tempDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes("UTF-8"));
    }

    private void assertSkipped(GraphScanReport report, String path, String reason) {
        for (GraphFileEntry entry : report.entries()) {
            if (path.equals(entry.relativePath())) {
                assertEquals(reason, entry.skipReason());
                return;
            }
        }
        throw new AssertionError("Missing skipped path: " + path);
    }

    private boolean hasSkipReason(GraphScanReport report, String reason) {
        for (GraphFileEntry entry : report.entries()) {
            if (reason.equals(entry.skipReason())) {
                return true;
            }
        }
        return false;
    }
}
