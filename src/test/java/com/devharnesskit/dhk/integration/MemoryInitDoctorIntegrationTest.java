package com.devharnesskit.dhk.integration;

import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.CommandRouter;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.util.Clock;
import com.devharnesskit.dhk.util.PathUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MemoryInitDoctorIntegrationTest {
    @TempDir
    Path tempDir;

    @Test
    void memoryInitCreatesProjectFilesAndIsIdempotent() {
        Path root = tempDir.resolve("demo");
        Harness firstHarness = new Harness(tempDir);
        int firstExit = new CommandRouter().run(new String[]{"memory", "init", "--project-root", "demo"}, firstHarness.context());

        Project firstProject = new ProjectService().readProject(PathUtil.projectJson(root));
        Harness secondHarness = new Harness(tempDir);
        int secondExit = new CommandRouter().run(new String[]{"memory", "init", "--project-root", "demo"}, secondHarness.context());
        Project secondProject = new ProjectService().readProject(PathUtil.projectJson(root));

        assertEquals(ExitCodes.SUCCESS, firstExit);
        assertEquals(ExitCodes.SUCCESS, secondExit);
        assertTrue(Files.isRegularFile(PathUtil.memoryDb(root)));
        assertTrue(Files.isRegularFile(PathUtil.projectJson(root)));
        assertTrue(Files.isDirectory(PathUtil.exportsDirectory(root)));
        assertTrue(Files.isDirectory(PathUtil.devharnessDirectory(root)));
        assertTrue(Files.isRegularFile(PathUtil.projectIndex(root)));
        assertEquals(firstProject.projectKey(), secondProject.projectKey());
        assertTrue(firstHarness.stdout().contains("memory init complete"));
        assertTrue(firstHarness.stdout().contains("sensitive_policy:"));
        assertTrue(secondHarness.stdout().contains("fts:"));
    }

    @Test
    void doctorReturnsNotFoundBeforeInit() {
        Harness harness = new Harness(tempDir);

        int exitCode = new CommandRouter().run(new String[]{"doctor", "--project-root", "missing"}, harness.context());

        assertEquals(ExitCodes.NOT_FOUND, exitCode);
        assertTrue(harness.stdout().contains("memory_db: missing"));

        Harness jsonHarness = new Harness(tempDir);
        int jsonExitCode = new CommandRouter().run(new String[]{
                "doctor", "--project-root", "missing", "--json"
        }, jsonHarness.context());
        assertEquals(ExitCodes.NOT_FOUND, jsonExitCode);
        assertTrue(jsonHarness.stdout().contains("\"command\": \"doctor\""));
        assertTrue(jsonHarness.stdout().contains("\"memory_db_ok\": false"));
        assertTrue(jsonHarness.stdout().contains("\"project_json_ok\": false"));
    }

    @Test
    void doctorReturnsSuccessAfterInit() {
        Path root = tempDir.resolve("demo");
        Harness initHarness = new Harness(tempDir);
        new CommandRouter().run(new String[]{"memory", "init", "--project-root", "demo"}, initHarness.context());

        Harness doctorHarness = new Harness(tempDir);
        int exitCode = new CommandRouter().run(new String[]{"doctor", "--project-root", root.toString()}, doctorHarness.context());

        assertEquals(ExitCodes.SUCCESS, exitCode);
        assertTrue(doctorHarness.stdout().contains("schema_version: ok (7)"));
        assertTrue(doctorHarness.stdout().contains("mysql_driver: ok"));
        assertTrue(doctorHarness.stdout().contains("sensitive_policy: default"));
        assertTrue(doctorHarness.stdout().contains("memory_total: 0"));
        assertTrue(doctorHarness.stdout().contains("checkpoint_total: 0"));

        Harness jsonDoctorHarness = new Harness(tempDir);
        int jsonExitCode = new CommandRouter().run(new String[]{
                "doctor", "--project-root", root.toString(), "--json"
        }, jsonDoctorHarness.context());
        assertEquals(ExitCodes.SUCCESS, jsonExitCode);
        assertTrue(jsonDoctorHarness.stdout().contains("\"command\": \"doctor\""));
        assertTrue(jsonDoctorHarness.stdout().contains("\"schema_version\": 7"));
        assertTrue(jsonDoctorHarness.stdout().contains("\"mysql_driver_loaded\": true"));
    }

    @Test
    void doctorReportsGoalConfigDiagnostics() throws Exception {
        Path root = tempDir.resolve("demo");
        Harness initHarness = new Harness(tempDir);
        new CommandRouter().run(new String[]{"memory", "init", "--project-root", "demo"}, initHarness.context());

        Files.createDirectories(PathUtil.goalProfilesDirectory(root));
        Files.write(PathUtil.goalProfile(root, "custom-api"), ("{\n"
                + "  \"workflow_key\": \"missing-workflow\",\n"
                + "  \"requires_spec\": \"maybe\",\n"
                + "  \"default_mode\": \"api\",\n"
                + "  \"actions\": \"inspect_existing_code,bad-action,verify,verify\",\n"
                + "  \"unexpected\": \"value\"\n"
                + "}\n").getBytes("UTF-8"));
        Files.write(PathUtil.goalCheckPolicy(root), ("{\n"
                + "  \"required_checks\": \"\",\n"
                + "  \"accepted_compile_statuses\": \"passed,unknown\",\n"
                + "  \"fail_pending_hard_gates\": \"maybe\",\n"
                + "  \"extra\": \"value\"\n"
                + "}\n").getBytes("UTF-8"));

        Harness doctorHarness = new Harness(tempDir);
        int exitCode = new CommandRouter().run(new String[]{
                "doctor", "--project-root", root.toString()
        }, doctorHarness.context());

        assertEquals(ExitCodes.SUCCESS, exitCode);
        assertTrue(doctorHarness.stderr().contains("WARNING goal_config"));
        assertTrue(doctorHarness.stderr().contains("unknown fields: unexpected"));
        assertTrue(doctorHarness.stderr().contains("workflow template not found: missing-workflow"));
        assertTrue(doctorHarness.stderr().contains("actions contains invalid item: bad-action"));
        assertTrue(doctorHarness.stderr().contains("actions contains duplicate item: verify"));
        assertTrue(doctorHarness.stderr().contains("required_checks is empty"));
        assertTrue(doctorHarness.stderr().contains("accepted_compile_statuses contains unsupported item: unknown"));
        assertTrue(doctorHarness.stderr().contains("fail_pending_hard_gates should be true/false"));

        Harness jsonDoctorHarness = new Harness(tempDir);
        int jsonExitCode = new CommandRouter().run(new String[]{
                "doctor", "--project-root", root.toString(), "--json"
        }, jsonDoctorHarness.context());
        assertEquals(ExitCodes.SUCCESS, jsonExitCode);
        assertTrue(jsonDoctorHarness.stdout().contains("\"goal_config_warnings\": ["));
        assertTrue(jsonDoctorHarness.stdout().contains("workflow template not found: missing-workflow"));
    }

    @Test
    void doctorAcceptsValidGoalConfig() throws Exception {
        Path root = tempDir.resolve("demo");
        Harness initHarness = new Harness(tempDir);
        new CommandRouter().run(new String[]{"memory", "init", "--project-root", "demo"}, initHarness.context());

        Files.createDirectories(PathUtil.goalProfilesDirectory(root));
        Files.write(PathUtil.goalProfile(root, "financial-api"), ("{\n"
                + "  \"workflow_key\": \"api-change\",\n"
                + "  \"requires_spec\": \"true\",\n"
                + "  \"default_mode\": \"api\",\n"
                + "  \"actions\": \"inspect_existing_code,create_change_plan,implement_minimal_change,verify\"\n"
                + "}\n").getBytes("UTF-8"));
        Files.write(PathUtil.goalCheckPolicy(root), ("{\n"
                + "  \"required_checks\": \"compile,test,sensitive,spec,workflow\",\n"
                + "  \"compile_command\": \"mvn -q -DskipTests compile\",\n"
                + "  \"test_command\": \"mvn -q test\",\n"
                + "  \"fail_pending_hard_gates\": \"false\",\n"
                + "  \"accepted_compile_statuses\": \"passed\",\n"
                + "  \"accepted_test_statuses\": \"passed\",\n"
                + "  \"accepted_sensitive_statuses\": \"passed\",\n"
                + "  \"accepted_spec_statuses\": \"passed\",\n"
                + "  \"accepted_workflow_statuses\": \"passed,skipped,waived\"\n"
                + "}\n").getBytes("UTF-8"));

        Harness doctorHarness = new Harness(tempDir);
        int exitCode = new CommandRouter().run(new String[]{
                "doctor", "--project-root", root.toString()
        }, doctorHarness.context());

        assertEquals(ExitCodes.SUCCESS, exitCode);
        assertTrue(doctorHarness.stdout().contains("goal_config: ok"));
        assertEquals("", doctorHarness.stderr());
    }

    @Test
    void memoryBackupCreatesZipArchive() throws Exception {
        Path root = tempDir.resolve("demo");
        Harness initHarness = new Harness(tempDir);
        new CommandRouter().run(new String[]{"memory", "init", "--project-root", "demo"}, initHarness.context());

        Path out = tempDir.resolve("backup.zip");
        Harness backupHarness = new Harness(tempDir);
        int exitCode = new CommandRouter().run(new String[]{
                "memory", "backup", "--project-root", "demo", "--out", out.toString()
        }, backupHarness.context());

        assertEquals(ExitCodes.SUCCESS, exitCode);
        assertTrue(backupHarness.stdout().contains("backup_path: " + out));
        assertTrue(Files.isRegularFile(out));
        try (ZipFile zip = new ZipFile(out.toFile())) {
            assertTrue(zip.getEntry(PathUtil.PROJECT_JSON) != null);
            assertTrue(zip.getEntry(PathUtil.MEMORY_DB) != null);
            assertTrue(zip.getEntry(PathUtil.EXPORTS_DIRECTORY + "/" + PathUtil.PROJECT_INDEX) != null);
        }
        assertTrue(Files.isDirectory(root.resolve(".agents").resolve("memory")));
    }

    private static final class Harness {
        private final Path workingDirectory;
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();
        private final ByteArrayOutputStream err = new ByteArrayOutputStream();

        private Harness(Path workingDirectory) {
            this.workingDirectory = workingDirectory;
        }

        private CommandContext context() {
            return new CommandContext(
                    workingDirectory.toAbsolutePath().normalize(),
                    new PrintStream(out),
                    new PrintStream(err),
                    new FixedClock()
            );
        }

        private String stdout() {
            return out.toString();
        }

        private String stderr() {
            return err.toString();
        }
    }

    private static final class FixedClock implements Clock {
        public Instant now() {
            return Instant.parse("2026-05-21T00:00:00Z");
        }
    }
}
