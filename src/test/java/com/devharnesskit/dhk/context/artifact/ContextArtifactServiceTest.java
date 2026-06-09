package com.devharnesskit.dhk.context.artifact;

import com.devharnesskit.dhk.context.compress.CompressResult;
import com.devharnesskit.dhk.context.compress.RetainedSpan;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.repository.ProjectRepository;
import com.devharnesskit.dhk.util.Clock;
import com.devharnesskit.dhk.util.PathUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.Instant;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ContextArtifactServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void persistsInlineArtifactAndRetrievesLineRange() throws Exception {
        PathUtil.createMemoryDirectories(tempDir);
        ContextArtifactService service = new ContextArtifactService();
        ContextArtifactRepository repository = new ContextArtifactRepository();

        try (Connection connection = new DbConnectionFactory().open(tempDir)) {
            new MigrationRunner().migrate(connection, new FixedClock());
            new ProjectRepository().upsert(connection, project());

            ContextArtifact artifact = service.persist(tempDir, connection, "project-1", "goal-1",
                    "build-goal-1-demo", "build-log", "mvn", "one\ntwo\nthree\n",
                    result("digest", 3, 1), new FixedClock());

            ContextArtifact stored = repository.findByKey(connection, "project-1", artifact.artifactKey());
            assertEquals("one\ntwo\nthree\n", service.originalContent(stored));
            assertEquals("two\nthree", service.range(service.originalContent(stored), 2, 3));
            assertEquals(1, repository.stats(connection, "project-1", "goal-1").artifactCount());
        }
    }

    @Test
    void storesLargeOriginalOutsideSQLite() throws Exception {
        PathUtil.createMemoryDirectories(tempDir);
        ContextArtifactService service = new ContextArtifactService();

        try (Connection connection = new DbConnectionFactory().open(tempDir)) {
            new MigrationRunner().migrate(connection, new FixedClock());
            new ProjectRepository().upsert(connection, project());

            String large = repeat("line content\n", 7000);
            ContextArtifact artifact = service.persist(tempDir, connection, "project-1", "goal-1",
                    "large-build-log", "build-log", "mvn", large, result("digest", 7000, 30),
                    new FixedClock());

            assertEquals("", artifact.originalText());
            assertTrue(artifact.sourcePath().replace('\\', '/').contains("/.agents/context/artifacts/large-build-log.txt"));
            assertTrue(Files.isRegularFile(PathUtil.contextArtifact(tempDir, "large-build-log")));
            assertEquals(large, service.originalContent(artifact));
        }
    }

    private CompressResult result(String text, int before, int after) {
        return new CompressResult("build-log", text,
                Arrays.asList(new RetainedSpan(1, "head", "one")), before - after, before, after, false);
    }

    private Project project() {
        return new Project("project-1", "Demo", tempDir.toString(), "java", "java",
                "unknown", "unknown", "now", "now");
    }

    private String repeat(String text, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(text);
        }
        return builder.toString();
    }

    private static final class FixedClock implements Clock {
        public Instant now() {
            return Instant.parse("2026-06-01T00:00:00Z");
        }
    }
}
