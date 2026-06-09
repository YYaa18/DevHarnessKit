package com.devharnesskit.dhk.context;

import com.devharnesskit.dhk.model.config.DevHarnessConfig;

public final class ContextBudgetPolicy {
    private ContextBudgetPolicy() {
    }

    public static ContextBudget defaults() {
        return ContextBudget.defaults();
    }

    public static ContextBudget fromConfig(DevHarnessConfig config) {
        if (config == null) {
            return defaults();
        }
        return new ContextBudget(
                config.contextBudgetTotalTokens(ContextBudget.DEFAULT_TOTAL_TOKENS),
                config.contextBudget("goal", ContextBudget.DEFAULT_GOAL_TOKENS),
                config.contextBudget("current_step", ContextBudget.DEFAULT_CURRENT_STEP_TOKENS),
                config.contextBudget("memory", ContextBudget.DEFAULT_MEMORY_TOKENS),
                config.contextBudget("graph", ContextBudget.DEFAULT_GRAPH_TOKENS),
                config.contextBudget("bdd", ContextBudget.DEFAULT_BDD_TOKENS),
                config.contextBudget("workflow", ContextBudget.DEFAULT_WORKFLOW_TOKENS),
                config.contextBudget("spec", ContextBudget.DEFAULT_SPEC_TOKENS),
                config.contextBudget("evidence", ContextBudget.DEFAULT_EVIDENCE_TOKENS),
                config.contextBudget("risks", ContextBudget.DEFAULT_RISKS_TOKENS),
                config.contextOutputHeadroomTokens(ContextBudget.DEFAULT_OUTPUT_HEADROOM_TOKENS));
    }
}
