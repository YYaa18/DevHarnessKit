package com.devharnesskit.dhk.service.graph;

import com.devharnesskit.dhk.model.graph.GraphConfig;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GraphConfigService {
    private static final Pattern STRING_VALUE = Pattern.compile("\"%s\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern NUMBER_VALUE = Pattern.compile("\"%s\"\\s*:\\s*(\\d+)");
    private static final Pattern STRING_IN_ARRAY = Pattern.compile("\"([^\"]*)\"");

    public GraphConfig load(Path projectRoot) {
        Path configPath = PathUtil.graphConfig(projectRoot);
        if (!Files.isRegularFile(configPath)) {
            return GraphConfig.defaults();
        }
        try {
            String text = new String(Files.readAllBytes(configPath), "UTF-8");
            GraphConfig defaults = GraphConfig.defaults();
            return new GraphConfig(
                    stringValue(text, "provider", defaults.provider()),
                    stringValue(text, "cgc_command", defaults.cgcCommand()),
                    stringArray(text, "include", defaults.include()),
                    stringArray(text, "exclude", defaults.exclude()),
                    numberValue(text, "max_file_bytes", defaults.maxFileBytes()),
                    numberValue(text, "max_indexed_files", defaults.maxIndexedFiles()),
                    numberValue(text, "max_impact_depth", defaults.maxImpactDepth()),
                    numberValue(text, "max_export_nodes", defaults.maxExportNodes()),
                    true);
        } catch (IOException ex) {
            return GraphConfig.defaults();
        }
    }

    public boolean writeDefaultIfMissing(Path projectRoot) throws IOException {
        PathUtil.createGraphDirectories(projectRoot);
        Path configPath = PathUtil.graphConfig(projectRoot);
        if (Files.isRegularFile(configPath)) {
            return false;
        }
        Files.write(configPath, toJson(GraphConfig.defaults()).getBytes("UTF-8"));
        return true;
    }

    public String toJson(GraphConfig config) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        builder.append("  \"schema_version\": \"devharness-graph-config/v1-alpha\",\n");
        builder.append("  \"provider\": ").append(JsonOutput.quote(config.provider())).append(",\n");
        builder.append("  \"cgc_command\": ").append(JsonOutput.quote(config.cgcCommand())).append(",\n");
        appendArray(builder, "include", config.include(), true);
        appendArray(builder, "exclude", config.exclude(), true);
        builder.append("  \"limits\": {\n");
        builder.append("    \"max_file_bytes\": ").append(config.maxFileBytes()).append(",\n");
        builder.append("    \"max_indexed_files\": ").append(config.maxIndexedFiles()).append(",\n");
        builder.append("    \"max_impact_depth\": ").append(config.maxImpactDepth()).append(",\n");
        builder.append("    \"max_export_nodes\": ").append(config.maxExportNodes()).append('\n');
        builder.append("  },\n");
        builder.append("  \"safety\": {\n");
        builder.append("    \"skip_protected_file_content\": true,\n");
        builder.append("    \"skip_sensitive_file_content\": true,\n");
        builder.append("    \"record_skipped_metadata\": true\n");
        builder.append("  }\n");
        builder.append("}\n");
        return builder.toString();
    }

    private void appendArray(StringBuilder builder, String name, List<String> values, boolean comma) {
        builder.append("  \"").append(name).append("\": [\n");
        for (int i = 0; i < values.size(); i++) {
            builder.append("    ").append(JsonOutput.quote(values.get(i)));
            if (i + 1 < values.size()) {
                builder.append(',');
            }
            builder.append('\n');
        }
        builder.append("  ]");
        if (comma) {
            builder.append(',');
        }
        builder.append('\n');
    }

    private String stringValue(String text, String field, String defaultValue) {
        Matcher matcher = Pattern.compile(String.format(STRING_VALUE.pattern(), field)).matcher(text);
        return matcher.find() ? matcher.group(1) : defaultValue;
    }

    private int numberValue(String text, String field, int defaultValue) {
        Matcher matcher = Pattern.compile(String.format(NUMBER_VALUE.pattern(), field)).matcher(text);
        if (!matcher.find()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private List<String> stringArray(String text, String field, List<String> defaultValue) {
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
        return values.isEmpty() ? defaultValue : values;
    }
}
