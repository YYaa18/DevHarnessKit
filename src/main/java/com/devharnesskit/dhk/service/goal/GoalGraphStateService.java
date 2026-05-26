package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.goal.GoalGraphState;
import com.devharnesskit.dhk.model.goal.GoalPlan;
import com.devharnesskit.dhk.model.goal.GoalProfile;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GoalGraphStateService {
    private static final Pattern SNAPSHOT_KEY = Pattern.compile("\"snapshot_key\"\\s*:\\s*\"([^\"]*)\"");

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
        String required = requiredGraphAction(profile, plan.currentAction(), snapshotExists, graphContextExists,
                impactMapExists);
        return new GoalGraphState(true, profile.graphProvider(), profile.graphRequireFreshSnapshot(),
                profile.graphRequireImpactMap(), profile.graphMaxStalenessMinutes(),
                snapshotPath.toString(), snapshotExists, snapshotKey(snapshotPath),
                graphContextPath.toString(), graphContextExists, impactMapPath.toString(), impactMapExists,
                required, graphCommand(projectRoot, required));
    }

    private String requiredGraphAction(GoalProfile profile, String currentAction, boolean snapshotExists,
                                       boolean graphContextExists, boolean impactMapExists) {
        if (!snapshotExists || !graphContextExists) {
            return "graph_index_export";
        }
        if (("graph_impact_analysis".equals(currentAction) || "graph_reimpact".equals(currentAction))
                && profile.graphRequireImpactMap() && !impactMapExists) {
            return "graph_impact";
        }
        if (isGraphAction(profile, currentAction)) {
            return "record_goal_step";
        }
        if (profile.graphRequireImpactMap() && !impactMapExists) {
            return "graph_impact";
        }
        return "none";
    }

    private boolean isGraphAction(GoalProfile profile, String action) {
        for (String graphAction : profile.graphActions()) {
            if (graphAction.equals(action)) {
                return true;
            }
        }
        return false;
    }

    private String graphCommand(Path projectRoot, String required) {
        String root = projectRoot.toAbsolutePath().normalize().toString();
        if ("graph_index_export".equals(required)) {
            return "dhk graph index --project-root " + root
                    + " && dhk graph export --project-root " + root;
        }
        if ("graph_impact".equals(required)) {
            return "dhk graph impact --project-root " + root
                    + " --file <path>|--symbol <symbol>|--sql-table <table>";
        }
        return "";
    }

    private String snapshotKey(Path snapshotPath) {
        if (!Files.isRegularFile(snapshotPath)) {
            return "";
        }
        try {
            Matcher matcher = SNAPSHOT_KEY.matcher(new String(Files.readAllBytes(snapshotPath), "UTF-8"));
            return matcher.find() ? matcher.group(1) : "";
        } catch (Exception ex) {
            return "";
        }
    }
}
