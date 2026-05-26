package com.devharnesskit.dhk.service.graph;

import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.export.GraphImpactRenderer;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.graph.GraphConfig;
import com.devharnesskit.dhk.model.graph.GraphData;
import com.devharnesskit.dhk.model.graph.GraphEdge;
import com.devharnesskit.dhk.model.graph.GraphFileEntry;
import com.devharnesskit.dhk.model.graph.GraphImpactRequest;
import com.devharnesskit.dhk.model.graph.GraphImpactResult;
import com.devharnesskit.dhk.model.graph.GraphNode;
import com.devharnesskit.dhk.model.graph.GraphSnapshot;
import com.devharnesskit.dhk.repository.graph.GraphRepository;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.service.goal.WorkspaceFingerprintService;
import com.devharnesskit.dhk.util.Clock;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public final class GraphImpactService {
    private final DbConnectionFactory connectionFactory;
    private final ProjectService projectService;
    private final GraphRepository graphRepository;
    private final GraphImpactRenderer renderer;
    private final GraphConfigService configService;
    private final GraphCgcAdapterService cgcAdapterService;
    private final WorkspaceFingerprintService fingerprintService;

    public GraphImpactService() {
        this(new DbConnectionFactory(), new ProjectService(), new GraphRepository(), new GraphImpactRenderer(),
                new GraphConfigService(), new GraphCgcAdapterService(), new WorkspaceFingerprintService());
    }

    GraphImpactService(DbConnectionFactory connectionFactory, ProjectService projectService,
                       GraphRepository graphRepository, GraphImpactRenderer renderer, GraphConfigService configService,
                       GraphCgcAdapterService cgcAdapterService, WorkspaceFingerprintService fingerprintService) {
        this.connectionFactory = connectionFactory;
        this.projectService = projectService;
        this.graphRepository = graphRepository;
        this.renderer = renderer;
        this.configService = configService;
        this.cgcAdapterService = cgcAdapterService;
        this.fingerprintService = fingerprintService;
    }

    public GraphImpactResult impact(Path projectRoot, GraphImpactRequest request, Clock clock) throws Exception {
        PathUtil.createGraphDirectories(projectRoot);
        GraphConfig config = configService.load(projectRoot);
        int requestedDepth = request.depth();
        int maxDepth = Math.max(1, config.maxImpactDepth());
        GraphImpactRequest effectiveRequest = new GraphImpactRequest(request.queryType(), request.query(),
                Math.min(Math.max(1, requestedDepth), maxDepth), request.allowStale(),
                request.allowStaleEvidence());
        boolean depthLimited = requestedDepth > effectiveRequest.depth();
        if ("cgc".equalsIgnoreCase(config.provider())) {
            GraphImpactResult result = cgcAdapterService.impact(projectRoot, config, effectiveRequest, clock,
                    requestedDepth, maxDepth, depthLimited);
            Files.write(PathUtil.graphImpactMap(projectRoot),
                    renderer.render(result, clock.now()).getBytes("UTF-8"));
            return result;
        }
        GraphData data = loadData(projectRoot);
        String currentWorkspaceFingerprint = fingerprintService.workspaceFingerprint(projectRoot);
        boolean snapshotStale = isSnapshotStale(data.snapshot(), currentWorkspaceFingerprint);
        if (snapshotStale && !effectiveRequest.allowStale()) {
            throw new IllegalStateException("STALE_GRAPH_SNAPSHOT: latest graph snapshot "
                    + data.snapshot().snapshotKey()
                    + " does not match the current workspace. Run `dhk graph index --project-root "
                    + projectRoot.toAbsolutePath().normalize()
                    + "` or pass --allow-stale with approval evidence to continue with a warning.");
        }
        List<GraphNode> startNodes = startNodes(data, effectiveRequest);
        List<GraphNode> candidates = startNodes.isEmpty() ? candidates(data, effectiveRequest.query()) : Collections.<GraphNode>emptyList();
        GraphImpactResult result;
        if (startNodes.isEmpty()) {
            result = new GraphImpactResult(effectiveRequest, data.snapshot(), false, startNodes,
                    Collections.<GraphNode>emptyList(), Collections.<GraphEdge>emptyList(),
                    Collections.<GraphEdge>emptyList(), Collections.<String>emptyList(),
                    Collections.<GraphNode>emptyList(), Collections.<String>emptyList(),
                    Collections.<String>emptyList(),
                    Collections.<GraphNode>emptyList(), Collections.<String>emptyList(), candidates,
                    PathUtil.graphImpactMap(projectRoot), requestedDepth, maxDepth, depthLimited,
                    currentWorkspaceFingerprint, snapshotStale, effectiveRequest.allowStale());
        } else {
            result = buildResult(projectRoot, data, effectiveRequest, startNodes, requestedDepth, maxDepth,
                    depthLimited, currentWorkspaceFingerprint, snapshotStale);
        }
        Files.write(PathUtil.graphImpactMap(projectRoot),
                renderer.render(result, clock.now()).getBytes("UTF-8"));
        return result;
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

    private boolean isSnapshotStale(GraphSnapshot snapshot, String currentWorkspaceFingerprint) {
        return snapshot != null
                && currentWorkspaceFingerprint != null
                && currentWorkspaceFingerprint.length() > 0
                && !currentWorkspaceFingerprint.equals(snapshot.workspaceFingerprint());
    }

    private GraphImpactResult buildResult(Path projectRoot, GraphData data, GraphImpactRequest request,
                                          List<GraphNode> startNodes, int requestedDepth, int maxImpactDepth,
                                          boolean depthLimited, String currentWorkspaceFingerprint,
                                          boolean snapshotStale) {
        Map<String, GraphNode> nodesByKey = nodesByKey(data.nodes());
        Map<String, List<GraphEdge>> outgoing = new LinkedHashMap<String, List<GraphEdge>>();
        Map<String, List<GraphEdge>> incoming = new LinkedHashMap<String, List<GraphEdge>>();
        for (GraphEdge edge : data.edges()) {
            addEdge(outgoing, edge.sourceNodeKey(), edge);
            addEdge(incoming, edge.targetNodeKey(), edge);
        }

        Set<String> impactedKeys = traverse(startNodes, nodesByKey, outgoing, incoming, request.depth());
        List<GraphNode> impactedNodes = nodesForKeys(nodesByKey, impactedKeys);
        List<GraphEdge> callers = directEdges(startNodes, incoming, nodesByKey, false);
        List<GraphEdge> callees = directEdges(startNodes, outgoing, nodesByKey, true);
        if (isUtilitySymbolQuery(request, startNodes)) {
            impactedNodes = filterNodesByFiles(impactedNodes, utilityImpactFiles(startNodes, callers, nodesByKey));
        } else if (isSqlParameterSymbolQuery(request, startNodes)) {
            impactedNodes = filterNodesByFiles(impactedNodes, sqlParameterImpactFiles(startNodes, callers, nodesByKey));
        }
        List<String> relatedFiles = expandLegacyDataFlowFiles(relatedFiles(impactedNodes), data.files(),
                request, startNodes, impactedNodes);
        List<String> tests = relatedTests(relatedFiles, data.files());
        relatedFiles = includeRelatedTests(relatedFiles, tests);
        List<String> missingTests = missingRelatedTests(relatedFiles, data.files(), request, startNodes);
        List<GraphNode> sql = relatedSqlNodes(impactedNodes, request);
        List<GraphNode> risks = riskNodes(impactedNodes);
        List<String> recommended = recommendedReadFiles(relatedFiles);

        return new GraphImpactResult(request, data.snapshot(), true, startNodes, impactedNodes, callers,
                callees, relatedFiles, sql, tests, missingTests, risks, recommended, Collections.<GraphNode>emptyList(),
                PathUtil.graphImpactMap(projectRoot), requestedDepth, maxImpactDepth, depthLimited,
                currentWorkspaceFingerprint, snapshotStale, request.allowStale());
    }

    private List<GraphNode> startNodes(GraphData data, GraphImpactRequest request) {
        List<GraphNode> matches = new ArrayList<GraphNode>();
        String query = normalize(request.query());
        for (GraphNode node : data.nodes()) {
            if ("file".equals(request.queryType())) {
                if (normalize(node.relativePath()).equals(query) && isActionableStartNode(node)) {
                    matches.add(node);
                }
            } else if ("sql-table".equals(request.queryType())) {
                if ("db_table".equals(node.nodeKind()) && normalize(node.name()).equals(query)) {
                    matches.add(node);
                }
            } else if ("symbol".equals(request.queryType())) {
                if (isActionableStartNode(node) && (normalize(node.qualifiedName()).equals(query)
                        || normalize(node.name()).equals(query) || normalize(node.nodeKey()).equals(query))) {
                    matches.add(node);
                }
            }
        }
        if (!matches.isEmpty() || !"symbol".equals(request.queryType())) {
            if ("symbol".equals(request.queryType())) {
                addPropertyAccessorMatches(matches, data.nodes(), query);
            }
            return matches;
        }
        for (GraphNode node : data.nodes()) {
            if (isActionableStartNode(node) && isPropertyAccessorMatch(node, query)) {
                matches.add(node);
            }
        }
        if (!matches.isEmpty()) {
            return matches;
        }
        List<GraphNode> productionMatches = new ArrayList<GraphNode>();
        for (GraphNode node : data.nodes()) {
            if (isActionableStartNode(node) && (normalize(node.qualifiedName()).contains(query)
                    || normalize(node.name()).contains(query) || normalize(node.nodeKey()).contains(query))) {
                if (!isTestNode(node)) {
                    productionMatches.add(node);
                } else {
                    matches.add(node);
                }
            }
        }
        return productionMatches.isEmpty() ? matches : productionMatches;
    }

    private Set<String> traverse(List<GraphNode> startNodes, Map<String, GraphNode> nodesByKey,
                                 Map<String, List<GraphEdge>> outgoing, Map<String, List<GraphEdge>> incoming,
                                 int depth) {
        Set<String> visited = new LinkedHashSet<String>();
        Queue<Step> queue = new ArrayDeque<Step>();
        for (GraphNode node : startNodes) {
            visited.add(node.nodeKey());
            queue.add(new Step(node.nodeKey(), 0));
        }
        int maxDepth = Math.max(1, depth);
        while (!queue.isEmpty()) {
            Step step = queue.remove();
            if (step.depth >= maxDepth) {
                continue;
            }
            GraphNode current = nodesByKey.get(step.nodeKey);
            if (!shouldExpandNode(current)) {
                continue;
            }
            visitNeighbors(outgoing.get(step.nodeKey), nodesByKey, current, visited, queue, step.depth + 1, true);
            visitNeighbors(incoming.get(step.nodeKey), nodesByKey, current, visited, queue, step.depth + 1, false);
        }
        return visited;
    }

    private void visitNeighbors(List<GraphEdge> edges, Map<String, GraphNode> nodesByKey, GraphNode current,
                                Set<String> visited, Queue<Step> queue, int nextDepth, boolean outgoing) {
        if (edges == null) {
            return;
        }
        for (GraphEdge edge : edges) {
            String neighbor = outgoing ? edge.targetNodeKey() : edge.sourceNodeKey();
            GraphNode neighborNode = nodesByKey.get(neighbor);
            if (!shouldTraverseEdge(edge, current, neighborNode)) {
                continue;
            }
            if (visited.add(neighbor)) {
                queue.add(new Step(neighbor, nextDepth));
            }
        }
    }

    private List<GraphNode> candidates(GraphData data, String query) {
        String normalized = normalize(query);
        List<GraphNode> matches = new ArrayList<GraphNode>();
        for (GraphNode node : data.nodes()) {
            String haystack = normalize(node.qualifiedName() + " " + node.name() + " " + node.nodeKey());
            if (haystack.contains(normalized) || normalized.contains(normalize(node.name()))) {
                matches.add(node);
            }
        }
        Collections.sort(matches, nodeComparator());
        return limitNodes(matches, 8);
    }

    private Map<String, GraphNode> nodesByKey(List<GraphNode> nodes) {
        Map<String, GraphNode> map = new LinkedHashMap<String, GraphNode>();
        for (GraphNode node : nodes) {
            map.put(node.nodeKey(), node);
        }
        return map;
    }

    private void addEdge(Map<String, List<GraphEdge>> map, String key, GraphEdge edge) {
        List<GraphEdge> edges = map.get(key);
        if (edges == null) {
            edges = new ArrayList<GraphEdge>();
            map.put(key, edges);
        }
        edges.add(edge);
    }

    private List<GraphNode> nodesForKeys(Map<String, GraphNode> nodesByKey, Set<String> keys) {
        List<GraphNode> nodes = new ArrayList<GraphNode>();
        for (String key : keys) {
            GraphNode node = nodesByKey.get(key);
            if (node != null) {
                nodes.add(node);
            }
        }
        Collections.sort(nodes, nodeComparator());
        return nodes;
    }

    private List<GraphEdge> directEdges(List<GraphNode> nodes, Map<String, List<GraphEdge>> edgeMap,
                                        Map<String, GraphNode> nodesByKey, boolean outgoing) {
        List<GraphEdge> result = new ArrayList<GraphEdge>();
        for (GraphNode node : nodes) {
            List<GraphEdge> edges = edgeMap.get(node.nodeKey());
            if (edges != null) {
                for (GraphEdge edge : edges) {
                    String neighbor = outgoing ? edge.targetNodeKey() : edge.sourceNodeKey();
                    if (shouldTraverseEdge(edge, node, nodesByKey.get(neighbor))) {
                        result.add(edge);
                    }
                }
            }
        }
        return result;
    }

    private List<String> relatedFiles(List<GraphNode> nodes) {
        Set<String> files = new LinkedHashSet<String>();
        for (GraphNode node : nodes) {
            if (node.relativePath().length() > 0) {
                files.add(node.relativePath());
            }
        }
        List<String> sorted = new ArrayList<String>(files);
        Collections.sort(sorted);
        return sorted;
    }

    private List<String> relatedTests(List<String> relatedFiles, List<GraphFileEntry> files) {
        Set<String> tests = new LinkedHashSet<String>();
        Set<String> indexedFiles = indexedFiles(files);
        for (String file : relatedFiles) {
            if (file.startsWith("src/test/")) {
                tests.add(file);
                continue;
            }
            String expected = expectedTestPath(file);
            if (expected.length() > 0 && indexedFiles.contains(expected)) {
                tests.add(expected);
            }
        }
        List<String> sorted = new ArrayList<String>(tests);
        Collections.sort(sorted);
        return sorted;
    }

    private List<String> missingRelatedTests(List<String> relatedFiles, List<GraphFileEntry> files,
                                             GraphImpactRequest request, List<GraphNode> startNodes) {
        Set<String> indexedFiles = indexedFiles(files);
        Set<String> missing = new LinkedHashSet<String>();
        for (String file : relatedFiles) {
            String expected = expectedTestPath(file);
            if (expected.length() > 0 && !indexedFiles.contains(expected)) {
                if (isRepositoryFile(file) && !isRepositoryTestGapRelevant(request, startNodes)) {
                    continue;
                }
                missing.add(expected);
            }
        }
        List<String> sorted = new ArrayList<String>(missing);
        Collections.sort(sorted);
        return sorted;
    }

    private List<String> includeRelatedTests(List<String> relatedFiles, List<String> tests) {
        Set<String> values = new LinkedHashSet<String>(relatedFiles);
        values.addAll(tests);
        List<String> sorted = new ArrayList<String>(values);
        Collections.sort(sorted);
        return sorted;
    }

    private Set<String> indexedFiles(List<GraphFileEntry> files) {
        Set<String> indexed = new LinkedHashSet<String>();
        for (GraphFileEntry file : files) {
            if (file.indexed()) {
                indexed.add(file.relativePath());
            }
        }
        return indexed;
    }

    private String expectedTestPath(String file) {
        if (file == null || !file.startsWith("src/main/java/") || !file.endsWith(".java")) {
            return "";
        }
        if (!(file.contains("/controller/") || file.contains("/service/") || file.contains("/repository/"))) {
            return "";
        }
        String className = file.substring(file.lastIndexOf('/') + 1, file.length() - ".java".length());
        if (className.startsWith("InMemory") || className.startsWith("Noop") || className.endsWith("Config")
                || className.endsWith("Publisher")) {
            return "";
        }
        String withoutPrefix = file.substring("src/main/java/".length(), file.length() - ".java".length());
        return "src/test/java/" + withoutPrefix + "Test.java";
    }

    private List<String> expandLegacyDataFlowFiles(List<String> relatedFiles, List<GraphFileEntry> files,
                                                   GraphImpactRequest request, List<GraphNode> startNodes,
                                                   List<GraphNode> impactedNodes) {
        if (!isLegacyProject(files) || isUtilitySymbolQuery(request, startNodes)
                || isSqlParameterSymbolQuery(request, startNodes)
                || !isLegacyDataFlowImpact(request, startNodes, impactedNodes)) {
            return relatedFiles;
        }
        boolean dataFlow = false;
        for (String file : relatedFiles) {
            if (file.contains("/service/") || file.contains("/mapper/") || file.contains("/mybatis/")
                    || file.contains("/web/")) {
                dataFlow = true;
                break;
            }
        }
        if (!dataFlow) {
            return relatedFiles;
        }
        Set<String> expanded = new LinkedHashSet<String>(relatedFiles);
        for (GraphFileEntry file : files) {
            if (file.indexed() && file.relativePath().contains("/dto/")) {
                expanded.add(file.relativePath());
            }
        }
        List<String> sorted = new ArrayList<String>(expanded);
        Collections.sort(sorted);
        return sorted;
    }

    private boolean isActionableStartNode(GraphNode node) {
        if (node == null) {
            return false;
        }
        String kind = node.nodeKind();
        return !("package".equals(kind) || "import".equals(kind) || "annotation".equals(kind)
                || "type_reference".equals(kind) || "method_reference".equals(kind)
                || "reference".equals(kind));
    }

    private boolean isPropertyAccessorMatch(GraphNode node, String query) {
        if (!"method".equals(node.nodeKind()) && !"test_case".equals(node.nodeKind())) {
            return false;
        }
        String name = normalize(node.name());
        String normalizedQuery = normalize(query);
        return name.equals("get" + normalizedQuery) || name.equals("set" + normalizedQuery)
                || name.equals("is" + normalizedQuery);
    }

    private void addPropertyAccessorMatches(List<GraphNode> matches, List<GraphNode> nodes, String query) {
        for (GraphNode node : nodes) {
            if (isActionableStartNode(node) && isPropertyAccessorMatch(node, query) && !containsNode(matches, node)) {
                matches.add(node);
            }
        }
    }

    private boolean containsNode(List<GraphNode> nodes, GraphNode candidate) {
        for (GraphNode node : nodes) {
            if (node.nodeKey().equals(candidate.nodeKey())) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldExpandNode(GraphNode node) {
        if (node == null) {
            return false;
        }
        String kind = node.nodeKind();
        return !("package".equals(kind) || "import".equals(kind) || "annotation".equals(kind)
                || "reference".equals(kind) || "type_reference".equals(kind)
                || "method_reference".equals(kind) || "test_case".equals(kind));
    }

    private boolean shouldTraverseEdge(GraphEdge edge, GraphNode current, GraphNode neighbor) {
        if (edge == null) {
            return false;
        }
        if ("imports".equals(edge.edgeKind())) {
            return false;
        }
        if (isExternalJavaKey(edge.sourceNodeKey()) || isExternalJavaKey(edge.targetNodeKey())) {
            return false;
        }
        if ("contains".equals(edge.edgeKind()) && current != null && "package".equals(current.nodeKind())) {
            return false;
        }
        if (neighbor == null) {
            return true;
        }
        String kind = neighbor.nodeKind();
        return !("package".equals(kind) || "import".equals(kind) || "annotation".equals(kind)
                || "reference".equals(kind) || "type_reference".equals(kind)
                || "method_reference".equals(kind));
    }

    private boolean isExternalJavaKey(String key) {
        return key != null && (key.startsWith("java_import:java.") || key.startsWith("java_type:java.")
                || key.startsWith("java_method:java."));
    }

    private boolean isTestNode(GraphNode node) {
        return node != null && ("test_case".equals(node.nodeKind())
                || node.relativePath().startsWith("src/test/"));
    }

    private boolean isUtilitySymbolQuery(GraphImpactRequest request, List<GraphNode> startNodes) {
        if (!"symbol".equals(request.queryType())) {
            return false;
        }
        for (GraphNode node : startNodes) {
            if (node.relativePath().contains("/util/") || node.relativePath().contains("/helper/")
                    || node.relativePath().contains("/support/")) {
                return true;
            }
        }
        return false;
    }

    private boolean isSqlParameterSymbolQuery(GraphImpactRequest request, List<GraphNode> startNodes) {
        if (!"symbol".equals(request.queryType())) {
            return false;
        }
        for (GraphNode node : startNodes) {
            if ("sql_parameter".equals(node.nodeKind())) {
                return true;
            }
        }
        return false;
    }

    private Set<String> utilityImpactFiles(List<GraphNode> startNodes, List<GraphEdge> directCallers,
                                           Map<String, GraphNode> nodesByKey) {
        return directImpactFiles(startNodes, directCallers, nodesByKey);
    }

    private Set<String> sqlParameterImpactFiles(List<GraphNode> startNodes, List<GraphEdge> directCallers,
                                                Map<String, GraphNode> nodesByKey) {
        return directImpactFiles(startNodes, directCallers, nodesByKey);
    }

    private Set<String> directImpactFiles(List<GraphNode> startNodes, List<GraphEdge> directCallers,
                                          Map<String, GraphNode> nodesByKey) {
        Set<String> files = new LinkedHashSet<String>();
        for (GraphNode node : startNodes) {
            addFile(files, node.relativePath());
        }
        for (GraphEdge edge : directCallers) {
            GraphNode caller = nodesByKey.get(edge.sourceNodeKey());
            if (caller != null) {
                addFile(files, caller.relativePath());
            }
        }
        return files;
    }

    private List<GraphNode> filterNodesByFiles(List<GraphNode> nodes, Set<String> files) {
        List<GraphNode> result = new ArrayList<GraphNode>();
        for (GraphNode node : nodes) {
            if (files.contains(node.relativePath())) {
                result.add(node);
            }
        }
        return result;
    }

    private void addFile(Set<String> files, String file) {
        if (file != null && file.length() > 0) {
            files.add(file);
        }
    }

    private boolean isLegacyProject(List<GraphFileEntry> files) {
        for (GraphFileEntry file : files) {
            String path = file.relativePath();
            if (path.startsWith("src/main/webapp/") || path.contains("/mybatis/")
                    || path.contains("/mapper/") || path.endsWith("web.xml")) {
                return true;
            }
        }
        return false;
    }

    private boolean isLegacyDataFlowImpact(GraphImpactRequest request, List<GraphNode> startNodes,
                                           List<GraphNode> impactedNodes) {
        if ("sql-table".equals(request.queryType())) {
            return true;
        }
        for (GraphNode node : startNodes) {
            if (node.relativePath().contains("/mapper/") || node.relativePath().contains("/mybatis/")) {
                return true;
            }
        }
        for (GraphNode node : impactedNodes) {
            if ("sql_statement".equals(node.nodeKind()) || "xml_mapper".equals(node.nodeKind())
                    || "db_table".equals(node.nodeKind())) {
                return true;
            }
        }
        return false;
    }

    private boolean isRepositoryFile(String file) {
        return file != null && file.contains("/repository/") && file.endsWith(".java");
    }

    private boolean isRepositoryTestGapRelevant(GraphImpactRequest request, List<GraphNode> startNodes) {
        if ("file".equals(request.queryType())) {
            return request.query().contains("/service/") || request.query().contains("/repository/");
        }
        for (GraphNode node : startNodes) {
            if (node.relativePath().contains("/service/") || node.relativePath().contains("/repository/")) {
                return true;
            }
        }
        return false;
    }

    private List<GraphNode> filterNodes(List<GraphNode> nodes, String... kinds) {
        Set<String> wanted = new LinkedHashSet<String>();
        Collections.addAll(wanted, kinds);
        List<GraphNode> result = new ArrayList<GraphNode>();
        for (GraphNode node : nodes) {
            if (wanted.contains(node.nodeKind())) {
                result.add(node);
            }
        }
        return result;
    }

    private List<GraphNode> relatedSqlNodes(List<GraphNode> nodes, GraphImpactRequest request) {
        List<GraphNode> result = new ArrayList<GraphNode>();
        String query = normalize(request.query());
        for (GraphNode node : nodes) {
            if ("xml_mapper".equals(node.nodeKind()) || "sql_statement".equals(node.nodeKind())
                    || "db_table".equals(node.nodeKind())) {
                result.add(node);
            } else if ("sql_parameter".equals(node.nodeKind())
                    && (!"symbol".equals(request.queryType()) || normalize(node.name()).equals(query))) {
                result.add(node);
            }
        }
        return result;
    }

    private List<GraphNode> riskNodes(List<GraphNode> nodes) {
        List<GraphNode> result = new ArrayList<GraphNode>();
        for (GraphNode node : nodes) {
            if ("route".equals(node.nodeKind()) || "sql_statement".equals(node.nodeKind())
                    || "db_table".equals(node.nodeKind()) || "xml_mapper".equals(node.nodeKind())) {
                result.add(node);
            }
        }
        return result;
    }

    private List<String> recommendedReadFiles(List<String> relatedFiles) {
        List<String> result = new ArrayList<String>();
        for (String file : relatedFiles) {
            if (!file.startsWith("target/") && !file.startsWith(".agents/")) {
                result.add(file);
            }
            if (result.size() >= 20) {
                break;
            }
        }
        return result;
    }

    private List<GraphNode> limitNodes(List<GraphNode> nodes, int limit) {
        if (nodes.size() <= limit) {
            return nodes;
        }
        return new ArrayList<GraphNode>(nodes.subList(0, limit));
    }

    private Comparator<GraphNode> nodeComparator() {
        return new Comparator<GraphNode>() {
            public int compare(GraphNode left, GraphNode right) {
                int file = left.relativePath().compareTo(right.relativePath());
                if (file != 0) {
                    return file;
                }
                return left.nodeKey().compareTo(right.nodeKey());
            }
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static final class Step {
        private final String nodeKey;
        private final int depth;

        private Step(String nodeKey, int depth) {
            this.nodeKey = nodeKey;
            this.depth = depth;
        }
    }
}
