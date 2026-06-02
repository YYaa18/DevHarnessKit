package com.devharnesskit.dhk.service.brief;

import com.devharnesskit.dhk.model.brief.InteractionRequest;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PreWorkGuardService {
    public void ensureBaseline(Path projectRoot, InteractionRequest request) throws Exception {
        if (!isPreWorkBlocking(request)) {
            return;
        }
        Map<String, GuardRecord> records = load(projectRoot);
        if (records.containsKey(request.requestId())) {
            return;
        }
        records.put(request.requestId(), new GuardRecord(request.requestId(),
                businessFingerprint(projectRoot), "", false));
        save(projectRoot, records);
    }

    public void markAnswered(Path projectRoot, String requestId) throws Exception {
        if (requestId == null || requestId.trim().length() == 0) {
            return;
        }
        Map<String, GuardRecord> records = load(projectRoot);
        String current = businessFingerprint(projectRoot);
        GuardRecord existing = records.get(requestId);
        if (existing == null) {
            records.put(requestId, new GuardRecord(requestId, current, current, false));
        } else {
            String baseline = existing.baselineFingerprint().length() == 0
                    ? current : existing.baselineFingerprint();
            records.put(requestId, new GuardRecord(requestId, baseline, current,
                    existing.violation() || !baseline.equals(current)));
        }
        save(projectRoot, records);
    }

    public List<Finding> violations(Path projectRoot, List<InteractionRequest> requests, String goalKey)
            throws Exception {
        Map<String, GuardRecord> records = load(projectRoot);
        List<Finding> findings = new ArrayList<Finding>();
        boolean hasGoalScoped = hasGoalScopedInteraction(requests, goalKey);
        String current = "";
        for (InteractionRequest request : requests) {
            if (!appliesToGoal(request, goalKey, hasGoalScoped)) {
                continue;
            }
            GuardRecord record = records.get(request.requestId());
            if (record == null || record.baselineFingerprint().length() == 0) {
                continue;
            }
            boolean violation = false;
            String observed = record.answerFingerprint();
            if ("answered".equals(request.status())) {
                violation = record.violation();
            } else if (request.openBlockingFor(goalKey)) {
                if (current.length() == 0) {
                    current = businessFingerprint(projectRoot);
                }
                observed = current;
                violation = !record.baselineFingerprint().equals(current);
            }
            if (violation) {
                findings.add(new Finding(request.requestId(), request.status(),
                        record.baselineFingerprint(), observed));
            }
        }
        return findings;
    }

    private boolean appliesToGoal(InteractionRequest request, String goalKey, boolean hasGoalScoped) {
        if (!isPreWorkBlocking(request)) {
            return false;
        }
        if (request.goalKey().length() > 0) {
            return request.goalKey().equals(goalKey);
        }
        // Unscoped orphan (e.g. from an earlier advise preview) must not pollute a goal
        // that already has its own scoped interaction.
        return !hasGoalScoped;
    }

    private boolean hasGoalScopedInteraction(List<InteractionRequest> requests, String goalKey) {
        if (goalKey == null || goalKey.length() == 0) {
            return false;
        }
        for (InteractionRequest request : requests) {
            if (goalKey.equals(request.goalKey())) {
                return true;
            }
        }
        return false;
    }

    private boolean isPreWorkBlocking(InteractionRequest request) {
        return request != null && request.blocksProgress() && "pre_work".equals(request.phase());
    }

    private Map<String, GuardRecord> load(Path projectRoot) throws Exception {
        Map<String, GuardRecord> records = new LinkedHashMap<String, GuardRecord>();
        Path store = PathUtil.preWorkGuardStore(projectRoot);
        if (!Files.isRegularFile(store)) {
            return records;
        }
        List<String> lines = Files.readAllLines(store, StandardCharsets.UTF_8);
        for (String line : lines) {
            if (line.trim().length() == 0 || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\\t", -1);
            if (parts.length < 4 || parts[0].trim().length() == 0) {
                continue;
            }
            records.put(parts[0], new GuardRecord(parts[0], parts[1], parts[2],
                    Boolean.parseBoolean(parts[3])));
        }
        return records;
    }

    private void save(Path projectRoot, Map<String, GuardRecord> records) throws Exception {
        Files.createDirectories(PathUtil.devharnessBriefsDirectory(projectRoot));
        StringBuilder builder = new StringBuilder();
        for (GuardRecord record : records.values()) {
            builder.append(record.requestId()).append('\t')
                    .append(record.baselineFingerprint()).append('\t')
                    .append(record.answerFingerprint()).append('\t')
                    .append(record.violation()).append('\n');
        }
        Files.write(PathUtil.preWorkGuardStore(projectRoot), builder.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String businessFingerprint(Path projectRoot) {
        if (projectRoot == null) {
            return "";
        }
        final Path root = projectRoot.toAbsolutePath().normalize();
        final List<Path> files = new ArrayList<Path>();
        try {
            if (!Files.exists(root)) {
                return "business:" + sha256("missing");
            }
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    return shouldSkip(root, dir) ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
                }

                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (attrs.isRegularFile() && !shouldSkip(root, file)) {
                        files.add(file.toAbsolutePath().normalize());
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception ex) {
            return "business:" + sha256("unreadable:" + ex.getClass().getName());
        }
        Collections.sort(files, new Comparator<Path>() {
            public int compare(Path left, Path right) {
                return root.relativize(left).toString().compareTo(root.relativize(right).toString());
            }
        });
        StringBuilder builder = new StringBuilder();
        for (Path file : files) {
            try {
                builder.append(normalizedRelative(root, file)).append('\n')
                        .append(Files.size(file)).append('\n')
                        .append(sha256(Files.readAllBytes(file))).append('\n');
            } catch (Exception ex) {
                builder.append(normalizedRelative(root, file)).append(":unreadable\n");
            }
        }
        return "business:" + sha256(builder.toString());
    }

    private boolean shouldSkip(Path root, Path path) {
        Path relative;
        try {
            relative = root.relativize(path.toAbsolutePath().normalize());
        } catch (Exception ex) {
            return true;
        }
        if (relative.getNameCount() == 0) {
            return false;
        }
        String first = relative.getName(0).toString();
        return ".git".equals(first)
                || ".agents".equals(first)
                || ".comate".equals(first)
                || "target".equals(first)
                || "build".equals(first)
                || ".gradle".equals(first)
                || "node_modules".equals(first);
    }

    private String normalizedRelative(Path root, Path file) {
        return root.relativize(file).toString().replace('\\', '/');
    }

    private String sha256(String text) {
        return sha256(text.getBytes(StandardCharsets.UTF_8));
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder builder = new StringBuilder();
            for (byte b : hash) {
                builder.append(String.format("%02x", b & 0xff));
            }
            return builder.toString();
        } catch (Exception ex) {
            return "";
        }
    }

    public static final class Finding {
        private final String requestId;
        private final String status;
        private final String baselineFingerprint;
        private final String observedFingerprint;

        private Finding(String requestId, String status, String baselineFingerprint, String observedFingerprint) {
            this.requestId = requestId == null ? "" : requestId;
            this.status = status == null ? "" : status;
            this.baselineFingerprint = baselineFingerprint == null ? "" : baselineFingerprint;
            this.observedFingerprint = observedFingerprint == null ? "" : observedFingerprint;
        }

        public String requestId() { return requestId; }
        public String status() { return status; }
        public String baselineFingerprint() { return baselineFingerprint; }
        public String observedFingerprint() { return observedFingerprint; }
    }

    private static final class GuardRecord {
        private final String requestId;
        private final String baselineFingerprint;
        private final String answerFingerprint;
        private final boolean violation;

        private GuardRecord(String requestId, String baselineFingerprint,
                            String answerFingerprint, boolean violation) {
            this.requestId = requestId == null ? "" : requestId;
            this.baselineFingerprint = baselineFingerprint == null ? "" : baselineFingerprint;
            this.answerFingerprint = answerFingerprint == null ? "" : answerFingerprint;
            this.violation = violation;
        }

        private String requestId() { return requestId; }
        private String baselineFingerprint() { return baselineFingerprint; }
        private String answerFingerprint() { return answerFingerprint; }
        private boolean violation() { return violation; }
    }
}
