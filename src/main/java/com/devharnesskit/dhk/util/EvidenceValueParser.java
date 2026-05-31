package com.devharnesskit.dhk.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EvidenceValueParser {
    private static final String FIELD_PREFIX = "--field";

    private EvidenceValueParser() {
    }

    public static String value(String evidence, String key) {
        if (evidence == null || key == null || key.trim().length() == 0) {
            return "";
        }
        Match best = latestPlainValue(evidence, key.trim(), Match.none());
        best = latestPrefixedFieldValue(evidence, key.trim(), best);
        return best.value;
    }

    private static Match latestPlainValue(String evidence, String key, Match best) {
        Pattern pattern = Pattern.compile("(?i)(?:^|[;\\n\\r])\\s*"
                + Pattern.quote(key) + "\\s*=\\s*([^;\\n\\r]*)");
        Matcher matcher = pattern.matcher(evidence);
        Match latest = best;
        while (matcher.find()) {
            latest = latest.select(matcher.start(), cleanEvidenceValue(matcher.group(1)));
        }
        return latest;
    }

    private static Match latestPrefixedFieldValue(String evidence, String key, Match best) {
        Match latest = best;
        int cursor = 0;
        while (cursor < evidence.length()) {
            int blockStart = nextFieldBlock(evidence, cursor);
            if (blockStart < 0) {
                return latest;
            }
            int next = blockStart;
            while (next >= 0 && next < evidence.length()) {
                FieldToken token = readFieldToken(evidence, next);
                if (token == null) {
                    break;
                }
                if (key.equalsIgnoreCase(token.key)) {
                    latest = latest.select(next, cleanEvidenceValue(token.value));
                }
                next = skipHorizontalWhitespace(evidence, token.end);
                if (!startsWithFieldPrefix(evidence, next)) {
                    break;
                }
            }
            cursor = Math.max(next + 1, blockStart + FIELD_PREFIX.length());
        }
        return latest;
    }

    private static int nextFieldBlock(String text, int from) {
        int cursor = Math.max(0, from);
        while (cursor < text.length()) {
            int index = text.indexOf(FIELD_PREFIX, cursor);
            if (index < 0) {
                return -1;
            }
            if (isFieldBlockStart(text, index)) {
                return index;
            }
            cursor = index + FIELD_PREFIX.length();
        }
        return -1;
    }

    private static boolean isFieldBlockStart(String text, int index) {
        int previous = index - 1;
        while (previous >= 0) {
            char ch = text.charAt(previous);
            if (ch == ' ' || ch == '\t') {
                previous--;
                continue;
            }
            return ch == ';' || ch == '\n' || ch == '\r';
        }
        return true;
    }

    private static FieldToken readFieldToken(String text, int start) {
        if (!startsWithFieldPrefix(text, start)) {
            return null;
        }
        int cursor = start + FIELD_PREFIX.length();
        if (cursor < text.length() && isNameChar(text.charAt(cursor))) {
            return null;
        }
        cursor = skipHorizontalWhitespace(text, cursor);
        int keyStart = cursor;
        while (cursor < text.length() && isNameChar(text.charAt(cursor))) {
            cursor++;
        }
        if (cursor == keyStart) {
            return null;
        }
        String key = text.substring(keyStart, cursor);
        cursor = skipHorizontalWhitespace(text, cursor);
        if (cursor >= text.length() || text.charAt(cursor) != '=') {
            return null;
        }
        cursor++;
        cursor = skipHorizontalWhitespace(text, cursor);
        ValueToken value = readValue(text, cursor);
        return new FieldToken(key, value.value, value.end);
    }

    private static boolean startsWithFieldPrefix(String text, int start) {
        return start >= 0
                && start + FIELD_PREFIX.length() <= text.length()
                && text.regionMatches(true, start, FIELD_PREFIX, 0, FIELD_PREFIX.length());
    }

    private static ValueToken readValue(String text, int start) {
        if (start >= text.length()) {
            return new ValueToken("", start);
        }
        char first = text.charAt(start);
        if (first == '"' || first == '\'') {
            int cursor = start + 1;
            StringBuilder builder = new StringBuilder();
            boolean escaped = false;
            while (cursor < text.length()) {
                char ch = text.charAt(cursor);
                if (escaped) {
                    builder.append(ch);
                    escaped = false;
                } else if (ch == '\\') {
                    escaped = true;
                } else if (ch == first) {
                    return new ValueToken(builder.toString(), cursor + 1);
                } else {
                    builder.append(ch);
                }
                cursor++;
            }
            return new ValueToken(builder.toString(), cursor);
        }
        int cursor = start;
        while (cursor < text.length()) {
            char ch = text.charAt(cursor);
            if (ch == ' ' || ch == '\t' || ch == ';' || ch == '\n' || ch == '\r') {
                break;
            }
            cursor++;
        }
        return new ValueToken(text.substring(start, cursor), cursor);
    }

    private static int skipHorizontalWhitespace(String text, int cursor) {
        int next = cursor;
        while (next < text.length()) {
            char ch = text.charAt(next);
            if (ch != ' ' && ch != '\t') {
                break;
            }
            next++;
        }
        return next;
    }

    private static boolean isNameChar(char ch) {
        return ch >= 'A' && ch <= 'Z'
                || ch >= 'a' && ch <= 'z'
                || ch >= '0' && ch <= '9'
                || ch == '_' || ch == '.' || ch == '-';
    }

    private static String cleanEvidenceValue(String value) {
        String text = value == null ? "" : value.trim();
        if (text.length() >= 2
                && (text.charAt(0) == '"' && text.charAt(text.length() - 1) == '"'
                || text.charAt(0) == '\'' && text.charAt(text.length() - 1) == '\'')) {
            return text.substring(1, text.length() - 1).trim();
        }
        return text;
    }

    private static final class Match {
        private final int start;
        private final String value;

        private Match(int start, String value) {
            this.start = start;
            this.value = value;
        }

        private static Match none() {
            return new Match(-1, "");
        }

        private static Match of(int start, String value) {
            return new Match(start, value == null ? "" : value);
        }

        private Match select(int candidateStart, String candidateValue) {
            if (candidateStart >= start) {
                return Match.of(candidateStart, candidateValue);
            }
            return this;
        }
    }

    private static final class FieldToken {
        private final String key;
        private final String value;
        private final int end;

        private FieldToken(String key, String value, int end) {
            this.key = key;
            this.value = value;
            this.end = end;
        }
    }

    private static final class ValueToken {
        private final String value;
        private final int end;

        private ValueToken(String value, int end) {
            this.value = value;
            this.end = end;
        }
    }
}
