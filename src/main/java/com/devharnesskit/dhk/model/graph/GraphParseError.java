package com.devharnesskit.dhk.model.graph;

public final class GraphParseError {
    private final String relativePath;
    private final String parser;
    private final String message;

    public GraphParseError(String relativePath, String parser, String message) {
        this.relativePath = value(relativePath);
        this.parser = value(parser);
        this.message = value(message);
    }

    public String relativePath() {
        return relativePath;
    }

    public String parser() {
        return parser;
    }

    public String message() {
        return message;
    }

    private static String value(String input) {
        return input == null ? "" : input;
    }
}
