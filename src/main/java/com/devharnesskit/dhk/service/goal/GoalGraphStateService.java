package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.goal.GoalGraphState;
import com.devharnesskit.dhk.model.goal.GoalPlan;
import com.devharnesskit.dhk.model.goal.GoalProfile;
import com.devharnesskit.dhk.model.policy.DevHarnessPolicy;
import com.devharnesskit.dhk.model.graph.GraphConfig;
import com.devharnesskit.dhk.model.graph.GraphScanReport;
import com.devharnesskit.dhk.service.graph.GraphConfigService;
import com.devharnesskit.dhk.service.graph.GraphFileScanner;
import com.devharnesskit.dhk.service.graph.GraphWorkspaceFingerprintService;
import com.devharnesskit.dhk.service.policy.DevHarnessPolicyService;
import com.devharnesskit.dhk.util.PathUtil;

import java.util.ArrayList;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GoalGraphStateService {
    private static final Pattern SNAPSHOT_KEY = Pattern.compile("\"snapshot_key\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern SNAPSHOT_WORKSPACE_FINGERPRINT =
            Pattern.compile("\"workspace_fingerprint\"\\s*:\\s*\"([^\"]*)\"");
    private final DevHarnessPolicyService policyService = new DevHarnessPolicyService();
    private final GraphConfigService configService = new GraphConfigService();
    private final GraphFileScanner scanner = new GraphFileScanner();
    private final GraphWorkspaceFingerprintService fingerprintService = new GraphWorkspaceFingerprintService();

    public GoalGraphState inspect(Path projectRoot, GoalProfile profile, GoalPlan plan) {
        if (profile == null || !profile.graphRequired()) {
            return GoalGraphState.disabled();
        }
        Path snapshotPath = PathUtil.graphSnapshotJson(projectRoot);
        Path graphContextPath = PathUtil.graphContext(projectRoot);
        Path impactMapPath = PathUtil.graphImpactMap(projectRoot);
        boolean snapshotExists = Files.isRegularFile(snapshotPath);
        boolean graphContextExists = Files.isRegularFile(graphContextPath);
        boolean impactMapExists = Files.isRegularFile(impactMapPath);
        String snapshotText = snapshotText(snapshotPath);
        String snapshotKey = jsonString(snapshotText, SNAPSHOT_KEY);
        String snapshotFingerprint = jsonString(snapshotText, SNAPSHOT_WORKSPACE_FINGERPRINT);
        String currentFingerprint = currentWorkspaceFingerprint(projectRoot);
        boolean snapshotStale = snapshotStale(snapshotExists, snapshotFingerprint, currentFingerprint);
        String freshnessStatus = freshnessStatus(snapshotExists, graphContextExists, snapshotFingerprint,
                currentFingerprint, snapshotStale);
        String required = requiredGraphAction(profile, plan.currentAction(), snapshotExists, graphContextExists,
                impactMapExists, snapshotStale);
        return new GoalGraphState(true, profile.graphProvider(), profile.graphRequireFreshSnapshot(),
                profile.graphRequireImpactMap(), profile.graphMaxStalenessMinutes(),
                snapshotPath.toString(), snapshotExists, snapshotKey,
                snapshotFingerprint, currentFingerprint, snapshotStale, freshnessStatus,
                graphContextPath.toString(), graphContextExists, impactMapPath.toString(), impactMapExists,
                required, graphCommand(projectRoot, required), protectedImpactFiles(projectRoot, impactMapPath));
    }

    private String requiredGraphAction(GoalProfile profile, String currentAction, boolean snapshotExists,
                                       boolean graphContextExists, boolean impactMapExists, boolean snapshotStale) {
        if (!snapshotExists || !graphContextExists) {
            return "refresh_graph_context";
        }
        if (profile.graphRequireFreshSnapshot() && snapshotStale) {
            return "refresh_graph_context";
        }
        if (profile.graphRequireImpactMap() && !impactMapExists) {
            return "prepare_impact_map";
        }
        return "none";
    }

    private String graphCommand(Path projectRoot, String required) {
        String root = projectRoot.toAbsolutePath().normalize().toString();
        if ("refresh_graph_context".equals(required)) {
            return "dhk graph index --project-root " + root
                    + " && dhk graph export --project-root " + root;
        }
        if ("prepare_impact_map".equals(required)) {
            return "dhk graph impact --project-root " + root
                    + " --file <path>|--symbol <symbol>|--sql-table <table>";
        }
        return "";
    }

    private String snapshotText(Path snapshotPath) {
        if (!Files.isRegularFile(snapshotPath)) {
            return "";
        }
        try {
            return new String(Files.readAllBytes(snapshotPath), "UTF-8");
        } catch (Exception ex) {
            return "";
        }
    }

    private String jsonString(String text, Pattern pattern) {
        if (text == null || text.length() == 0) {
            return "";
        }
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String currentWorkspaceFingerprint(Path projectRoot) {
        try {
            GraphConfig config = configService.load(projectRoot);
            GraphScanReport scan = scanner.scan(projectRoot, config,
                    policyService.load(projectRoot).protectedFiles());
            return fingerprintService.fingerprint(projectRoot, scan);
        } catch (RuntimeException ex) {
            return "";
        }
    }

    private boolean snapshotStale(boolean snapshotExists, String snapshotFingerprint, String currentFingerprint) {
        return snapshotExists
                && snapshotFingerprint.length() > 0
                && currentFingerprint.length() > 0
                && !snapshotFingerprint.equals(currentFingerprint);
    }

    private String freshnessStatus(boolean snapshotExists, boolean graphContextExists, String snapshotFingerprint,
                                   String currentFingerprint, boolean snapshotStale) {
        if (!snapshotExists) {
            return "missing_snapshot";
        }
        if (!graphContextExists) {
            return "missing_graph_context";
        }
        if (snapshotFingerprint.length() == 0 || currentFingerprint.length() == 0) {
            return "unknown";
        }
        return snapshotStale ? "stale" : "fresh";
    }

    private String[] protectedImpactFiles(Path projectRoot, Path impactMapPath) {
        if (!Files.isRegularFile(impactMapPath)) {
            return new String[0];
        }
        try {
            String text = new String(Files.readAllBytes(impactMapPath), "UTF-8");
            DevHarnessPolicy policy = policyService.load(projectRoot);
            List<String> result = new ArrayList<String>();
            String[] lines = text.split("\\r?\\n");
            for (String line : lines) {
                String file = relatedFile(line);
                if (file.length() == 0) {
                    continue;
                }
                if (line.contains("[protected_file]") || matchesAny(file, policy.protectedFiles())) {
                    result.add(file);
                }
            }
            return result.toArray(new String[result.size()]);
        } catch (Exception ex) {
            return new String[0];
        }
    }

    private String relatedFile(String line) {
        String trimmed = line == null ? "" : line.trim();
        if (!trimmed.startsWith("- ")) {
            return "";
        }
        String value = trimmed.substring(2).trim();
        int bracket = value.indexOf(" [");
        if (bracket >= 0) {
            value = value.substring(0, bracket).trim();
        }
        return value.startsWith("src/") || value.startsWith(".agents/") ? value : "";
    }

    private boolean matchesAny(String value, String[] globs) {
        for (String glob : globs == null ? new String[0] : globs) {
            if (globMatches(normalize(value), normalize(glob))) {
                return true;
            }
        }
        return false;
    }

    private boolean globMatches(String value, String glob) {
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

    private String normalize(String value) {
        return value == null ? "" : value.trim().replace('\\', '/');
    }
}
