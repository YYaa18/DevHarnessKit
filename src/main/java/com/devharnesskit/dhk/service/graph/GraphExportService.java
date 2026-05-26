package com.devharnesskit.dhk.service.graph;

import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.export.GraphExportRenderer;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.graph.GraphData;
import com.devharnesskit.dhk.model.graph.GraphExportResult;
import com.devharnesskit.dhk.model.graph.GraphSnapshot;
import com.devharnesskit.dhk.repository.graph.GraphRepository;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.service.SensitiveDataGuard;
import com.devharnesskit.dhk.util.Clock;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;

public final class GraphExportService {
    private final DbConnectionFactory connectionFactory;
    private final ProjectService projectService;
    private final GraphRepository graphRepository;
    private final GraphExportRenderer renderer;
    private final SensitiveDataGuard sensitiveDataGuard;

    public GraphExportService() {
        this(new DbConnectionFactory(), new ProjectService(), new GraphRepository(), new GraphExportRenderer(),
                new SensitiveDataGuard());
    }

    GraphExportService(DbConnectionFactory connectionFactory, ProjectService projectService,
                       GraphRepository graphRepository, GraphExportRenderer renderer,
                       SensitiveDataGuard sensitiveDataGuard) {
        this.connectionFactory = connectionFactory;
        this.projectService = projectService;
        this.graphRepository = graphRepository;
        this.renderer = renderer;
        this.sensitiveDataGuard = sensitiveDataGuard;
    }

    public GraphExportResult export(Path projectRoot, Clock clock) throws Exception {
        PathUtil.createGraphDirectories(projectRoot);
        GraphData data = loadData(projectRoot);
        String context = guard(renderer.renderContext(data, clock.now()));
        String snapshot = guard(renderer.renderSnapshotJson(data, clock.now()));
        Files.write(PathUtil.graphContext(projectRoot), context.getBytes("UTF-8"));
        Files.write(PathUtil.graphSnapshotJson(projectRoot), snapshot.getBytes("UTF-8"));
        return new GraphExportResult(data, PathUtil.graphContext(projectRoot), PathUtil.graphSnapshotJson(projectRoot));
    }

    private GraphData loadData(Path projectRoot) throws Exception {
        if (!Files.isRegularFile(PathUtil.memoryDb(projectRoot)) || !Files.isRegularFile(PathUtil.projectJson(projectRoot))) {
            throw new IllegalStateException("No graph snapshot found. Run `dhk graph index` first.");
        }
        Project project = projectService.readProject(PathUtil.projectJson(projectRoot));
        try (Connection connection = connectionFactory.open(projectRoot)) {
            GraphSnapshot snapshot = graphRepository.latestCompletedSnapshot(connection, project.projectKey());
            if (snapshot == null) {
                throw new IllegalStateException("No completed graph snapshot found. Run `dhk graph index` first.");
            }
            return graphRepository.loadGraphData(connection, snapshot);
        }
    }

    private String guard(String text) {
        String redacted = sensitiveDataGuard.redact(text);
        List<String> matches = sensitiveDataGuard.findMatches(redacted);
        if (!matches.isEmpty()) {
            throw new IllegalStateException("Graph export contains sensitive data: " + matches);
        }
        return redacted;
    }
}
