package com.devharnesskit.dhk.export;

import com.devharnesskit.dhk.model.bdd.BddBinding;
import com.devharnesskit.dhk.model.graph.GraphImpactResult;
import com.devharnesskit.dhk.model.graph.GraphNode;
import com.devharnesskit.dhk.model.graph.ScenarioGraphImpactResult;

import java.time.Instant;

public final class ScenarioGraphImpactRenderer {
    public String render(ScenarioGraphImpactResult result, Instant generatedAt) {
        StringBuilder builder = new StringBuilder();
        builder.append("# SCENARIO_IMPACT_MAP\n\n");
        builder.append("<generated-at>").append(generatedAt.toString()).append("</generated-at>\n\n");
        builder.append("<summary>\n");
        builder.append("- scenario_key: ").append(safe(result.scenarioKey())).append('\n');
        builder.append("- input_bindings: ").append(result.inputBindings().size()).append('\n');
        builder.append("- impact_results: ").append(result.impactResults().size()).append('\n');
        builder.append("- found_results: ").append(result.foundCount()).append('\n');
        builder.append("- related_files: ").append(result.relatedFiles().size()).append('\n');
        builder.append("- related_tests: ").append(result.relatedTests().size()).append('\n');
        builder.append("- related_sql: ").append(result.relatedSql().size()).append('\n');
        builder.append("- risk_nodes: ").append(result.riskNodes().size()).append('\n');
        builder.append("- snapshot_stale: ").append(result.snapshotStale()).append('\n');
        builder.append("- allow_stale: ").append(result.staleAllowed()).append('\n');
        builder.append("</summary>\n\n");

        builder.append("<scenario-impact-boundary>\n");
        builder.append("- source: bdd scenario graph bindings\n");
        builder.append("- accepted_binding_types: file,symbol,sql_table\n");
        builder.append("- precision: advisory\n");
        builder.append("- must_verify_with_tests: true\n");
        builder.append("- do_not_treat_as_correctness_proof: true\n");
        builder.append("</scenario-impact-boundary>\n\n");

        builder.append("<impact-inputs>\n");
        for (BddBinding binding : result.inputBindings()) {
            builder.append("- ").append(safe(binding.bindingType())).append(' ')
                    .append(safe(binding.bindingKey()))
                    .append(" [relation=").append(safe(binding.relation())).append("]\n");
        }
        builder.append("</impact-inputs>\n\n");

        builder.append("<impact-results>\n");
        for (GraphImpactResult impact : result.impactResults()) {
            builder.append("- ").append(safe(impact.request().queryType())).append(' ')
                    .append(safe(impact.request().query()))
                    .append(" [found=").append(impact.found())
                    .append(", related_files=").append(impact.relatedFiles().size())
                    .append(", related_tests=").append(impact.relatedTests().size())
                    .append(", risk_nodes=").append(impact.riskNodes().size())
                    .append(", snapshot_stale=").append(impact.snapshotStale())
                    .append("]\n");
        }
        builder.append("</impact-results>\n\n");

        appendStrings(builder, "related-files", result.relatedFiles());
        appendStrings(builder, "related-tests", result.relatedTests());
        appendNodes(builder, "related-sql", result.relatedSql());
        appendNodes(builder, "risk-nodes", result.riskNodes());
        appendStrings(builder, "recommended-read-files", result.recommendedReadFiles());
        return builder.toString();
    }

    private void appendNodes(StringBuilder builder, String section, Iterable<GraphNode> nodes) {
        builder.append('<').append(section).append(">\n");
        for (GraphNode node : nodes) {
            builder.append("- ").append(safe(node.nodeKind())).append(' ')
                    .append(safe(displayName(node)))
                    .append(" [file=").append(safe(node.relativePath()))
                    .append(", line=").append(node.startLine())
                    .append(", confidence=").append(node.confidence())
                    .append(", source=").append(safe(node.source()))
                    .append(", evidence=").append(safe(node.evidence()))
                    .append("]\n");
        }
        builder.append("</").append(section).append(">\n\n");
    }

    private void appendStrings(StringBuilder builder, String section, Iterable<String> values) {
        builder.append('<').append(section).append(">\n");
        for (String value : values) {
            builder.append("- ").append(safe(value)).append('\n');
        }
        builder.append("</").append(section).append(">\n\n");
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\n', ' ').replace('\r', ' ').replace('[', '(').replace(']', ')');
    }

    private String displayName(GraphNode node) {
        return node.qualifiedName().length() > 0 ? node.qualifiedName() : node.name();
    }
}
