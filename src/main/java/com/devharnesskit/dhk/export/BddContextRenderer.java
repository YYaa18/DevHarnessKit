package com.devharnesskit.dhk.export;

import com.devharnesskit.dhk.model.bdd.BddFeature;
import com.devharnesskit.dhk.model.bdd.BddBinding;
import com.devharnesskit.dhk.model.bdd.BddScenarioView;
import com.devharnesskit.dhk.model.bdd.BddStep;

import java.util.List;

public final class BddContextRenderer {
    public String renderContext(List<BddFeature> features, List<BddScenarioView> scenarios,
                                String generatedAt) {
        StringBuilder builder = new StringBuilder();
        builder.append("# BDD_CONTEXT\n\n");
        builder.append("<generated-at>").append(generatedAt).append("</generated-at>\n\n");
        builder.append("<boundary>\n");
        builder.append("- BDD rows are specification-level acceptance context\n");
        builder.append("- BDD exports are generated artifacts, not source of truth\n");
        builder.append("- scenario evidence and coverage are separate checks\n");
        builder.append("</boundary>\n\n");
        builder.append("<features>\n");
        for (BddFeature feature : features) {
            builder.append("- ").append(feature.featureKey()).append(" [")
                    .append(feature.status()).append("] ")
                    .append(feature.title()).append(" (module=")
                    .append(feature.moduleName()).append(")\n");
        }
        builder.append("</features>\n\n");
        builder.append("<scenarios>\n");
        for (BddScenarioView view : scenarios) {
            builder.append("- ").append(view.scenario().scenarioKey()).append(" [")
                    .append(view.scenario().status()).append("] ")
                    .append(view.scenario().title()).append('\n');
            builder.append("  feature: ").append(view.feature().featureKey()).append('\n');
            builder.append("  type: ").append(view.scenario().scenarioType()).append('\n');
            builder.append("  priority: ").append(view.scenario().priority()).append('\n');
            if (view.scenario().tags().length() > 0) {
                builder.append("  tags: ").append(view.scenario().tags()).append('\n');
            }
            for (BddStep step : view.steps()) {
                builder.append("  ").append(step.stepType()).append(": ")
                        .append(step.stepText()).append('\n');
            }
            appendBindings(builder, view.bindings(), "  ");
        }
        builder.append("</scenarios>\n\n");
        builder.append("<agent-instructions>\n");
        builder.append("- Treat BDD scenarios as acceptance intent, not proof of implementation\n");
        builder.append("- Do not claim behavior is verified until evidence and checks pass\n");
        builder.append("- Keep scenario changes small and tied to the current goal/spec\n");
        builder.append("</agent-instructions>\n");
        return limit(builder.toString(), 20 * 1024);
    }

    public String renderFeature(BddFeature feature, List<BddScenarioView> scenarios) {
        StringBuilder builder = new StringBuilder();
        builder.append("Feature: ").append(feature.title()).append('\n');
        if (feature.description().length() > 0) {
            builder.append("  ").append(feature.description()).append('\n');
        }
        builder.append('\n');
        for (BddScenarioView view : scenarios) {
            if (!feature.featureKey().equals(view.feature().featureKey())) {
                continue;
            }
            if (view.scenario().tags().length() > 0) {
                builder.append("  @").append(view.scenario().tags().replace(",", " @")).append('\n');
            }
            if (!view.bindings().isEmpty()) {
                builder.append("  # bindings: ");
                for (int i = 0; i < view.bindings().size(); i++) {
                    if (i > 0) {
                        builder.append("; ");
                    }
                    BddBinding binding = view.bindings().get(i);
                    builder.append(binding.bindingType()).append(' ')
                            .append(binding.bindingKey()).append(" (")
                            .append(binding.relation()).append(')');
                }
                builder.append('\n');
            }
            builder.append("  Scenario: ").append(view.scenario().title()).append('\n');
            for (BddStep step : view.steps()) {
                builder.append("    ").append(capitalize(step.stepType())).append(' ')
                        .append(step.stepText()).append('\n');
            }
            builder.append('\n');
        }
        return builder.toString();
    }

    private void appendBindings(StringBuilder builder, List<BddBinding> bindings, String indent) {
        if (bindings.isEmpty()) {
            return;
        }
        builder.append(indent).append("bindings:\n");
        for (BddBinding binding : bindings) {
            builder.append(indent).append("- ").append(binding.bindingType()).append(' ')
                    .append(binding.bindingKey()).append(" (")
                    .append(binding.relation()).append(")\n");
        }
    }

    private String capitalize(String value) {
        if (value == null || value.length() == 0) {
            return "";
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private String limit(String text, int maxChars) {
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars - 80) + "\n\n<!-- truncated: bdd context budget exceeded -->\n";
    }
}
