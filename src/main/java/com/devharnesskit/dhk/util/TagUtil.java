package com.devharnesskit.dhk.util;

import java.util.LinkedHashSet;
import java.util.Set;

public final class TagUtil {
    private TagUtil() {
    }

    public static String normalize(String rawTags) {
        if (rawTags == null || rawTags.trim().length() == 0) {
            return "";
        }
        String[] parts = rawTags.split(",");
        Set<String> tags = new LinkedHashSet<String>();
        for (String part : parts) {
            String tag = part.trim().toLowerCase();
            if (tag.length() > 0) {
                tags.add(tag);
            }
        }
        StringBuilder builder = new StringBuilder();
        int index = 0;
        for (String tag : tags) {
            if (index++ > 0) {
                builder.append(',');
            }
            builder.append(tag);
        }
        return builder.toString();
    }
}
