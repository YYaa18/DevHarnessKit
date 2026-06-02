package com.devharnesskit.dhk.service;

import java.security.MessageDigest;
import java.util.Locale;

public final class MemoryIdentity {
    private MemoryIdentity() {
    }

    public static String fingerprint(String title, String content) {
        return sha256(normalizeText(title) + "\n" + normalizeText(content));
    }

    public static String canonicalKey(String module, String title) {
        String normalizedModule = normalizeToken(module);
        if (normalizedModule.length() == 0) {
            normalizedModule = "global";
        }
        String normalizedTitle = normalizeText(title);
        if (normalizedTitle.length() == 0) {
            normalizedTitle = "untitled";
        }
        return normalizedModule + ":" + normalizedTitle;
    }

    public static String normalizeText(String value) {
        String raw = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        raw = raw.replace('_', ' ').replace('-', ' ');
        raw = raw.replaceAll("[\\s\\p{Punct}]+", " ");
        return raw.trim();
    }

    private static String normalizeToken(String value) {
        String raw = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        raw = raw.replaceAll("[^a-z0-9_.-]+", "-");
        raw = raw.replaceAll("-+", "-");
        if (raw.startsWith("-")) {
            raw = raw.substring(1);
        }
        if (raw.endsWith("-")) {
            raw = raw.substring(0, raw.length() - 1);
        }
        return raw;
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes("UTF-8"));
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(hash[i] & 0xff);
                if (hex.length() == 1) {
                    builder.append('0');
                }
                builder.append(hex);
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to compute memory fingerprint", ex);
        }
    }
}
