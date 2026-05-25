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
        assertScriptPair(skillRoot, "goal-complete");
        assertScriptPair(skillRoot, "goal-status");
        assertScriptPair(skillRoot, "goal-export");

        String skill = new String(Files.readAllBytes(skillRoot.resolve("SKILL.md")), "UTF-8");
        assertTrue(skill.contains("goal-check.sh --all"));
        assertTrue(skill.contains("goal-evaluate.sh"));
        assertTrue(skill.contains("goal-complete.sh"));
    }

    private void assertScriptPair(Path skillRoot, String name) {
        assertTrue(Files.isRegularFile(skillRoot.resolve("scripts/" + name + ".sh")));
        assertTrue(Files.isRegularFile(skillRoot.resolve("scripts/" + name + ".bat")));
    }
}
