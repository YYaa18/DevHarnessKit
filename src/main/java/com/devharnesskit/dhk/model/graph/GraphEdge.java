package com.devharnesskit.dhk.model.graph;

public final class GraphEdge {
    private final String edgeKind;
    private final String sourceNodeKey;
    private final String targetNodeKey;
    private final String relativePath;
    private final int confidence;
    private final String source;
    private final String evidence;

    public GraphEdge(String edgeKind, String sourceNodeKey, String targetNodeKey,
                     String relativePath, int confidence, String source, String evidence) {
        this.edgeKind = value(edgeKind);
        this.sourceNodeKey = value(sourceNodeKey);
        this.targetNodeKey = value(targetNodeKey);
        this.relativePath = value(relativePath);
        this.confidence = confidence;
        this.source = value(source);
        this.evidence = value(evidence);
    }

    public String edgeKind() {
        return edgeKind;
    }

    public String sourceNodeKey() {
        return sourceNodeKey;
    }

    public String targetNodeKey() {
        return targetNodeKey;
    }

    public String relativePath() {
        return relativePath;
    }

    public int confidence() {
        return confidence;
    }

    public String source() {
        return source;
    }

    public String evidence() {
        return evidence;
    }

    public String identity() {
        return edgeKind + "|" + sourceNodeKey + "|" + targetNodeKey + "|" + relativePath + "|" + evidence;
    }

    private static String value(String input) {
        return input == null ? "" : input;
    }
}
