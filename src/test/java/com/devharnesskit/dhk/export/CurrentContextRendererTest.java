package com.devharnesskit.dhk.export;

import com.devharnesskit.dhk.model.MemoryItem;
import com.devharnesskit.dhk.model.Project;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CurrentContextRendererTest {
    @Test
    void rendersSectionBudgetsAndTruncationReport() {
        Project project = new Project("demo", "Demo", "/tmp/demo", "java", "java",
                "spring", "mysql", "now", "now");
        List<MemoryItem> memory = new ArrayList<MemoryItem>();
        for (int i = 0; i < 40; i++) {
            memory.add(memory(i, repeat("memory-" + i + " ", 400)));
        }

        String markdown = new CurrentContextRenderer().render(project, "Task", "order", "api",
                "gateway", "now", memory, null, repeat("workflow ", 800), repeat("spec ", 800));

        assertTrue(markdown.length() <= 20 * 1024);
        assertTrue(markdown.contains("<truncation-report>"));
        assertTrue(markdown.contains("relevant_memory_total: 40"));
        assertTrue(markdown.contains("relevant_memory_truncated: true"));
        assertTrue(markdown.contains("workflow_context_truncated: true"));
        assertTrue(markdown.contains("spec_context_truncated: true"));
        assertFalse(markdown.contains("memory-39"));
    }

    private MemoryItem memory(int id, String content) {
        return new MemoryItem(id, "demo", "order", "project_fact", "project",
                "Memory " + id, content, "tag-" + id, "confirmed", 90,
                "manual", "now", "manual", "", "", "", "",
                "now", "now", "", 0);
    }

    private String repeat(String text, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(text);
        }
        return builder.toString();
    }
}
