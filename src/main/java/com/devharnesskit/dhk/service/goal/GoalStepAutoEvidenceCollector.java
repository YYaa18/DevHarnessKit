package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.service.policy.DevHarnessPolicyService;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

public final class GoalStepAutoEvidenceCollector {
    private final DevHarnessPolicyService policyService;

    public GoalStepAutoEvidenceCollector() {
        this(new DevHarnessPolicyService());
    }

    GoalStepAutoEvidenceCollector(DevHarnessPolicyService policyService) {
        this.policyService = policyService;
    }

    public String changedFiles(Path projectRoot) {
        return normalizeCsv(gitOutput(projectRoot, new String[]{"git", "diff", "--name-only", "HEAD", "--"}));
    }

    public String evidence(Path projectRoot, String changedFiles) {
        StringBuilder builder = new StringBuilder();
        appendField(builder, "auto_evidence", "objective_git_diff_facts");
        appendField(builder, "diff_stat", gitOutput(projectRoot, new String[]{"git", "diff", "--stat", "HEAD", "--"}));
        appendField(builder, "touched_modules", touchedModules(changedFiles));
        appendField(builder, "protected_file_hits", protectedFileHits(projectRoot, changedFiles));
        appendField(builder, "risk_flags", riskFlags(changedFiles));
        return builder.toString();
    }

    private String touchedModules(String changedFiles) {
        Set<String> modules = new LinkedHashSet<String>();
        for (String file : splitFiles(changedFiles)) {
            int slash = file.indexOf('/');
            modules.add(slash > 0 ? file.substring(0, slash) : "root");
        }
        return modules.isEmpty() ? "none" : join(modules);
    }

    private String protectedFileHits(Path projectRoot, String changedFiles) {
        Set<String> hits = new LinkedHashSet<String>();
        String[] protectedPatterns = policyService.load(projectRoot).protectedFiles();
        for (String file : splitFiles(changedFiles)) {
            String lower = file.toLowerCase(java.util.Locale.ROOT);
            if (lower.contains(".env") || lower.contains("secret") || lower.contains("application-prod")
                    || lower.contains("schema") || lower.contains("migration")) {
                hits.add(file);
                continue;
            }
            for (String pattern : protectedPatterns) {
                if (matchesPattern(file, pattern)) {
                    hits.add(file);
                    break;
                }
            }
        }
        return hits.isEmpty() ? "none" : join(hits);
    }

    private String riskFlags(String changedFiles) {
        Set<String> flags = new LinkedHashSet<String>();
        for (String file : splitFiles(changedFiles)) {
            String lower = file.toLowerCase(java.util.Locale.ROOT);
            if (lower.contains("auth") || lower.contains("security") || lower.contains("permission")) {
                flags.add("security_or_permission");
            }
            if (lower.contains("payment") || lower.contains("settlement") || lower.contains("amount")) {
                flags.add("payment_or_finance");
            }
            if (lower.contains("schema") || lower.contains("migration") || lower.endsWith(".sql")) {
                flags.add("schema_or_sql");
            }
            if (lower.contains("application") || lower.endsWith(".properties") || lower.endsWith(".yml")
                    || lower.endsWith(".yaml") || lower.endsWith("pom.xml") || lower.endsWith("build.gradle")) {
                flags.add("configuration");
            }
        }
        return flags.isEmpty() ? "none" : join(flags);
    }

    private boolean matchesPattern(String file, String pattern) {
        String cleaned = pattern == null ? "" : pattern.trim();
        if (cleaned.length() == 0) {
            return false;
        }
        if (cleaned.endsWith("/**")) {
            return file.startsWith(cleaned.substring(0, cleaned.length() - 3));
        }
        if (cleaned.indexOf('*') >= 0) {
            return file.contains(cleaned.replace("*", ""));
        }
        return file.equals(cleaned) || file.startsWith(cleaned + "/");
    }

    private String[] splitFiles(String changedFiles) {
        String text = changedFiles == null ? "" : changedFiles.trim();
        if (text.length() == 0 || "none".equals(text)) {
            return new String[0];
        }
        String[] raw = text.split("[,;\\n]");
        java.util.List<String> cleaned = new java.util.ArrayList<String>();
        for (String item : raw) {
            String file = item.trim();
            if (file.length() > 0 && !"none".equals(file)) {
                cleaned.add(file);
            }
        }
        return cleaned.toArray(new String[cleaned.size()]);
    }

    private String normalizeCsv(String text) {
        StringBuilder builder = new StringBuilder();
        String[] lines = text == null ? new String[0] : text.split("[;\\r\\n]+");
        for (String line : lines) {
            String value = line.trim();
            if (value.length() == 0) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(value);
        }
        return builder.length() == 0 ? "none" : builder.toString();
    }

    private String gitOutput(Path projectRoot, String[] command) {
        try {
            Process process = new ProcessBuilder(command)
                    .directory(projectRoot.toFile())
                    .redirectErrorStream(true)
                    .start();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            InputStream stream = process.getInputStream();
            byte[] buffer = new byte[4096];
            long deadline = System.currentTimeMillis() + 5000L;
            while (process.isAlive() && System.currentTimeMillis() < deadline) {
                drain(stream, output, buffer);
                Thread.sleep(10L);
            }
            if (process.isAlive()) {
                process.destroy();
                process.destroyForcibly();
                return "unavailable";
            }
            drain(stream, output, buffer);
            if (process.waitFor() != 0) {
                return "unavailable";
            }
            String text = output.toString("UTF-8").trim().replace('\r', ' ').replace('\n', ';');
            return text.length() == 0 ? "none" : text;
        } catch (Exception ex) {
            return "unavailable";
        }
    }

    private void drain(InputStream stream, ByteArrayOutputStream output, byte[] buffer) throws Exception {
        while (stream.available() > 0 && output.size() < 8192) {
            int read = stream.read(buffer, 0, Math.min(buffer.length, 8192 - output.size()));
            if (read < 0) {
                return;
            }
            output.write(buffer, 0, read);
        }
    }

    private void appendField(StringBuilder builder, String key, String value) {
        String text = value == null ? "" : value.trim();
        if (text.length() == 0) {
            return;
        }
        if (builder.length() > 0) {
            builder.append('\n');
        }
        builder.append(key).append('=').append(text);
    }

    private String join(Set<String> values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            String cleaned = value == null ? "" : value.trim();
            if (cleaned.length() == 0) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(cleaned);
        }
        return builder.length() == 0 ? "none" : builder.toString();
    }
}
