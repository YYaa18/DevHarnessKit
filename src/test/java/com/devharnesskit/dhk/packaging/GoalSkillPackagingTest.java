package com.devharnesskit.dhk.packaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GoalSkillPackagingTest {
    @TempDir
    Path tempDir;

    @Test
    void goalFirstSkillPackageContainsRequiredProtocolAndScripts() throws Exception {
        Path skillRoot = Paths.get(".agents/skills/devharness-goal-development");

        assertTrue(Files.isRegularFile(skillRoot.resolve("SKILL.md")));
        assertTrue(Files.isRegularFile(skillRoot.resolve("references/goal-protocol.md")));
        assertTrue(Files.isRegularFile(skillRoot.resolve("references/evidence-format.md")));
        assertTrue(Files.isRegularFile(skillRoot.resolve("references/self-check-format.md")));
        assertTrue(Files.isRegularFile(skillRoot.resolve("references/forbidden-actions.md")));

        assertScriptPair(skillRoot, "dhk");
        assertScriptPair(skillRoot, "quickstart");
        assertScriptPair(skillRoot, "goal-start");
        assertScriptPair(skillRoot, "goal-resume");
        assertScriptPair(skillRoot, "goal-next");
        assertScriptPair(skillRoot, "goal-step");
        assertScriptPair(skillRoot, "goal-check");
        assertScriptPair(skillRoot, "goal-evaluate");
        assertScriptPair(skillRoot, "goal-verify");
        assertScriptPair(skillRoot, "goal-complete");
        assertScriptPair(skillRoot, "goal-status");
        assertScriptPair(skillRoot, "goal-export");

        String skill = read(skillRoot.resolve("SKILL.md"));
        assertTrue(skill.contains("## Core Path"));
        assertTrue(skill.contains("## Full Protocol"));
        assertTrue(skill.contains("goal-verify.sh"));
        assertTrue(skill.contains("Work Brief"));
        assertTrue(skill.contains("AGENT_BRIEF.json"));
        assertTrue(skill.contains("quickstart.sh"));
        assertTrue(skill.contains("current_action` = `completed`"));
        assertTrue(skill.contains("Even typo or documentation-only edits use the lightweight patch flow"));
        assertTrue(skill.contains("latest user request verbatim"));
        assertTrue(skill.contains("goal_task_matches_user_request"));
        assertTrue(skill.contains("Do not show `harness_commands`"));
        assertTrue(skill.contains("goal-check.sh --all"));
        assertTrue(skill.contains("goal-evaluate.sh"));
        assertTrue(skill.contains("goal-complete.sh"));
        assertTrue(skill.contains("required evidence keys from GOAL_CONTEXT"));
        assertTrue(skill.contains("Perform only the `current_action`"));
        assertFalse(skill.contains("Do not bypass failed discipline checks"));

        assertProjectRootInjection(skillRoot);
        assertQuickstartWrapper(skillRoot);
        assertGoalWrapper(skillRoot, "goal-start", "start");
        assertGoalWrapper(skillRoot, "goal-resume", "resume");
        assertGoalWrapper(skillRoot, "goal-next", "next");
        assertGoalWrapper(skillRoot, "goal-step", "step");
        assertGoalWrapper(skillRoot, "goal-check", "check");
        assertGoalWrapper(skillRoot, "goal-evaluate", "evaluate");
        assertGoalWrapper(skillRoot, "goal-verify", "verify");
        assertGoalWrapper(skillRoot, "goal-complete", "complete");
        assertGoalWrapper(skillRoot, "goal-status", "status");
        assertGoalWrapper(skillRoot, "goal-export", "export");

        String protocol = read(skillRoot.resolve("references/goal-protocol.md"));
        String evidence = read(skillRoot.resolve("references/evidence-format.md"));
        String forbidden = read(skillRoot.resolve("references/forbidden-actions.md"));
        String selfCheck = read(skillRoot.resolve("references/self-check-format.md"));
        assertTrue(protocol.contains("Use wrapper scripts under `.agents/skills/devharness-goal-development/scripts/`"));
        assertTrue(protocol.contains("Run `goal verify` before final completion"));
        assertTrue(evidence.contains("Mirror required evidence keys exactly"));
        assertTrue(forbidden.contains("direct lower-level `dhk memory ...`"));
        assertTrue(forbidden.contains("`db sql`"));
        assertTrue(forbidden.contains("claiming completion before `goal verify`"));
        assertTrue(selfCheck.contains("goal evaluate"));
    }

    @Test
    void legacyMemoryFirstSkillPackageIsRemoved() {
        assertFalse(Files.exists(Paths.get(".agents/skills/devharness-java-development")));
    }

    @Test
    void perfSmokeBatchKeepsParityWithShellBudgets() throws Exception {
        String shell = read(Paths.get("scripts/perf-smoke.sh"));
        String batch = read(Paths.get("scripts/perf-smoke.bat"));

        assertTrue(shell.contains("MAX_HELP_MS"));
        assertTrue(shell.contains("MAX_DOCTOR_MS"));
        assertTrue(shell.contains("JAR_WARN_BYTES"));
        assertTrue(shell.contains("Possible lingering dhk Java process detected."));
        assertFalse(shell.contains("pgrep -fl \"dhk-cli|devharnesskit|dhk.jar"));
        assertTrue(shell.contains("grep -F \"java -jar $JAR\""));
        assertTrue(shell.contains("grep -v -F \"grep -F\""));

        assertTrue(batch.contains("DHK_PERF_MAX_HELP_MS"));
        assertTrue(batch.contains("DHK_PERF_MAX_DOCTOR_MS"));
        assertTrue(batch.contains("DHK_PERF_MAX_MEMORY_SEARCH_MS"));
        assertTrue(batch.contains("DHK_PERF_MAX_MEMORY_EXPORT_MS"));
        assertTrue(batch.contains("DHK_PERF_JAR_WARN_BYTES"));
        assertTrue(batch.contains("DHK_PERF_JAR_MAX_BYTES"));
        assertTrue(batch.contains("doctor|--project-root"));
        assertTrue(batch.contains("jar size:"));
        assertTrue(batch.contains(":check_lingering_processes"));
        assertTrue(batch.contains("java(\\.exe)?\\s+-jar"));
        assertTrue(batch.contains("[IO.Path]::GetFileName($env:JAR)"));
        assertTrue(batch.contains(":cleanup"));
        assertTrue(batch.contains("Sort-Object LastWriteTime -Descending"));
    }

    @Test
    void ciAndReleaseWorkflowsUseReleaseArchiveAndStableGates() throws Exception {
        String ci = read(Paths.get(".github/workflows/ci.yml"));
        String release = read(Paths.get(".github/workflows/release.yml"));

        assertTrue(ci.contains("mvn -B -DskipTests package -P release-archive"));
        assertTrue(ci.contains("scripts/check-class-size.sh"));
        assertTrue(ci.contains("scripts/perf-smoke.sh \"$DHK_JAR\""));
        assertTrue(ci.contains("ls -lh \"$DHK_JAR\" \"$DHK_ARCHIVE_PREFIX\".*"));

        assertTrue(release.contains("mvn -B clean package -P release-archive"));
        assertTrue(release.contains("scripts/check-class-size.sh"));
        assertTrue(release.contains("scripts/release-gate.sh --skip-package"));
        assertTrue(release.contains("target/SHA256SUMS"));
    }

    @Test
    void releaseArchiveUsesUserFacingWhitelist() throws Exception {
        String assembly = read(Paths.get("src/assembly/release.xml"));
        String releaseGate = read(Paths.get("scripts/release-gate.sh"));
        int scriptsStart = assembly.indexOf("<directory>${project.basedir}/scripts</directory>");
        assertTrue(scriptsStart >= 0);
        String scriptsFileSet = assembly.substring(scriptsStart, assembly.indexOf("</fileSet>", scriptsStart));

        assertTrue(assembly.contains("<include>README.md</include>"));
        assertTrue(assembly.contains("<include>LICENSE</include>"));
        assertTrue(assembly.contains("<include>THIRD_PARTY_NOTICES.md</include>"));
        assertTrue(assembly.contains("<include>SECURITY.md</include>"));
        assertTrue(assembly.contains("<include>CHANGELOG.md</include>"));
        assertFalse(assembly.contains("<directory>${project.basedir}/docs</directory>"));
        assertFalse(assembly.contains("<include>RELEASE.md</include>"));
        assertFalse(assembly.contains("<include>CONTRIBUTING.md</include>"));
        assertFalse(assembly.contains("<include>CODE_OF_CONDUCT.md</include>"));
        assertTrue(scriptsFileSet.contains("<include>devharness-control-panel.sh</include>"));
        assertTrue(scriptsFileSet.contains("<include>install-agent-adapters.sh</include>"));
        assertFalse(scriptsFileSet.contains("<include>**/*</include>"));
        assertFalse(scriptsFileSet.contains("lark-codex-bridge"));

        assertTrue(releaseGate.contains("require_no_zip_entry \"$entry\""));
        assertTrue(releaseGate.contains("\"docs/\""));
        assertTrue(releaseGate.contains("\"RELEASE.md\""));
        assertTrue(releaseGate.contains("\"scripts/lark-codex-bridge.sh\""));
    }

    @Test
    void versionMetadataGateChecksSchemaVersionDrift() throws Exception {
        String gate = read(Paths.get("scripts/check-version-metadata.sh"));
        String readme = read(Paths.get("README.md"));
        String compatibility = read(Paths.get("docs/COMPATIBILITY.md"));
        String migrations = read(Paths.get("docs/MIGRATIONS.md"));
        String stableContract = read(Paths.get("docs/STABLE_CONTRACT.md"));

        assertTrue(gate.contains("CURRENT_SCHEMA_VERSION = MigrationRunner"));
        assertTrue(gate.contains("Current schema version is v$SCHEMA_VERSION"));
        assertTrue(gate.contains("Current schema version: \\`$SCHEMA_VERSION\\`"));
        assertTrue(readme.contains("Current schema version is v15"));
        assertTrue(compatibility.contains("Current schema version: `15`"));
        assertTrue(migrations.contains("Current schema version: `15`"));
        assertTrue(stableContract.contains("For 1.0 and later"));
        assertTrue(stableContract.contains("under semantic"));
        assertTrue(stableContract.contains("1.0 Boundary Decisions"));
    }

    @Test
    void comateReleaseRulesUseGoalFirstProtocol() throws Exception {
        Path rulesRoot = Paths.get(".comate/rules");

        assertTrue(Files.isRegularFile(rulesRoot.resolve("devharness-goal-protocol.mdr")));
        assertTrue(Files.isRegularFile(rulesRoot.resolve("devharness-graph-aware-protocol.mdr")));
        assertFalse(Files.exists(rulesRoot.resolve("project-memory-bootstrap.mdr")));
        assertFalse(Files.exists(rulesRoot.resolve("java-development-guard.mdr")));

        String goalRule = read(rulesRoot.resolve("devharness-goal-protocol.mdr"));
        String graphRule = read(rulesRoot.resolve("devharness-graph-aware-protocol.mdr"));
        assertTrue(goalRule.contains("quickstart.sh"));
        assertTrue(goalRule.contains("goal-next.sh"));
        assertTrue(goalRule.contains("goal-verify.sh"));
        assertTrue(goalRule.contains("AGENT_BRIEF.json"));
        assertTrue(goalRule.contains("Do not assume a global `dhk` command exists"));
        assertTrue(goalRule.contains("Treat an Agent Brief with `current_action` = `completed` as historical"));
        assertTrue(goalRule.contains("latest user request verbatim"));
        assertTrue(goalRule.contains("Never copy the task from an old Work Brief or GOAL_CONTEXT"));
        assertTrue(goalRule.contains("goal_task_matches_user_request"));
        assertTrue(goalRule.contains("Even typo or documentation-only edits must have a lightweight patch goal"));
        assertTrue(goalRule.contains("After any file edit, immediately run `.agents/skills/devharness-goal-development/scripts/goal-step.sh`"));
        assertTrue(goalRule.contains("Do not put `--field ...` inside `--evidence`"));
        assertTrue(goalRule.contains("--field \"manual_evidence_status=passed\""));
        assertTrue(goalRule.contains("self-repair by recording the missing goal step"));
        assertTrue(goalRule.contains("Harness self-check"));
        assertTrue(goalRule.contains("step_recorded_after_edit"));
        assertTrue(goalRule.contains("Do not bypass goal"));
        assertTrue(graphRule.contains("graph_required=true"));
        assertTrue(graphRule.contains("Do not use --allow-stale"));
    }

    @Test
    void graphAwareSkillPackageContainsRequiredProtocolAndScripts() throws Exception {
        Path skillRoot = Paths.get(".agents/skills/devharness-graph-aware-development");

        assertTrue(Files.isRegularFile(skillRoot.resolve("SKILL.md")));
        assertTrue(Files.isRegularFile(skillRoot.resolve("references/graph-protocol.md")));
        assertTrue(Files.isRegularFile(skillRoot.resolve("references/graph-evidence-format.md")));
        assertTrue(Files.isRegularFile(skillRoot.resolve("references/graph-forbidden-actions.md")));

        assertScriptPair(skillRoot, "graph-index");
        assertScriptPair(skillRoot, "graph-export");
        assertScriptPair(skillRoot, "graph-index-export");
        assertScriptPair(skillRoot, "graph-impact");
        assertScriptPair(skillRoot, "graph-status");

        String skill = read(skillRoot.resolve("SKILL.md"));
        assertTrue(skill.contains("graph-assist"));
        assertTrue(skill.contains("current action"));
        assertTrue(skill.contains("GRAPH_CONTEXT.md"));
        assertTrue(skill.contains("IMPACT_MAP.md"));
        assertTrue(skill.contains("ready_to_complete"));
        assertTrue(skill.contains("snapshot-bound generated facts"));
        assertTrue(skill.contains("AGENT_BRIEF.json"));
        assertTrue(skill.contains("legacy-java-small-fix-with-graph"));
        assertTrue(skill.contains("rollback_plan"));

        String protocol = read(skillRoot.resolve("references/graph-protocol.md"));
        String evidence = read(skillRoot.resolve("references/graph-evidence-format.md"));
        String forbidden = read(skillRoot.resolve("references/graph-forbidden-actions.md"));
        assertTrue(protocol.contains("Re-run `graph-impact` after implementation"));
        assertTrue(protocol.contains("current verify step"));
        assertTrue(protocol.contains("rollback plan artifact"));
        assertTrue(evidence.contains("post_change_impact_map"));
        assertTrue(evidence.contains("changed_files_covered"));
        assertTrue(evidence.contains("manual_evidence_status=passed"));
        assertTrue(forbidden.contains("editing before graph snapshot and impact map are ready"));
        assertTrue(forbidden.contains("graph-assist"));
        assertTrue(forbidden.contains("failed or stale `graph` / `impact` checks"));
        assertTrue(forbidden.contains("protected-impact-risk"));

        assertGraphWrapper(skillRoot, "graph-index", "index");
        assertGraphWrapper(skillRoot, "graph-export", "export");
        assertGraphWrapper(skillRoot, "graph-impact", "impact");
        assertGraphWrapper(skillRoot, "graph-status", "status");
        String indexExportShell = read(skillRoot.resolve("scripts/graph-index-export.sh"));
        String indexExportBatch = read(skillRoot.resolve("scripts/graph-index-export.bat"));
        assertTrue(indexExportShell.contains("graph index"));
        assertTrue(indexExportShell.contains("graph export"));
        assertTrue(indexExportBatch.contains("graph index"));
        assertTrue(indexExportBatch.contains("graph export"));
    }

    @Test
    void agentAdapterInstallerInstallsGoalFirstAdaptersAndRemovesLegacyEntries() throws Exception {
        Path project = tempDir.resolve("adapter-project");
        prepareAdapterProject(project);
        Files.createDirectories(project.resolve(".agents/skills/devharness-java-development"));
        Files.createDirectories(project.resolve(".comate/rules"));
        Files.write(project.resolve(".comate/rules/project-memory-bootstrap.mdr"), bytes("old memory rule"));
        Files.write(project.resolve(".comate/rules/java-development-guard.mdr"), bytes("old java rule"));
        Path jar = tempDir.resolve("dhk.jar");
        Files.write(jar, bytes("fake jar"));

        CommandResult result = runInstaller("--project-root", project.toString(),
                "--target", "all", "--force", "--jar", jar.toString());

        assertEquals(0, result.exitCode, result.stderr);
        assertTrue(Files.isRegularFile(project.resolve(".claude/skills/devharness-goal-development/SKILL.md")));
        assertTrue(Files.isRegularFile(project.resolve(".claude/skills/devharness-graph-aware-development/SKILL.md")));
        assertTrue(Files.isRegularFile(project.resolve("CLAUDE.md")));
        assertTrue(read(project.resolve("CLAUDE.md")).contains("Do not bypass goal"));
        assertTrue(read(project.resolve("CLAUDE.md")).contains("Do not use graph impact --allow-stale"));
        assertTrue(Files.isRegularFile(project.resolve("AGENTS.md")));
        assertTrue(read(project.resolve("AGENTS.md")).contains("Do not call lower-level memory/workflow/spec/db commands"));
        assertTrue(Files.isRegularFile(project.resolve(".comate/rules/devharness-goal-protocol.mdr")));
        assertTrue(Files.isRegularFile(project.resolve(".comate/rules/devharness-graph-aware-protocol.mdr")));
        String comateRule = read(project.resolve(".comate/rules/devharness-goal-protocol.mdr"));
        assertTrue(comateRule.contains("AGENT_BRIEF.json"));
        assertTrue(comateRule.contains("Do not assume a global `dhk` command exists"));
        assertTrue(comateRule.contains("Treat an Agent Brief with `current_action` = `completed` as historical"));
        assertTrue(comateRule.contains("quickstart.sh --task"));
        assertTrue(comateRule.contains("latest user request verbatim"));
        assertTrue(comateRule.contains("Never copy the task from an old Work Brief or GOAL_CONTEXT"));
        assertTrue(comateRule.contains("goal_task_matches_user_request"));
        assertTrue(comateRule.contains("Even typo or documentation-only edits must have a lightweight patch goal"));
        assertTrue(comateRule.contains("After any file edit, immediately run `.agents/skills/devharness-goal-development/scripts/goal-step.sh`"));
        assertTrue(comateRule.contains("Do not put `--field ...` inside `--evidence`"));
        assertTrue(comateRule.contains("--field \"manual_evidence_status=passed\""));
        assertTrue(comateRule.contains("self-repair by recording the missing goal step"));
        assertTrue(comateRule.contains("Harness self-check"));
        assertTrue(read(project.resolve(".comate/rules/devharness-graph-aware-protocol.mdr"))
                .contains("Do not use --allow-stale"));
        assertFalse(Files.exists(project.resolve(".claude/skills/devharness-java-development")));
        assertFalse(Files.exists(project.resolve(".agents/skills/devharness-java-development")));
        assertFalse(Files.exists(project.resolve(".comate/rules/project-memory-bootstrap.mdr")));
        assertFalse(Files.exists(project.resolve(".comate/rules/java-development-guard.mdr")));
        assertTrue(Files.isRegularFile(project.resolve(".agents/tools/devharness-kit/dhk.jar")));
        assertTrue(Files.isExecutable(project.resolve(".claude/skills/devharness-goal-development/scripts/goal-start.sh")));
        assertTrue(Files.isExecutable(project.resolve(".agents/skills/devharness-goal-development/scripts/goal-start.sh")));
    }

    @Test
    void controlPanelConfigureWritesManifestInstallStateAndAdapters() throws Exception {
        Path project = tempDir.resolve("control-panel-project");
        prepareAdapterProject(project);
        Files.createDirectories(project.resolve(".agents/skills/devharness-java-development"));
        Files.createDirectories(project.resolve(".comate/rules"));
        Files.write(project.resolve(".comate/rules/project-memory-bootstrap.mdr"), bytes("old memory rule"));
        Files.write(project.resolve(".comate/rules/java-development-guard.mdr"), bytes("old java rule"));

        CommandResult result = runControlPanel("configure",
                "--project-root", project.toString(),
                "--target", "all",
                "--preset", "springboot-manual-ide-test",
                "--compile-mode", "manual",
                "--test-mode", "manual",
                "--graph", "required",
                "--force");

        assertEquals(0, result.exitCode, result.stderr);
        assertTrue(Files.isRegularFile(project.resolve(".agents/devharness/config.json")));
        assertTrue(Files.isRegularFile(project.resolve(".agents/devharness/policy.json")));
        assertTrue(Files.isRegularFile(project.resolve(".agents/devharness/agent-manifest.json")));
        assertTrue(Files.isRegularFile(project.resolve(".agents/devharness/install-state.json")));
        assertTrue(Files.isRegularFile(project.resolve(".agents/graph/config.json")));
        assertTrue(Files.isRegularFile(project.resolve(".claude/skills/devharness-goal-development/SKILL.md")));
        assertTrue(Files.isRegularFile(project.resolve(".claude/skills/devharness-graph-aware-development/SKILL.md")));
        assertTrue(Files.isRegularFile(project.resolve("AGENTS.md")));
        assertTrue(Files.isRegularFile(project.resolve("CLAUDE.md")));
        assertTrue(Files.isRegularFile(project.resolve(".comate/rules/devharness-goal-protocol.mdr")));
        assertTrue(Files.isRegularFile(project.resolve(".comate/rules/devharness-graph-aware-protocol.mdr")));
        assertFalse(Files.exists(project.resolve(".agents/skills/devharness-java-development")));
        assertFalse(Files.exists(project.resolve(".comate/rules/project-memory-bootstrap.mdr")));
        assertFalse(Files.exists(project.resolve(".comate/rules/java-development-guard.mdr")));

        String config = read(project.resolve(".agents/devharness/config.json"));
        assertTrue(config.contains("\"schema_version\": \"devharness-config/v1-alpha\""));
        assertTrue(config.contains("\"verification.compile.mode\": \"manual\""));
        assertTrue(config.contains("\"verification.test.mode\": \"manual\""));
        assertTrue(config.contains("\"verification.graph.mode\": \"required\""));
        assertTrue(config.contains("\"verification.graph.required\": \"true\""));
        assertFalse(config.contains("\"verification\": {"));

        String manifest = read(project.resolve(".agents/devharness/agent-manifest.json"));
        assertTrue(manifest.contains("\"default_skill\": \"devharness-goal-development\""));
        assertTrue(manifest.contains("\"name\": \"devharness-graph-aware-development\""));
        assertTrue(manifest.contains("\"remove_memory_first_skill\": true"));

        String state = read(project.resolve(".agents/devharness/install-state.json"));
        assertTrue(state.contains("\"target\": \"all\""));
        assertTrue(state.contains("\"mode\": \"copy\""));
        assertTrue(state.contains("\"preset\": \"springboot-manual-ide-test\""));
        assertTrue(state.contains("\"compile_mode\": \"manual\""));
        assertTrue(state.contains("\"test_mode\": \"manual\""));
        assertTrue(state.contains("\"graph\": \"required\""));
        assertTrue(state.contains("\"managed_files\": ["));
        assertTrue(state.contains("\"path\": \"CLAUDE.md\""));
        assertTrue(state.contains("\"path\": \"AGENTS.md\""));
        assertTrue(state.contains("\"sha256\": \""));

        CommandResult statusJson = runControlPanel("status",
                "--project-root", project.toString(),
                "--status-format", "json");
        assertEquals(0, statusJson.exitCode, statusJson.stderr);
        assertTrue(statusJson.stdout.contains("\"manifest\": \"ok\""));
        assertTrue(statusJson.stdout.contains("\"install_state\": \"ok\""));
        assertTrue(statusJson.stdout.contains("\"legacy_memory_first\": \"removed\""));

        CommandResult statusMarkdown = runControlPanel("status",
                "--project-root", project.toString(),
                "--status-format", "markdown");
        assertEquals(0, statusMarkdown.exitCode, statusMarkdown.stderr);
        assertTrue(statusMarkdown.stdout.contains("| manifest | ok |"));
        assertTrue(statusMarkdown.stdout.contains("| comate_graph_adapter | ok |"));

        CommandResult doctor = runControlPanel("doctor",
                "--project-root", project.toString(),
                "--target", "all");
        assertEquals(0, doctor.exitCode, doctor.stderr);
        assertTrue(doctor.stdout.contains("doctor: ok"));
    }

    @Test
    void controlPanelConfigureSupportsDemoNoBuildPresetForEmptyProjects() throws Exception {
        Path project = tempDir.resolve("control-panel-demo-project");
        prepareAdapterProject(project);
        Path jar = tempDir.resolve("dhk.jar");
        Files.write(jar, bytes("fake jar"));

        CommandResult result = runControlPanel("configure",
                "--project-root", project.toString(),
                "--target", "comate",
                "--preset", "demo-no-build",
                "--jar", jar.toString(),
                "--force");

        assertEquals(0, result.exitCode, result.stderr);
        String config = read(project.resolve(".agents/devharness/config.json"));
        String state = read(project.resolve(".agents/devharness/install-state.json"));
        assertTrue(config.contains("\"preset\": \"demo-no-build\""));
        assertTrue(config.contains("\"project.type\": \"demo-no-build\""));
        assertTrue(config.contains("\"project.runtime\": \"local-demo\""));
        assertTrue(config.contains("\"verification.compile.mode\": \"disabled\""));
        assertTrue(config.contains("\"verification.test.mode\": \"disabled\""));
        assertTrue(config.contains("\"verification.graph.mode\": \"off\""));
        assertTrue(state.contains("\"preset\": \"demo-no-build\""));
        assertTrue(state.contains("\"compile_mode\": \"disabled\""));
        assertTrue(state.contains("\"test_mode\": \"disabled\""));
        assertTrue(state.contains("\"graph\": \"off\""));
        assertTrue(Files.isRegularFile(project.resolve(".comate/rules/devharness-goal-protocol.mdr")));
    }

    @Test
    void controlPanelPlanAndUninstallDryRunDoNotModifyTargetProject() throws Exception {
        Path project = tempDir.resolve("control-panel-dry-run-project");
        prepareAdapterProject(project);

        CommandResult plan = runControlPanel("plan",
                "--project-root", project.toString(),
                "--target", "all",
                "--dry-run");

        assertEquals(0, plan.exitCode, plan.stderr);
        assertTrue(plan.stdout.contains("DevHarness plan"));
        assertTrue(plan.stdout.contains("will_write:"));
        assertTrue(plan.stdout.contains("will_copy:"));
        assertTrue(plan.stdout.contains("will_remove:"));
        assertTrue(plan.stdout.contains("plan is read-only"));
        assertFalse(Files.exists(project.resolve(".agents/devharness/config.json")));
        assertFalse(Files.exists(project.resolve(".claude")));
        assertFalse(Files.exists(project.resolve("AGENTS.md")));

        CommandResult configure = runControlPanel("configure",
                "--project-root", project.toString(),
                "--target", "all",
                "--force");
        assertEquals(0, configure.exitCode, configure.stderr);
        assertTrue(Files.isRegularFile(project.resolve("AGENTS.md")));

        CommandResult dryRunUninstall = runControlPanel("uninstall",
                "--project-root", project.toString(),
                "--target", "all",
                "--dry-run");
        assertEquals(0, dryRunUninstall.exitCode, dryRunUninstall.stderr);
        assertTrue(dryRunUninstall.stdout.contains("[dry-run]"));
        assertTrue(Files.isRegularFile(project.resolve("AGENTS.md")));
        assertTrue(Files.isRegularFile(project.resolve("CLAUDE.md")));

        CommandResult uninstall = runControlPanel("uninstall",
                "--project-root", project.toString(),
                "--target", "all");
        assertEquals(0, uninstall.exitCode, uninstall.stderr);
        assertFalse(Files.exists(project.resolve("AGENTS.md")));
        assertFalse(Files.exists(project.resolve("CLAUDE.md")));
        assertFalse(Files.exists(project.resolve(".comate/rules/devharness-goal-protocol.mdr")));
    }

    @Test
    void controlPanelDoctorFailsWithRepairSuggestionWhenAdaptersAreMissing() throws Exception {
        Path project = tempDir.resolve("control-panel-doctor-project");
        prepareAdapterProject(project);

        CommandResult configure = runControlPanel("configure",
                "--project-root", project.toString(),
                "--target", "all",
                "--force");
        assertEquals(0, configure.exitCode, configure.stderr);
        Files.delete(project.resolve("AGENTS.md"));

        CommandResult doctor = runControlPanel("doctor",
                "--project-root", project.toString(),
                "--target", "all");

        assertEquals(3, doctor.exitCode);
        assertTrue(doctor.stdout.contains("opencode_adapter: missing"));
        assertTrue(doctor.stderr.contains("doctor warning: missing AGENTS.md"));
        assertTrue(doctor.stderr.contains("doctor suggestion: run scripts/devharness-control-panel.sh repair"));

        CommandResult repair = runControlPanel("repair",
                "--project-root", project.toString(),
                "--target", "all",
                "--force");
        assertEquals(0, repair.exitCode, repair.stderr);
        assertTrue(Files.isRegularFile(project.resolve("AGENTS.md")));
    }

    @Test
    void controlPanelDoctorDetectsManagedFileDriftAndRepairRefreshesState() throws Exception {
        Path project = tempDir.resolve("control-panel-drift-project");
        prepareAdapterProject(project);

        CommandResult configure = runControlPanel("configure",
                "--project-root", project.toString(),
                "--target", "all",
                "--force");
        assertEquals(0, configure.exitCode, configure.stderr);

        Files.write(project.resolve("AGENTS.md"), bytes("locally edited adapter"));

        CommandResult doctor = runControlPanel("doctor",
                "--project-root", project.toString(),
                "--target", "all");
        assertEquals(3, doctor.exitCode);
        assertTrue(doctor.stderr.contains("doctor warning: managed file drift AGENTS.md"), doctor.stderr);
        assertTrue(doctor.stderr.contains("doctor suggestion: run scripts/devharness-control-panel.sh repair"));

        CommandResult repair = runControlPanel("repair",
                "--project-root", project.toString(),
                "--target", "all",
                "--force");
        assertEquals(0, repair.exitCode, repair.stderr);

        CommandResult doctorAfterRepair = runControlPanel("doctor",
                "--project-root", project.toString(),
                "--target", "all");
        assertEquals(0, doctorAfterRepair.exitCode, doctorAfterRepair.stderr);
        assertTrue(doctorAfterRepair.stdout.contains("doctor: ok"));
    }

    @Test
    void agentAdapterInstallerDryRunDoesNotModifyTargetProject() throws Exception {
        Path project = tempDir.resolve("dry-run-project");
        prepareAdapterProject(project);

        CommandResult result = runInstaller("--project-root", project.toString(),
                "--target", "all", "--dry-run");

        assertEquals(0, result.exitCode, result.stderr);
        assertTrue(result.stdout.contains("[dry-run]"));
        assertFalse(Files.exists(project.resolve(".claude")));
        assertFalse(Files.exists(project.resolve("CLAUDE.md")));
        assertFalse(Files.exists(project.resolve("AGENTS.md")));
        assertFalse(Files.exists(project.resolve(".comate")));
    }

    @Test
    void agentAdapterInstallerBootstrapsMissingPackagedSkill() throws Exception {
        Path project = tempDir.resolve("missing-skill-project");
        Files.createDirectories(project.resolve(".agents/skills"));
        copyTree(Paths.get(".agents/skills/devharness-goal-development"),
                project.resolve(".agents/skills/devharness-goal-development"));

        CommandResult result = runInstaller("--project-root", project.toString(),
                "--target", "all", "--force");

        assertEquals(0, result.exitCode, result.stderr);
        assertTrue(Files.isRegularFile(project.resolve(".agents/skills/devharness-graph-aware-development/SKILL.md")));
        assertTrue(Files.isRegularFile(project.resolve(".claude/skills/devharness-graph-aware-development/SKILL.md")));
        assertTrue(Files.isRegularFile(project.resolve(".comate/rules/devharness-graph-aware-protocol.mdr")));
    }

    @Test
    void formalAgentAdapterInstallerScriptExistsAndIsExecutable() {
        Path rootInstaller = Paths.get("install.sh");
        Path installer = Paths.get("scripts/install-agent-adapters.sh");
        Path controlPanel = Paths.get("scripts/devharness-control-panel.sh");
        Path versionMetadata = Paths.get("scripts/check-version-metadata.sh");
        Path coverageThreshold = Paths.get("scripts/check-coverage-threshold.sh");
        assertTrue(Files.isRegularFile(rootInstaller));
        assertTrue(Files.isExecutable(rootInstaller));
        assertTrue(Files.isRegularFile(installer));
        assertTrue(Files.isExecutable(installer));
        assertTrue(Files.isRegularFile(controlPanel));
        assertTrue(Files.isExecutable(controlPanel));
        assertTrue(Files.isRegularFile(versionMetadata));
        assertTrue(Files.isExecutable(versionMetadata));
        assertTrue(Files.isRegularFile(coverageThreshold));
        assertTrue(Files.isExecutable(coverageThreshold));
    }

    @Test
    void agentAdapterInstallerDocumentsGuidedJarFirstSetup() throws Exception {
        String rootInstaller = read(Paths.get("install.sh"));
        String installer = read(Paths.get("scripts/install-agent-adapters.sh"));

        assertTrue(rootInstaller.contains("scripts/install-agent-adapters.sh"));
        assertTrue(rootInstaller.contains("release package root"));
        assertTrue(rootInstaller.contains("DevHarnessKit 安装向导"));
        assertTrue(rootInstaller.contains("代码项目绝对路径"));
        assertTrue(rootInstaller.contains("使用场景"));
        assertTrue(installer.contains("DevHarnessKit 交互式安装向导"));
        assertTrue(installer.contains("傻瓜式安装流程"));
        assertTrue(installer.contains("项目绝对路径"));
        assertTrue(installer.contains("请输入以 / 开头的绝对路径"));
        assertFalse(installer.contains("~/Desktop"));
        assertFalse(installer.contains("Desktop/simple-java"));
        assertTrue(installer.contains("使用场景"));
        assertTrue(installer.contains("Please run this installer from the DevHarnessKit release package"));
        assertTrue(installer.contains("lib/dhk.jar"));
        assertTrue(installer.contains("install_packaged_assets_to_project"));
        assertFalse(installer.contains("/Users/yangyang/Desktop/DevHarnessKit"));

        CommandResult help = runInstaller("--help");
        assertEquals(0, help.exitCode, help.stderr);
        assertTrue(help.stdout.contains("DevHarnessKit 交互式安装向导"));
        assertTrue(help.stdout.contains("傻瓜式安装流程"));
    }

    @Test
    void scriptedInstallerBootstrapsPackagedAssetsIntoEmptyProject() throws Exception {
        Path project = tempDir.resolve("empty-scripted-install-project");
        Files.createDirectories(project);
        Path jar = tempDir.resolve("dhk.jar");
        Files.write(jar, bytes("fake jar"));

        CommandResult result = runInstaller("--project-root", project.toString(),
                "--target", "comate",
                "--jar", jar.toString(),
                "--force");

        assertEquals(0, result.exitCode, result.stderr);
        assertTrue(result.stdout.contains("configure complete"), result.stdout);
        assertTrue(Files.isRegularFile(project.resolve(".agents/devharness/config.json")));
        assertTrue(Files.isRegularFile(project.resolve(".agents/devharness/policy.json")));
        assertTrue(Files.isRegularFile(project.resolve(".agents/devharness/agent-manifest.json")));
        assertTrue(Files.isRegularFile(project.resolve(".agents/devharness/install-state.json")));
        assertTrue(Files.isRegularFile(project.resolve(".agents/graph/config.json")));
        assertTrue(Files.isRegularFile(project.resolve(".agents/skills/devharness-goal-development/SKILL.md")));
        assertTrue(Files.isRegularFile(project.resolve(".agents/skills/devharness-goal-development/scripts/quickstart.sh")));
        assertTrue(Files.isRegularFile(project.resolve(".agents/skills/devharness-graph-aware-development/SKILL.md")));
        assertTrue(Files.isRegularFile(project.resolve(".comate/rules/devharness-goal-protocol.mdr")));
        assertTrue(Files.isRegularFile(project.resolve(".agents/tools/devharness-kit/dhk.jar")));
    }

    private void assertScriptPair(Path skillRoot, String name) {
        assertTrue(Files.isRegularFile(skillRoot.resolve("scripts/" + name + ".sh")));
        assertTrue(Files.isRegularFile(skillRoot.resolve("scripts/" + name + ".bat")));
    }

    private void assertProjectRootInjection(Path skillRoot) throws Exception {
        String dhkSh = read(skillRoot.resolve("scripts/dhk.sh"));
        String dhkBat = read(skillRoot.resolve("scripts/dhk.bat"));
        assertTrue(dhkSh.contains("PROJECT_ROOT=$(CDPATH= cd -- \"$SCRIPT_DIR/../../../..\" && pwd)"));
        assertTrue(dhkSh.contains("--project-root \"$PROJECT_ROOT\""));
        assertTrue(dhkSh.contains("dhk-cli-*-all.jar"));
        assertFalse(dhkSh.matches("(?s).*target/dhk-cli-[0-9].*-all\\.jar.*"));
        assertTrue(dhkBat.contains("PROJECT_ROOT=%%~fI"));
        assertTrue(dhkBat.contains("--project-root \"%PROJECT_ROOT%\""));
        assertTrue(dhkBat.contains("dhk-cli-*-all.jar"));
        assertFalse(dhkBat.matches("(?s).*target\\\\dhk-cli-[0-9].*-all\\.jar.*"));
    }

    private void assertGoalWrapper(Path skillRoot, String scriptName, String goalCommand) throws Exception {
        String shell = read(skillRoot.resolve("scripts/" + scriptName + ".sh"));
        String batch = read(skillRoot.resolve("scripts/" + scriptName + ".bat"));
        assertTrue(shell.contains("exec \"$SCRIPT_DIR/dhk.sh\" goal " + goalCommand + " \"$@\""));
        assertTrue(batch.contains("dhk.bat\" goal " + goalCommand + " %*"));
    }

    private void assertQuickstartWrapper(Path skillRoot) throws Exception {
        String shell = read(skillRoot.resolve("scripts/quickstart.sh"));
        String batch = read(skillRoot.resolve("scripts/quickstart.bat"));
        assertTrue(shell.contains("exec \"$SCRIPT_DIR/dhk.sh\" quickstart \"$@\""));
        assertTrue(batch.contains("dhk.bat\" quickstart %*"));
    }

    private void assertGraphWrapper(Path skillRoot, String scriptName, String graphCommand) throws Exception {
        String shell = read(skillRoot.resolve("scripts/" + scriptName + ".sh"));
        String batch = read(skillRoot.resolve("scripts/" + scriptName + ".bat"));
        assertTrue(shell.contains("devharness-goal-development/scripts/dhk.sh\" graph "
                + graphCommand + " \"$@\""));
        assertTrue(batch.contains("devharness-goal-development\\scripts\\dhk.bat\" graph "
                + graphCommand + " %*"));
    }

    private String read(Path path) throws Exception {
        return new String(Files.readAllBytes(path), "UTF-8");
    }

    private void prepareAdapterProject(Path project) throws Exception {
        copyTree(Paths.get(".agents/skills/devharness-goal-development"),
                project.resolve(".agents/skills/devharness-goal-development"));
        copyTree(Paths.get(".agents/skills/devharness-graph-aware-development"),
                project.resolve(".agents/skills/devharness-graph-aware-development"));
    }

    private void copyTree(Path source, Path target) throws Exception {
        try (Stream<Path> paths = Files.walk(source)) {
            List<Path> all = new ArrayList<Path>();
            paths.forEach(all::add);
            for (Path path : all) {
                Path relative = source.relativize(path);
                Path destination = target.resolve(relative);
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.createDirectories(destination.getParent());
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.COPY_ATTRIBUTES);
                }
            }
        }
    }

    private CommandResult runInstaller(String... args) throws Exception {
        List<String> command = new ArrayList<String>();
        command.add("sh");
        command.add(Paths.get("scripts/install-agent-adapters.sh").toAbsolutePath().toString());
        for (String arg : args) {
            command.add(arg);
        }
        Process process = new ProcessBuilder(command)
                .directory(Paths.get(".").toAbsolutePath().toFile())
                .start();
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        drain(process.getInputStream(), stdout);
        drain(process.getErrorStream(), stderr);
        int exitCode = process.waitFor();
        return new CommandResult(exitCode, stdout.toString("UTF-8"), stderr.toString("UTF-8"));
    }

    private CommandResult runControlPanel(String... args) throws Exception {
        List<String> command = new ArrayList<String>();
        command.add("sh");
        command.add(Paths.get("scripts/devharness-control-panel.sh").toAbsolutePath().toString());
        for (String arg : args) {
            command.add(arg);
        }
        Process process = new ProcessBuilder(command)
                .directory(Paths.get(".").toAbsolutePath().toFile())
                .start();
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        drain(process.getInputStream(), stdout);
        drain(process.getErrorStream(), stderr);
        int exitCode = process.waitFor();
        return new CommandResult(exitCode, stdout.toString("UTF-8"), stderr.toString("UTF-8"));
    }

    private void drain(InputStream input, ByteArrayOutputStream output) throws Exception {
        byte[] buffer = new byte[4096];
        int read;
        while ((read = input.read(buffer)) >= 0) {
            output.write(buffer, 0, read);
        }
    }

    private byte[] bytes(String value) throws Exception {
        return value.getBytes("UTF-8");
    }

    private static final class CommandResult {
        private final int exitCode;
        private final String stdout;
        private final String stderr;

        private CommandResult(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }
    }
}
