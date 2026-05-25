package com.devharnesskit.dhk.util;

import com.devharnesskit.dhk.cli.Args;

import java.util.List;

public final class JsonOutput {
    private JsonOutput() {
    }

    public static boolean enabled(Args args) {
        return args.hasFlag("json") || "json".equalsIgnoreCase(args.option("format", ""));
    }

    public static Field stringField(String name, String value) {
        return new Field(name, quote(value == null ? "" : value));
    }

    public static Field numberField(String name, long value) {
        return new Field(name, String.valueOf(value));
    }

    public static Field booleanField(String name, boolean value) {
        return new Field(name, value ? "true" : "false");
    }

    public static Field rawField(String name, String rawJson) {
        return new Field(name, rawJson == null || rawJson.length() == 0 ? "null" : rawJson);
    }

    public static String object(Field... fields) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        for (int i = 0; i < fields.length; i++) {
            builder.append("  ").append(quote(fields[i].name)).append(": ").append(fields[i].rawValue);
            if (i + 1 < fields.length) {
                builder.append(',');
            }
            builder.append('\n');
        }
        builder.append("}\n");
        return builder.toString();
    }

    public static String array(List<String> rawJsonValues) {
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        for (int i = 0; i < rawJsonValues.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(rawJsonValues.get(i));
        }
        builder.append(']');
        return builder.toString();
    }

    public static String stringArray(String[] values) {
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(quote(values[i]));
        }
        builder.append(']');
        return builder.toString();
    }

    public static String quote(String value) {
        StringBuilder builder = new StringBuilder();
        builder.append('"');
        String text = value == null ? "" : value;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            switch (ch) {
                case '\\':
                    builder.append("\\\\");
                    break;
                case '"':
                    builder.append("\\\"");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    if (ch < 0x20) {
                        builder.append("\\u");
                        String hex = Integer.toHexString(ch);
                        for (int j = hex.length(); j < 4; j++) {
                            builder.append('0');
                        }
                        builder.append(hex);
                    } else {
                        builder.append(ch);
                    }
                    break;
            }
        }
        builder.append('"');
        return builder.toString();
    }

    public static final class Field {
        private final String name;
        private final String rawValue;

        private Field(String name, String rawValue) {
            this.name = name;
            this.rawValue = rawValue;
        }
    }
}
