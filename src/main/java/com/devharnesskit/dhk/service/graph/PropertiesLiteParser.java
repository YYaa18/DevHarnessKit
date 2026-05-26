package com.devharnesskit.dhk.service.graph;

import com.devharnesskit.dhk.model.graph.GraphFileEntry;
import com.devharnesskit.dhk.model.graph.GraphNode;
import com.devharnesskit.dhk.model.graph.GraphParseResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class PropertiesLiteParser implements GraphSourceParser {
    public boolean supports(GraphFileEntry entry) {
        return "properties".equals(entry.language());
    }

    public void parse(Path projectRoot, GraphFileEntry entry, GraphParseResult.Builder builder) throws Exception {
        Path file = projectRoot.resolve(entry.relativePath());
        List<String> lines = Files.readAllLines(file);
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index).trim();
            if (line.length() == 0 || line.startsWith("#") || line.startsWith("!")) {
                continue;
            }
            String key = key(line);
            if (key.length() == 0) {
                continue;
            }
            int lineNumber = index + 1;
            builder.addNode(new GraphNode("config_key:" + entry.relativePath() + ":" + key,
                    "config_key", key, key, entry.relativePath(), lineNumber, lineNumber,
                    "properties", "", "", 80, "lite", "properties key"));
        }
    }

    private String key(String line) {
        int equals = line.indexOf('=');
        int colon = line.indexOf(':');
        int split = -1;
        if (equals >= 0 && colon >= 0) {
            split = Math.min(equals, colon);
        } else if (equals >= 0) {
            split = equals;
        } else if (colon >= 0) {
            split = colon;
        }
        if (split < 0) {
            String[] parts = line.split("\\s+", 2);
            return parts.length == 2 ? parts[0].trim() : "";
        }
        return line.substring(0, split).trim();
    }
}
