package com.devharnesskit.dhk.service;

import com.devharnesskit.dhk.util.JsonUtil;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SensitiveDataGuard {
    private static final NamedPattern[] PATTERNS = new NamedPattern[]{
            pattern("password=", "(?i)\\bpassword\\s*=", "[REDACTED_SECRET]"),
            pattern("passwd=", "(?i)\\bpasswd\\s*=", "[REDACTED_SECRET]"),
            pattern("secret=", "(?i)\\bsecret\\s*=", "[REDACTED_SECRET]"),
            pattern("token=", "(?i)\\btoken\\s*=", "[REDACTED_TOKEN]"),
            pattern("api_key=", "(?i)\\bapi[_-]?key\\s*=", "[REDACTED_API_KEY]"),
            pattern("private_key=", "(?i)\\bprivate[_-]?key\\s*=", "[REDACTED_PRIVATE_KEY]"),
            pattern("accessKey=", "(?i)\\baccesskey\\s*=", "[REDACTED_ACCESS_KEY]"),
            pattern("secretKey=", "(?i)\\bsecretkey\\s*=", "[REDACTED_SECRET_KEY]"),
            pattern("jdbc:mysql://", "(?i)jdbc:mysql://", "[REDACTED_JDBC_URL]"),
            pattern("authorization:", "(?i)\\bauthorization\\s*:", "[REDACTED_AUTHORIZATION]"),
            pattern("bearer credential", "(?i)\\bbearer\\s+[A-Za-z0-9._\\-]{10,}", "[REDACTED_BEARER_TOKEN]"),
            pattern("private key block", "-----BEGIN (RSA |DSA |EC |OPENSSH )?PRIVATE KEY-----", "[REDACTED_PRIVATE_KEY_BLOCK]"),
            pattern("aws access key", "\\bAKIA[A-Z0-9]{8,}\\b", "[REDACTED_AWS_ACCESS_KEY]"),
            pattern("github token", "\\b(ghp|gho|ghu|ghs|ghr)_[A-Za-z0-9_]{20,}\\b", "[REDACTED_GITHUB_TOKEN]"),
            pattern("github pat", "\\bgithub_pat_[A-Za-z0-9_]{20,}\\b", "[REDACTED_GITHUB_PAT]"),
            pattern("jwt", "\\beyJ[A-Za-z0-9_\\-]{10,}\\.[A-Za-z0-9_\\-]{10,}\\.[A-Za-z0-9_\\-]{10,}\\b", "[REDACTED_JWT]"),
            pattern("url credential", "(?i)\\b[a-z][a-z0-9+.-]*://[^\\s/@:]+:[^\\s/@]+@", "[REDACTED_URL_CREDENTIAL]"),
            pattern("aliyun access key", "\\bLTAI[A-Za-z0-9]{12,}\\b", "[REDACTED_ALIYUN_ACCESS_KEY]"),
            pattern("google api key", "\\bAIza[0-9A-Za-z_\\-]{20,}\\b", "[REDACTED_GOOGLE_API_KEY]"),
            pattern("slack token", "\\bxox[baprs]-[A-Za-z0-9\\-]{10,}\\b", "[REDACTED_SLACK_TOKEN]"),
            pattern("cookie:", "(?i)\\bcookie\\s*[:=]", "[REDACTED_COOKIE]"),
            pattern("email", "\\b[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}\\b", "[REDACTED_EMAIL]"),
            pattern("phone", "(?<!\\d)1[3-9]\\d{9}(?!\\d)", "[REDACTED_PHONE]"),
            pattern("identity number", "(?<![0-9A-Za-z])\\d{17}[0-9Xx](?![0-9A-Za-z])", "[REDACTED_IDENTITY_NUMBER]")
    };
    private static final ThreadLocal<SensitiveDataPolicy> CURRENT_POLICY = new ThreadLocal<SensitiveDataPolicy>();

    public static void useProjectPolicy(Path projectRoot) {
        CURRENT_POLICY.set(SensitiveDataPolicy.load(projectRoot));
    }

    public static void clearProjectPolicy() {
        CURRENT_POLICY.remove();
    }

    public boolean containsSensitiveData(String text) {
        return !findMatches(text).isEmpty();
    }

    public List<String> findMatches(String text) {
        if (text == null || text.length() == 0) {
            return Collections.emptyList();
        }
        List<String> matches = new ArrayList<String>();
        SensitiveDataPolicy policy = currentPolicy();
        for (NamedPattern pattern : PATTERNS) {
            if (policy.actionFor(pattern.name) == SensitiveAction.REJECT
                    && pattern.pattern.matcher(text).find()) {
                matches.add(pattern.name);
            }
        }
        return matches;
    }

    public String redact(String text) {
        if (text == null || text.length() == 0) {
            return text;
        }
        String redacted = text;
        SensitiveDataPolicy policy = currentPolicy();
        for (NamedPattern pattern : PATTERNS) {
            if (policy.actionFor(pattern.name) == SensitiveAction.REDACT) {
                redacted = pattern.pattern.matcher(redacted)
                        .replaceAll(Matcher.quoteReplacement(pattern.replacement));
            }
        }
        return redacted;
    }

    public List<String> redactedMatches(String text) {
        if (text == null || text.length() == 0) {
            return Collections.emptyList();
        }
        List<String> matches = new ArrayList<String>();
        SensitiveDataPolicy policy = currentPolicy();
        for (NamedPattern pattern : PATTERNS) {
            if (policy.actionFor(pattern.name) == SensitiveAction.REDACT
                    && pattern.pattern.matcher(text).find()) {
                matches.add(pattern.name);
            }
        }
        return matches;
    }

    private SensitiveDataPolicy currentPolicy() {
        SensitiveDataPolicy policy = CURRENT_POLICY.get();
        return policy == null ? SensitiveDataPolicy.defaultPolicy() : policy;
    }

    private static NamedPattern pattern(String name, String regex, String replacement) {
        return new NamedPattern(name, Pattern.compile(regex), replacement);
    }

    private static final class NamedPattern {
        private final String name;
        private final Pattern pattern;
        private final String replacement;

        private NamedPattern(String name, Pattern pattern, String replacement) {
            this.name = name;
            this.pattern = pattern;
            this.replacement = replacement;
        }
    }

    private enum SensitiveAction {
        REJECT,
        REDACT,
        ALLOW
    }

    private static final class SensitiveDataPolicy {
        private final Map<String, SensitiveAction> actions;

        private SensitiveDataPolicy(Map<String, SensitiveAction> actions) {
            this.actions = actions;
        }

        private static SensitiveDataPolicy defaultPolicy() {
            return new SensitiveDataPolicy(Collections.<String, SensitiveAction>emptyMap());
        }

        private static SensitiveDataPolicy load(Path projectRoot) {
            if (projectRoot == null) {
                return defaultPolicy();
            }
            Path policyPath = PathUtil.sensitivePolicy(projectRoot);
            if (!Files.isRegularFile(policyPath)) {
                return defaultPolicy();
            }
            try {
                Map<String, String> raw = JsonUtil.parseObject(new String(Files.readAllBytes(policyPath), "UTF-8"));
                Map<String, SensitiveAction> actions = new LinkedHashMap<String, SensitiveAction>();
                for (Map.Entry<String, String> entry : raw.entrySet()) {
                    SensitiveAction action = parseAction(entry.getValue());
                    String patternName = normalizeName(entry.getKey());
                    if (action != null && patternName.length() > 0) {
                        actions.put(patternName, action);
                    }
                }
                return new SensitiveDataPolicy(actions);
            } catch (Exception ex) {
                return defaultPolicy();
            }
        }

        private SensitiveAction actionFor(String patternName) {
            SensitiveAction action = actions.get(normalizeName(patternName));
            return action == null ? SensitiveAction.REJECT : action;
        }

        private static SensitiveAction parseAction(String value) {
            String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
            if ("reject".equals(normalized)) {
                return SensitiveAction.REJECT;
            }
            if ("redact".equals(normalized)) {
                return SensitiveAction.REDACT;
            }
            if ("allow".equals(normalized)) {
                return SensitiveAction.ALLOW;
            }
            return null;
        }

        private static String normalizeName(String value) {
            String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
                    .replace('_', ' ')
                    .replace('-', ' ');
            normalized = normalized.replaceAll("\\s+", " ");
            if ("id".equals(normalized) || "id card".equals(normalized)
                    || "identity".equals(normalized) || "identity number".equals(normalized)
                    || "identity card".equals(normalized) || "chinese id".equals(normalized)) {
                return "identity number";
            }
            if ("mobile".equals(normalized) || "mobile phone".equals(normalized)
                    || "phone number".equals(normalized)) {
                return "phone";
            }
            if ("mail".equals(normalized) || "e mail".equals(normalized)) {
                return "email";
            }
            if ("api key".equals(normalized)) {
                return "api_key=";
            }
            if ("private key".equals(normalized)) {
                return "private key block";
            }
            return normalized;
        }
    }
}
