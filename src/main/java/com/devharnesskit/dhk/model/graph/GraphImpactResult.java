package com.devharnesskit.dhk.model.graph;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GraphImpactResult {
    private final GraphImpactRequest request;
    private final GraphSnapshot snapshot;
    private final boolean found;
    private final List<GraphNode> startNodes;
    private final List<GraphNode> impactedNodes;
    private final List<GraphEdge> directCallers;
    private final List<GraphEdge> directCallees;
    private final List<String> relatedFiles;
    private final List<GraphNode> relatedSql;
    private final List<String> relatedTests;
    private final List<String> missingRelatedTests;
    private final List<GraphNode> riskNodes;
    private final List<String> recommendedReadFiles;
    private final List<GraphNode> candidates;
    private final Path impactMapPath;
    private final int requestedDepth;
    private final int maxImpactDepth;
    private final boolean depthLimited;
    private final String currentWorkspaceFingerprint;
    private final boolean snapshotStale;
    private final boolean staleAllowed;

    public GraphImpactResult(GraphImpactRequest request, GraphSnapshot snapshot, boolean found,
                             List<GraphNode> startNodes, List<GraphNode> impactedNodes,
                             List<GraphEdge> directCallers, List<GraphEdge> directCallees,
                             List<String> relatedFiles, List<GraphNode> relatedSql,
                             List<String> relatedTests, List<GraphNode> riskNodes,
                             List<String> recommendedReadFiles, List<GraphNode> candidates,
                             Path impactMapPath) {
        this(request, snapshot, found, startNodes, impactedNodes, directCallers, directCallees,
                relatedFiles, relatedSql, relatedTests, new ArrayList<String>(), riskNodes, recommendedReadFiles, candidates,
                impactMapPath, request.depth(), request.depth(), false, "", false, false);
    }

    public GraphImpactResult(GraphImpactRequest request, GraphSnapshot snapshot, boolean found,
                             List<GraphNode> startNodes, List<GraphNode> impactedNodes,
                             List<GraphEdge> directCallers, List<GraphEdge> directCallees,
                             List<String> relatedFiles, List<GraphNode> relatedSql,
                             List<String> relatedTests, List<String> missingRelatedTests, List<GraphNode> riskNodes,
                             List<String> recommendedReadFiles, List<GraphNode> candidates,
                             Path impactMapPath, int requestedDepth, int maxImpactDepth, boolean depthLimited) {
        this(request, snapshot, found, startNodes, impactedNodes, directCallers, directCallees, relatedFiles,
                relatedSql, relatedTests, missingRelatedTests, riskNodes, recommendedReadFiles, candidates,
                impactMapPath, requestedDepth, maxImpactDepth, depthLimited, "", false, false);
    }

    public GraphImpactResult(GraphImpactRequest request, GraphSnapshot snapshot, boolean found,
                             List<GraphNode> startNodes, List<GraphNode> impactedNodes,
                             List<GraphEdge> directCallers, List<GraphEdge> directCallees,
                             List<String> relatedFiles, List<GraphNode> relatedSql,
                             List<String> relatedTests, List<String> missingRelatedTests, List<GraphNode> riskNodes,
                             List<String> recommendedReadFiles, List<GraphNode> candidates,
                             Path impactMapPath, int requestedDepth, int maxImpactDepth, boolean depthLimited,
                             String currentWorkspaceFingerprint, boolean snapshotStale, boolean staleAllowed) {
        this.request = request;
        this.snapshot = snapshot;
        this.found = found;
        this.startNodes = copyNodes(startNodes);
        this.impactedNodes = copyNodes(impactedNodes);
        this.directCallers = copyEdges(directCallers);
        this.directCallees = copyEdges(directCallees);
        this.relatedFiles = copyStrings(relatedFiles);
        this.relatedSql = copyNodes(relatedSql);
        this.relatedTests = copyStrings(relatedTests);
        this.missingRelatedTests = copyStrings(missingRelatedTests);
        this.riskNodes = copyNodes(riskNodes);
        this.recommendedReadFiles = copyStrings(recommendedReadFiles);
        this.candidates = copyNodes(candidates);
        this.impactMapPath = impactMapPath;
        this.requestedDepth = requestedDepth;
        this.maxImpactDepth = maxImpactDepth;
        this.depthLimited = depthLimited;
        this.currentWorkspaceFingerprint = currentWorkspaceFingerprint == null ? "" : currentWorkspaceFingerprint;
        this.snapshotStale = snapshotStale;
        this.staleAllowed = staleAllowed;
    }

    public GraphImpactRequest request() {
        return request;
    }

    public GraphSnapshot snapshot() {
        return snapshot;
    }

    public boolean found() {
        return found;
    }

    public List<GraphNode> startNodes() {
        return startNodes;
    }

    public List<GraphNode> impactedNodes() {
        return impactedNodes;
    }

    public List<GraphEdge> directCallers() {
        return directCallers;
    }

    public List<GraphEdge> directCallees() {
        return directCallees;
    }

    public List<String> relatedFiles() {
        return relatedFiles;
    }

    public List<GraphNode> relatedSql() {
        return relatedSql;
    }

    public List<String> relatedTests() {
        return relatedTests;
    }

    public List<String> missingRelatedTests() {
        return missingRelatedTests;
    }

    public List<GraphNode> riskNodes() {
        return riskNodes;
    }

    public List<String> recommendedReadFiles() {
        return recommendedReadFiles;
    }

    public List<GraphNode> candidates() {
        return candidates;
    }

    public Path impactMapPath() {
        return impactMapPath;
    }

    public int requestedDepth() {
        return requestedDepth;
    }

    public int maxImpactDepth() {
        return maxImpactDepth;
    }

    public boolean depthLimited() {
        return depthLimited;
    }

    public String currentWorkspaceFingerprint() {
        return currentWorkspaceFingerprint;
    }

    public boolean snapshotStale() {
        return snapshotStale;
    }

    public boolean staleAllowed() {
        return staleAllowed;
    }

    private static List<GraphNode> copyNodes(List<GraphNode> values) {
        return Collections.unmodifiableList(new ArrayList<GraphNode>(values));
    }

    private static List<GraphEdge> copyEdges(List<GraphEdge> values) {
        return Collections.unmodifiableList(new ArrayList<GraphEdge>(values));
    }

    private static List<String> copyStrings(List<String> values) {
        return Collections.unmodifiableList(new ArrayList<String>(values));
    }
}
