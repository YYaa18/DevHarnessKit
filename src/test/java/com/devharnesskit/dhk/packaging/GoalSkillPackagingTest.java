package com.devharnesskit.dhk.packaging;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class GoalSkillPackagingTest {
    @Test
    void goalFirstSkillPackageContainsRequiredProtocolAndScripts() throws Exception {
        Path skillRoot = Paths.get(".agents/skills/devharness-goal-development");

        assertTrue(Files.isRegularFile(skillRoot.resolve("SKILL.md")));
        assertTrue(Files.isRegularFile(skillRoot.resolve("references/goal-protocol.md")));
        assertTrue(Files.isRegularFile(skillRoot.resolve("references/evidence-format.md")));
        assertTrue(Files.isRegularFile(skillRoot.resolve("references/self-check-format.md")));
        assertTrue(Files.isRegularFile(skillRoot.resolve("references/forbidden-actions.md")));

        assertScriptPair(skillRoot, "dhk");
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
        assertTrue(skill.contains("goal-verify.sh"));
        assertTrue(skill.contains("goal-check.sh --all"));
        assertTrue(skill.contains("goal-evaluate.sh"));
        assertTrue(skill.contains("goal-complete.sh"));
        assertTrue(skill.contains("required evidence keys from GOAL_CONTEXT"));
        assertTrue(skill.contains("Perform only the `current_action`"));

        assertProjectRootInjection(skillRoot);
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
        assertTrue(skill.contains("graph_index_export"));
        assertTrue(skill.contains("graph_impact"));
        assertTrue(skill.contains("GRAPH_CONTEXT.md"));
        assertTrue(skill.contains("IMPACT_MAP.md"));
        assertTrue(skill.contains("ready_to_complete"));
        assertTrue(skill.contains("snapshot-bound generated facts"));
        assertTrue(skill.contains("legacy-java-small-fix-with-graph"));
        assertTrue(skill.contains("rollback_plan"));

        String protocol = read(skillRoot.resolve("references/graph-protocol.md"));
        String evidence = read(skillRoot.resolve("references/graph-evidence-format.md"));
        String forbidden = read(skillRoot.resolve("references/graph-forbidden-actions.md"));
        assertTrue(protocol.contains("Re-run `graph-impact` after implementation"));
        assertTrue(protocol.contains("rollback plan artifact"));
        assertTrue(evidence.contains("post_change_impact_map"));
        assertTrue(evidence.contains("changed_files_covered"));
        assertTrue(evidence.contains("manual_evidence_status=passed"));
        assertTrue(forbidden.contains("editing before graph snapshot and impact map are ready"));
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

    private void assertScriptPair(Path skillRoot, String name) {
        assertTrue(Files.isRegularFile(skillRoot.resolve("scripts/" + name + ".sh")));
        assertTrue(Files.isRegularFile(skillRoot.resolve("scripts/" + name + ".bat")));
    }

    private void assertProjectRootInjection(Path skillRoot) throws Exception {
        String dhkSh = read(skillRoot.resolve("scripts/dhk.sh"));
        String dhkBat = read(skillRoot.resolve("scripts/dhk.bat"));
        assertTrue(dhkSh.contains("PROJECT_ROOT=$(CDPATH= cd -- \"$SCRIPT_DIR/../../../..\" && pwd)"));
        assertTrue(dhkSh.contains("--project-root \"$PROJECT_ROOT\""));
        assertTrue(dhkBat.contains("PROJECT_ROOT=%%~fI"));
        assertTrue(dhkBat.contains("--project-root \"%PROJECT_ROOT%\""));
    }

    private void assertGoalWrapper(Path skillRoot, String scriptName, String goalCommand) throws Exception {
        String shell = read(skillRoot.resolve("scripts/" + scriptName + ".sh"));
        String batch = read(skillRoot.resolve("scripts/" + scriptName + ".bat"));
        assertTrue(shell.contains("exec \"$SCRIPT_DIR/dhk.sh\" goal " + goalCommand + " \"$@\""));
        assertTrue(batch.contains("dhk.bat\" goal " + goalCommand + " %*"));
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
}
