package com.devharnesskit.dhk.context;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ContextGovernorTest {
    @Test
    void keepsRequiredItemsEvenWhenBudgetIsExceeded() {
        ContextGovernor governor = new ContextGovernor(
                new ContextBudget(20, 10, 10, 10, 10, 10, 5), null);

        ContextRenderResult result = governor.render(Arrays.asList(
                new ContextItem(ContextItemType.MEMORY_FACT, ContextPriority.NORMAL,
                        repeat("normal ", 400), "memory-1", true),
                new ContextItem(ContextItemType.BDD_EVIDENCE, ContextPriority.REQUIRED,
                        repeat("required evidence ", 400), "bdd-required", false)));

        assertTrue(result.text().contains("required evidence"));
        assertTrue(result.text().contains("required_item_exceeds_budget:bdd-required"));
        assertTrue(result.risks().length > 0);
    }

    @Test
    void omitsLowerPriorityItemsWhenBudgetIsFull() {
        ContextGovernor governor = new ContextGovernor(
                new ContextBudget(80, 10, 10, 10, 10, 10, 5), null);

        ContextRenderResult result = governor.render(Arrays.asList(
                new ContextItem(ContextItemType.GOAL, ContextPriority.REQUIRED,
                        repeat("goal ", 20), "goal", false),
                new ContextItem(ContextItemType.MEMORY_CANDIDATE, ContextPriority.LOW,
                        repeat("candidate ", 500), "candidate-1", true)));

        assertEquals(1, result.omittedItems());
        assertTrue(result.text().contains("goal"));
        assertTrue(result.text().contains("omitted_item:candidate-1"));
    }

    @Test
    void codeBodyIsMarkedSelectionOnlyWhenCompressible() {
        ContextGovernor governor = new ContextGovernor(
                new ContextBudget(200, 10, 10, 10, 10, 10, 5), null);

        ContextRenderResult result = governor.render(Arrays.asList(
                new ContextItem(ContextItemType.CODE_BODY, ContextPriority.NORMAL,
                        "class Demo {}", "src/Demo.java", true)));

        assertTrue(result.text().contains("code_body_selection_only: true"));
        assertTrue(result.text().contains("read_file: src/Demo.java"));
        assertFalse(result.text().contains("class Demo {}"));
    }

    @Test
    void appliesSharedSectionBudgetForEvidenceItems() {
        ContextGovernor governor = new ContextGovernor(
                new ContextBudget(1000, 200, 100, 200, 200, 200, 100, 100, 40, 100, 50),
                null);

        ContextRenderResult result = governor.render(Arrays.asList(
                new ContextItem(ContextItemType.BUILD_LOG, ContextPriority.NORMAL,
                        "compile digest", "build-log-1", true),
                new ContextItem(ContextItemType.TEST_LOG, ContextPriority.NORMAL,
                        repeat("test failure digest ", 80), "test-log-1", true)));

        assertTrue(result.text().contains("compile digest"));
        assertEquals(1, result.omittedItems());
        assertTrue(result.text().contains("omitted_item:test-log-1; section=evidence"));
    }

    private String repeat(String text, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(text);
        }
        return builder.toString();
    }
}
