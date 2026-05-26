package com.devharnesskit.dhk.model.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GraphData {
    private final GraphSnapshot snapshot;
    private final List<GraphFileEntry> files;
    private final List<GraphNode> nodes;
    private final List<GraphEdge> edges;

    public GraphData(GraphSnapshot snapshot, List<GraphFileEntry> files, List<GraphNode> nodes,
                     List<GraphEdge> edges) {
        this.snapshot = snapshot;
        this.files = Collections.unmodifiableList(new ArrayList<GraphFileEntry>(files));
        this.nodes = Collections.unmodifiableList(new ArrayList<GraphNode>(nodes));
        this.edges = Collections.unmodifiableList(new ArrayList<GraphEdge>(edges));
    }

    public GraphSnapshot snapshot() {
        return snapshot;
    }

    public List<GraphFileEntry> files() {
        return files;
    }

    public List<GraphNode> nodes() {
        return nodes;
    }

    public List<GraphEdge> edges() {
        return edges;
    }
}
