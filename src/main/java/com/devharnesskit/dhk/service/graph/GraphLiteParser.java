package com.devharnesskit.dhk.service.graph;

import com.devharnesskit.dhk.model.graph.GraphFileEntry;
import com.devharnesskit.dhk.model.graph.GraphParseError;
import com.devharnesskit.dhk.model.graph.GraphParseResult;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public final class GraphLiteParser {
    private final List<GraphSourceParser> parsers;

    public GraphLiteParser() {
        this(Arrays.<GraphSourceParser>asList(
                new JavaLiteParser(),
                new JspLiteParser(),
                new MyBatisXmlLiteParser(),
                new SqlLiteParser(),
                new PropertiesLiteParser()));
    }

    GraphLiteParser(List<GraphSourceParser> parsers) {
        this.parsers = parsers;
    }

    public GraphParseResult parse(Path projectRoot, List<GraphFileEntry> entries) {
        GraphParseResult.Builder builder = new GraphParseResult.Builder();
        for (GraphFileEntry entry : entries) {
            if (!entry.indexed()) {
                continue;
            }
            GraphSourceParser parser = parserFor(entry);
            if (parser == null) {
                continue;
            }
            try {
                parser.parse(projectRoot, entry, builder);
            } catch (Exception ex) {
                builder.addError(new GraphParseError(entry.relativePath(), parser.getClass().getSimpleName(),
                        ex.getClass().getSimpleName() + ": " + ex.getMessage()));
            }
        }
        return builder.build();
    }

    private GraphSourceParser parserFor(GraphFileEntry entry) {
        for (GraphSourceParser parser : parsers) {
            if (parser.supports(entry)) {
                return parser;
            }
        }
        return null;
    }
}
