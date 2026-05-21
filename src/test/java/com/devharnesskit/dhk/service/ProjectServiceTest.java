package com.devharnesskit.dhk.service;

import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.util.Clock;
import com.devharnesskit.dhk.util.PathUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProjectServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void ensureProjectCreatesAndPreservesStableProjectKey() {
        Path root = tempDir.resolve("Project With Spaces");
        PathUtil.createMemoryDirectories(root);
        ProjectService service = new ProjectService();

        Project first = service.ensureProject(root, new FixedClock("2026-05-21T00:00:00Z"));
        Project second = service.ensureProject(root, new FixedClock("2026-05-21T01:00:00Z"));

        assertTrue(Files.isRegularFile(PathUtil.projectJson(root)));
        assertTrue(first.projectKey().startsWith("dhk-"));
        assertEquals(first.projectKey(), second.projectKey());
        assertEquals("Project With Spaces", second.projectName());
        assertEquals(root.toAbsolutePath().normalize().toString(), second.rootPath());
        assertEquals("2026-05-21T00:00:00Z", second.createdAt());
        assertEquals("2026-05-21T01:00:00Z", second.updatedAt());
    }

    private static final class FixedClock implements Clock {
        private final Instant now;

        private FixedClock(String now) {
            this.now = Instant.parse(now);
        }

        public Instant now() {
            return now;
        }
    }
}
