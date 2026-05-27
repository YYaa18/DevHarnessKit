package com.devharnesskit.dhk.export;

import com.devharnesskit.dhk.model.bdd.BddEvidence;
import com.devharnesskit.dhk.model.bdd.BddAdapterResult;
import com.devharnesskit.dhk.service.bdd.BddVerificationService.BddScenarioEvidenceResult;
import com.devharnesskit.dhk.service.bdd.BddVerificationService.BddVerificationResult;

public final class BddEvidenceRenderer {
    public String renderEvidence(BddVerificationResult result, String generatedAt) {
        StringBuilder builder = new StringBuilder();
        builder.append("# BDD_EVIDENCE\n\n");
        builder.append("<generated-at>").append(generatedAt).append("</generated-at>\n\n");
        builder.append("<summary>\n");
        builder.append("- scenario_count: ").append(result.scenarioCount()).append('\n');
        builder.append("- covered_count: ").append(result.coveredCount()).append('\n');
        builder.append("- missing_count: ").append(result.missingCount()).append('\n');
        builder.append("- pending_count: ").append(result.pendingCount()).append('\n');
        builder.append("- failed_count: ").append(result.failedCount()).append('\n');
        if (result.goalKey().length() > 0) {
            builder.append("- goal_key: ").append(result.goalKey()).append('\n');
        }
        builder.append("</summary>\n\n");
        builder.append("<scenario-evidence>\n");
        for (BddScenarioEvidenceResult scenario : result.scenarios()) {
            builder.append("- ").append(scenario.view().scenario().scenarioKey())
                    .append(" [").append(scenario.status()).append("] ")
                    .append(scenario.view().scenario().title()).append('\n');
            BddAdapterResult adapterResult = scenario.adapterResult();
            if (adapterResult != null) {
                builder.append("  adapter: ").append(adapterResult.adapterKey())
                        .append(" source_status=").append(adapterResult.sourceStatus())
                        .append(" normalized_status=").append(adapterResult.normalizedStatus())
                        .append('\n');
            }
            for (BddEvidence evidence : scenario.evidence()) {
                builder.append("  - #").append(evidence.id()).append(' ')
                        .append(evidence.status()).append(' ')
                        .append(evidence.evidenceType());
                if (evidence.goalKey().length() > 0) {
                    builder.append(" goal=").append(evidence.goalKey());
                }
                builder.append(": ").append(evidence.summary()).append('\n');
                if (evidence.evidencePath().length() > 0) {
                    builder.append("    path: ").append(evidence.evidencePath()).append('\n');
                }
                if (evidence.command().length() > 0) {
                    builder.append("    command: ").append(evidence.command()).append('\n');
                }
            }
        }
        builder.append("</scenario-evidence>\n");
        return builder.toString();
    }

    public String renderCoverage(BddVerificationResult result, String generatedAt) {
        StringBuilder builder = new StringBuilder();
        builder.append("# BDD_COVERAGE\n\n");
        builder.append("<generated-at>").append(generatedAt).append("</generated-at>\n\n");
        builder.append("<coverage-summary>\n");
        builder.append("- scenario_count: ").append(result.scenarioCount()).append('\n');
        builder.append("- covered_count: ").append(result.coveredCount()).append('\n');
        builder.append("- missing_count: ").append(result.missingCount()).append('\n');
        builder.append("- pending_count: ").append(result.pendingCount()).append('\n');
        builder.append("- failed_count: ").append(result.failedCount()).append('\n');
        builder.append("- coverage_status: ").append(result.passed() ? "passed" : "incomplete").append('\n');
        builder.append("</coverage-summary>\n\n");
        builder.append("<scenario-coverage>\n");
        for (BddScenarioEvidenceResult scenario : result.scenarios()) {
            builder.append("- ").append(scenario.view().scenario().scenarioKey())
                    .append(" [").append(scenario.status()).append("] ")
                    .append(scenario.view().scenario().title())
                    .append(" evidence_count=").append(scenario.evidence().size()).append('\n');
            if (scenario.adapterResult() != null) {
                builder.append("  adapter=").append(scenario.adapterResult().adapterKey())
                        .append(" normalized_status=")
                        .append(scenario.adapterResult().normalizedStatus()).append('\n');
            }
        }
        builder.append("</scenario-coverage>\n");
        return builder.toString();
    }
}
