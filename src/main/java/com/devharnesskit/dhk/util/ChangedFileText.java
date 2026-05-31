package com.devharnesskit.dhk.util;

import java.nio.file.Path;
import java.util.Set;

public final class ChangedFileText {
    private ChangedFileText() {
    }

    public static void addFiles(Set<String> files, String raw) {
        addFiles(files, null, raw);
    }

    public static void addFiles(Set<String> files, Path projectRoot, String raw) {
        if (files == null || !isMeaningful(raw)) {
            return;
        }
        String[] parts = raw.split("[,;\\n\\r]+");
        for (String part : parts) {
            String normalized = normalizeRelativePath(projectRoot, part);
            if (isMeaningful(normalized)) {
                files.add(normalized);
            }
        }
    }

    public static String normalizeRelativePath(Path projectRoot, String value) {
        String text = normalizePathText(value);
        if (text.length() == 0 || projectRoot == null) {
            return text;
        }
        try {
            Path path = projectRoot.getFileSystem().getPath(text);
            if (path.isAbsolute()) {
                text = projectRoot.toAbsolutePath().normalize()
                        .relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/');
            }
        } catch (Exception ignored) {
            return text;
        }
        while (text.startsWith("./")) {
            text = text.substring(2);
        }
        return text;
    }

    public static String normalizePathText(String value) {
        String text = value == null ? "" : value.trim().replace('\\', '/');
        while (text.startsWith("./")) {
            text = text.substring(2);
        }
        return text;
    }

    public static boolean isMeaningful(String value) {
        String text = normalizePathText(value);
        if (text.length() == 0) {
            return false;
        }
        String lower = text.toLowerCase(java.util.Locale.ROOT);
        return !"none".equals(lower)
                && !"unavailable".equals(lower)
                && !"unknown".equals(lower)
                && !"n/a".equals(lower)
                && !"na".equals(lower)
                && !"not_available".equals(lower);
    }
}
