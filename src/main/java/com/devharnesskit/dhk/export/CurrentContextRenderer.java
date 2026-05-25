package com.devharnesskit.dhk.export;

import com.devharnesskit.dhk.model.Checkpoint;
import com.devharnesskit.dhk.model.MemoryItem;
import com.devharnesskit.dhk.model.Project;

import java.util.List;

public final class CurrentContextRenderer {
    private static final int TOTAL_BUDGET = 20 * 1024;
    private static final int MEMORY_BUDGET = 8 * 1024;
    private static final int WORKFLOW_BUDGET = 3 * 1024;
    private static final int SPEC_BUDGET = 3 * 1024;

    public String render(Project project, String task, String module, String mode, String keywords,
                         String generatedAt, List<MemoryItem> memory, Checkpoint checkpoint) {
        return render(project, task, module, mode, keywords, generatedAt, memory, checkpoint, "");
    }

    public String render(Project project, String task, String module, String mode, String keywords,
                         String generatedAt, List<MemoryItem> memory, Checkpoint checkpoint,
                         String workflowContext) {
        return render(project, task, module, mode, keywords, generatedAt, memory, checkpoint,
                workflowContext, "");
    }

    public String render(Project project, String task, String module, String mode, String keywords,
                         String generatedAt, List<MemoryItem> memory, Checkpoint checkpoint,
                         String workflowContext, String specContext) {
        StringBuilder builder = new StringBuilder();
        builder.append("# CURRENT_CONTEXT\n\n");
        builder.append("<generated-at>").append(generatedAt).append("</generated-at>\n\n");
        builder.append("<task>\n").append(task).append("\n</task>\n\n");
        builder.append("<project-summary>\n");
        builder.append("- project_key: ").append(project.projectKey()).append('\n');
        builder.append("- project_type: ").append(project.projectType()).append('\n');
        builder.append("- framework: ").append(project.framework()).append('\n');
        builder.append("- database: ").append(project.databaseType()).append('\n');
        builder.append("- module: ").append(module).append('\n');
        builder.append("- mode: ").append(mode).append('\n');
        builder.append("- keywords: ").append(keywords).append('\n');
        builder.append("</project-summary>\n\n");
        builder.append("<must-follow>\n");
        builder.append("- Use confirmed memory only; never treat draft as fact.\n");
        builder.append("- Do not rely on chat history for project structure.\n");
        builder.append("- Do not write secrets, tokens, JDBC URLs, or Authorization headers to memory.\n");
        builder.append("</must-follow>\n\n");

        SectionResult memorySection = renderMemorySection(memory, MEMORY_BUDGET);
        builder.append(memorySection.text);
        builder.append('\n');

        builder.append("<recent-checkpoint>\n");
        if (checkpoint == null) {
            builder.append("none\n");
        } else {
            builder.append("- task: ").append(checkpoint.taskName()).append('\n');
            builder.append("- module: ").append(checkpoint.moduleName()).append('\n');
            builder.append("- summary: ").append(checkpoint.summary()).append('\n');
            builder.append("- pending_items: ").append(checkpoint.pendingItems()).append('\n');
        }
        builder.append("</recent-checkpoint>\n\n");

        SectionResult workflowSection = section("workflow-context", workflowContext, WORKFLOW_BUDGET);
        if (workflowSection.text.length() > 0) {
            builder.append(workflowSection.text).append('\n');
        }
        SectionResult specSection = section("spec-context", specContext, SPEC_BUDGET);
        if (specSection.text.length() > 0) {
            builder.append(specSection.text).append('\n');
        }
        builder.append("<agent-instructions>\n");
        builder.append("1. First make a minimal change plan from this context.\n");
        builder.append("2. Do not guess missing classes, fields, or database tables.\n");
        builder.append("3. Create a checkpoint after development.\n");
        builder.append("</agent-instructions>\n\n");
        builder.append("<truncation-report>\n");
        builder.append("- relevant_memory_total: ").append(memory.size()).append('\n');
        builder.append("- relevant_memory_exported: ").append(memorySection.itemsExported).append('\n');
        builder.append("- relevant_memory_truncated: ").append(memorySection.truncated).append('\n');
        builder.append("- workflow_context_truncated: ").append(workflowSection.truncated).append('\n');
        builder.append("- spec_context_truncated: ").append(specSection.truncated).append('\n');
        builder.append("</truncation-report>\n");
        return finalLimit(builder.toString());
    }

    private SectionResult renderMemorySection(List<MemoryItem> memory, int budget) {
        StringBuilder builder = new StringBuilder();
        builder.append("<relevant-conventions>\n");
        int exported = 0;
        boolean truncated = false;
        for (MemoryItem item : memory) {
            String chunk = memoryChunk(item);
            if (builder.length() + chunk.length() + "</relevant-conventions>\n".length() > budget) {
                truncated = true;
                break;
            }
            builder.append(chunk);
            exported++;
        }
        builder.append("</relevant-conventions>\n");
        return new SectionResult(builder.toString(), truncated, exported);
    }

    private String memoryChunk(MemoryItem item) {
        StringBuilder builder = new StringBuilder();
        builder.append("## ").append(item.title()).append('\n');
        builder.append(item.content()).append('\n');
        if (item.evidence().length() > 0) {
            builder.append("Evidence: ").append(item.evidence()).append('\n');
        }
        builder.append("Tags: ").append(item.tags()).append('\n');
        builder.append("Match: confirmed memory; module=").append(item.moduleName())
                .append("; confidence=").append(item.confidence()).append("\n\n");
        return builder.toString();
    }

    private SectionResult section(String name, String text, int budget) {
        if (text == null || text.length() == 0) {
            return new SectionResult("", false, 0);
        }
        String wrapperStart = "<" + name + ">\n";
        String wrapperEnd = "</" + name + ">\n";
        int contentBudget = budget - wrapperStart.length() - wrapperEnd.length() - 80;
        if (contentBudget < 200) {
            contentBudget = 200;
        }
        boolean truncated = text.length() > contentBudget;
        String content = truncated ? truncateAtLine(text, contentBudget) + "\n<!-- section truncated -->\n" : text;
        return new SectionResult(wrapperStart + content + "\n" + wrapperEnd, truncated, 0);
    }

    private String truncateAtLine(String text, int maxChars) {
        if (text.length() <= maxChars) {
            return text;
        }
        int end = Math.max(0, maxChars);
        int newline = text.lastIndexOf('\n', end);
        if (newline > maxChars / 2) {
            end = newline;
        }
        return text.substring(0, end);
    }

    private String finalLimit(String text) {
        if (text.length() <= TOTAL_BUDGET) {
            return text;
        }
        return truncateAtLine(text, TOTAL_BUDGET - 120)
                + "\n\n<!-- truncated: output exceeded total context budget -->\n";
    }

    private static final class SectionResult {
        private final String text;
        private final boolean truncated;
        private final int itemsExported;

        private SectionResult(String text, boolean truncated, int itemsExported) {
            this.text = text;
            this.truncated = truncated;
            this.itemsExported = itemsExported;
        }
    }
}
