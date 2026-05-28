package com.devharnesskit.dhk.service.knowledge;

import com.devharnesskit.dhk.model.knowledge.KnowledgePackEntry;
import com.devharnesskit.dhk.model.knowledge.KnowledgeSnippet;

import java.nio.file.Files;
import java.nio.file.Path;

final class KnowledgeSnippetLoader {
    KnowledgeSnippet load(Path packDirectory, KnowledgePackEntry entry) {
        Path file = packDirectory.resolve(entry.file()).normalize();
        if (!Files.isRegularFile(file)) {
            return new KnowledgeSnippet(packRef(entry), entry.ruleId(), entry.domain(), entry.severity(),
                    entry.file(), new String[]{"Knowledge file missing: " + entry.file()},
                    new String[0]);
        }
        try {
            String markdown = new String(Files.readAllBytes(file), "UTF-8");
            return new KnowledgeSnippet(packRef(entry), entry.ruleId(), entry.domain(), entry.severity(),
                    ".agents/knowledge/packs/" + entry.packKey() + "/" + entry.file(),
                    limitLines(section(markdown, "inject-summary"), entry.budgetLines()),
                    limitLines(section(markdown, "evidence-checklist"), Math.max(3, entry.budgetLines() / 2)));
        } catch (Exception ex) {
            return new KnowledgeSnippet(packRef(entry), entry.ruleId(), entry.domain(), entry.severity(),
                    entry.file(), new String[]{"Knowledge file unreadable: " + ex.getMessage()},
                    new String[0]);
        }
    }

    private String packRef(KnowledgePackEntry entry) {
        return entry.packKey() + "@" + entry.packVersion();
    }

    private String[] section(String markdown, String tag) {
        String start = "<" + tag + ">";
        String end = "</" + tag + ">";
        int from = markdown.indexOf(start);
        int to = markdown.indexOf(end);
        if (from < 0 || to <= from) {
            return new String[0];
        }
        String body = markdown.substring(from + start.length(), to).trim();
        if (body.length() == 0) {
            return new String[0];
        }
        java.util.List<String> lines = new java.util.ArrayList<String>();
        for (String raw : body.split("\\r?\\n")) {
            String line = raw.trim();
            if (line.length() == 0) {
                continue;
            }
            if (line.startsWith("- ")) {
                line = line.substring(2).trim();
            }
            lines.add(line);
        }
        return lines.toArray(new String[lines.size()]);
    }

    private String[] limitLines(String[] lines, int max) {
        if (lines.length <= max) {
            return lines;
        }
        String[] result = new String[max];
        System.arraycopy(lines, 0, result, 0, max);
        return result;
    }
}
