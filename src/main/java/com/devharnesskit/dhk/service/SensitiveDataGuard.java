package com.devharnesskit.dhk.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public final class SensitiveDataGuard {
    private static final NamedPattern[] PATTERNS = new NamedPattern[]{
            pattern("password=", "(?i)\\bpassword\\s*="),
            pattern("passwd=", "(?i)\\bpasswd\\s*="),
            pattern("secret=", "(?i)\\bsecret\\s*="),
            pattern("token=", "(?i)\\btoken\\s*="),
            pattern("accessKey=", "(?i)\\baccesskey\\s*="),
            pattern("secretKey=", "(?i)\\bsecretkey\\s*="),
            pattern("jdbc:mysql://", "(?i)jdbc:mysql://"),
            pattern("authorization:", "(?i)\\bauthorization\\s*:"),
            pattern("bearer credential", "(?i)\\bbearer\\s+[A-Za-z0-9._\\-]{10,}"),
            pattern("aws access key", "\\bAKIA[A-Z0-9]{8,}\\b"),
            pattern("cookie:", "(?i)\\bcookie\\s*[:=]"),
            pattern("email", "\\b[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}\\b"),
            pattern("phone", "(?<!\\d)1[3-9]\\d{9}(?!\\d)"),
            pattern("identity number", "(?<![0-9A-Za-z])\\d{17}[0-9Xx](?![0-9A-Za-z])")
    };

    public boolean containsSensitiveData(String text) {
        return !findMatches(text).isEmpty();
    }

    public List<String> findMatches(String text) {
        if (text == null || text.length() == 0) {
            return Collections.emptyList();
        }
        List<String> matches = new ArrayList<String>();
        for (NamedPattern pattern : PATTERNS) {
            if (pattern.pattern.matcher(text).find()) {
                matches.add(pattern.name);
            }
        }
        return matches;
    }

    private static NamedPattern pattern(String name, String regex) {
        return new NamedPattern(name, Pattern.compile(regex));
    }

    private static final class NamedPattern {
        private final String name;
        private final Pattern pattern;

        private NamedPattern(String name, Pattern pattern) {
            this.name = name;
            this.pattern = pattern;
        }
    }
}
