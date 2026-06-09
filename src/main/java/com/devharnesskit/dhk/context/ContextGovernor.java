package com.devharnesskit.dhk.context;

import com.devharnesskit.dhk.context.token.CharsOverFourTokenEstimator;
import com.devharnesskit.dhk.context.token.TokenEstimator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ContextGovernor {
    private final ContextBudget budget;
    private final TokenEstimator estimator;

    public ContextGovernor() {
        this(ContextBudgetPolicy.defaults(), new CharsOverFourTokenEstimator());
    }

    public ContextGovernor(ContextBudget budget, TokenEstimator estimator) {
        this.budget = budget == null ? ContextBudgetPolicy.defaults() : budget;
        this.estimator = estimator == null ? new CharsOverFourTokenEstimator() : estimator;
    }

    public ContextRenderResult render(List<ContextItem> items) {
        List<ContextItem> sorted = items == null
                ? new ArrayList<ContextItem>() : new ArrayList<ContextItem>(items);
        Collections.sort(sorted, new Comparator<ContextItem>() {
            public int compare(ContextItem left, ContextItem right) {
                return left.priority().ordinal() - right.priority().ordinal();
            }
        });
        StringBuilder builder = new StringBuilder();
        List<String> risks = new ArrayList<String>();
        Map<String, Integer> sectionUsage = new LinkedHashMap<String, Integer>();
        int tokenBefore = 0;
        int omitted = 0;
        for (ContextItem item : sorted) {
            tokenBefore += estimator.estimate(item.rawText());
            String chunk = chunk(item);
            int chunkTokens = estimator.estimate(chunk);
            int projected = estimator.estimate(builder.toString() + chunk);
            boolean sectionOk = sectionFits(item.type(), sectionUsage, chunkTokens);
            if ((projected <= budget.totalTokens() && sectionOk) || ContextPriority.REQUIRED.equals(item.priority())) {
                builder.append(chunk);
                addSectionUsage(sectionUsage, item.type(), chunkTokens);
                if (projected > budget.totalTokens()) {
                    risks.add("required_item_exceeds_budget:" + item.sourceRef());
                }
                if (!sectionOk) {
                    risks.add("required_item_exceeds_section_budget:" + section(item.type())
                            + ":" + item.sourceRef());
                }
            } else {
                omitted++;
                risks.add("omitted_item:" + item.sourceRef() + "; section=" + section(item.type()));
            }
        }
        String report = report(tokenBefore, estimator.estimate(builder.toString()), omitted, risks);
        builder.append(report);
        return new ContextRenderResult(builder.toString(), tokenBefore,
                estimator.estimate(builder.toString()), omitted,
                risks.toArray(new String[risks.size()]));
    }

    private String chunk(ContextItem item) {
        StringBuilder builder = new StringBuilder();
        builder.append("<context-item type=\"").append(item.type().name())
                .append("\" priority=\"").append(item.priority().name())
                .append("\" source=\"").append(safe(item.sourceRef())).append("\">\n");
        if (ContextItemType.CODE_BODY.equals(item.type()) && item.compressible()) {
            builder.append("code_body_selection_only: true\n");
            builder.append("read_file: ").append(safe(item.sourceRef())).append('\n');
            builder.append("code_body_omitted: true\n");
            builder.append("</context-item>\n\n");
            return builder.toString();
        }
        builder.append(item.rawText()).append('\n');
        builder.append("</context-item>\n\n");
        return builder.toString();
    }

    private boolean sectionFits(ContextItemType type, Map<String, Integer> sectionUsage, int chunkTokens) {
        int sectionBudget = sectionBudget(type);
        if (sectionBudget <= 0) {
            return true;
        }
        return used(sectionUsage, section(type)) + chunkTokens <= sectionBudget;
    }

    private void addSectionUsage(Map<String, Integer> sectionUsage, ContextItemType type, int chunkTokens) {
        String section = section(type);
        sectionUsage.put(section, Integer.valueOf(used(sectionUsage, section) + chunkTokens));
    }

    private int used(Map<String, Integer> sectionUsage, String section) {
        Integer value = sectionUsage.get(section);
        return value == null ? 0 : value.intValue();
    }

    private int sectionBudget(ContextItemType type) {
        String section = section(type);
        if ("goal".equals(section)) {
            return budget.goalTokens();
        }
        if ("current_step".equals(section)) {
            return budget.currentStepTokens();
        }
        if ("memory".equals(section)) {
            return budget.memoryTokens();
        }
        if ("evidence".equals(section)) {
            return budget.evidenceTokens();
        }
        if ("graph".equals(section)) {
            return budget.graphTokens();
        }
        if ("bdd".equals(section)) {
            return budget.bddTokens();
        }
        if ("risks".equals(section)) {
            return budget.risksTokens();
        }
        return 0;
    }

    private String section(ContextItemType type) {
        if (ContextItemType.GOAL.equals(type) || ContextItemType.AGENT_RULE.equals(type)) {
            return "goal";
        }
        if (ContextItemType.CURRENT_STEP.equals(type)) {
            return "current_step";
        }
        if (ContextItemType.MEMORY_FACT.equals(type) || ContextItemType.MEMORY_CANDIDATE.equals(type)) {
            return "memory";
        }
        if (ContextItemType.BDD_SCENARIO.equals(type)) {
            return "bdd";
        }
        if (ContextItemType.BDD_EVIDENCE.equals(type)
                || ContextItemType.BUILD_LOG.equals(type)
                || ContextItemType.TEST_LOG.equals(type)
                || ContextItemType.SHELL_OUTPUT.equals(type)) {
            return "evidence";
        }
        if (ContextItemType.GRAPH_IMPACT.equals(type)) {
            return "graph";
        }
        if (ContextItemType.RISK.equals(type) || ContextItemType.RECOVERY_STATE.equals(type)) {
            return "risks";
        }
        return "code";
    }

    private String report(int tokenBefore, int tokenAfterBeforeReport, int omitted, List<String> risks) {
        StringBuilder builder = new StringBuilder();
        builder.append("<context-governor-report>\n");
        builder.append("- token_before: ").append(tokenBefore).append('\n');
        builder.append("- token_after_before_report: ").append(tokenAfterBeforeReport).append('\n');
        builder.append("- omitted_items: ").append(omitted).append('\n');
        builder.append("- risks:\n");
        if (risks.isEmpty()) {
            builder.append("  - none\n");
        } else {
            for (String risk : risks) {
                builder.append("  - ").append(risk).append('\n');
            }
        }
        builder.append("</context-governor-report>\n");
        return builder.toString();
    }

    private String safe(String value) {
        return value == null ? "" : value.replace('\n', ' ').replace('\r', ' ');
    }
}
