package com.devharnesskit.dhk.export;

import com.devharnesskit.dhk.model.spec.SpecAcceptance;
import com.devharnesskit.dhk.model.spec.SpecChange;
import com.devharnesskit.dhk.model.spec.SpecDocument;
import com.devharnesskit.dhk.model.spec.SpecTask;
import com.devharnesskit.dhk.model.spec.WorkflowSpecBinding;

import java.util.List;

public final class SpecContextRenderer {
    public String renderFull(SpecChange change, List<SpecDocument> documents,
                             List<SpecTask> tasks, List<SpecAcceptance> acceptances,
                             List<WorkflowSpecBinding> bindings, String generatedAt) {
        StringBuilder builder = new StringBuilder();
        builder.append("# SPEC_CONTEXT\n\n");
        builder.append("<generated-at>").append(generatedAt).append("</generated-at>\n\n");
        builder.append("<spec-change>\n");
        builder.append("change_key: ").append(change.changeKey()).append('\n');
        builder.append("title: ").append(change.title()).append('\n');
        builder.append("status: ").append(change.status()).append('\n');
        builder.append("module: ").append(change.moduleName()).append('\n');
        builder.append("mode: ").append(change.mode()).append('\n');
        builder.append("priority: ").append(change.priority()).append('\n');
        if (change.summary().length() > 0) {
            builder.append("summary: ").append(change.summary()).append('\n');
        }
        builder.append("</spec-change>\n\n");

        for (SpecDocument document : documents) {
            builder.append('<').append(document.documentType()).append(">\n");
            builder.append("title: ").append(document.title()).append('\n');
            builder.append("status: ").append(document.status()).append(" v").append(document.version()).append("\n\n");
            builder.append(document.content()).append('\n');
            builder.append("</").append(document.documentType()).append(">\n\n");
        }

        builder.append("<tasks>\n");
        for (SpecTask task : tasks) {
            builder.append('[').append(task.status()).append("] ").append(task.taskKey())
                    .append(' ').append(task.title()).append('\n');
            if (task.description().length() > 0) {
                builder.append("  description: ").append(task.description()).append('\n');
            }
            if (task.phaseKey().length() > 0) {
                builder.append("  phase: ").append(task.phaseKey()).append('\n');
            }
            if (task.evidence().length() > 0) {
                builder.append("  evidence: ").append(task.evidence()).append('\n');
            }
        }
        builder.append("</tasks>\n\n");

        builder.append("<acceptance>\n");
        for (SpecAcceptance acceptance : acceptances) {
            builder.append('[').append(acceptance.status()).append("] ")
                    .append(acceptance.acceptanceKey()).append(' ')
                    .append(acceptance.description()).append('\n');
            if (acceptance.expectedResult().length() > 0) {
                builder.append("  expected: ").append(acceptance.expectedResult()).append('\n');
            }
            if (acceptance.evidence().length() > 0) {
                builder.append("  evidence: ").append(acceptance.evidence()).append('\n');
            }
        }
        builder.append("</acceptance>\n\n");

        builder.append("<bound-workflows>\n");
        for (WorkflowSpecBinding binding : bindings) {
            builder.append("- ").append(binding.runKey()).append(' ')
                    .append(binding.bindingType()).append('\n');
        }
        builder.append("</bound-workflows>\n\n");

        builder.append("<agent-instructions>\n");
        builder.append("1. Treat this spec as the task contract.\n");
        builder.append("2. Do not implement outside the stated scope.\n");
        builder.append("3. Update spec task and acceptance status after work.\n");
        builder.append("</agent-instructions>\n");
        return limit(builder.toString(), 20 * 1024);
    }

    public String renderInline(SpecChange change, List<SpecTask> tasks,
                               List<SpecAcceptance> acceptances) {
        StringBuilder builder = new StringBuilder();
        builder.append("<spec-context>\n");
        builder.append("change_key: ").append(change.changeKey()).append('\n');
        builder.append("title: ").append(change.title()).append('\n');
        builder.append("status: ").append(change.status()).append('\n');
        builder.append("module: ").append(change.moduleName()).append('\n');
        builder.append("mode: ").append(change.mode()).append('\n');
        if (change.summary().length() > 0) {
            builder.append("summary: ").append(change.summary()).append('\n');
        }
        builder.append("\nActive tasks:\n");
        for (SpecTask task : tasks) {
            if (!"done".equals(task.status()) && !"skipped".equals(task.status())) {
                builder.append("- [").append(task.status()).append("] ")
                        .append(task.taskKey()).append(' ').append(task.title()).append('\n');
            }
        }
        builder.append("\nAcceptance:\n");
        for (SpecAcceptance acceptance : acceptances) {
            if (!"passed".equals(acceptance.status()) && !"waived".equals(acceptance.status())) {
                builder.append("- [").append(acceptance.status()).append("] ")
                        .append(acceptance.acceptanceKey()).append(' ')
                        .append(acceptance.description()).append('\n');
            }
        }
        builder.append("</spec-context>\n");
        return limit(builder.toString(), 6 * 1024);
    }

    private String limit(String text, int maxChars) {
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars - 80) + "\n\n<!-- truncated: spec context budget exceeded -->\n";
    }
}
