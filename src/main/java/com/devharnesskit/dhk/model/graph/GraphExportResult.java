package com.devharnesskit.dhk.model.graph;

import java.nio.file.Path;

public final class GraphExportResult {
    private final GraphData data;
    private final Path contextPath;
    private final Path snapshotPath;

    public GraphExportResult(GraphData data, Path contextPath, Path snapshotPath) {
        this.data = data;
        this.contextPath = contextPath;
        this.snapshotPath = snapshotPath;
    }

    public GraphData data() {
        return data;
    }

    public Path contextPath() {
        return contextPath;
    }

    public Path snapshotPath() {
        return snapshotPath;
    }
}
