package com.devharnesskit.dhk.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class TextUtil {
    private TextUtil() {
    }

    public static List<String> tokens(String text) {
        Set<String> tokens = new LinkedHashSet<String>();
        if (text == null) {
            return new ArrayList<String>(tokens);
        }
        String[] parts = text.split("[\\s,]+");
        for (String part : parts) {
            addToken(tokens, part);
            String expandedCamel = part
                    .replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2")
                    .replaceAll("([a-z0-9])([A-Z])", "$1 $2");
            String[] splitParts = expandedCamel.split("[^\\p{L}\\p{N}\\u4E00-\\u9FFF]+");
            for (String split : splitParts) {
                addToken(tokens, split);
            }
        }
        return new ArrayList<String>(tokens);
    }

    private static void addToken(Set<String> tokens, String raw) {
        String token = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        token = token.replaceAll("^[^\\p{L}\\p{N}\\u4E00-\\u9FFF]+", "")
                .replaceAll("[^\\p{L}\\p{N}\\u4E00-\\u9FFF]+$", "");
        if (token.length() > 0) {
            tokens.add(token);
        }
    }
}
