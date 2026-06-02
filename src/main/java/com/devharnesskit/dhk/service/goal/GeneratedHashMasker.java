package com.devharnesskit.dhk.service.goal;

final class GeneratedHashMasker {
    private GeneratedHashMasker() {
    }

    static String mask(String text) {
        if (text == null || text.length() == 0) {
            return text;
        }
        String masked = text.replaceAll("\\b(sha256|git|fallback|config|context|check|workspace|artifact):[0-9a-fA-F]{32,}\\b",
                "$1:[GENERATED_HASH]");
        return masked.replaceAll("\\b([A-Za-z0-9_.-]*(fingerprint|hash)):\\s*[0-9a-fA-F]{32,}\\b",
                "$1: [GENERATED_HASH]");
    }
}
