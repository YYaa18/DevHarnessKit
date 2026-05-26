package com.devharnesskit.dhk.model.graph;

import java.nio.file.Path;

public final class GraphIndexResult {
    private final GraphSnapshot snapshot;
    private final GraphScanReport report;
    private final Path reportPath;

    public GraphIndexResult(GraphSnapshot snapshot, GraphScanReport report, Path reportPath) {
        this.snapshot = snapshot;
        this.report = report;
        this.reportPath = reportPath;
    }

    public GraphSnapshot snapshot() {
        return snapshot;
    }

    public GraphScanReport report() {
        return report;
    }

    public Path reportPath() {
        return reportPath;
    }
}
