package com.devharnesskit.dhk.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SensitiveDataGuard {
    private static final String[] PATTERNS = new String[]{
            "password=",
            "passwd=",
            "secret=",
            "token=",
            "accesskey",
            "secretkey",
            "jdbc:mysql://",
            "authorization:",
            "bearer",
            "akia",
            "cookie:"
    };

    public boolean containsSensitiveData(String text) {
        return !findMatches(text).isEmpty();
    }

    public List<String> findMatches(String text) {
        if (text == null || text.length() == 0) {
            return Collections.emptyList();
        }
        String lower = text.toLowerCase();
        List<String> matches = new ArrayList<String>();
        for (String pattern : PATTERNS) {
            if (lower.contains(pattern)) {
                matches.add(pattern);
            }
        }
        return matches;
    }
}
