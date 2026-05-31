package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.goal.GoalStep;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class GoalCheckSupportTest {
    @Test
    void latestEvidenceValueReadsFieldArgumentsEmbeddedInEvidenceText() {
        List<GoalStep> steps = Arrays.asList(step("compile_result=passed; --tests-run=manual test; "
                + "--field manual_evidence_status=\"passed\" "
                + "--field compile_scope=\"goal module\" "
                + "--field manual_evidence_path=.agents/verification/manual.md "
                + "--field tester=developer"));

        assertEquals("passed", GoalCheckSupport.latestEvidenceValue(steps, "manual_evidence_status"));
        assertEquals("goal module", GoalCheckSupport.latestEvidenceValue(steps, "compile_scope"));
        assertEquals(".agents/verification/manual.md",
                GoalCheckSupport.latestEvidenceValue(steps, "manual_evidence_path"));
        assertEquals("developer", GoalCheckSupport.latestEvidenceValue(steps, "tester"));
    }

    @Test
    void evidenceValueStillReadsPlainEvidenceAndUsesLastValueInStep() {
        List<GoalStep> steps = Arrays.asList(step("manual_evidence_status=failed; "
                + "manual_evidence_status=passed\ncompile_scope=goal module"));

        assertEquals("passed", GoalCheckSupport.evidenceValue(steps, "manual_evidence_status"));
        assertEquals("goal module", GoalCheckSupport.evidenceValue(steps, "compile_scope"));
    }

    @Test
    void latestEvidenceValueHandlesRepeatedEmbeddedFieldsAcrossSteps() {
        List<GoalStep> steps = Arrays.asList(
                step("--field manual_evidence_status=failed"),
                step("--field manual_evidence_status=passed --field test_scope=GoalCheckSupportTest"));

        assertEquals("passed", GoalCheckSupport.latestEvidenceValue(steps, "manual_evidence_status"));
        assertEquals("GoalCheckSupportTest", GoalCheckSupport.latestEvidenceValue(steps, "test_scope"));
    }

    @Test
    void evidenceValueDoesNotSplitPlainValuesContainingFieldLikeText() {
        List<GoalStep> steps = Arrays.asList(
                step("scope_justification=We set --field inject=false for safety; --field tester=developer"));

        assertEquals("We set --field inject=false for safety",
                GoalCheckSupport.evidenceValue(steps, "scope_justification"));
        assertEquals("", GoalCheckSupport.evidenceValue(steps, "inject"));
        assertEquals("developer", GoalCheckSupport.evidenceValue(steps, "tester"));
    }

    @Test
    void evidenceValueKeepsQuotedFieldValuesContainingFieldLikeText() {
        List<GoalStep> steps = Arrays.asList(step("--field scope_justification=\"We set --field inject=false\" "
                + "--field tester=developer"));

        assertEquals("We set --field inject=false",
                GoalCheckSupport.latestEvidenceValue(steps, "scope_justification"));
        assertEquals("developer", GoalCheckSupport.latestEvidenceValue(steps, "tester"));
    }

    @Test
    void changedFileAggregationIgnoresUnavailableSentinels() {
        Set<String> files = new LinkedHashSet<String>();

        GoalCheckSupport.addChangedFiles(files, Paths.get("/tmp/project"),
                "unavailable, src/main/java/Demo.java; none\n./src/test/java/DemoTest.java");

        assertEquals(Arrays.asList("src/main/java/Demo.java", "src/test/java/DemoTest.java"),
                new ArrayList<String>(files));
    }

    @Test
    void stepEvidenceValidatorChangedFilesIgnoresUnavailableSentinels() {
        GoalStepEvidenceValidator validator = new GoalStepEvidenceValidator();
        List<GoalStep> steps = Arrays.asList(
                stepWithChangedFiles("unavailable"),
                stepWithChangedFiles("src/main/java/Demo.java\nunknown"));

        assertEquals("src/main/java/Demo.java", validator.changedFiles(steps));
    }

    private GoalStep step(String evidence) {
        return new GoalStep(1L, "goal", 1, "verify", "summary", "", evidence,
                "recorded", "now");
    }

    private GoalStep stepWithChangedFiles(String changedFiles) {
        return new GoalStep(1L, "goal", 1, "verify", "summary", changedFiles, "",
                "recorded", "now");
    }
}
