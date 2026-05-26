package com.devharnesskit.dhk.service.graph;

import com.devharnesskit.dhk.model.graph.GraphEdge;
import com.devharnesskit.dhk.model.graph.GraphFileEntry;
import com.devharnesskit.dhk.model.graph.GraphNode;
import com.devharnesskit.dhk.model.graph.GraphParseError;
import com.devharnesskit.dhk.model.graph.GraphParseResult;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class MyBatisXmlLiteParser implements GraphSourceParser {
    private static final Pattern MAPPER = Pattern.compile("<mapper\\b[^>]*namespace\\s*=\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern RESULT_MAP = Pattern.compile("<resultMap\\b([^>]*)>", Pattern.CASE_INSENSITIVE);
    private static final Pattern STATEMENT = Pattern.compile("<(select|insert|update|delete)\\b([^>]*)>(.*?)</\\1>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern ATTR = Pattern.compile("([A-Za-z_][\\w.-]*)\\s*=\\s*\"([^\"]*)\"");
    private static final Pattern TABLE_FROM = Pattern.compile("(?i)\\b(?:from|join|update)\\s+([A-Za-z_][\\w.]*)");
    private static final Pattern TABLE_INSERT = Pattern.compile("(?i)\\binsert\\s+into\\s+([A-Za-z_][\\w.]*)");
    private static final Pattern COLUMN_ATTR = Pattern.compile("column\\s*=\\s*\"([A-Za-z_][\\w.]*)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern QUALIFIED_COLUMN = Pattern.compile("\\b[A-Za-z_][\\w]*\\.([A-Za-z_][\\w]*)\\b");

    public boolean supports(GraphFileEntry entry) {
        return "xml".equals(entry.language());
    }

    public void parse(Path projectRoot, GraphFileEntry entry, GraphParseResult.Builder builder) throws Exception {
        Path file = projectRoot.resolve(entry.relativePath());
        String content = new String(Files.readAllBytes(file), "UTF-8");
        validateXml(content, entry, builder);

        Matcher mapperMatcher = MAPPER.matcher(content);
        if (!mapperMatcher.find()) {
            return;
        }
        String namespace = mapperMatcher.group(1);
        String mapperKey = mapperKey(namespace);
        int mapperLine = lineOf(content, mapperMatcher.start());
        builder.addNode(new GraphNode(mapperKey, "xml_mapper", simpleName(namespace), namespace,
                entry.relativePath(), mapperLine, mapperLine, "xml", "", "", 85, "lite",
                "mybatis mapper namespace"));
        addMapperJavaReference(builder, entry, mapperKey, namespace, mapperLine);
        addResultMaps(builder, entry, content, mapperKey);
        addStatements(builder, entry, content, namespace, mapperKey);
    }

    private void validateXml(String content, GraphFileEntry entry, GraphParseResult.Builder builder) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            disableFeature(factory, "http://apache.org/xml/features/nonvalidating/load-external-dtd");
            disableFeature(factory, "http://xml.org/sax/features/external-general-entities");
            disableFeature(factory, "http://xml.org/sax/features/external-parameter-entities");
            factory.setExpandEntityReferences(false);
            DocumentBuilder documentBuilder = factory.newDocumentBuilder();
            documentBuilder.setEntityResolver(new org.xml.sax.EntityResolver() {
                public InputSource resolveEntity(String publicId, String systemId) {
                    return new InputSource(new StringReader(""));
                }
            });
            documentBuilder.setErrorHandler(new ErrorHandler() {
                public void warning(SAXParseException exception) throws SAXException {
                    throw exception;
                }

                public void error(SAXParseException exception) throws SAXException {
                    throw exception;
                }

                public void fatalError(SAXParseException exception) throws SAXException {
                    throw exception;
                }
            });
            documentBuilder.parse(new InputSource(new StringReader(content)));
        } catch (Exception ex) {
            builder.addError(new GraphParseError(entry.relativePath(), "MyBatisXmlLiteParser",
                    safeMessage(ex.getMessage())));
        }
    }

    private void disableFeature(DocumentBuilderFactory factory, String feature) {
        try {
            factory.setFeature(feature, false);
        } catch (Exception ignored) {
            // Some JAXP providers do not expose every hardening feature.
        }
    }

    private void addMapperJavaReference(GraphParseResult.Builder builder, GraphFileEntry entry,
                                        String mapperKey, String namespace, int lineNumber) {
        String javaTypeKey = "java_type:" + namespace;
        builder.addNode(new GraphNode(javaTypeKey, "type_reference", simpleName(namespace), namespace,
                entry.relativePath(), lineNumber, lineNumber, "xml", "", "", 65, "lite",
                "mybatis namespace maps to Java mapper"));
        builder.addEdge(new GraphEdge("maps_to", mapperKey, javaTypeKey, entry.relativePath(), 70,
                "lite", "mapper namespace maps to Java type"));
    }

    private void addResultMaps(GraphParseResult.Builder builder, GraphFileEntry entry, String content,
                               String mapperKey) {
        Matcher resultMatcher = RESULT_MAP.matcher(content);
        while (resultMatcher.find()) {
            String id = attr(resultMatcher.group(1), "id");
            if (id.length() == 0) {
                continue;
            }
            int lineNumber = lineOf(content, resultMatcher.start());
            String key = mapperKey + ":resultMap:" + id;
            builder.addNode(new GraphNode(key, "xml_result_map", id, id, entry.relativePath(),
                    lineNumber, lineNumber, "xml", "", "", 75, "lite", "mybatis resultMap"));
            builder.addEdge(new GraphEdge("contains", mapperKey, key, entry.relativePath(), 75,
                    "lite", "mapper contains resultMap"));
        }
    }

    private void addStatements(GraphParseResult.Builder builder, GraphFileEntry entry, String content,
                               String namespace, String mapperKey) {
        Matcher statementMatcher = STATEMENT.matcher(content);
        while (statementMatcher.find()) {
            String sqlType = statementMatcher.group(1).toLowerCase(Locale.ROOT);
            String attrs = statementMatcher.group(2);
            String body = statementMatcher.group(3);
            String id = attr(attrs, "id");
            if (id.length() == 0) {
                continue;
            }
            int lineNumber = lineOf(content, statementMatcher.start());
            String qualifiedName = namespace + "." + id;
            String statementKey = "sql_statement:" + qualifiedName;
            builder.addNode(new GraphNode(statementKey, "sql_statement", id, qualifiedName,
                    entry.relativePath(), lineNumber, lineNumber, "xml", "", sqlType, 80, "lite",
                    "mybatis " + sqlType + " statement id"));
            builder.addEdge(new GraphEdge("contains", mapperKey, statementKey, entry.relativePath(), 80,
                    "lite", "mapper contains SQL statement"));
            String methodKey = "java_method:" + namespace + "#" + id;
            builder.addNode(new GraphNode(methodKey, "method", id, namespace + "#" + id, entry.relativePath(),
                    lineNumber, lineNumber, "xml", "", "", 65, "lite", "statement id maps to mapper method"));
            builder.addEdge(new GraphEdge("maps_to", statementKey, methodKey, entry.relativePath(), 70,
                    "lite", "statement id maps to mapper method"));
            addTableEdges(builder, entry, statementKey, sqlType, body, lineNumber);
            addColumnEdges(builder, entry, statementKey, body, lineNumber);
        }
    }

    private void addTableEdges(GraphParseResult.Builder builder, GraphFileEntry entry, String statementKey,
                               String sqlType, String body, int lineNumber) {
        Set<String> tables = new LinkedHashSet<String>();
        collect(TABLE_FROM, body, tables);
        collect(TABLE_INSERT, body, tables);
        for (String table : tables) {
            String tableKey = "db_table:" + table;
            builder.addNode(new GraphNode(tableKey, "db_table", table, table, entry.relativePath(),
                    lineNumber, lineNumber, "sql", "", "", 70, "lite", "SQL table heuristic"));
            builder.addEdge(new GraphEdge(tableEdge(sqlType), statementKey, tableKey, entry.relativePath(),
                    70, "lite", "SQL table heuristic"));
        }
    }

    private void addColumnEdges(GraphParseResult.Builder builder, GraphFileEntry entry, String statementKey,
                                String body, int lineNumber) {
        Set<String> columns = new LinkedHashSet<String>();
        collect(COLUMN_ATTR, body, columns);
        collect(QUALIFIED_COLUMN, body, columns);
        for (String column : columns) {
            String columnKey = "db_column:" + column;
            builder.addNode(new GraphNode(columnKey, "db_column", column, column, entry.relativePath(),
                    lineNumber, lineNumber, "sql", "", "", 60, "lite", "SQL column heuristic"));
            builder.addEdge(new GraphEdge("references", statementKey, columnKey, entry.relativePath(),
                    60, "lite", "SQL column heuristic"));
        }
    }

    private void collect(Pattern pattern, String text, Set<String> output) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String value = matcher.group(1).trim();
            if (value.length() > 0 && !value.startsWith("#{") && !value.startsWith("${")) {
                output.add(value);
            }
        }
    }

    private String attr(String attrs, String name) {
        Matcher matcher = ATTR.matcher(attrs == null ? "" : attrs);
        while (matcher.find()) {
            if (name.equalsIgnoreCase(matcher.group(1))) {
                return matcher.group(2);
            }
        }
        return "";
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

    private String mapperKey(String namespace) {
        return "xml_mapper:" + namespace;
    }

    private String simpleName(String qualified) {
        int dot = qualified.lastIndexOf('.');
        return dot >= 0 ? qualified.substring(dot + 1) : qualified;
    }

    private String safeMessage(String message) {
        return message == null ? "XML parse failed" : message.replace('\n', ' ').replace('\r', ' ');
    }
}
