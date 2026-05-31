package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.model.goal.GoalStep;
import com.devharnesskit.dhk.model.policy.DevHarnessPolicy;
import com.devharnesskit.dhk.repository.goal.GoalStepRepository;
import com.devharnesskit.dhk.model.bdd.BddBinding;
import com.devharnesskit.dhk.service.policy.DevHarnessPolicyService;
import com.devharnesskit.dhk.util.ChangedFileText;
import com.devharnesskit.dhk.util.EvidenceValueParser;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class GoalCheckSupport {
    private GoalCheckSupport() {
    }

    static String evidenceValue(List<GoalStep> steps, String key) {
        if (steps == null || key == null || key.length() == 0) {
            return "";
        }
        for (GoalStep step : steps) {
            String value = EvidenceValueParser.value(step.evidence(), key);
            if (value.length() > 0) {
                return value;
            }
        }
        return "";
    }

    static String latestEvidenceValue(List<GoalStep> steps, String key) {
        if (steps == null || key == null || key.length() == 0) {
            return "";
        }
        String latest = "";
        for (GoalStep step : steps) {
            String candidate = EvidenceValueParser.value(step.evidence(), key);
            if (candidate.length() > 0) {
                latest = candidate;
            }
        }
        return latest;
    }

    static boolean containsEvidenceFlag(List<GoalStep> steps, String key) {
        return "true".equalsIgnoreCase(evidenceValue(steps, key))
                || "yes".equalsIgnoreCase(evidenceValue(steps, key));
    }

    static boolean artifactExists(Path projectRoot, String pathText) {
        if (pathText == null || pathText.trim().length() == 0) {
            return false;
        }
        try {
            Path raw = projectRoot.getFileSystem().getPath(pathText.trim());
            Path path = raw.isAbsolute() ? raw.toAbsolutePath().normalize()
                    : projectRoot.resolve(raw).toAbsolutePath().normalize();
            Path root = projectRoot.toAbsolutePath().normalize();
            return path.startsWith(root) && Files.isRegularFile(path);
        } catch (Exception ex) {
            return false;
        }
    }

    static String empty(String value, String fallback) {
        return value == null || value.length() == 0 ? fallback : value;
    }

    static String firstNonEmpty(String first, String second) {
        if (first != null && first.length() > 0) {
            return first;
        }
        return second == null ? "" : second;
    }

    static List<String> protectedImpactFiles(Path projectRoot, DevHarnessPolicyService policyService)
            throws Exception {
        List<String> result = new ArrayList<String>();
        Path impact = PathUtil.graphImpactMap(projectRoot);
        if (!Files.isRegularFile(impact)) {
            return result;
        }
        String impactText = new String(Files.readAllBytes(impact), "UTF-8");
        DevHarnessPolicy policy = policyService.load(projectRoot);
        String[] protectedGlobs = policy.protectedFiles();
        String[] lines = impactText.split("\\r?\\n");
        for (String line : lines) {
            String value = relatedFile(line);
            if (value.length() == 0) {
                continue;
            }
            if (line.contains("[protected_file]") || matchesAny(value, protectedGlobs)) {
                result.add(value);
            }
        }
        return result;
    }

    static String relatedFile(String line) {
        String trimmed = line == null ? "" : line.trim();
        if (!trimmed.startsWith("- ")) {
            return "";
        }
        String value = trimmed.substring(2).trim();
        int bracket = value.indexOf(" [");
        if (bracket >= 0) {
            value = value.substring(0, bracket).trim();
        }
        if (value.startsWith("src/") || value.startsWith(".agents/") || value.indexOf('/') >= 0) {
            return value;
        }
        return "";
    }

    static boolean matchesAny(String value, String[] globs) {
        for (String glob : globs == null ? new String[0] : globs) {
            if (globMatches(normalizePath(value), normalizePath(glob))) {
                return true;
            }
        }
        return false;
    }

    static boolean globMatches(String value, String glob) {
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < glob.length(); i++) {
            char ch = glob.charAt(i);
            if (ch == '*') {
                if (i + 1 < glob.length() && glob.charAt(i + 1) == '*') {
                    regex.append(".*");
                    i++;
                } else {
                    regex.append("[^/]*");
                }
            } else {
                regex.append(Pattern.quote(String.valueOf(ch)));
            }
        }
        return value.matches(regex.toString());
    }

    static String normalizePath(String value) {
        return value == null ? "" : value.trim().replace('\\', '/');
    }

    static void addAgeFailure(String label, String generatedAt, int maxMinutes, String now,
                              String command, List<String> failures) {
        if (maxMinutes <= 0 || generatedAt == null || generatedAt.length() == 0) {
            return;
        }
        try {
            Duration age = Duration.between(Instant.parse(generatedAt), Instant.parse(now));
            if (age.toMinutes() > maxMinutes) {
                failures.add(label + " stale: age_minutes=" + age.toMinutes()
                        + " max_minutes=" + maxMinutes + "; next_command=" + command);
            }
        } catch (Exception ex) {
            failures.add(label + " stale: invalid generated_at; next_command=" + command);
        }
    }

    static List<String> changedFilesForCoverage(GoalStepRepository stepRepository, Connection connection,
                                                Path projectRoot, GoalRun goal) throws Exception {
        Set<String> files = new LinkedHashSet<String>();
        for (GoalStep step : stepRepository.listByGoal(connection, goal.goalKey())) {
            addChangedFiles(files, projectRoot, step.changedFiles());
            addChangedFilesFromEvidence(files, projectRoot, step.evidence());
        }
        return new ArrayList<String>(files);
    }

    static void addChangedFilesFromEvidence(Set<String> files, Path projectRoot, String evidence) {
        if (evidence == null || evidence.length() == 0) {
            return;
        }
        String[] lines = evidence.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("changed_files=")) {
                addChangedFiles(files, projectRoot, trimmed.substring("changed_files=".length()));
            }
        }
    }

    static void addChangedFiles(Set<String> files, Path projectRoot, String raw) {
        ChangedFileText.addFiles(files, projectRoot, raw);
    }

    static boolean requiresImpactCoverage(String file) {
        if (file.startsWith("src/test/")) {
            return false;
        }
        return file.startsWith("src/")
                && (file.endsWith(".java") || file.endsWith(".xml") || file.endsWith(".jsp")
                || file.endsWith(".jspx") || file.endsWith(".sql") || file.endsWith(".properties")
                || file.endsWith(".yml") || file.endsWith(".yaml"));
    }

    static String graphRefreshCommand(Path projectRoot) {
        String root = projectRoot.toAbsolutePath().normalize().toString();
        return "dhk graph index --project-root " + root
                + " && dhk graph export --project-root " + root;
    }

    static String impactRefreshCommand(Path projectRoot, Connection connection, GoalRun goal,
                                       GoalStepRepository stepRepository) throws Exception {
        List<String> changed = changedFilesForCoverage(stepRepository, connection, projectRoot, goal);
        for (String file : changed) {
            if (requiresImpactCoverage(file)) {
                return commandForChangedFile(projectRoot, file);
            }
        }
        return "dhk graph impact --project-root " + projectRoot.toAbsolutePath().normalize()
                + " --file <changed-file>";
    }

    static String scenarioImpactCommand(Path projectRoot, List<BddBinding> bindings) {
        String scenarioKey = "<scenario-key>";
        if (bindings != null && !bindings.isEmpty()) {
            scenarioKey = bindings.get(0).scenarioKey();
        }
        return "dhk graph impact --project-root " + projectRoot.toAbsolutePath().normalize()
                + " --scenario " + scenarioKey;
    }

    static String commandForChangedFile(Path projectRoot, String file) {
        return "dhk graph impact --project-root " + projectRoot.toAbsolutePath().normalize()
                + " --file " + file;
    }

    static String jsonString(String json, String key) {
        if (json == null || json.length() == 0 || key == null || key.length() == 0) {
            return "";
        }
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : "";
    }

    static String tagValue(String text, String tag) {
        if (text == null || text.length() == 0 || tag == null || tag.length() == 0) {
            return "";
        }
        Pattern pattern = Pattern.compile("<" + Pattern.quote(tag) + ">([^<]+)</" + Pattern.quote(tag) + ">");
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    static List<String> sectionValues(String text, String section) {
        List<String> values = new ArrayList<String>();
        if (text == null || text.length() == 0 || section == null || section.length() == 0) {
            return values;
        }
        String start = "<" + section + ">";
        String end = "</" + section + ">";
        boolean inSection = false;
        for (String line : text.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (start.equals(trimmed)) {
                inSection = true;
                continue;
            }
            if (end.equals(trimmed)) {
                break;
            }
            if (inSection && trimmed.startsWith("- ")) {
                values.add(trimmed.substring(2).trim());
            }
        }
        return values;
    }

    static String lineValue(String text, String prefix) {
        if (text == null || text.length() == 0 || prefix == null || prefix.length() == 0) {
            return "";
        }
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith(prefix)) {
                return trimmed.substring(prefix.length()).trim();
            }
        }
        return "";
    }

    static boolean impactArtifactExists(Path projectRoot, String pathText) {
        if (pathText == null || pathText.trim().length() == 0) {
            return false;
        }
        String text = pathText.trim();
        if ("IMPACT_MAP.md".equals(text) || text.endsWith("/IMPACT_MAP.md")) {
            return Files.isRegularFile(PathUtil.graphImpactMap(projectRoot));
        }
        return artifactExists(projectRoot, text);
    }

    static boolean acceptedCoverageEvidence(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return "yes".equals(normalized) || "true".equals(normalized) || "covered".equals(normalized)
                || "passed".equals(normalized) || normalized.indexOf("covered") >= 0;
    }

    static boolean multiImpactEvidenceCovers(Path projectRoot, List<GoalStep> steps, String file) {
        if (!acceptedCoverageEvidence(latestEvidenceValue(steps, "multi_impact_evidence_status"))) {
            return false;
        }
        if (!artifactExists(projectRoot, latestEvidenceValue(steps, "multi_impact_evidence_path"))) {
            return false;
        }
        String covered = latestEvidenceValue(steps, "impact_covered_files");
        for (String part : covered.split("[,\\n\\r]+")) {
            if (normalizePath(part).equals(normalizePath(file))) {
                return true;
            }
        }
        return false;
    }

    static boolean impactExpanded(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return normalized.indexOf("expanded") >= 0
                || normalized.indexOf("increased") >= 0
                || normalized.indexOf("newly_impacted") >= 0
                || normalized.indexOf("new impacted") >= 0
                || normalized.indexOf("wider") >= 0;
    }

    static String matchedScenarioKey(List<BddBinding> bindings, String scenarioImpactText) {
        if (scenarioImpactText == null) {
            return "";
        }
        for (BddBinding binding : bindings) {
            String scenarioKey = binding.scenarioKey();
            if (scenarioKey != null && scenarioKey.length() > 0
                    && scenarioImpactText.indexOf("scenario_key: " + scenarioKey) >= 0) {
                return scenarioKey;
            }
        }
        return "";
    }

    static String join(String[] command) {
        StringBuilder builder = new StringBuilder();
        for (String part : command) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(part);
        }
        return builder.toString();
    }
}
