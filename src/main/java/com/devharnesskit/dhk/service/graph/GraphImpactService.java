package com.devharnesskit.dhk.service.graph;

import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.export.GraphImpactRenderer;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.graph.GraphData;
import com.devharnesskit.dhk.model.graph.GraphEdge;
import com.devharnesskit.dhk.model.graph.GraphFileEntry;
import com.devharnesskit.dhk.model.graph.GraphImpactRequest;
import com.devharnesskit.dhk.model.graph.GraphImpactResult;
import com.devharnesskit.dhk.model.graph.GraphNode;
import com.devharnesskit.dhk.model.graph.GraphSnapshot;
import com.devharnesskit.dhk.repository.graph.GraphRepository;
import com.devharnesskit.dhk.service.ProjectService;
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

    public GraphImpactService() {
        this(new DbConnectionFactory(), new ProjectService(), new GraphRepository(), new GraphImpactRenderer());
    }

    GraphImpactService(DbConnectionFactory connectionFactory, ProjectService projectService,
                       GraphRepository graphRepository, GraphImpactRenderer renderer) {
        this.connectionFactory = connectionFactory;
        this.projectService = projectService;
        this.graphRepository = graphRepository;
        this.renderer = renderer;
    }

    public GraphImpactResult impact(Path projectRoot, GraphImpactRequest request, Clock clock) throws Exception {
        PathUtil.createGraphDirectories(projectRoot);
        GraphData data = loadData(projectRoot);
        List<GraphNode> startNodes = startNodes(data, request);
        List<GraphNode> candidates = startNodes.isEmpty() ? candidates(data, request.query()) : Collections.<GraphNode>emptyList();
        GraphImpactResult result;
        if (startNodes.isEmpty()) {
            result = new GraphImpactResult(request, data.snapshot(), false, startNodes,
                    Collections.<GraphNode>emptyList(), Collections.<GraphEdge>emptyList(),
                    Collections.<GraphEdge>emptyList(), Collections.<String>emptyList(),
                    Collections.<GraphNode>emptyList(), Collections.<String>emptyList(),
                    Collections.<GraphNode>emptyList(), Collections.<String>emptyList(), candidates,
                    PathUtil.graphImpactMap(projectRoot));
        } else {
            result = buildResult(projectRoot, data, request, startNodes);
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

    private GraphImpactResult buildResult(Path projectRoot, GraphData data, GraphImpactRequest request,
                                          List<GraphNode> startNodes) {
        Map<String, GraphNode> nodesByKey = nodesByKey(data.nodes());
        Map<String, List<GraphEdge>> outgoing = new LinkedHashMap<String, List<GraphEdge>>();
        Map<String, List<GraphEdge>> incoming = new LinkedHashMap<String, List<GraphEdge>>();
        for (GraphEdge edge : data.edges()) {
            addEdge(outgoing, edge.sourceNodeKey(), edge);
            addEdge(incoming, edge.targetNodeKey(), edge);
        }

        Set<String> impactedKeys = traverse(startNodes, outgoing, incoming, request.depth());
        List<GraphNode> impactedNodes = nodesForKeys(nodesByKey, impactedKeys);
        List<GraphEdge> callers = directEdges(startNodes, incoming);
        List<GraphEdge> callees = directEdges(startNodes, outgoing);
        List<String> relatedFiles = expandLegacyDataFlowFiles(relatedFiles(impactedNodes), data.files());
        List<String> tests = relatedTests(relatedFiles, data.files());
        List<GraphNode> sql = filterNodes(impactedNodes, "xml_mapper", "sql_statement", "db_table", "db_column");
        List<GraphNode> risks = riskNodes(impactedNodes);
        List<String> recommended = recommendedReadFiles(relatedFiles);

        return new GraphImpactResult(request, data.snapshot(), true, startNodes, impactedNodes, callers,
                callees, relatedFiles, sql, tests, risks, recommended, Collections.<GraphNode>emptyList(),
                PathUtil.graphImpactMap(projectRoot));
    }

    private List<GraphNode> startNodes(GraphData data, GraphImpactRequest request) {
        List<GraphNode> matches = new ArrayList<GraphNode>();
        String query = normalize(request.query());
        for (GraphNode node : data.nodes()) {
            if ("file".equals(request.queryType())) {
                if (normalize(node.relativePath()).equals(query)) {
                    matches.add(node);
                }
            } else if ("sql-table".equals(request.queryType())) {
                if ("db_table".equals(node.nodeKind()) && normalize(node.name()).equals(query)) {
                    matches.add(node);
                }
            } else if ("symbol".equals(request.queryType())) {
                if (normalize(node.qualifiedName()).equals(query) || normalize(node.name()).equals(query)
                        || normalize(node.nodeKey()).equals(query)) {
                    matches.add(node);
                }
            }
        }
        if (!matches.isEmpty() || !"symbol".equals(request.queryType())) {
            return matches;
        }
        for (GraphNode node : data.nodes()) {
            if (normalize(node.qualifiedName()).contains(query) || normalize(node.name()).contains(query)
                    || normalize(node.nodeKey()).contains(query)) {
                matches.add(node);
            }
        }
        return matches;
    }

    private Set<String> traverse(List<GraphNode> startNodes, Map<String, List<GraphEdge>> outgoing,
                                 Map<String, List<GraphEdge>> incoming, int depth) {
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
            visitNeighbors(outgoing.get(step.nodeKey), visited, queue, step.depth + 1, true);
            visitNeighbors(incoming.get(step.nodeKey), visited, queue, step.depth + 1, false);
        }
        return visited;
    }

    private void visitNeighbors(List<GraphEdge> edges, Set<String> visited, Queue<Step> queue,
                                int nextDepth, boolean outgoing) {
        if (edges == null) {
            return;
        }
        for (GraphEdge edge : edges) {
            String neighbor = outgoing ? edge.targetNodeKey() : edge.sourceNodeKey();
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

    private List<GraphEdge> directEdges(List<GraphNode> nodes, Map<String, List<GraphEdge>> edgeMap) {
        List<GraphEdge> result = new ArrayList<GraphEdge>();
        for (GraphNode node : nodes) {
            List<GraphEdge> edges = edgeMap.get(node.nodeKey());
            if (edges != null) {
                result.addAll(edges);
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
        for (String file : relatedFiles) {
            if (file.startsWith("src/test/")) {
                tests.add(file);
            }
        }
        if (tests.isEmpty()) {
            for (GraphFileEntry file : files) {
                if (file.indexed() && file.relativePath().startsWith("src/test/")) {
                    tests.add(file.relativePath());
                }
            }
        }
        return new ArrayList<String>(tests);
    }

    private List<String> expandLegacyDataFlowFiles(List<String> relatedFiles, List<GraphFileEntry> files) {
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

    private List<GraphNode> riskNodes(List<GraphNode> nodes) {
        List<GraphNode> result = new ArrayList<GraphNode>();
        for (GraphNode node : nodes) {
            if ("route".equals(node.nodeKind()) || "sql_statement".equals(node.nodeKind())
                    || "db_table".equals(node.nodeKind()) || "db_column".equals(node.nodeKind())
                    || "xml_mapper".equals(node.nodeKind())) {
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
