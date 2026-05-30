package com.devharnesskit.dhk.service.graph;

import com.devharnesskit.dhk.model.graph.GraphEdge;
import com.devharnesskit.dhk.model.graph.GraphFileEntry;
import com.devharnesskit.dhk.model.graph.GraphNode;
import com.devharnesskit.dhk.model.graph.GraphParseResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SqlLiteParser implements GraphSourceParser {
    private static final Pattern TABLE_FROM = Pattern.compile("(?i)\\b(?:from|join|update)\\s+([A-Za-z_][\\w.]*)");
    private static final Pattern TABLE_INSERT = Pattern.compile("(?i)\\binsert\\s+into\\s+([A-Za-z_][\\w.]*)");
    private static final Pattern QUALIFIED_COLUMN = Pattern.compile("\\b[A-Za-z_][\\w]*\\.([A-Za-z_][\\w]*)\\b");
    private static final Pattern NAMED_PARAMETER = Pattern.compile("(?<!:)\\:([A-Za-z_][\\w]*)");

    public boolean supports(GraphFileEntry entry) {
        return "sql".equals(entry.language());
    }

    public void parse(Path projectRoot, GraphFileEntry entry, GraphParseResult.Builder builder) throws Exception {
        Path file = projectRoot.resolve(entry.relativePath());
        String content = new String(Files.readAllBytes(file), "UTF-8");
        String resourceKey = "sql_resource:" + entry.relativePath();
        builder.addNode(new GraphNode(resourceKey, "sql_resource", fileName(entry.relativePath()),
                entry.relativePath(), entry.relativePath(), 1, 1, "sql", "", "", 75, "lite",
                "SQL resource file"));

        int index = 0;
        for (String raw : content.split(";")) {
            String statement = raw.trim();
            if (statement.length() == 0) {
                continue;
            }
            index++;
            String statementType = statementType(statement);
            String statementKey = "sql_statement:" + entry.relativePath() + "#statement" + index;
            int line = lineOf(content, content.indexOf(raw));
            builder.addNode(new GraphNode(statementKey, "sql_statement", "statement" + index,
                    entry.relativePath() + "#statement" + index, entry.relativePath(), line, line,
                    "sql", "", statementType, 70, "lite", "SQL resource statement"));
            builder.addEdge(new GraphEdge("contains", resourceKey, statementKey, entry.relativePath(),
                    75, "lite", "SQL resource contains statement"));
            addTableEdges(builder, entry, statementKey, statementType, statement, line);
            addColumnEdges(builder, entry, statementKey, statement, line);
            addParameterEdges(builder, entry, statementKey, statement, line);
        }
    }

    private void addTableEdges(GraphParseResult.Builder builder, GraphFileEntry entry, String statementKey,
                               String sqlType, String statement, int line) {
        Set<String> tables = new LinkedHashSet<String>();
        collect(TABLE_FROM, statement, tables);
        collect(TABLE_INSERT, statement, tables);
        for (String table : tables) {
            String tableKey = "db_table:" + table;
            builder.addNode(new GraphNode(tableKey, "db_table", table, table, entry.relativePath(),
                    line, line, "sql", "", "", 65, "lite", "SQL resource table heuristic"));
            builder.addEdge(new GraphEdge(tableEdge(sqlType), statementKey, tableKey, entry.relativePath(),
                    65, "lite", "SQL resource table heuristic"));
        }
    }

    private void addColumnEdges(GraphParseResult.Builder builder, GraphFileEntry entry, String statementKey,
                                String statement, int line) {
        Set<String> columns = new LinkedHashSet<String>();
        collect(QUALIFIED_COLUMN, statement, columns);
        for (String column : columns) {
            String columnKey = "db_column:" + column;
            builder.addNode(new GraphNode(columnKey, "db_column", column, column, entry.relativePath(),
                    line, line, "sql", "", "", 55, "lite", "SQL resource column heuristic"));
            builder.addEdge(new GraphEdge("references", statementKey, columnKey, entry.relativePath(),
                    55, "lite", "SQL resource column heuristic"));
        }
    }

    private void addParameterEdges(GraphParseResult.Builder builder, GraphFileEntry entry, String statementKey,
                                   String statement, int line) {
        Set<String> parameters = new LinkedHashSet<String>();
        collect(NAMED_PARAMETER, statement, parameters);
        for (String parameter : parameters) {
            String parameterKey = "sql_parameter:" + entry.relativePath() + ":" + parameter;
            builder.addNode(new GraphNode(parameterKey, "sql_parameter", parameter,
                    entry.relativePath() + "#" + parameter, entry.relativePath(), line, line,
                    "sql", "", "", 60, "lite", "SQL resource named parameter"));
            builder.addEdge(new GraphEdge("references", statementKey, parameterKey, entry.relativePath(),
                    60, "lite", "SQL resource named parameter"));
        }
    }

    private void collect(Pattern pattern, String text, Set<String> output) {
        Matcher matcher = pattern.matcher(text == null ? "" : text);
        while (matcher.find()) {
            String value = matcher.group(matcher.groupCount()).trim();
            if (value.length() > 0) {
                output.add(value);
            }
        }
    }

    private String statementType(String statement) {
        String lower = statement == null ? "" : statement.trim().toLowerCase(Locale.ROOT);
        if (lower.startsWith("select")) {
            return "select";
        }
        if (lower.startsWith("insert")) {
            return "insert";
        }
        if (lower.startsWith("update")) {
            return "update";
        }
        if (lower.startsWith("delete")) {
            return "delete";
        }
        return "sql";
    }

    private String tableEdge(String sqlType) {
        if ("select".equals(sqlType)) {
            return "reads";
        }
        if ("insert".equals(sqlType) || "update".equals(sqlType) || "delete".equals(sqlType)) {
            return "writes";
        }
        return "references";
    }

    private int lineOf(String content, int offset) {
        int line = 1;
        int max = Math.min(content.length(), Math.max(0, offset));
        for (int index = 0; index < max; index++) {
            if (content.charAt(index) == '\n') {
                line++;
            }
        }
        return line;
    }

    private String fileName(String path) {
        int slash = path == null ? -1 : path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }
}
