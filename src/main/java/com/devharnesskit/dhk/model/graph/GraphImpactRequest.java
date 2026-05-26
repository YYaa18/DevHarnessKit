package com.devharnesskit.dhk.model.graph;

public final class GraphImpactRequest {
    private final String queryType;
    private final String query;
    private final int depth;
    private final boolean allowStale;
    private final String allowStaleEvidence;

    public GraphImpactRequest(String queryType, String query, int depth) {
        this(queryType, query, depth, false, "");
    }

    public GraphImpactRequest(String queryType, String query, int depth, boolean allowStale) {
        this(queryType, query, depth, allowStale, "");
    }

    public GraphImpactRequest(String queryType, String query, int depth, boolean allowStale,
                              String allowStaleEvidence) {
        this.queryType = queryType == null ? "" : queryType;
        this.query = query == null ? "" : query;
        this.depth = depth;
        this.allowStale = allowStale;
        this.allowStaleEvidence = allowStaleEvidence == null ? "" : allowStaleEvidence.trim();
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

    public String allowStaleEvidence() {
        return allowStaleEvidence;
    }
}
