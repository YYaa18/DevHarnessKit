package com.devharnesskit.dhk.model.graph;

public final class GraphImpactRequest {
    private final String queryType;
    private final String query;
    private final int depth;
    private final boolean allowStale;

    public GraphImpactRequest(String queryType, String query, int depth) {
        this(queryType, query, depth, false);
    }

    public GraphImpactRequest(String queryType, String query, int depth, boolean allowStale) {
        this.queryType = queryType == null ? "" : queryType;
        this.query = query == null ? "" : query;
        this.depth = depth;
        this.allowStale = allowStale;
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

    public boolean allowStale() {
        return allowStale;
    }
}
