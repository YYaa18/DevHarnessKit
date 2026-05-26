package com.devharnesskit.dhk.service.graph;

import com.devharnesskit.dhk.export.GraphIndexReportRenderer;
import com.devharnesskit.dhk.model.graph.GraphConfig;
import com.devharnesskit.dhk.model.graph.GraphInitResult;
import com.devharnesskit.dhk.model.graph.GraphScanReport;
import com.devharnesskit.dhk.util.Clock;
import com.devharnesskit.dhk.util.PathUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class GraphService {
    private final GraphConfigService configService;
    private final GraphFileScanner scanner;
    private final GraphIndexReportRenderer renderer;

    public GraphService() {
        this(new GraphConfigService(), new GraphFileScanner(), new GraphIndexReportRenderer());
    }

    GraphService(GraphConfigService configService, GraphFileScanner scanner, GraphIndexReportRenderer renderer) {
        this.configService = configService;
        this.scanner = scanner;
        this.renderer = renderer;
    }

    public GraphInitResult init(Path projectRoot) throws IOException {
        boolean created = configService.writeDefaultIfMissing(projectRoot);
        return new GraphInitResult(PathUtil.graphConfig(projectRoot), PathUtil.graphExportsDirectory(projectRoot), created);
    }

    public GraphScanReport status(Path projectRoot, Clock clock) throws IOException {
        PathUtil.createGraphDirectories(projectRoot);
        GraphConfig config = configService.load(projectRoot);
        GraphScanReport report = scanner.scan(projectRoot, config);
        Files.write(PathUtil.graphIndexReport(projectRoot),
                renderer.render(report, clock.now()).getBytes("UTF-8"));
        return report;
    }
}
