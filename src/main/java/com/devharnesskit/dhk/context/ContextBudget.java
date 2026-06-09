package com.devharnesskit.dhk.context;

public final class ContextBudget {
    public static final int DEFAULT_TOTAL_TOKENS = 12000;
    public static final int DEFAULT_GOAL_TOKENS = 2500;
    public static final int DEFAULT_CURRENT_STEP_TOKENS = 1500;
    public static final int DEFAULT_MEMORY_TOKENS = 3000;
    public static final int DEFAULT_GRAPH_TOKENS = 2500;
    public static final int DEFAULT_BDD_TOKENS = 1500;
    public static final int DEFAULT_WORKFLOW_TOKENS = 1600;
    public static final int DEFAULT_SPEC_TOKENS = 1600;
    public static final int DEFAULT_EVIDENCE_TOKENS = 1800;
    public static final int DEFAULT_RISKS_TOKENS = 800;
    public static final int DEFAULT_OUTPUT_HEADROOM_TOKENS = 1500;

    private final int totalTokens;
    private final int goalTokens;
    private final int currentStepTokens;
    private final int memoryTokens;
    private final int graphTokens;
    private final int bddTokens;
    private final int workflowTokens;
    private final int specTokens;
    private final int evidenceTokens;
    private final int risksTokens;
    private final int outputHeadroomTokens;

    public ContextBudget(int totalTokens, int memoryTokens, int graphTokens, int workflowTokens,
                         int specTokens, int evidenceTokens, int outputHeadroomTokens) {
        this(totalTokens, DEFAULT_GOAL_TOKENS, DEFAULT_CURRENT_STEP_TOKENS, memoryTokens, graphTokens,
                DEFAULT_BDD_TOKENS, workflowTokens, specTokens, evidenceTokens, DEFAULT_RISKS_TOKENS,
                outputHeadroomTokens);
    }

    public ContextBudget(int totalTokens, int goalTokens, int currentStepTokens, int memoryTokens,
                         int graphTokens, int bddTokens, int workflowTokens, int specTokens,
                         int evidenceTokens, int risksTokens, int outputHeadroomTokens) {
        this.totalTokens = positive(totalTokens, DEFAULT_TOTAL_TOKENS);
        this.goalTokens = positive(goalTokens, DEFAULT_GOAL_TOKENS);
        this.currentStepTokens = positive(currentStepTokens, DEFAULT_CURRENT_STEP_TOKENS);
        this.memoryTokens = positive(memoryTokens, DEFAULT_MEMORY_TOKENS);
        this.graphTokens = positive(graphTokens, DEFAULT_GRAPH_TOKENS);
        this.bddTokens = positive(bddTokens, DEFAULT_BDD_TOKENS);
        this.workflowTokens = positive(workflowTokens, DEFAULT_WORKFLOW_TOKENS);
        this.specTokens = positive(specTokens, DEFAULT_SPEC_TOKENS);
        this.evidenceTokens = positive(evidenceTokens, DEFAULT_EVIDENCE_TOKENS);
        this.risksTokens = positive(risksTokens, DEFAULT_RISKS_TOKENS);
        this.outputHeadroomTokens = positive(outputHeadroomTokens, DEFAULT_OUTPUT_HEADROOM_TOKENS);
    }

    public static ContextBudget defaults() {
        return new ContextBudget(DEFAULT_TOTAL_TOKENS, DEFAULT_MEMORY_TOKENS, DEFAULT_GRAPH_TOKENS,
                DEFAULT_WORKFLOW_TOKENS, DEFAULT_SPEC_TOKENS, DEFAULT_EVIDENCE_TOKENS,
                DEFAULT_OUTPUT_HEADROOM_TOKENS);
    }

    public int totalTokens() {
        return totalTokens;
    }

    public int goalTokens() {
        return goalTokens;
    }

    public int currentStepTokens() {
        return currentStepTokens;
    }

    public int memoryTokens() {
        return memoryTokens;
    }

    public int graphTokens() {
        return graphTokens;
    }

    public int bddTokens() {
        return bddTokens;
    }

    public int workflowTokens() {
        return workflowTokens;
    }

    public int specTokens() {
        return specTokens;
    }

    public int evidenceTokens() {
        return evidenceTokens;
    }

    public int risksTokens() {
        return risksTokens;
    }

    public int outputHeadroomTokens() {
        return outputHeadroomTokens;
    }

    public int totalChars() {
        return chars(totalTokens);
    }

    public int memoryChars() {
        return chars(memoryTokens);
    }

    public int workflowChars() {
        return chars(workflowTokens);
    }

    public int specChars() {
        return chars(specTokens);
    }

    private int chars(int tokens) {
        if (tokens > Integer.MAX_VALUE / 4) {
            return Integer.MAX_VALUE;
        }
        return tokens * 4;
    }

    private int positive(int value, int defaultValue) {
        return value > 0 ? value : defaultValue;
    }
}
