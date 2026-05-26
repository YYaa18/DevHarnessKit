package com.devharnesskit.dhk.model.graph;

public final class GraphPruneResult {
    private final int keep;
    private final int deletedSnapshots;
    private final int remainingSnapshots;
    private final int deletedFiles;
    private final int deletedNodes;
    private final int deletedEdges;
    private final int deletedQueryCacheRows;
    private final int deletedGoalGraphBindings;

    public GraphPruneResult(int keep, int deletedSnapshots, int remainingSnapshots,
                            int deletedFiles, int deletedNodes, int deletedEdges,
                            int deletedQueryCacheRows, int deletedGoalGraphBindings) {
        this.keep = keep;
        this.deletedSnapshots = deletedSnapshots;
        this.remainingSnapshots = remainingSnapshots;
        this.deletedFiles = deletedFiles;
        this.deletedNodes = deletedNodes;
        this.deletedEdges = deletedEdges;
        this.deletedQueryCacheRows = deletedQueryCacheRows;
        this.deletedGoalGraphBindings = deletedGoalGraphBindings;
    }

    public int keep() { return keep; }
    public int deletedSnapshots() { return deletedSnapshots; }
    public int remainingSnapshots() { return remainingSnapshots; }
    public int deletedFiles() { return deletedFiles; }
    public int deletedNodes() { return deletedNodes; }
    public int deletedEdges() { return deletedEdges; }
    public int deletedQueryCacheRows() { return deletedQueryCacheRows; }
    public int deletedGoalGraphBindings() { return deletedGoalGraphBindings; }
}
