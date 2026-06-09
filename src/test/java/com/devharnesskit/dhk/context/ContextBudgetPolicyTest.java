package com.devharnesskit.dhk.context;

import com.devharnesskit.dhk.model.config.DevHarnessConfig;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ContextBudgetPolicyTest {
    @Test
    void usesDefaultsWhenConfigIsMissing() {
        ContextBudget budget = ContextBudgetPolicy.fromConfig(null);

        assertEquals(12000, budget.totalTokens());
        assertEquals(2500, budget.goalTokens());
        assertEquals(1500, budget.currentStepTokens());
        assertEquals(3000, budget.memoryTokens());
        assertEquals(2500, budget.graphTokens());
        assertEquals(1500, budget.bddTokens());
        assertEquals(1600, budget.workflowTokens());
        assertEquals(1600, budget.specTokens());
        assertEquals(1800, budget.evidenceTokens());
        assertEquals(800, budget.risksTokens());
        assertEquals(1500, budget.outputHeadroomTokens());
    }

    @Test
    void readsBlueprintBudgetKeysBeforeLegacyAliases() {
        DevHarnessConfig config = config(
                "context.budget.goal", "2100",
                "context.budget.current_step", "1200",
                "context.budget.memory", "2200",
                "context.budget.memory_tokens", "9999",
                "context.budget.graph", "1300",
                "context.budget.bdd", "900",
                "context.budget.workflow", "700",
                "context.budget.spec", "600",
                "context.budget.evidence", "500",
                "context.budget.risks", "400");
        ContextBudget budget = ContextBudgetPolicy.fromConfig(config);

        assertEquals(2100, budget.goalTokens());
        assertEquals(1200, budget.currentStepTokens());
        assertEquals(2200, budget.memoryTokens());
        assertEquals(1300, budget.graphTokens());
        assertEquals(900, budget.bddTokens());
        assertEquals(700, budget.workflowTokens());
        assertEquals(600, budget.specTokens());
        assertEquals(500, budget.evidenceTokens());
        assertEquals(400, budget.risksTokens());
        assertEquals(2200, config.contextBudget("memory", 1));
        assertEquals(1200, config.contextBudget("current-step", 1));
    }

    @Test
    void readsConfiguredPositiveTokenBudgets() {
        ContextBudget budget = ContextBudgetPolicy.fromConfig(config(
                "context.budget.total_tokens", "64000",
                "context.budget.memory_tokens", "8000",
                "context.budget.graph_tokens", "5000",
                "context.budget.workflow_tokens", "3000",
                "context.budget.spec_tokens", "2500",
                "context.budget.evidence_tokens", "7000",
                "context.output.headroom_tokens", "4096"));

        assertEquals(64000, budget.totalTokens());
        assertEquals(8000, budget.memoryTokens());
        assertEquals(5000, budget.graphTokens());
        assertEquals(3000, budget.workflowTokens());
        assertEquals(2500, budget.specTokens());
        assertEquals(7000, budget.evidenceTokens());
        assertEquals(4096, budget.outputHeadroomTokens());
    }

    @Test
    void fallsBackForInvalidOrNonPositiveValues() {
        DevHarnessConfig config = config(
                "context.budget.total_tokens", "not-a-number",
                "context.budget.memory_tokens", "0",
                "context.budget.graph_tokens", "-1");
        ContextBudget budget = ContextBudgetPolicy.fromConfig(config);

        assertEquals(ContextBudget.DEFAULT_TOTAL_TOKENS, budget.totalTokens());
        assertEquals(ContextBudget.DEFAULT_MEMORY_TOKENS, budget.memoryTokens());
        assertEquals(ContextBudget.DEFAULT_GRAPH_TOKENS, budget.graphTokens());
        assertEquals(123, config.contextBudget("missing", 123));
        assertEquals(456, config.contextOutputHeadroomTokens(456));
    }

    @Test
    void contextCompressEvidenceAliasOverridesEnabledKey() {
        DevHarnessConfig config = config(
                "context.compress.enabled", "true",
                "context.compress.evidence", "false");

        assertEquals(false, config.contextCompressEnabled());
    }

    private DevHarnessConfig config(String... keyValues) {
        Map<String, String> values = new LinkedHashMap<String, String>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            values.put(keyValues[i], keyValues[i + 1]);
        }
        return new DevHarnessConfig(values);
    }
}
