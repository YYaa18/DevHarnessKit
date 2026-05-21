package com.devharnesskit.dhk.export;

import com.devharnesskit.dhk.model.Checkpoint;
import com.devharnesskit.dhk.model.MemoryItem;

import java.util.List;

public final class RecoveryContextRenderer {
    public String render(String generatedAt, Checkpoint checkpoint, List<MemoryItem> memory) {
        StringBuilder builder = new StringBuilder();
        builder.append("# RECOVERY_CONTEXT\n\n");
        builder.append("<generated-at>").append(generatedAt).append("</generated-at>\n\n");
        builder.append("<latest-checkpoint>\n");
        builder.append("- task: ").append(checkpoint.taskName()).append('\n');
        builder.append("- module: ").append(checkpoint.moduleName()).append('\n');
        builder.append("- summary: ").append(checkpoint.summary()).append('\n');
        builder.append("- changed_files: ").append(checkpoint.changedFiles()).append('\n');
        builder.append("- pending_items: ").append(checkpoint.pendingItems()).append('\n');
        builder.append("- verify_status: ").append(checkpoint.verifyStatus()).append('\n');
        builder.append("- next_read_files: ").append(checkpoint.nextReadFiles()).append('\n');
        builder.append("</latest-checkpoint>\n\n");
        builder.append("<relevant-memory>\n");
        for (MemoryItem item : memory) {
            builder.append("## ").append(item.title()).append('\n');
            builder.append(item.content()).append('\n');
            builder.append("Tags: ").append(item.tags()).append("\n\n");
        }
        builder.append("</relevant-memory>\n\n");
        builder.append("<agent-instructions>\n");
        builder.append("1. Continue from pending_items.\n");
        builder.append("2. If next_read_files exists, read those files first.\n");
        builder.append("3. Verify assumptions from the checkpoint before editing.\n");
        builder.append("</agent-instructions>\n");
        return limit(builder.toString(), 20 * 1024);
    }

    private String limit(String text, int maxChars) {
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars - 80) + "\n\n<!-- truncated: output exceeded MVP recovery budget -->\n";
    }
}
