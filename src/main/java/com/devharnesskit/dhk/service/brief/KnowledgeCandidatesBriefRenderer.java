package com.devharnesskit.dhk.service.brief;

import com.devharnesskit.dhk.model.brief.KnowledgeCandidate;

import java.util.List;

final class KnowledgeCandidatesBriefRenderer {
    String render(List<KnowledgeCandidate> candidates) {
        StringBuilder builder = new StringBuilder();
        builder.append("# Knowledge Candidates\n\n");
        builder.append("用户确认前，候选知识不会进入 confirmed memory。\n\n");
        for (KnowledgeCandidate candidate : candidates) {
            if ("rejected".equals(candidate.status())) {
                continue;
            }
            builder.append("## ").append(candidate.title()).append("\n\n");
            builder.append("- candidate_id: ").append(candidate.candidateId()).append('\n');
            builder.append("- goal_key: ").append(candidate.goalKey()).append('\n');
            builder.append("- type: ").append(candidate.type()).append('\n');
            builder.append("- suggested_destination: ").append(candidate.suggestedDestination()).append('\n');
            builder.append("- confidence: ").append(candidate.confidence()).append('\n');
            builder.append("- requires_confirmation: ").append(candidate.requiresConfirmation()).append('\n');
            builder.append("- sensitive_scan_status: ").append(candidate.sensitiveScanStatus()).append('\n');
            appendSource(builder, candidate);
            builder.append("- status: ").append(candidate.status()).append("\n\n");
            builder.append(candidate.summary()).append("\n\n");
        }
        return builder.toString();
    }

    private void appendSource(StringBuilder builder, KnowledgeCandidate candidate) {
        if (candidate.sourceRuleId().length() == 0) {
            return;
        }
        builder.append("- source_rule_id: ").append(candidate.sourceRuleId()).append('\n');
        builder.append("- source_pack_key: ").append(candidate.sourcePackKey()).append('\n');
        builder.append("- domain: ").append(candidate.domain()).append('\n');
        builder.append("- severity: ").append(candidate.severity()).append('\n');
        builder.append("- destination_reason: ").append(candidate.destinationReason()).append('\n');
    }
}
