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
    private final GraphSnapshot latestSnapshot;
    private final String currentWorkspaceFingerprint;
    private final boolean latestSnapshotStale;

    public GraphScanReport(Path projectRoot, GraphConfig config, List<GraphFileEntry> entries) {
        this(projectRoot, config, entries, GraphParseResult.empty());
    }

    public GraphScanReport(Path projectRoot, GraphConfig config, List<GraphFileEntry> entries,
                           GraphParseResult parseResult) {
        this(projectRoot, config, entries, parseResult, null);
    }

    public GraphScanReport(Path projectRoot, GraphConfig config, List<GraphFileEntry> entries,
                           GraphParseResult parseResult, GraphSnapshot latestSnapshot) {
        this(projectRoot, config, entries, parseResult, latestSnapshot, "", false);
    }

    public GraphScanReport(Path projectRoot, GraphConfig config, List<GraphFileEntry> entries,
                           GraphParseResult parseResult, GraphSnapshot latestSnapshot,
                           String currentWorkspaceFingerprint, boolean latestSnapshotStale) {
        this.projectRoot = projectRoot;
        this.config = config;
        this.entries = Collections.unmodifiableList(new ArrayList<GraphFileEntry>(entries));
        this.parseResult = parseResult == null ? GraphParseResult.empty() : parseResult;
        this.latestSnapshot = latestSnapshot;
        this.currentWorkspaceFingerprint = currentWorkspaceFingerprint == null ? "" : currentWorkspaceFingerprint;
        this.latestSnapshotStale = latestSnapshotStale;
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

    public GraphSnapshot latestSnapshot() {
        return latestSnapshot;
    }

    public String currentWorkspaceFingerprint() {
        return currentWorkspaceFingerprint;
    }

    public boolean latestSnapshotStale() {
        return latestSnapshotStale;
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

    public int skippedFiles(String reason) {
        int count = 0;
        for (GraphFileEntry entry : entries) {
            if (!entry.indexed() && reason.equals(entry.skipReason())) {
                count++;
            }
        }
        return count;
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
