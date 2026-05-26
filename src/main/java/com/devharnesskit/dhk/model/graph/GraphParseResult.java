package com.devharnesskit.dhk.model.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GraphParseResult {
    private static final GraphParseResult EMPTY = new GraphParseResult(
            Collections.<GraphNode>emptyList(),
            Collections.<GraphEdge>emptyList(),
            Collections.<GraphParseError>emptyList());

    private final List<GraphNode> nodes;
    private final List<GraphEdge> edges;
    private final List<GraphParseError> errors;

    public GraphParseResult(List<GraphNode> nodes, List<GraphEdge> edges, List<GraphParseError> errors) {
        this.nodes = Collections.unmodifiableList(new ArrayList<GraphNode>(nodes));
        this.edges = Collections.unmodifiableList(new ArrayList<GraphEdge>(edges));
        this.errors = Collections.unmodifiableList(new ArrayList<GraphParseError>(errors));
    }

    public static GraphParseResult empty() {
        return EMPTY;
    }

    public List<GraphNode> nodes() {
        return nodes;
    }

    public List<GraphEdge> edges() {
        return edges;
    }

    public List<GraphParseError> errors() {
        return errors;
    }

    public Map<String, Integer> nodeKindCounts() {
        Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
        for (GraphNode node : nodes) {
            Integer current = counts.get(node.nodeKind());
            counts.put(node.nodeKind(), current == null ? 1 : current + 1);
        }
        return counts;
    }

    public static final class Builder {
        private final Map<String, GraphNode> nodes = new LinkedHashMap<String, GraphNode>();
        private final List<GraphEdge> edges = new ArrayList<GraphEdge>();
        private final Set<String> edgeKeys = new LinkedHashSet<String>();
        private final List<GraphParseError> errors = new ArrayList<GraphParseError>();

        public void addNode(GraphNode node) {
            if (node == null || node.nodeKey().length() == 0) {
                return;
            }
            GraphNode existing = nodes.get(node.nodeKey());
            if (existing == null || shouldReplace(existing, node)) {
                nodes.put(node.nodeKey(), node);
            }
        }

        public void addEdge(GraphEdge edge) {
            if (edge == null || edge.sourceNodeKey().length() == 0 || edge.targetNodeKey().length() == 0) {
                return;
            }
            if (edgeKeys.add(edge.identity())) {
                edges.add(edge);
            }
        }

        public void addError(GraphParseError error) {
            if (error != null) {
                errors.add(error);
            }
        }

        public GraphParseResult build() {
            return new GraphParseResult(new ArrayList<GraphNode>(nodes.values()), edges, errors);
        }

        private boolean shouldReplace(GraphNode existing, GraphNode candidate) {
            return isReference(existing.nodeKind()) && !isReference(candidate.nodeKind());
        }

        private boolean isReference(String kind) {
            return "reference".equals(kind) || "type_reference".equals(kind) || "import".equals(kind);
        }
    }
}
