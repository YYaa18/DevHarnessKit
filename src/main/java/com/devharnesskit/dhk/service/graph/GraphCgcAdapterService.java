package com.devharnesskit.dhk.service.graph;

import com.devharnesskit.dhk.model.graph.GraphConfig;
import com.devharnesskit.dhk.model.graph.GraphEdge;
import com.devharnesskit.dhk.model.graph.GraphImpactRequest;
import com.devharnesskit.dhk.model.graph.GraphImpactResult;
import com.devharnesskit.dhk.model.graph.GraphNode;
import com.devharnesskit.dhk.model.graph.GraphSnapshot;
import com.devharnesskit.dhk.util.Clock;
import com.devharnesskit.dhk.util.PathUtil;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public final class GraphCgcAdapterService {
    private static final int CGC_TIMEOUT_SECONDS = 30;
    private static final int MAX_OUTPUT_BYTES = 1024 * 1024;

    public GraphImpactResult impact(Path projectRoot, GraphConfig config, GraphImpactRequest request, Clock clock,
                                    int requestedDepth, int maxImpactDepth, boolean depthLimited) throws Exception {
        String output = execute(projectRoot, config, request);
        NormalizedCgcImpact normalized = normalize(output);
        List<String> relatedFiles = relatedFiles(normalized);
        GraphSnapshot snapshot = new GraphSnapshot(0L, "",
                "cgc-prototype-" + clock.now().toString().replaceAll("[^0-9A-Za-z]", ""),
                "cgc", "external:cgc", "", "completed",
                relatedFiles.size(), normalized.nodes.size(), normalized.edges.size(), 0,
                config.maxFileBytes(), config.maxIndexedFiles(),
                "provider=cgc;source=prototype-adapter;command=" + config.cgcCommand(),
                clock.now().toString(), clock.now().toString());
        return new GraphImpactResult(request, snapshot, !normalized.startNodes.isEmpty() || !normalized.nodes.isEmpty(),
                normalized.startNodes.isEmpty() ? normalized.nodes : normalized.startNodes,
                normalized.nodes, normalized.callers, normalized.callees, relatedFiles,
                normalized.relatedSql, normalized.relatedTests, normalized.missingRelatedTests,
                normalized.riskNodes, recommendedReadFiles(relatedFiles), Collections.<GraphNode>emptyList(),
                PathUtil.graphImpactMap(projectRoot), requestedDepth, maxImpactDepth, depthLimited);
    }

    private String execute(Path projectRoot, GraphConfig config, GraphImpactRequest request) throws Exception {
        List<String> command = command(config.cgcCommand());
        if (command.isEmpty()) {
            throw new IllegalStateException("CGC provider is enabled but cgc_command is empty");
        }
        command.add("analyze");
        command.add("impact");
        command.add("--type");
        command.add(request.queryType());
        command.add("--query");
        command.add(request.query());
        command.add("--depth");
        command.add(String.valueOf(request.depth()));
        command.add("--format");
        command.add("devharness-tsv");

        Process process = null;
        try {
            process = new ProcessBuilder(command)
                    .directory(projectRoot.toFile())
                    .redirectErrorStream(true)
                    .start();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            InputStream input = process.getInputStream();
            byte[] buffer = new byte[4096];
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(CGC_TIMEOUT_SECONDS);
            while (System.nanoTime() < deadline) {
                drain(input, output, buffer);
                if (process.waitFor(50L, TimeUnit.MILLISECONDS)) {
                    drain(input, output, buffer);
                    String text = new String(output.toByteArray(), "UTF-8");
                    if (process.exitValue() == 0) {
                        return text;
                    }
                    throw new IllegalStateException("CGC command failed with exit " + process.exitValue()
                            + ": " + firstLine(text));
                }
            }
            process.destroy();
            if (!process.waitFor(200L, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
            }
            throw new IllegalStateException("CGC command timed out after " + CGC_TIMEOUT_SECONDS + "s");
        } catch (Exception ex) {
            if (process != null) {
                process.destroyForcibly();
            }
            throw ex;
        }
    }

    private NormalizedCgcImpact normalize(String output) {
        NormalizedCgcImpact result = new NormalizedCgcImpact();
        String[] lines = output == null ? new String[0] : output.split("\\r?\\n");
        for (String line : lines) {
            if (line.trim().length() == 0 || line.trim().startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\\t", -1);
            if (parts.length == 0) {
                continue;
            }
            String kind = parts[0].trim();
            if ("node".equals(kind) || "start_node".equals(kind) || "risk_node".equals(kind)
                    || "sql_node".equals(kind)) {
                GraphNode node = node(parts);
                addNode(result.nodes, node);
                if ("start_node".equals(kind)) {
                    addNode(result.startNodes, node);
                }
                if ("risk_node".equals(kind)) {
                    addNode(result.riskNodes, node);
                }
                if ("sql_node".equals(kind)) {
                    addNode(result.relatedSql, node);
                }
            } else if ("edge".equals(kind) || "caller".equals(kind) || "callee".equals(kind)) {
                GraphEdge edge = edge(parts);
                result.edges.add(edge);
                if ("caller".equals(kind)) {
                    result.callers.add(edge);
                } else if ("callee".equals(kind)) {
                    result.callees.add(edge);
                }
            } else if ("related_file".equals(kind) && parts.length > 1) {
                addString(result.relatedFiles, parts[1]);
            } else if ("related_test".equals(kind) && parts.length > 1) {
                addString(result.relatedTests, parts[1]);
            } else if ("missing_related_test".equals(kind) && parts.length > 1) {
                addString(result.missingRelatedTests, parts[1]);
            }
        }
        if (result.startNodes.isEmpty() && !result.nodes.isEmpty()) {
            result.startNodes.add(result.nodes.get(0));
        }
        return result;
    }

    private GraphNode node(String[] parts) {
        String nodeKey = value(parts, 1);
        String nodeKind = value(parts, 2);
        String name = value(parts, 3);
        String qualifiedName = value(parts, 4);
        String relativePath = value(parts, 5);
        int startLine = number(value(parts, 6), 0);
        int endLine = number(value(parts, 7), startLine);
        String language = value(parts, 8);
        int confidence = number(value(parts, 9), 70);
        String evidence = value(parts, 10);
        return new GraphNode(nodeKey, nodeKind, name, qualifiedName, relativePath,
                startLine, endLine, language, "", "", confidence, "cgc", evidence);
    }

    private GraphEdge edge(String[] parts) {
        return new GraphEdge(value(parts, 1), value(parts, 2), value(parts, 3),
                value(parts, 4), number(value(parts, 5), 70), "cgc", value(parts, 6));
    }

    private List<String> relatedFiles(NormalizedCgcImpact result) {
        Set<String> files = new LinkedHashSet<String>(result.relatedFiles);
        for (GraphNode node : result.nodes) {
            if (node.relativePath().length() > 0) {
                files.add(node.relativePath());
            }
        }
        List<String> sorted = new ArrayList<String>(files);
        Collections.sort(sorted);
        return sorted;
    }

    private List<String> recommendedReadFiles(List<String> relatedFiles) {
        List<String> result = new ArrayList<String>();
        for (String file : relatedFiles) {
            if (!file.startsWith("target/") && !file.startsWith(".agents/")) {
                result.add(file);
            }
            if (result.size() >= 20) {
                break;
            }
        }
        return result;
    }

    private void addNode(List<GraphNode> nodes, GraphNode node) {
        for (GraphNode existing : nodes) {
            if (existing.nodeKey().equals(node.nodeKey())) {
                return;
            }
        }
        nodes.add(node);
    }

    private void addString(List<String> values, String value) {
        String text = value == null ? "" : value.trim();
        if (text.length() > 0 && !values.contains(text)) {
            values.add(text);
        }
    }

    private List<String> command(String commandLine) {
        List<String> parts = new ArrayList<String>();
        String text = commandLine == null ? "" : commandLine.trim();
        if (text.length() == 0) {
            return parts;
        }
        String[] split = text.split("\\s+");
        for (String part : split) {
            if (part.length() > 0) {
                parts.add(part);
            }
        }
        return parts;
    }

    private void drain(InputStream input, ByteArrayOutputStream output, byte[] buffer) throws Exception {
        while (input.available() > 0 && output.size() < MAX_OUTPUT_BYTES) {
            int read = input.read(buffer, 0, Math.min(buffer.length, MAX_OUTPUT_BYTES - output.size()));
            if (read < 0) {
                return;
            }
            output.write(buffer, 0, read);
        }
    }

    private String firstLine(String text) {
        String value = text == null ? "" : text.trim();
        int newline = value.indexOf('\n');
        return newline >= 0 ? value.substring(0, newline).trim() : value;
    }

    private String value(String[] parts, int index) {
        return index < parts.length && parts[index] != null ? parts[index].trim() : "";
    }

    private int number(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static final class NormalizedCgcImpact {
        private final List<GraphNode> nodes = new ArrayList<GraphNode>();
        private final List<GraphNode> startNodes = new ArrayList<GraphNode>();
        private final List<GraphNode> relatedSql = new ArrayList<GraphNode>();
        private final List<GraphNode> riskNodes = new ArrayList<GraphNode>();
        private final List<GraphEdge> edges = new ArrayList<GraphEdge>();
        private final List<GraphEdge> callers = new ArrayList<GraphEdge>();
        private final List<GraphEdge> callees = new ArrayList<GraphEdge>();
        private final List<String> relatedFiles = new ArrayList<String>();
        private final List<String> relatedTests = new ArrayList<String>();
        private final List<String> missingRelatedTests = new ArrayList<String>();
    }
}
