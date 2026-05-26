package com.devharnesskit.dhk.service.graph;

import com.devharnesskit.dhk.export.GraphIndexReportRenderer;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.db.TransactionTemplate;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.graph.GraphConfig;
import com.devharnesskit.dhk.model.graph.GraphIndexResult;
import com.devharnesskit.dhk.model.graph.GraphInitResult;
import com.devharnesskit.dhk.model.graph.GraphParseResult;
import com.devharnesskit.dhk.model.graph.GraphPruneResult;
import com.devharnesskit.dhk.model.graph.GraphScanReport;
import com.devharnesskit.dhk.model.graph.GraphSnapshot;
import com.devharnesskit.dhk.repository.ProjectRepository;
import com.devharnesskit.dhk.repository.graph.GraphRepository;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.service.goal.WorkspaceFingerprintService;
import com.devharnesskit.dhk.service.policy.DevHarnessPolicyService;
import com.devharnesskit.dhk.util.Clock;
import com.devharnesskit.dhk.util.PathUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class GraphService {
    private static final int GIT_TIMEOUT_SECONDS = 5;

    private final GraphConfigService configService;
    private final GraphFileScanner scanner;
    private final GraphLiteParser parser;
    private final GraphIndexReportRenderer renderer;
    private final DbConnectionFactory connectionFactory;
    private final MigrationRunner migrationRunner;
    private final ProjectService projectService;
    private final ProjectRepository projectRepository;
    private final GraphRepository graphRepository;
    private final TransactionTemplate transactionTemplate;
    private final WorkspaceFingerprintService fingerprintService;
    private final DevHarnessPolicyService policyService;

    public GraphService() {
        this(new GraphConfigService(), new GraphFileScanner(), new GraphLiteParser(), new GraphIndexReportRenderer(),
                new DbConnectionFactory(), new MigrationRunner(), new ProjectService(), new ProjectRepository(),
                new GraphRepository(), new TransactionTemplate(), new WorkspaceFingerprintService(),
                new DevHarnessPolicyService());
    }

    GraphService(GraphConfigService configService, GraphFileScanner scanner, GraphLiteParser parser,
                 GraphIndexReportRenderer renderer, DbConnectionFactory connectionFactory,
                 MigrationRunner migrationRunner, ProjectService projectService, ProjectRepository projectRepository,
                 GraphRepository graphRepository, TransactionTemplate transactionTemplate,
                 WorkspaceFingerprintService fingerprintService, DevHarnessPolicyService policyService) {
        this.configService = configService;
        this.scanner = scanner;
        this.parser = parser;
        this.renderer = renderer;
        this.connectionFactory = connectionFactory;
        this.migrationRunner = migrationRunner;
        this.projectService = projectService;
        this.projectRepository = projectRepository;
        this.graphRepository = graphRepository;
        this.transactionTemplate = transactionTemplate;
        this.fingerprintService = fingerprintService;
        this.policyService = policyService;
    }

    public GraphInitResult init(Path projectRoot) throws IOException {
        boolean created = configService.writeDefaultIfMissing(projectRoot);
        return new GraphInitResult(PathUtil.graphConfig(projectRoot), PathUtil.graphExportsDirectory(projectRoot), created);
    }

    public GraphIndexResult index(Path projectRoot, Clock clock) throws Exception {
        PathUtil.createMemoryDirectories(projectRoot);
        PathUtil.createGraphDirectories(projectRoot);
        GraphConfig config = configService.load(projectRoot);
        GraphScanReport report = scan(projectRoot, config, null);
        Project project = projectService.ensureProject(projectRoot, clock);
        String createdAt = clock.now().toString();
        String completedAt = clock.now().toString();
        String workspaceFingerprint = fingerprintService.workspaceFingerprint(projectRoot);
        String snapshotKey = snapshotKey(clock);
        String configHash = configHash(config);
        String summary = summary(projectRoot, report);

        GraphSnapshot snapshot;
        try (Connection connection = connectionFactory.open(projectRoot)) {
            migrationRunner.migrate(connection, clock);
            projectRepository.upsert(connection, project);
            snapshot = transactionTemplate.execute(connection, new TransactionTemplate.Work<GraphSnapshot>() {
                public GraphSnapshot execute() throws Exception {
                    return graphRepository.saveCompletedSnapshot(connection, project.projectKey(), snapshotKey,
                            configHash, workspaceFingerprint, summary, createdAt, completedAt, report);
                }
            });
        }
        GraphScanReport reportWithSnapshot = new GraphScanReport(projectRoot, config, report.entries(),
                report.parseResult(), snapshot, workspaceFingerprint, false);
        Files.write(PathUtil.graphIndexReport(projectRoot),
                renderer.render(reportWithSnapshot, clock.now()).getBytes("UTF-8"));
        return new GraphIndexResult(snapshot, reportWithSnapshot, PathUtil.graphIndexReport(projectRoot));
    }

    public GraphScanReport status(Path projectRoot, Clock clock) throws IOException {
        PathUtil.createGraphDirectories(projectRoot);
        GraphConfig config = configService.load(projectRoot);
        GraphSnapshot latestSnapshot = latestSnapshot(projectRoot);
        String currentWorkspaceFingerprint = fingerprintService.workspaceFingerprint(projectRoot);
        GraphScanReport scanned = scan(projectRoot, config, latestSnapshot);
        GraphScanReport report = new GraphScanReport(projectRoot, config, scanned.entries(), scanned.parseResult(),
                latestSnapshot, currentWorkspaceFingerprint, isSnapshotStale(latestSnapshot, currentWorkspaceFingerprint));
        Files.write(PathUtil.graphIndexReport(projectRoot),
                renderer.render(report, clock.now()).getBytes("UTF-8"));
        return report;
    }

    public GraphPruneResult prune(Path projectRoot, int keep, Clock clock) throws Exception {
        return prune(projectRoot, keep, clock, false);
    }

    public GraphPruneResult prune(Path projectRoot, int keep, Clock clock, final boolean dryRun) throws Exception {
        if (keep < 1) {
            throw new IllegalArgumentException("--keep must be >= 1");
        }
        if (dryRun) {
            return previewPrune(projectRoot, keep);
        }
        PathUtil.createMemoryDirectories(projectRoot);
        PathUtil.createGraphDirectories(projectRoot);
        Project project = projectService.ensureProject(projectRoot, clock);
        try (Connection connection = connectionFactory.open(projectRoot)) {
            migrationRunner.migrate(connection, clock);
            projectRepository.upsert(connection, project);
            return transactionTemplate.execute(connection, new TransactionTemplate.Work<GraphPruneResult>() {
                public GraphPruneResult execute() throws Exception {
                    return graphRepository.pruneCompletedSnapshots(connection, project.projectKey(), keep);
                }
            });
        }
    }

    private GraphPruneResult previewPrune(Path projectRoot, int keep) throws Exception {
        if (!Files.isRegularFile(PathUtil.memoryDb(projectRoot))
                || !Files.isRegularFile(PathUtil.projectJson(projectRoot))) {
            return new GraphPruneResult(keep, 0, 0, 0, 0, 0, 0, 0, true, new String[0]);
        }
        Project project = projectService.readProject(PathUtil.projectJson(projectRoot));
        try (Connection connection = connectionFactory.open(projectRoot)) {
            return graphRepository.previewPruneCompletedSnapshots(connection, project.projectKey(), keep);
        }
    }

    private GraphScanReport scan(Path projectRoot, GraphConfig config, GraphSnapshot latestSnapshot) {
        GraphScanReport scanReport = scanner.scan(projectRoot, config, policyService.load(projectRoot).protectedFiles());
        GraphParseResult parseResult = parser.parse(projectRoot, scanReport.entries());
        return new GraphScanReport(projectRoot, config, scanReport.entries(), parseResult, latestSnapshot);
    }

    private GraphSnapshot latestSnapshot(Path projectRoot) {
        if (!Files.isRegularFile(PathUtil.memoryDb(projectRoot)) || !Files.isRegularFile(PathUtil.projectJson(projectRoot))) {
            return null;
        }
        try {
            Project project = projectService.readProject(PathUtil.projectJson(projectRoot));
            try (Connection connection = connectionFactory.open(projectRoot)) {
                return graphRepository.latestCompletedSnapshot(connection, project.projectKey());
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isSnapshotStale(GraphSnapshot snapshot, String currentWorkspaceFingerprint) {
        return snapshot != null
                && currentWorkspaceFingerprint != null
                && currentWorkspaceFingerprint.length() > 0
                && !currentWorkspaceFingerprint.equals(snapshot.workspaceFingerprint());
    }

    private String snapshotKey(Clock clock) {
        String timestamp = clock.now().toString().replaceAll("[^0-9A-Za-z]", "");
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "graph-" + timestamp + "-" + suffix;
    }

    private String configHash(GraphConfig config) {
        StringBuilder builder = new StringBuilder();
        builder.append(config.provider()).append('\n')
                .append(config.cgcCommand()).append('\n')
                .append(config.include()).append('\n')
                .append(config.exclude()).append('\n')
                .append(config.maxFileBytes()).append('\n')
                .append(config.maxIndexedFiles()).append('\n')
                .append(config.maxImpactDepth()).append('\n')
                .append(config.maxExportNodes()).append('\n');
        return "config:" + sha256(builder.toString());
    }

    private String summary(Path projectRoot, GraphScanReport report) {
        StringBuilder builder = new StringBuilder();
        builder.append("files=").append(report.filesConsidered())
                .append(";indexed=").append(report.indexedFiles())
                .append(";skipped=").append(report.skippedFiles())
                .append(";nodes=").append(report.parseResult().nodes().size())
                .append(";edges=").append(report.parseResult().edges().size())
                .append(";git_commit=").append(gitOutput(projectRoot, "rev-parse", "--short", "HEAD"))
                .append(";git_dirty=").append(gitOutput(projectRoot, "status", "--porcelain").length() > 0);
        return builder.toString();
    }

    private String gitOutput(Path projectRoot, String... args) {
        try {
            String[] command = new String[args.length + 3];
            command[0] = "git";
            command[1] = "-C";
            command[2] = projectRoot.toAbsolutePath().normalize().toString();
            System.arraycopy(args, 0, command, 3, args.length);
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            InputStream input = process.getInputStream();
            byte[] buffer = new byte[4096];
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(GIT_TIMEOUT_SECONDS);
            while (System.nanoTime() < deadline) {
                while (input.available() > 0) {
                    int read = input.read(buffer);
                    if (read < 0) {
                        break;
                    }
                    output.write(buffer, 0, read);
                }
                if (process.waitFor(50L, TimeUnit.MILLISECONDS)) {
                    while (input.available() > 0) {
                        int read = input.read(buffer);
                        if (read < 0) {
                            break;
                        }
                        output.write(buffer, 0, read);
                    }
                    return process.exitValue() == 0 ? new String(output.toByteArray(), "UTF-8").trim() : "";
                }
            }
            process.destroyForcibly();
            return "";
        } catch (Exception ex) {
            return "";
        }
    }

    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(text.getBytes("UTF-8"));
            StringBuilder builder = new StringBuilder();
            for (byte b : bytes) {
                String hex = Integer.toHexString(b & 0xff);
                if (hex.length() == 1) {
                    builder.append('0');
                }
                builder.append(hex);
            }
            return builder.toString();
        } catch (Exception ex) {
            return "";
        }
    }
}
