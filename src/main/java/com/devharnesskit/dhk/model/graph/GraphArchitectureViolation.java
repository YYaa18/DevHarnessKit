package com.devharnesskit.dhk.model.graph;

public final class GraphArchitectureViolation {
    private final String sourceLayer;
    private final String targetLayer;
    private final String sourceName;
    private final String targetName;
    private final String sourcePath;
    private final String targetPath;
    private final String edgeKind;
    private final String reason;

    public GraphArchitectureViolation(String sourceLayer, String targetLayer,
                                      String sourceName, String targetName,
                                      String sourcePath, String targetPath,
                                      String edgeKind, String reason) {
        this.sourceLayer = value(sourceLayer);
        this.targetLayer = value(targetLayer);
        this.sourceName = value(sourceName);
        this.targetName = value(targetName);
        this.sourcePath = value(sourcePath);
        this.targetPath = value(targetPath);
        this.edgeKind = value(edgeKind);
        this.reason = value(reason);
    }

    public String sourceLayer() { return sourceLayer; }
    public String targetLayer() { return targetLayer; }
    public String sourceName() { return sourceName; }
    public String targetName() { return targetName; }
    public String sourcePath() { return sourcePath; }
    public String targetPath() { return targetPath; }
    public String edgeKind() { return edgeKind; }
    public String reason() { return reason; }

    public String toLogLine() {
        return sourceLayer + " -> " + targetLayer + ": " + sourceName + " -> " + targetName
                + " [" + edgeKind + "] source=" + sourcePath + " target=" + targetPath
                + " reason=" + reason;
    }

    private static String value(String input) {
        return input == null ? "" : input;
    }
}
