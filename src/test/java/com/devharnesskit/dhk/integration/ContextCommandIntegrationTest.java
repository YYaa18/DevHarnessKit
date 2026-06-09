package com.devharnesskit.dhk.integration;

import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.CommandRouter;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.context.artifact.ContextArtifact;
import com.devharnesskit.dhk.context.artifact.ContextArtifactService;
import com.devharnesskit.dhk.context.compress.CompressResult;
import com.devharnesskit.dhk.context.compress.RetainedSpan;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.util.Clock;
import com.devharnesskit.dhk.util.PathUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.Instant;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ContextCommandIntegrationTest {
    @TempDir
    Path tempDir;

    @Test
    void contextCommandsListStatsAndRetrieveArtifacts() throws Exception {
        Path root = tempDir.resolve("demo");
        Harness init = new Harness(tempDir);
        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "memory", "init", "--project-root", "demo"
        }, init.context()));
        Project project = new ProjectService().readProject(PathUtil.projectJson(root));
        String artifactKey;
        try (Connection connection = new DbConnectionFactory().open(root)) {
            ContextArtifact artifact = new ContextArtifactService().persist(root, connection,
                    project.projectKey(), "goal-demo", "build-goal-demo",
                    "build-log", "mvn", "alpha\nbeta\ngamma\n",
                    new CompressResult("build-log", "digest", Arrays.asList(new RetainedSpan(2, "signal", "beta")),
                            1, 12, 3, false),
                    new FixedClock());
            artifactKey = artifact.artifactKey();
            new ContextArtifactService().persist(root, connection,
                    project.projectKey(), "goal-demo", "test-goal-demo",
                    "test-log", "mvn test", "test alpha\ntest beta\n",
                    new CompressResult("test-log", "test digest",
                            Arrays.asList(new RetainedSpan(1, "head", "test alpha")),
                            1, 10, 4, false),
                    new FixedClock());
            new ContextArtifactService().persist(root, connection,
                    project.projectKey(), "goal-other", "build-goal-other",
                    "build-log", "mvn", "other alpha\n",
                    new CompressResult("build-log", "other digest",
                            Arrays.asList(new RetainedSpan(1, "head", "other alpha")),
                            0, 6, 4, false),
                    new FixedClock());
        }

        Harness stats = new Harness(tempDir);
        int statsExit = new CommandRouter().run(new String[]{
                "context", "stats", "--project-root", root.toString(), "--goal", "goal-demo", "--json"
        }, stats.context());
        assertEquals(ExitCodes.SUCCESS, statsExit);
        assertTrue(stats.stdout().contains("\"command\": \"context stats\""));
        assertTrue(stats.stdout().contains("\"artifact_count\": 2"));
        assertTrue(stats.stdout().contains("\"token_reduction_percent\":"));
        assertTrue(stats.stdout().contains("\"sections\": ["));
        assertTrue(stats.stdout().contains("\"compressed_artifacts\": ["));
        assertTrue(stats.stdout().contains("\"source_type\": \"build-log\""));
        assertTrue(stats.stdout().contains("\"risks\": ["));

        Harness artifacts = new Harness(tempDir);
        int artifactsExit = new CommandRouter().run(new String[]{
                "context", "artifacts", "--project-root", root.toString()
        }, artifacts.context());
        assertEquals(ExitCodes.SUCCESS, artifactsExit);
        assertTrue(artifacts.stdout().contains(artifactKey));
        assertTrue(artifacts.stdout().contains("tokens: 12 -> 3"));

        Harness typedArtifacts = new Harness(tempDir);
        int typedArtifactsExit = new CommandRouter().run(new String[]{
                "context", "artifacts", "--project-root", root.toString(), "--type", "shell-output",
                "--limit", "10", "--offset", "0", "--json"
        }, typedArtifacts.context());
        assertEquals(ExitCodes.SUCCESS, typedArtifactsExit);
        assertTrue(typedArtifacts.stdout().contains("\"count\": 0"));
        assertTrue(typedArtifacts.stdout().contains("\"limit\": 10"));
        assertTrue(typedArtifacts.stdout().contains("\"offset\": 0"));

        Harness pagedArtifacts = new Harness(tempDir);
        int pagedArtifactsExit = new CommandRouter().run(new String[]{
                "context", "artifacts", "--project-root", root.toString(), "--goal", "goal-demo",
                "--limit", "1", "--offset", "1", "--json"
        }, pagedArtifacts.context());
        assertEquals(ExitCodes.SUCCESS, pagedArtifactsExit);
        assertTrue(pagedArtifacts.stdout().contains("\"count\": 1"));
        assertTrue(pagedArtifacts.stdout().contains("\"goal_key\": \"goal-demo\""));
        assertTrue(!pagedArtifacts.stdout().contains("goal-other"));

        Harness retrieve = new Harness(tempDir);
        int retrieveExit = new CommandRouter().run(new String[]{
                "context", "retrieve", artifactKey, "--project-root", root.toString(), "--lines", "2-2"
        }, retrieve.context());
        assertEquals(ExitCodes.SUCCESS, retrieveExit);
        assertEquals("beta\n", retrieve.stdout());

        Harness missing = new Harness(tempDir);
        int missingExit = new CommandRouter().run(new String[]{
                "context", "retrieve", "ctx-missing", "--project-root", root.toString()
        }, missing.context());
        assertEquals(ExitCodes.NOT_FOUND, missingExit);

        Harness doctor = new Harness(tempDir);
        int doctorExit = new CommandRouter().run(new String[]{
                "context", "doctor", "--project-root", root.toString(), "--goal", "goal-demo", "--json"
        }, doctor.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, doctorExit);
        assertTrue(doctor.stdout().contains("\"command\": \"context doctor\""));
        assertTrue(doctor.stdout().contains("current-context-present"));
    }

    @Test
    void contextDoctorDetectsRequiredEvidenceAndStaleGraphRisks() throws Exception {
        Path root = tempDir.resolve("doctor-risk-demo");
        Harness init = new Harness(tempDir);
        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "memory", "init", "--project-root", root.toString()
        }, init.context()));
        Project project = new ProjectService().readProject(PathUtil.projectJson(root));
        try (Connection connection = new DbConnectionFactory().open(root)) {
            new ContextArtifactService().persist(root, connection,
                    project.projectKey(), "goal-risk", "bdd-required-risk",
                    "bdd-evidence", "bdd", "Given risky evidence\nWhen it is compressed\nThen fail\n",
                    new CompressResult("bdd-evidence", "context_digest:\n  degraded: true\n",
                            Arrays.asList(new RetainedSpan(1, "head", "Given risky evidence")),
                            2, 12, 4, true),
                    new FixedClock());
        }
        java.nio.file.Files.createDirectories(PathUtil.exportsDirectory(root));
        java.nio.file.Files.writeString(PathUtil.currentContext(root),
                "# CURRENT_CONTEXT\n\n<context-budget-report>\n</context-budget-report>\n\n"
                        + "<truncation-report>\n</truncation-report>\n\n"
                        + "required_evidence=manual_evidence_status\n");
        java.nio.file.Files.writeString(PathUtil.goalContext(root),
                "# GOAL_CONTEXT\n\nsnapshot_stale: true\n");

        Harness doctor = new Harness(tempDir);
        int doctorExit = new CommandRouter().run(new String[]{
                "context", "doctor", "--project-root", root.toString(), "--goal", "goal-risk", "--json"
        }, doctor.context());

        assertEquals(ExitCodes.VALIDATION_ERROR, doctorExit);
        assertTrue(doctor.stdout().contains("\"key\": \"stale-graph-visible\""));
        assertTrue(doctor.stdout().contains("\"key\": \"high-priority-not-compressed\""));
        assertTrue(doctor.stdout().contains("\"key\": \"required-evidence-not-omitted\""));
        assertTrue(doctor.stdout().contains("\"decision\": \"failed\""));
    }

    @Test
    void contextDoctorPassesHealthyContext() throws Exception {
        Path root = tempDir.resolve("doctor-healthy-demo");
        Harness init = new Harness(tempDir);
        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "memory", "init", "--project-root", root.toString()
        }, init.context()));
        java.nio.file.Files.createDirectories(PathUtil.exportsDirectory(root));
        java.nio.file.Files.writeString(PathUtil.currentContext(root),
                "# CURRENT_CONTEXT\n\n<context-budget-report>\n"
                        + "- total_tokens_budget: 12000\n"
                        + "</context-budget-report>\n\n"
                        + "<truncation-report>\n"
                        + "- relevant_memory_truncated: false\n"
                        + "</truncation-report>\n");
        java.nio.file.Files.writeString(PathUtil.goalContext(root),
                "# GOAL_CONTEXT\n\n<graph-context-digest>\n"
                        + "graph_snapshot: status=fresh; confidence=advisory_only\n"
                        + "</graph-context-digest>\n");

        Harness doctor = new Harness(tempDir);
        int doctorExit = new CommandRouter().run(new String[]{
                "context", "doctor", "--project-root", root.toString(), "--json"
        }, doctor.context());

        assertEquals(ExitCodes.SUCCESS, doctorExit);
        assertTrue(doctor.stdout().contains("\"command\": \"context doctor\""));
        assertTrue(doctor.stdout().contains("\"decision\": \"passed\""));
        assertTrue(doctor.stdout().contains("\"key\": \"total-budget\""));
    }

    @Test
    void contextRenderRegeneratesGoalContextForExistingGoal() throws Exception {
        Path root = tempDir.resolve("render-demo");
        Harness quickstart = new Harness(tempDir);
        int quickstartExit = new CommandRouter().run(new String[]{
                "quickstart", "--project-root", root.toString(), "--task", "Render context", "--module", "context",
                "--preset", "safe-refactor-graph", "--graph", "required", "--force"
        }, quickstart.context());
        assertEquals(ExitCodes.SUCCESS, quickstartExit);
        String goalKey = lineValue(quickstart.stdout(), "goal_key: ");

        Harness render = new Harness(tempDir);
        int renderExit = new CommandRouter().run(new String[]{
                "context", "render", "--project-root", root.toString(), "--goal", goalKey, "--json"
        }, render.context());
        assertEquals(ExitCodes.SUCCESS, renderExit);
        assertTrue(render.stdout().contains("\"command\": \"context render\""));
        assertTrue(render.stdout().contains("\"goal_key\": \"" + goalKey + "\""));
        assertTrue(java.nio.file.Files.readString(PathUtil.currentContext(root)).contains("<context-budget-report>"));
        assertTrue(java.nio.file.Files.readString(PathUtil.goalContext(root)).contains("<goal>"));

        Harness headroom = new Harness(tempDir);
        int headroomExit = new CommandRouter().run(new String[]{
                "context", "render", "--project-root", root.toString(), "--goal", goalKey,
                "--compressor", "headroom", "--json"
        }, headroom.context());
        assertEquals(ExitCodes.SUCCESS, headroomExit);
        assertTrue(headroom.stdout().contains("\"compressor\": \"headroom\""));
        assertTrue(headroom.stdout().contains("\"experimental\": true"));
        assertTrue(headroom.stdout().contains("\"total_tokens_budget\": 10500"));
    }

    private static final class Harness {
        private final Path workingDirectory;
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();
        private final ByteArrayOutputStream err = new ByteArrayOutputStream();

        private Harness(Path workingDirectory) {
            this.workingDirectory = workingDirectory;
        }

        CommandContext context() {
            return new CommandContext(
                    workingDirectory,
                    com.devharnesskit.dhk.testsupport.Utf8HarnessSupport.printStream(out),
                    com.devharnesskit.dhk.testsupport.Utf8HarnessSupport.printStream(err),
                    new FixedClock()
            );
        }

        String stdout() {
            return com.devharnesskit.dhk.testsupport.Utf8HarnessSupport.text(out);
        }
    }

    private String lineValue(String text, String prefix) {
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            if (line.startsWith(prefix)) {
                return line.substring(prefix.length()).trim();
            }
        }
        return "";
    }

    private static final class FixedClock implements Clock {
        public Instant now() {
            return Instant.parse("2026-06-01T00:00:00Z");
        }
    }
}
