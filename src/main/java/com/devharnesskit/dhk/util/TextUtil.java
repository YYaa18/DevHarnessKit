package com.devharnesskit.dhk.util;

import java.util.ArrayList;
import java.util.List;

public final class TextUtil {
    private TextUtil() {
    }

    public static List<String> tokens(String text) {
        List<String> tokens = new ArrayList<String>();
        if (text == null) {
            return tokens;
        }
        String[] parts = text.toLowerCase().split("[\\s,]+");
        for (String part : parts) {
            String token = part.trim();
            if (token.length() > 0) {
                tokens.add(token);
            }
        }
        return tokens;
    }
}
