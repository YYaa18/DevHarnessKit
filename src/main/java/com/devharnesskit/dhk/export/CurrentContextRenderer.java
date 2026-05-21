package com.devharnesskit.dhk.export;

import com.devharnesskit.dhk.model.Checkpoint;
import com.devharnesskit.dhk.model.MemoryItem;
import com.devharnesskit.dhk.model.Project;

import java.util.List;

public final class CurrentContextRenderer {
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
        builder.append("<relevant-conventions>\n");
        for (MemoryItem item : memory) {
            builder.append("## ").append(item.title()).append('\n');
            builder.append(item.content()).append('\n');
            if (item.evidence().length() > 0) {
                builder.append("Evidence: ").append(item.evidence()).append('\n');
            }
            builder.append("Tags: ").append(item.tags()).append('\n');
            builder.append("Match: confirmed memory; module=").append(item.moduleName())
                    .append("; confidence=").append(item.confidence()).append("\n\n");
        }
        builder.append("</relevant-conventions>\n\n");
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
        if (workflowContext != null && workflowContext.length() > 0) {
            builder.append(workflowContext).append('\n');
        }
        if (specContext != null && specContext.length() > 0) {
            builder.append(specContext).append('\n');
        }
        builder.append("<agent-instructions>\n");
        builder.append("1. First make a minimal change plan from this context.\n");
        builder.append("2. Do not guess missing classes, fields, or database tables.\n");
        builder.append("3. Create a checkpoint after development.\n");
        builder.append("</agent-instructions>\n");
        return limit(builder.toString(), 20 * 1024);
    }

    private String limit(String text, int maxChars) {
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars - 80) + "\n\n<!-- truncated: output exceeded MVP context budget -->\n";
    }
}
