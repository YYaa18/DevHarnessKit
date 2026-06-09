package com.devharnesskit.dhk.context.artifact;

import com.devharnesskit.dhk.context.compress.CompressResult;
import com.devharnesskit.dhk.service.SensitiveDataGuard;
import com.devharnesskit.dhk.util.Clock;
import com.devharnesskit.dhk.util.PathUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.util.List;

public final class ContextArtifactService {
    private static final int INLINE_ORIGINAL_BYTES = 64 * 1024;

    private final ContextArtifactRepository repository;
    private final SensitiveDataGuard sensitiveDataGuard;

    public ContextArtifactService() {
        this(new ContextArtifactRepository(), new SensitiveDataGuard());
    }

    ContextArtifactService(ContextArtifactRepository repository, SensitiveDataGuard sensitiveDataGuard) {
        this.repository = repository;
        this.sensitiveDataGuard = sensitiveDataGuard;
    }

    public ContextArtifact persist(Path projectRoot, Connection connection, String projectKey, String goalKey,
                                   String artifactKey, String sourceType, String sourcePath,
                                   String originalText, CompressResult result, Clock clock)
            throws Exception {
        String original = safe(originalText);
        String compressed = result == null ? "" : safe(result.compressedText());
        rejectIfSensitive(original, compressed);
        String key = artifactKey == null || artifactKey.trim().length() == 0
                ? generatedKey(goalKey, sourceType, original) : normalize(artifactKey.trim());
        String sha256 = sha256(original);
        String storedOriginal = original;
        String storedPath = sourcePath == null ? "" : sourcePath;
        if (byteLength(original) > INLINE_ORIGINAL_BYTES) {
            PathUtil.createContextDirectories(projectRoot);
            Path artifactPath = PathUtil.contextArtifact(projectRoot, key);
            Files.write(artifactPath, original.getBytes("UTF-8"));
            storedOriginal = "";
            storedPath = PathUtil.displayPath(artifactPath);
        }
        ContextArtifact artifact = new ContextArtifact(0L, projectKey, goalKey, key,
                result == null || result.sourceType().length() == 0 ? sourceType : result.sourceType(),
                storedPath, sha256, storedOriginal, compressed,
                result == null ? "[]" : result.retainedSpansJson(),
                result == null ? 0 : result.omittedLines(),
                result == null ? 0 : result.tokenBefore(),
                result == null ? 0 : result.tokenAfter(),
                clock.now().toString());
        repository.upsert(connection, artifact);
        ContextArtifact stored = repository.findByKey(connection, projectKey, key);
        return stored == null ? artifact : stored;
    }

    public String originalContent(ContextArtifact artifact) throws IOException {
        if (artifact == null) {
            return "";
        }
        if (artifact.originalText().length() > 0) {
            return artifact.originalText();
        }
        if (artifact.sourcePath().length() > 0) {
            Path path = java.nio.file.Paths.get(artifact.sourcePath());
            if (Files.isRegularFile(path)) {
                return new String(Files.readAllBytes(path), "UTF-8");
            }
        }
        return artifact.compressedText();
    }

    public String range(String text, int startLine, int endLine) {
        if (startLine <= 0 && endLine <= 0) {
            return text == null ? "" : text;
        }
        String[] lines = (text == null ? "" : text).split("\\r?\\n", -1);
        int start = startLine <= 0 ? 1 : startLine;
        int end = endLine <= 0 ? lines.length : Math.min(endLine, lines.length);
        if (start > end || start > lines.length) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = start; i <= end; i++) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(lines[i - 1]);
        }
        return builder.toString();
    }

    private String generatedKey(String goalKey, String sourceType, String original) throws Exception {
        String prefix = sourceType == null || sourceType.length() == 0 ? "context" : sourceType;
        String goal = goalKey == null || goalKey.length() == 0 ? "global" : goalKey;
        return normalize(prefix) + "-" + normalize(goal) + "-" + sha256(original).substring(0, 16);
    }

    private String normalize(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9_.-]+", "-");
        if (normalized.length() == 0) {
            return "context";
        }
        return normalized.length() > 48 ? normalized.substring(0, 48) : normalized;
    }

    private String safe(String text) {
        return sensitiveDataGuard.redact(text == null ? "" : text);
    }

    private void rejectIfSensitive(String... values) {
        java.util.ArrayList<String> matches = new java.util.ArrayList<String>();
        for (String value : values) {
            List<String> found = sensitiveDataGuard.findMatches(value);
            for (String match : found) {
                if (!matches.contains(match)) {
                    matches.add(match);
                }
            }
        }
        if (!matches.isEmpty()) {
            throw new IllegalStateException("Sensitive data rejected while storing context artifact: " + matches);
        }
    }

    private int byteLength(String text) throws Exception {
        return text == null ? 0 : text.getBytes("UTF-8").length;
    }

    private String sha256(String text) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = digest.digest((text == null ? "" : text).getBytes("UTF-8"));
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(b & 0xff);
            if (hex.length() == 1) {
                builder.append('0');
            }
            builder.append(hex);
        }
        return builder.toString();
    }
}
