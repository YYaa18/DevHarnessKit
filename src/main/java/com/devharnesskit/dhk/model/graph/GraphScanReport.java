package com.devharnesskit.dhk.model.graph;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GraphScanReport {
    private final Path projectRoot;
    private final GraphConfig config;
    private final List<GraphFileEntry> entries;
    private final GraphParseResult parseResult;

    public GraphScanReport(Path projectRoot, GraphConfig config, List<GraphFileEntry> entries) {
        this(projectRoot, config, entries, GraphParseResult.empty());
    }

    public GraphScanReport(Path projectRoot, GraphConfig config, List<GraphFileEntry> entries,
                           GraphParseResult parseResult) {
        this.projectRoot = projectRoot;
        this.config = config;
        this.entries = Collections.unmodifiableList(new ArrayList<GraphFileEntry>(entries));
        this.parseResult = parseResult == null ? GraphParseResult.empty() : parseResult;
    }

    public Path projectRoot() {
        return projectRoot;
    }

    public GraphConfig config() {
        return config;
    }

    public List<GraphFileEntry> entries() {
        return entries;
    }

    public GraphParseResult parseResult() {
        return parseResult;
    }

    public int filesConsidered() {
        return entries.size();
    }

    public int indexedFiles() {
        int count = 0;
        for (GraphFileEntry entry : entries) {
            if (entry.indexed()) {
                count++;
            }
        }
        return count;
    }

    public int skippedFiles() {
        return filesConsidered() - indexedFiles();
    }

    public Map<String, Integer> languageCounts() {
        Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
        for (GraphFileEntry entry : entries) {
            if (!entry.indexed()) {
                continue;
            }
            Integer current = counts.get(entry.language());
            counts.put(entry.language(), current == null ? 1 : current + 1);
        }
        return counts;
    }
}
