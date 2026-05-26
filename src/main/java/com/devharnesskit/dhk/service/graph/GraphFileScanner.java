package com.devharnesskit.dhk.service.graph;

import com.devharnesskit.dhk.model.graph.GraphConfig;
import com.devharnesskit.dhk.model.graph.GraphFileEntry;
import com.devharnesskit.dhk.model.graph.GraphScanReport;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class GraphFileScanner {
    public GraphScanReport scan(final Path projectRoot, final GraphConfig config) {
        return scan(projectRoot, config, new String[0]);
    }

    public GraphScanReport scan(final Path projectRoot, final GraphConfig config, final String[] protectedGlobs) {
        final List<GraphFileEntry> entries = new ArrayList<GraphFileEntry>();
        final int[] indexed = new int[]{0};
        try {
            Files.walkFileTree(projectRoot, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    String relative = relative(projectRoot, dir);
                    if (relative.length() > 0 && isExcludedDirectory(relative, config)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    consider(projectRoot, file, attrs, config, protectedGlobs, entries, indexed);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
            entries.add(new GraphFileEntry("<scan-error>", "unknown", "error", "",
                    0L, false, "scan_error:" + ex.getMessage()));
        }
        return new GraphScanReport(projectRoot, config, entries);
    }

    private void consider(Path projectRoot, Path file, BasicFileAttributes attrs, GraphConfig config,
                          String[] protectedGlobs, List<GraphFileEntry> entries, int[] indexed) {
        String relative = relative(projectRoot, file);
        boolean included = matchesAny(relative, config.include());
        boolean sensitive = isSensitivePath(relative);
        boolean protectedFile = matchesAny(relative, protectedGlobs == null ? new String[0] : protectedGlobs);
        boolean excluded = matchesAny(relative, config.exclude()) || isBinaryPath(relative);
        if (!included && !sensitive && !protectedFile) {
            return;
        }
        String language = language(relative);
        String fileKind = fileKind(relative);
        if (protectedFile) {
            entries.add(skipped(relative, language, fileKind, attrs.size(), "protected_file"));
            return;
        }
        if (sensitive) {
            entries.add(skipped(relative, language, fileKind, attrs.size(), "sensitive_filename"));
            return;
        }
        if (excluded) {
            entries.add(skipped(relative, language, fileKind, attrs.size(), "excluded"));
            return;
        }
        if (attrs.size() > config.maxFileBytes()) {
            entries.add(skipped(relative, language, fileKind, attrs.size(), "max_file_bytes"));
            return;
        }
        if (indexed[0] >= config.maxIndexedFiles()) {
            entries.add(skipped(relative, language, fileKind, attrs.size(), "max_indexed_files"));
            return;
        }
        entries.add(new GraphFileEntry(relative, language, fileKind, hash(file), attrs.size(), true, ""));
        indexed[0]++;
    }

    private GraphFileEntry skipped(String relative, String language, String fileKind, long size, String reason) {
        return new GraphFileEntry(relative, language, fileKind, "", size, false, reason);
    }

    private boolean isExcludedDirectory(String relative, GraphConfig config) {
        return matchesAny(relative + "/", config.exclude())
                || relative.equals("target")
                || relative.startsWith("target/")
                || relative.equals("build")
                || relative.startsWith("build/")
                || relative.equals("node_modules")
                || relative.startsWith("node_modules/")
                || relative.equals(".git")
                || relative.startsWith(".git/")
                || relative.equals(".agents/memory")
                || relative.startsWith(".agents/memory/")
                || relative.equals(".agents/graph/exports")
                || relative.startsWith(".agents/graph/exports/")
                || relative.equals(".agents/graph/snapshots")
                || relative.startsWith(".agents/graph/snapshots/")
                || relative.equals(".agents/graph/cache")
                || relative.startsWith(".agents/graph/cache/");
    }

    private boolean matchesAny(String relative, List<String> patterns) {
        for (String pattern : patterns) {
            if (matches(relative, pattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesAny(String relative, String[] patterns) {
        for (String pattern : patterns) {
            if (matches(relative, pattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean matches(String relative, String pattern) {
        return Pattern.compile(globToRegex(pattern)).matcher(relative).matches();
    }

    private String globToRegex(String pattern) {
        StringBuilder regex = new StringBuilder();
        regex.append('^');
        for (int i = 0; i < pattern.length(); i++) {
            char ch = pattern.charAt(i);
            if (ch == '*') {
                if (i + 1 < pattern.length() && pattern.charAt(i + 1) == '*') {
                    regex.append(".*");
                    i++;
                } else {
                    regex.append("[^/]*");
                }
            } else if (ch == '?') {
                regex.append("[^/]");
            } else {
                if ("\\.[]{}()+-^$|".indexOf(ch) >= 0) {
                    regex.append('\\');
                }
                regex.append(ch);
            }
        }
        regex.append('$');
        return regex.toString();
    }

    private String relative(Path root, Path path) {
        if (root.equals(path)) {
            return "";
        }
        return root.relativize(path).toString().replace('\\', '/');
    }

    private boolean isSensitivePath(String relative) {
        String lower = relative.toLowerCase();
        return lower.contains("secret")
                || lower.contains("password")
                || lower.endsWith(".pem")
                || lower.endsWith(".key");
    }

    private boolean isBinaryPath(String relative) {
        String lower = relative.toLowerCase();
        return lower.endsWith(".jar") || lower.endsWith(".class");
    }

    private String language(String relative) {
        String lower = relative.toLowerCase();
        if (lower.endsWith(".java")) {
            return "java";
        }
        if (lower.endsWith(".xml") || "pom.xml".equals(lower)) {
            return "xml";
        }
        if (lower.endsWith(".jsp")) {
            return "jsp";
        }
        if (lower.endsWith(".properties")) {
            return "properties";
        }
        if (lower.endsWith(".sql")) {
            return "sql";
        }
        return "unknown";
    }

    private String fileKind(String relative) {
        if ("pom.xml".equals(relative)) {
            return "build";
        }
        if (relative.startsWith("src/test/")) {
            return "test";
        }
        if (relative.startsWith("src/main/webapp/")) {
            return "web";
        }
        if (relative.startsWith("src/main/resources/")) {
            return "resource";
        }
        if (relative.startsWith("src/main/java/")) {
            return "source";
        }
        return "other";
    }

    private String hash(Path file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = Files.readAllBytes(file);
            byte[] raw = digest.digest(bytes);
            StringBuilder builder = new StringBuilder("sha256:");
            for (byte value : raw) {
                String hex = Integer.toHexString(value & 0xff);
                if (hex.length() == 1) {
                    builder.append('0');
                }
                builder.append(hex);
            }
            return builder.toString();
        } catch (Exception ex) {
            return "";
        }
    }
}
