package com.devharnesskit.dhk.service.graph;

import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.graph.GraphArchitectureCheckResult;
import com.devharnesskit.dhk.model.graph.GraphArchitectureConfig;
import com.devharnesskit.dhk.model.graph.GraphArchitectureViolation;
import com.devharnesskit.dhk.model.graph.GraphData;
import com.devharnesskit.dhk.model.graph.GraphEdge;
import com.devharnesskit.dhk.model.graph.GraphNode;
import com.devharnesskit.dhk.model.graph.GraphSnapshot;
import com.devharnesskit.dhk.repository.graph.GraphRepository;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GraphArchitectureCheckService {
    private static final Pattern STRING_VALUE = Pattern.compile("\"%s\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern STRING_IN_ARRAY = Pattern.compile("\"([^\"]*)\"");

    private final ProjectService projectService;
    private final GraphRepository graphRepository;

    public GraphArchitectureCheckService() {
        this(new ProjectService(), new GraphRepository());
    }

    GraphArchitectureCheckService(ProjectService projectService, GraphRepository graphRepository) {
        this.projectService = projectService;
        this.graphRepository = graphRepository;
    }

    public GraphArchitectureCheckResult check(Connection connection, Path projectRoot) throws Exception {
        GraphArchitectureConfig config = load(projectRoot);
        GraphData data = loadGraphData(connection, projectRoot);
        if (data == null) {
            String command = "dhk graph index --project-root " + projectRoot.toAbsolutePath().normalize()
                    + " && dhk graph export --project-root " + projectRoot.toAbsolutePath().normalize();
            String output = "architecture_config: " + PathUtil.graphArchitectureConfig(projectRoot) + "\n"
                    + "next_command: " + command + "\n"
                    + "graph_snapshot: missing\n";
            return new GraphArchitectureCheckResult("failed",
                    "architecture check failed: no completed graph snapshot", output,
                    new ArrayList<GraphArchitectureViolation>(), new ArrayList<String>());
        }

        List<GraphArchitectureViolation> violations = violations(config, data);
        List<String> publicApiImpactFiles = publicApiImpactFiles(projectRoot, config);
        String decision = violations.isEmpty() ? "passed" : (config.failMode() ? "failed" : "warning");
        String status = "failed".equals(decision) ? "failed" : "passed";
        String summary = "architecture " + decision
                + "; violations=" + violations.size()
                + " public_api_impact=" + (!publicApiImpactFiles.isEmpty())
                + " mode=" + config.mode();
        return new GraphArchitectureCheckResult(status, summary,
                output(projectRoot, config, data, violations, publicApiImpactFiles),
                violations, publicApiImpactFiles);
    }

    private GraphData loadGraphData(Connection connection, Path projectRoot) throws Exception {
        if (!Files.isRegularFile(PathUtil.projectJson(projectRoot))) {
            return null;
        }
        Project project = projectService.readProject(PathUtil.projectJson(projectRoot));
        GraphSnapshot snapshot = graphRepository.latestCompletedSnapshot(connection, project.projectKey());
        if (snapshot == null) {
            return null;
        }
        return graphRepository.loadGraphData(connection, snapshot);
    }

    private GraphArchitectureConfig load(Path projectRoot) {
        Path path = PathUtil.graphArchitectureConfig(projectRoot);
        GraphArchitectureConfig defaults = GraphArchitectureConfig.defaults();
        if (!Files.isRegularFile(path)) {
            return defaults;
        }
        try {
            String text = new String(Files.readAllBytes(path), "UTF-8");
            return new GraphArchitectureConfig(
                    mode(stringValue(text, "mode", defaults.mode())),
                    stringArray(text, "controller_patterns", defaults.controllerPatterns()),
                    stringArray(text, "service_patterns", defaults.servicePatterns()),
                    stringArray(text, "repository_patterns", defaults.repositoryPatterns()),
                    stringArray(text, "domain_patterns", defaults.domainPatterns()),
                    stringArray(text, "dto_patterns", defaults.dtoPatterns()),
                    stringArray(text, "public_api_patterns", defaults.publicApiPatterns()),
                    stringArray(text, "forbidden_dependencies", defaults.forbiddenDependencies()));
        } catch (Exception ex) {
            return defaults;
        }
    }

    private String mode(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        return "fail".equals(value) ? "fail" : "warn";
    }

    private List<GraphArchitectureViolation> violations(GraphArchitectureConfig config, GraphData data) {
        List<GraphArchitectureViolation> violations = new ArrayList<GraphArchitectureViolation>();
        Map<String, GraphNode> nodesByKey = nodesByKey(data.nodes());
        Map<String, GraphNode> typesByQualifiedName = typesByQualifiedName(data.nodes());
        Set<String> forbidden = normalizedSet(config.forbiddenDependencies());
        Set<String> seen = new LinkedHashSet<String>();

        for (GraphEdge edge : data.edges()) {
            if (!"imports".equals(edge.edgeKind())) {
                continue;
            }
            GraphNode source = nodesByKey.get(edge.sourceNodeKey());
            GraphNode target = nodesByKey.get(edge.targetNodeKey());
            if (source == null || target == null) {
                continue;
            }
            String sourceLayer = layerForPath(config, source.relativePath());
            GraphNode targetType = typesByQualifiedName.get(target.qualifiedName());
            String targetPath = targetType == null ? "" : targetType.relativePath();
            String targetLayer = targetType == null
                    ? layerForQualifiedName(target.qualifiedName())
                    : layerForPath(config, targetType.relativePath());
            String dependency = sourceLayer + "->" + targetLayer;
            if (sourceLayer.length() == 0 || targetLayer.length() == 0 || !forbidden.contains(dependency)) {
                continue;
            }
            String identity = dependency + "|" + source.qualifiedName() + "|" + target.qualifiedName();
            if (!seen.add(identity)) {
                continue;
            }
            violations.add(new GraphArchitectureViolation(sourceLayer, targetLayer,
                    displayName(source), displayName(target), source.relativePath(),
                    targetPath.length() == 0 ? target.relativePath() : targetPath,
                    edge.edgeKind(), "forbidden dependency " + dependency));
        }
        return violations;
    }

    private Map<String, GraphNode> nodesByKey(List<GraphNode> nodes) {
        Map<String, GraphNode> result = new LinkedHashMap<String, GraphNode>();
        for (GraphNode node : nodes) {
            result.put(node.nodeKey(), node);
        }
        return result;
    }

    private Map<String, GraphNode> typesByQualifiedName(List<GraphNode> nodes) {
        Map<String, GraphNode> result = new LinkedHashMap<String, GraphNode>();
        for (GraphNode node : nodes) {
            if (isType(node) && node.qualifiedName().length() > 0) {
                result.put(node.qualifiedName(), node);
            }
        }
        return result;
    }

    private boolean isType(GraphNode node) {
        return "class".equals(node.nodeKind())
                || "interface".equals(node.nodeKind())
                || "enum".equals(node.nodeKind());
    }

    private String layerForPath(GraphArchitectureConfig config, String path) {
        if (matchesAny(path, config.controllerPatterns())) {
            return "controller";
        }
        if (matchesAny(path, config.servicePatterns())) {
            return "service";
        }
        if (matchesAny(path, config.repositoryPatterns())) {
            return "repository";
        }
        if (matchesAny(path, config.domainPatterns())) {
            return "domain";
        }
        if (matchesAny(path, config.dtoPatterns())) {
            return "dto";
        }
        return "";
    }

    private String layerForQualifiedName(String qualifiedName) {
        String value = qualifiedName == null ? "" : qualifiedName.toLowerCase(Locale.ROOT);
        if (value.indexOf(".controller.") >= 0) {
            return "controller";
        }
        if (value.indexOf(".service.") >= 0) {
            return "service";
        }
        if (value.indexOf(".repository.") >= 0) {
            return "repository";
        }
        if (value.indexOf(".domain.") >= 0) {
            return "domain";
        }
        if (value.indexOf(".dto.") >= 0) {
            return "dto";
        }
        return "";
    }

    private List<String> publicApiImpactFiles(Path projectRoot, GraphArchitectureConfig config) throws Exception {
        Set<String> files = new LinkedHashSet<String>();
        Path impactMap = PathUtil.graphImpactMap(projectRoot);
        if (!Files.isRegularFile(impactMap)) {
            return new ArrayList<String>(files);
        }
        String[] lines = new String(Files.readAllBytes(impactMap), "UTF-8").split("\\r?\\n");
        for (String line : lines) {
            String file = relatedFile(line);
            if (file.length() > 0 && matchesAny(file, config.publicApiPatterns())) {
                files.add(file);
            }
            if (line.trim().startsWith("- route ")) {
                String routeFile = bracketFile(line);
                if (routeFile.length() > 0) {
                    files.add(routeFile);
                }
            }
        }
        return new ArrayList<String>(files);
    }

    private String relatedFile(String line) {
        String trimmed = line == null ? "" : line.trim();
        if (!trimmed.startsWith("- ")) {
            return "";
        }
        String value = trimmed.substring(2).trim();
        int bracket = value.indexOf(" [");
        if (bracket >= 0) {
            value = value.substring(0, bracket).trim();
        }
        if (value.startsWith("src/") || value.indexOf('/') >= 0) {
            return value;
        }
        return "";
    }

    private String bracketFile(String line) {
        Matcher matcher = Pattern.compile("\\[file=([^,\\]]+)").matcher(line == null ? "" : line);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private String output(Path projectRoot, GraphArchitectureConfig config, GraphData data,
                          List<GraphArchitectureViolation> violations,
                          List<String> publicApiImpactFiles) {
        StringBuilder builder = new StringBuilder();
        builder.append("architecture_config: ").append(PathUtil.graphArchitectureConfig(projectRoot)).append('\n');
        builder.append("mode: ").append(config.mode()).append('\n');
        builder.append("snapshot_key: ").append(data.snapshot().snapshotKey()).append('\n');
        builder.append("forbidden_dependencies: ").append(join(config.forbiddenDependencies())).append('\n');
        builder.append("violations: ").append(violations.size()).append('\n');
        for (GraphArchitectureViolation violation : violations) {
            builder.append("- ").append(violation.toLogLine()).append('\n');
        }
        builder.append("public_api_impact: ").append(!publicApiImpactFiles.isEmpty()).append('\n');
        builder.append("public_api_impact_files: ").append(publicApiImpactFiles).append('\n');
        builder.append("alpha_limits: import/path heuristic; fully-qualified references without imports may be missed\n");
        return builder.toString();
    }

    private boolean matchesAny(String value, String[] globs) {
        for (String glob : globs == null ? new String[0] : globs) {
            if (globMatches(normalizePath(value), normalizePath(glob))) {
                return true;
            }
        }
        return false;
    }

    private boolean globMatches(String value, String glob) {
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < glob.length(); i++) {
            char ch = glob.charAt(i);
            if (ch == '*') {
                if (i + 1 < glob.length() && glob.charAt(i + 1) == '*') {
                    regex.append(".*");
                    i++;
                } else {
                    regex.append("[^/]*");
                }
            } else {
                regex.append(Pattern.quote(String.valueOf(ch)));
            }
        }
        return value.matches(regex.toString());
    }

    private String normalizePath(String value) {
        return value == null ? "" : value.trim().replace('\\', '/');
    }

    private Set<String> normalizedSet(String[] values) {
        Set<String> result = new LinkedHashSet<String>();
        for (String value : values == null ? new String[0] : values) {
            String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
            if (normalized.length() > 0) {
                result.add(normalized);
            }
        }
        return result;
    }

    private String displayName(GraphNode node) {
        return node.qualifiedName().length() > 0 ? node.qualifiedName() : node.name();
    }

    private String join(String[] values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values == null ? new String[0] : values) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private String stringValue(String text, String field, String defaultValue) {
        Matcher matcher = Pattern.compile(String.format(STRING_VALUE.pattern(), field)).matcher(text);
        return matcher.find() ? matcher.group(1) : defaultValue;
    }

    private String[] stringArray(String text, String field, String[] defaultValue) {
        Pattern pattern = Pattern.compile("\"" + field + "\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return defaultValue;
        }
        List<String> values = new ArrayList<String>();
        Matcher itemMatcher = STRING_IN_ARRAY.matcher(matcher.group(1));
        while (itemMatcher.find()) {
            values.add(itemMatcher.group(1));
        }
        return values.isEmpty() ? defaultValue : values.toArray(new String[values.size()]);
    }
}
