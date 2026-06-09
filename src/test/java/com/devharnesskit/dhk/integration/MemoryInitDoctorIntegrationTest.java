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
        assertTrue(doctorHarness.stdout().contains("schema_version: ok (18)"));
        assertTrue(doctorHarness.stdout().contains("mysql_driver: ok"));
        assertTrue(doctorHarness.stdout().contains("sensitive_policy: default"));
        assertTrue(doctorHarness.stdout().contains("devharness_policy: default"));
        assertTrue(doctorHarness.stdout().contains("memory_total: 0"));
        assertTrue(doctorHarness.stdout().contains("checkpoint_total: 0"));

        Harness jsonDoctorHarness = new Harness(tempDir);
        int jsonExitCode = new CommandRouter().run(new String[]{
                "doctor", "--project-root", root.toString(), "--json"
        }, jsonDoctorHarness.context());
        assertEquals(ExitCodes.SUCCESS, jsonExitCode);
        assertTrue(jsonDoctorHarness.stdout().contains("\"command\": \"doctor\""));
        assertTrue(jsonDoctorHarness.stdout().contains("\"schema_version\": 18"));
        assertTrue(jsonDoctorHarness.stdout().contains("\"mysql_driver_loaded\": true"));
        assertTrue(jsonDoctorHarness.stdout().contains("\"devharness_policy\": \"default\""));
    }

    @Test
    void doctorReportsGoalConfigDiagnostics() throws Exception {
        Path root = tempDir.resolve("demo");
        Harness initHarness = new Harness(tempDir);
        new CommandRouter().run(new String[]{"memory", "init", "--project-root", "demo"}, initHarness.context());

        Files.createDirectories(PathUtil.goalProfilesDirectory(root));
        Files.write(PathUtil.goalProfile(root, "custom-api"), ("{\n"
                + "  \"profile_key\": \"wrong-key\",\n"
                + "  \"workflow_key\": \"missing-workflow\",\n"
                + "  \"requires_spec\": \"maybe\",\n"
                + "  \"default_mode\": \"api\",\n"
                + "  \"actions\": \"inspect_existing_code,bad-action,verify,verify\",\n"
                + "  \"required_checks\": \"compile,custom\",\n"
                + "  \"completion_allow_skipped_checks\": \"maybe\",\n"
                + "  \"graph_required\": \"sometimes\",\n"
                + "  \"graph_provider\": \"unknown\",\n"
                + "  \"graph_require_fresh_snapshot\": \"maybe\",\n"
                + "  \"graph_require_impact_map\": \"nah\",\n"
                + "  \"graph_max_staleness_minutes\": \"0\",\n"
                + "  \"graph_actions\": \"graph_index_or_refresh,bad-action\",\n"
                + "  \"required_evidence.verify\": \"compile_result,,sensitive_result\",\n"
                + "  \"required_evidence.ghost\": \"ghost_evidence\",\n"
                + "  \"unexpected\": \"value\"\n"
                + "}\n").getBytes("UTF-8"));
        Files.write(PathUtil.goalProfile(root, "mapping-bad"), ("{\n"
                + "  \"workflow_key\": \"api-change\",\n"
                + "  \"requires_spec\": \"true\",\n"
                + "  \"default_mode\": \"api\",\n"
                + "  \"actions\": \"verify\",\n"
                + "  \"mapping.verify.workflow_phase\": \"missing_phase\",\n"
                + "  \"mapping.verify.required_gates\": \"missing_gate\",\n"
                + "  \"mapping.verify.spec_acceptance_update\": \"robot\",\n"
                + "  \"mapping.ghost.workflow_phase\": \"verify_tests\",\n"
                + "  \"acceptance.Business.source\": \"robot\",\n"
                + "  \"acceptance.business_rule.required_checks\": \"sensitive,custom\",\n"
                + "  \"acceptance.evidence_rule.source\": \"evidence\"\n"
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
        assertTrue(doctorHarness.stderr().contains("profile_key does not match filename"));
        assertTrue(doctorHarness.stderr().contains("unknown fields: unexpected"));
        assertTrue(doctorHarness.stderr().contains("workflow template not found: missing-workflow"));
        assertTrue(doctorHarness.stderr().contains("actions contains invalid item: bad-action"));
        assertTrue(doctorHarness.stderr().contains("actions contains duplicate item: verify"));
        assertTrue(doctorHarness.stderr().contains("required_checks contains unsupported item: custom"));
        assertTrue(doctorHarness.stderr().contains("completion_allow_skipped_checks should be true/false"));
        assertTrue(doctorHarness.stderr().contains("graph_required should be true/false"));
        assertTrue(doctorHarness.stderr().contains("graph_provider should be one of"));
        assertTrue(doctorHarness.stderr().contains("graph_require_fresh_snapshot should be true/false"));
        assertTrue(doctorHarness.stderr().contains("graph_require_impact_map should be true/false"));
        assertTrue(doctorHarness.stderr().contains("graph_max_staleness_minutes should be a positive integer"));
        assertTrue(doctorHarness.stderr().contains("graph_actions contains invalid item: bad-action"));
        assertTrue(doctorHarness.stderr().contains("graph_actions references action not listed in actions: graph_index_or_refresh"));
        assertTrue(doctorHarness.stderr().contains("required_evidence.verify contains an empty item"));
        assertTrue(doctorHarness.stderr().contains("required_evidence.ghost references action not listed in actions"));
        assertTrue(doctorHarness.stderr().contains("mapping.verify.workflow_phase references unknown workflow phase: missing_phase"));
        assertTrue(doctorHarness.stderr().contains("mapping.verify.required_gates contains unsupported item: missing_gate"));
        assertTrue(doctorHarness.stderr().contains("mapping.verify.spec_acceptance_update should be one of"));
        assertTrue(doctorHarness.stderr().contains("mapping.ghost.workflow_phase references action not listed in actions"));
        assertTrue(doctorHarness.stderr().contains("acceptance.Business.source has invalid acceptance key"));
        assertTrue(doctorHarness.stderr().contains("acceptance.Business.source should be one of"));
        assertTrue(doctorHarness.stderr().contains("acceptance.business_rule.source is required"));
        assertTrue(doctorHarness.stderr().contains("acceptance.business_rule.required_checks contains unsupported item: custom"));
        assertTrue(doctorHarness.stderr().contains("acceptance.evidence_rule.evidence_key is required when source is evidence"));
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
                + "  \"profile_key\": \"financial-api\",\n"
                + "  \"workflow_key\": \"api-change\",\n"
                + "  \"requires_spec\": \"true\",\n"
                + "  \"default_mode\": \"api\",\n"
                + "  \"actions\": \"inspect_existing_code,create_change_plan,implement_minimal_change,verify\",\n"
                + "  \"required_checks\": \"compile,test,sensitive,spec,workflow\",\n"
                + "  \"completion_require_fresh_checks\": \"true\",\n"
                + "  \"completion_allow_skipped_checks\": \"false\",\n"
                + "  \"completion_require_checkpoint\": \"true\",\n"
                + "  \"strict_workflow_phase_order\": \"true\",\n"
                + "  \"graph_required\": \"true\",\n"
                + "  \"graph_provider\": \"lite\",\n"
                + "  \"graph_require_fresh_snapshot\": \"true\",\n"
                + "  \"graph_require_impact_map\": \"true\",\n"
                + "  \"graph_max_staleness_minutes\": \"60\",\n"
                + "  \"graph_actions\": \"inspect_existing_code\",\n"
                + "  \"required_evidence.inspect_existing_code\": \"existing_controller,existing_service,existing_mapper,existing_tests\",\n"
                + "  \"required_evidence.create_change_plan\": \"impacted_files,risk_points,verification_plan\",\n"
                + "  \"mapping.inspect_existing_code.workflow_phase\": \"inspect_existing_code\",\n"
                + "  \"mapping.create_change_plan.workflow_phase\": \"create_change_plan\",\n"
                + "  \"mapping.create_change_plan.phase_pass_mode\": \"step\",\n"
                + "  \"mapping.create_change_plan.required_gates\": \"impacted_files_listed,verification_plan_ready\",\n"
                + "  \"mapping.create_change_plan.gate_pass_mode\": \"step\",\n"
                + "  \"mapping.verify.workflow_phase\": \"verify_tests\",\n"
                + "  \"mapping.verify.phase_pass_mode\": \"check\",\n"
                + "  \"mapping.verify.required_gates\": \"tests_recorded\",\n"
                + "  \"mapping.verify.gate_pass_mode\": \"check\",\n"
                + "  \"mapping.verify.spec_acceptance_update\": \"manual\",\n"
                + "  \"mapping.verify.acceptance_source\": \"manual\",\n"
                + "  \"mapping.verify.required_checks\": \"compile,test,sensitive\",\n"
                + "  \"acceptance.business_rule.description\": \"Business rule is verified\",\n"
                + "  \"acceptance.business_rule.expected\": \"Business rule evidence is present\",\n"
                + "  \"acceptance.business_rule.source\": \"evidence\",\n"
                + "  \"acceptance.business_rule.evidence_key\": \"business_verified\"\n"
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
    void doctorReportsDevHarnessPolicyDiagnostics() throws Exception {
        Path root = tempDir.resolve("demo");
        Harness initHarness = new Harness(tempDir);
        new CommandRouter().run(new String[]{"memory", "init", "--project-root", "demo"}, initHarness.context());

        Files.write(PathUtil.devharnessPolicy(root), ("{\n"
                + "  \"mode\": \"experimental\",\n"
                + "  \"allowed_dhk_commands\": \"goal start,goal start,Goal Bad\",\n"
                + "  \"forbidden_dhk_commands\": \"\",\n"
                + "  \"protected_files\": \"/etc/passwd,../secrets.env\",\n"
                + "  \"allowed_write_paths\": \"src/**,,docs/**\",\n"
                + "  \"db_sql_requires_explicit_request\": \"maybe\",\n"
                + "  \"db_require_readonly_credentials\": \"sometimes\",\n"
                + "  \"db_allowed_environments\": \"dev,QA!\",\n"
                + "  \"context_export_require_sensitive_scan\": \"maybe\",\n"
                + "  \"context_export_block_on_sensitive\": \"maybe\",\n"
                + "  \"context_export_allowed_files\": \"CURRENT_CONTEXT.md\",\n"
                + "  \"context_export_forbidden_files\": \"../raw.sql\",\n"
                + "  \"skill_contract_required\": \"true\",\n"
                + "  \"extra\": \"value\"\n"
                + "}\n").getBytes("UTF-8"));

        Harness doctorHarness = new Harness(tempDir);
        int exitCode = new CommandRouter().run(new String[]{
                "doctor", "--project-root", root.toString()
        }, doctorHarness.context());

        assertEquals(ExitCodes.SUCCESS, exitCode);
        assertTrue(doctorHarness.stdout().contains("devharness_policy: configured"));
        assertTrue(doctorHarness.stderr().contains("WARNING devharness_policy"));
        assertTrue(doctorHarness.stderr().contains("unknown fields: extra"));
        assertTrue(doctorHarness.stderr().contains("mode should be one of: strict,guided,expert"));
        assertTrue(doctorHarness.stderr().contains("allowed_dhk_commands contains duplicate item: goal start"));
        assertTrue(doctorHarness.stderr().contains("allowed_dhk_commands contains invalid command pattern: Goal Bad"));
        assertTrue(doctorHarness.stderr().contains("forbidden_dhk_commands is empty"));
        assertTrue(doctorHarness.stderr().contains("protected_files should contain project-relative safe globs only: /etc/passwd"));
        assertTrue(doctorHarness.stderr().contains("protected_files should contain project-relative safe globs only: ../secrets.env"));
        assertTrue(doctorHarness.stderr().contains("allowed_write_paths contains an empty item"));
        assertTrue(doctorHarness.stderr().contains("db_sql_requires_explicit_request should be true/false"));
        assertTrue(doctorHarness.stderr().contains("db_require_readonly_credentials should be true/false"));
        assertTrue(doctorHarness.stderr().contains("db_allowed_environments contains invalid environment key: QA!"));
        assertTrue(doctorHarness.stderr().contains("context_export_require_sensitive_scan should be true/false"));
        assertTrue(doctorHarness.stderr().contains("context_export_block_on_sensitive should be true/false"));
        assertTrue(doctorHarness.stderr().contains("context_export_forbidden_files should contain project-relative safe globs only: ../raw.sql"));
        assertTrue(doctorHarness.stderr().contains("skill_contract_required is true but skill_key is missing"));

        Harness jsonDoctorHarness = new Harness(tempDir);
        int jsonExitCode = new CommandRouter().run(new String[]{
                "doctor", "--project-root", root.toString(), "--json"
        }, jsonDoctorHarness.context());
        assertEquals(ExitCodes.SUCCESS, jsonExitCode);
        assertTrue(jsonDoctorHarness.stdout().contains("\"devharness_policy\": \"configured\""));
        assertTrue(jsonDoctorHarness.stdout().contains("\"policy_warnings\": ["));
        assertTrue(jsonDoctorHarness.stdout().contains("mode should be one of"));
    }

    @Test
    void doctorAcceptsValidDevHarnessPolicy() throws Exception {
        Path root = tempDir.resolve("demo");
        Harness initHarness = new Harness(tempDir);
        new CommandRouter().run(new String[]{"memory", "init", "--project-root", "demo"}, initHarness.context());

        Files.write(PathUtil.devharnessPolicy(root), ("{\n"
                + "  \"schema_version\": \"devharness-policy/v1\",\n"
                + "  \"mode\": \"strict\",\n"
                + "  \"allowed_dhk_commands\": \"goal start,goal resume,goal next,goal step,goal verify,goal complete\",\n"
                + "  \"forbidden_dhk_commands\": \"workflow gate waive,spec archive,memory confirm,db sql\",\n"
                + "  \"protected_files\": \".env,application-prod.yml,deploy/**\",\n"
                + "  \"allowed_write_paths\": \"src/**,docs/**,README.md\",\n"
                + "  \"db_sql_requires_explicit_request\": \"true\",\n"
                + "  \"db_require_readonly_credentials\": \"true\",\n"
                + "  \"db_allowed_environments\": \"dev,test,readonly\",\n"
                + "  \"context_export_require_sensitive_scan\": \"true\",\n"
                + "  \"context_export_block_on_sensitive\": \"true\",\n"
                + "  \"context_export_allowed_files\": \".agents/memory/exports/*.md\",\n"
                + "  \"context_export_forbidden_files\": \".env,application-prod.yml\",\n"
                + "  \"skill_contract_required\": \"true\",\n"
                + "  \"skill_key\": \"devharness-goal-development\"\n"
                + "}\n").getBytes("UTF-8"));
        write(root, ".agents/skills/devharness-goal-development/contract.json",
                "{\n"
                        + "  \"skill_key\": \"devharness-goal-development\",\n"
                        + "  \"version\": \"0.7.1\",\n"
                        + "  \"task_type\": \"coding\",\n"
                        + "  \"data_access_level\": \"context\",\n"
                        + "  \"allowed_commands\": \"dhk goal next,dhk goal step\",\n"
                        + "  \"forbidden_commands\": \"dhk db sql\"\n"
                        + "}\n");

        Harness doctorHarness = new Harness(tempDir);
        int exitCode = new CommandRouter().run(new String[]{
                "doctor", "--project-root", root.toString()
        }, doctorHarness.context());

        assertEquals(ExitCodes.SUCCESS, exitCode);
        assertTrue(doctorHarness.stdout().contains("devharness_policy: configured"));
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

    private void write(Path root, String relativePath, String content) throws Exception {
        Path path = root.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.write(path, content.getBytes("UTF-8"));
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
                    com.devharnesskit.dhk.testsupport.Utf8HarnessSupport.printStream(out),
                    com.devharnesskit.dhk.testsupport.Utf8HarnessSupport.printStream(err),
                    new FixedClock()
            );
        }

        private String stdout() {
            return com.devharnesskit.dhk.testsupport.Utf8HarnessSupport.text(out);
        }

        private String stderr() {
            return com.devharnesskit.dhk.testsupport.Utf8HarnessSupport.text(err);
        }
    }

    private static final class FixedClock implements Clock {
        public Instant now() {
            return Instant.parse("2026-05-21T00:00:00Z");
        }
    }
}
