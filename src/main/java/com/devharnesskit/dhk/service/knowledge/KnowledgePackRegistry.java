package com.devharnesskit.dhk.service.knowledge;

import com.devharnesskit.dhk.model.knowledge.KnowledgePack;
import com.devharnesskit.dhk.model.knowledge.KnowledgePackEntry;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class KnowledgePackRegistry {
    public List<KnowledgePack> load(Path projectRoot) {
        List<KnowledgePack> packs = new ArrayList<KnowledgePack>();
        Path directory = PathUtil.knowledgePacksDirectory(projectRoot);
        if (!Files.isDirectory(directory)) {
            Path fallback = PathUtil.knowledgePacksDirectory(new java.io.File(".").toPath().toAbsolutePath().normalize());
            if (!Files.isDirectory(fallback)) {
                return packs;
            }
            directory = fallback;
        }
        try (java.util.stream.Stream<Path> stream = Files.list(directory)) {
            java.util.Iterator<Path> iterator = stream.iterator();
            while (iterator.hasNext()) {
                Path packDirectory = iterator.next();
                Path manifest = packDirectory.resolve("knowledge-pack.json");
                if (Files.isRegularFile(manifest)) {
                    packs.add(parse(new String(Files.readAllBytes(manifest), "UTF-8")));
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load knowledge packs: " + ex.getMessage(), ex);
        }
        return packs;
    }

    private KnowledgePack parse(String json) {
        String packKey = string(json, "pack_key");
        String version = string(json, "version");
        String title = string(json, "title");
        String status = string(json, "status");
        String sourcePolicy = string(json, "source_policy");
        List<KnowledgePackEntry> entries = new ArrayList<KnowledgePackEntry>();
        for (String object : entryObjects(json)) {
            entries.add(new KnowledgePackEntry(packKey, version,
                    string(object, "file"),
                    string(object, "rule_id"),
                    string(object, "domain"),
                    string(object, "severity"),
                    array(object, "applies_to_profiles"),
                    array(object, "applies_to_actions"),
                    array(object, "risk_flags"),
                    integer(object, "budget_lines", 8)));
        }
        return new KnowledgePack(packKey, title, version, status, sourcePolicy,
                entries.toArray(new KnowledgePackEntry[entries.size()]));
    }

    private List<String> entryObjects(String json) {
        List<String> result = new ArrayList<String>();
        int entriesIndex = json.indexOf("\"entries\"");
        if (entriesIndex < 0) {
            return result;
        }
        int arrayStart = json.indexOf('[', entriesIndex);
        int arrayEnd = findMatching(json, arrayStart, '[', ']');
        if (arrayStart < 0 || arrayEnd < 0) {
            return result;
        }
        int index = arrayStart + 1;
        while (index < arrayEnd) {
            int objectStart = json.indexOf('{', index);
            if (objectStart < 0 || objectStart >= arrayEnd) {
                break;
            }
            int objectEnd = findMatching(json, objectStart, '{', '}');
            if (objectEnd < 0 || objectEnd > arrayEnd) {
                break;
            }
            result.add(json.substring(objectStart, objectEnd + 1));
            index = objectEnd + 1;
        }
        return result;
    }

    private int findMatching(String value, int start, char open, char close) {
        if (start < 0) {
            return -1;
        }
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < value.length(); i++) {
            char c = value.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (c == open) {
                depth++;
            } else if (c == close) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private String string(String json, String key) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"")
                .matcher(json);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private String[] array(String json, String key) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\\[(.*?)\\]",
                Pattern.DOTALL).matcher(json);
        if (!matcher.find()) {
            return new String[0];
        }
        Matcher valueMatcher = Pattern.compile("\"([^\"]*)\"").matcher(matcher.group(1));
        List<String> values = new ArrayList<String>();
        while (valueMatcher.find()) {
            values.add(valueMatcher.group(1).trim());
        }
        return values.toArray(new String[values.size()]);
    }

    private int integer(String json, String key, int fallback) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*([0-9]+)")
                .matcher(json);
        if (!matcher.find()) {
            return fallback;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
