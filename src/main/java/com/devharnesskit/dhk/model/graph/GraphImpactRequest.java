package com.devharnesskit.dhk.model.graph;

public final class GraphImpactRequest {
    private final String queryType;
    private final String query;
    private final int depth;

    public GraphImpactRequest(String queryType, String query, int depth) {
        this.queryType = queryType == null ? "" : queryType;
        this.query = query == null ? "" : query;
        this.depth = depth;
    }

    public String queryType() {
        return queryType;
    }

    public String query() {
        return query;
    }

    public int depth() {
        return depth;
    }
}
