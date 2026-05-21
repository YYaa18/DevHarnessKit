package com.devharnesskit.dhk.util;

import java.util.LinkedHashMap;
import java.util.Map;

public final class JsonUtil {
    private JsonUtil() {
    }

    public static String toObject(Map<String, String> fields) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        int index = 0;
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            builder.append("  \"")
                    .append(escape(entry.getKey()))
                    .append("\": \"")
                    .append(escape(entry.getValue()))
                    .append("\"");
            if (++index < fields.size()) {
                builder.append(',');
            }
            builder.append('\n');
        }
        builder.append("}\n");
        return builder.toString();
    }

    public static Map<String, String> parseObject(String json) {
        Parser parser = new Parser(json);
        return parser.parseObject();
    }

    private static String escape(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
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
                    builder.append(ch);
                    break;
            }
        }
        return builder.toString();
    }

    private static final class Parser {
        private final String json;
        private int index;

        private Parser(String json) {
            this.json = json == null ? "" : json;
        }

        private Map<String, String> parseObject() {
            Map<String, String> result = new LinkedHashMap<String, String>();
            skipWhitespace();
            expect('{');
            skipWhitespace();
            if (peek('}')) {
                index++;
                skipWhitespace();
                ensureEnd();
                return result;
            }

            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                skipWhitespace();
                String value = parseString();
                result.put(key, value);
                skipWhitespace();
                if (peek(',')) {
                    index++;
                    continue;
                }
                expect('}');
                skipWhitespace();
                ensureEnd();
                return result;
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder builder = new StringBuilder();
            while (index < json.length()) {
                char ch = json.charAt(index++);
                if (ch == '"') {
                    return builder.toString();
                }
                if (ch == '\\') {
                    if (index >= json.length()) {
                        throw new IllegalArgumentException("Invalid JSON string escape");
                    }
                    char escaped = json.charAt(index++);
                    switch (escaped) {
                        case '\\':
                            builder.append('\\');
                            break;
                        case '"':
                            builder.append('"');
                            break;
                        case 'n':
                            builder.append('\n');
                            break;
                        case 'r':
                            builder.append('\r');
                            break;
                        case 't':
                            builder.append('\t');
                            break;
                        default:
                            throw new IllegalArgumentException("Unsupported JSON escape: \\" + escaped);
                    }
                } else {
                    builder.append(ch);
                }
            }
            throw new IllegalArgumentException("Unterminated JSON string");
        }

        private boolean peek(char expected) {
            return index < json.length() && json.charAt(index) == expected;
        }

        private void expect(char expected) {
            if (index >= json.length() || json.charAt(index) != expected) {
                throw new IllegalArgumentException("Expected '" + expected + "' at character " + index);
            }
            index++;
        }

        private void skipWhitespace() {
            while (index < json.length()) {
                char ch = json.charAt(index);
                if (ch == ' ' || ch == '\n' || ch == '\r' || ch == '\t') {
                    index++;
                } else {
                    return;
                }
            }
        }

        private void ensureEnd() {
            if (index != json.length()) {
                throw new IllegalArgumentException("Unexpected JSON content at character " + index);
            }
        }
    }
}
