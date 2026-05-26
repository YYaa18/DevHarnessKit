package com.devharnesskit.dhk.model.graph;

public final class GraphNode {
    private final String nodeKey;
    private final String nodeKind;
    private final String name;
    private final String qualifiedName;
    private final String relativePath;
    private final int startLine;
    private final int endLine;
    private final String language;
    private final String visibility;
    private final String signature;
    private final int confidence;
    private final String source;
    private final String evidence;

    public GraphNode(String nodeKey, String nodeKind, String name, String qualifiedName,
                     String relativePath, int startLine, int endLine, String language,
                     String visibility, String signature, int confidence, String source,
                     String evidence) {
        this.nodeKey = value(nodeKey);
        this.nodeKind = value(nodeKind);
        this.name = value(name);
        this.qualifiedName = value(qualifiedName);
        this.relativePath = value(relativePath);
        this.startLine = startLine;
        this.endLine = endLine;
        this.language = value(language);
        this.visibility = value(visibility);
        this.signature = value(signature);
        this.confidence = confidence;
        this.source = value(source);
        this.evidence = value(evidence);
    }

    public String nodeKey() {
        return nodeKey;
    }

    public String nodeKind() {
        return nodeKind;
    }

    public String name() {
        return name;
    }

    public String qualifiedName() {
        return qualifiedName;
    }

    public String relativePath() {
        return relativePath;
    }

    public int startLine() {
        return startLine;
    }

    public int endLine() {
        return endLine;
    }

    public String language() {
        return language;
    }

    public String visibility() {
        return visibility;
    }

    public String signature() {
        return signature;
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

    private static String value(String input) {
        return input == null ? "" : input;
    }
}
