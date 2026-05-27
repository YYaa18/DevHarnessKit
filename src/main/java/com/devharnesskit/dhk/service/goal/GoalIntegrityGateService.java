package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.goal.GoalArtifact;
import com.devharnesskit.dhk.model.goal.GoalCheck;
import com.devharnesskit.dhk.model.goal.GoalEvaluation;
import com.devharnesskit.dhk.model.goal.GoalProfile;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.model.goal.GoalStep;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public final class GoalIntegrityGateService {
    public GoalEvaluation applyPreComplete(GoalRun goal, GoalEvaluation evaluation,
                                           GoalCheckPolicy policy, GoalProfile profile,
                                           List<GoalStep> steps, List<GoalCheck> checks) {
        if (evaluation == null || !evaluation.readyToComplete()) {
            return evaluation;
        }
        List<String> failures = preCompleteFailures(policy, profile, steps, checks);
        if (failures.isEmpty()) {
            return evaluation;
        }
        return blocked(goal, "integrity pre gate failed", failures);
    }

    public GoalEvaluation finalComplete(GoalRun goal, Path summaryPath, Path passportPath,
                                        List<GoalArtifact> artifacts) {
        List<String> failures = finalCompleteFailures(summaryPath, passportPath, artifacts);
        if (failures.isEmpty()) {
            return new GoalEvaluation("ready_to_complete", new String[0], new String[0],
                    "complete_goal", "dhk goal complete --goal " + goal.goalKey());
        }
        return blocked(goal, "integrity final gate failed", failures);
    }

    public List<String> preCompleteFailures(GoalCheckPolicy policy, GoalProfile profile,
                                            List<GoalStep> steps, List<GoalCheck> checks) {
        List<String> failures = new ArrayList<String>();
        int recordedSteps = steps == null ? 0 : steps.size();
        for (String required : policy.requiredChecks(profile)) {
            GoalCheck check = find(checks, required);
            if (check == null) {
                failures.add("check " + required + " missing");
                continue;
            }
            if (check.checkFingerprint().length() == 0) {
                failures.add("check " + required + " missing check_fingerprint");
            }
            if ((profile == null || profile.completionRequireFreshChecks())
                    && check.stepCountAtCheck() < recordedSteps) {
                failures.add("check " + required + " is stale at integrity gate: checked_at_step="
                        + check.stepCountAtCheck() + " current_step=" + recordedSteps);
            }
            if ((profile == null || profile.completionRequireFreshChecks())
                    && check.workspaceFingerprint().length() == 0) {
                failures.add("check " + required + " missing workspace_fingerprint");
            }
            if (check.evidencePath().length() > 0 && !Files.isRegularFile(path(check.evidencePath()))) {
                failures.add("check " + required + " evidence file missing: " + check.evidencePath());
            }
        }
        return failures;
    }

    public List<String> finalCompleteFailures(Path summaryPath, Path passportPath, List<GoalArtifact> artifacts) {
        List<String> failures = new ArrayList<String>();
        if (summaryPath == null || !Files.isRegularFile(summaryPath)) {
            failures.add("GOAL_SUMMARY.md missing");
        }
        if (passportPath == null || !Files.isRegularFile(passportPath)) {
            failures.add("ARTIFACT_PASSPORT.json missing");
        }
        GoalArtifact summary = findArtifact(artifacts, "goal_summary");
        GoalArtifact passport = findArtifact(artifacts, "artifact_passport");
        GoalArtifact checkpoint = findArtifact(artifacts, "checkpoint");
        if (summary == null) {
            failures.add("goal_artifact row missing: goal_summary");
        } else if (summary.filePath().length() > 0 && !Files.isRegularFile(path(summary.filePath()))) {
            failures.add("goal_summary artifact file missing: " + summary.filePath());
        }
        if (passport == null) {
            failures.add("goal_artifact row missing: artifact_passport");
        } else {
            if (passport.filePath().length() == 0 || !Files.isRegularFile(path(passport.filePath()))) {
                failures.add("artifact_passport file missing: " + passport.filePath());
            }
            if (passport.contentHash().length() == 0) {
                failures.add("artifact_passport content_hash missing");
            } else if (passport.filePath().length() > 0 && Files.isRegularFile(path(passport.filePath()))
                    && !passport.contentHash().equals(fileHash(path(passport.filePath())))) {
                failures.add("artifact_passport content_hash mismatch");
            }
        }
        if (checkpoint == null) {
            failures.add("goal_artifact row missing: checkpoint");
        }
        return failures;
    }

    private GoalEvaluation blocked(GoalRun goal, String prefix, List<String> failures) {
        List<String> missing = new ArrayList<String>();
        for (String failure : failures) {
            missing.add(prefix + ": " + failure);
        }
        return new GoalEvaluation("not_ready", missing.toArray(new String[missing.size()]), new String[0],
                "repair_integrity_evidence", "dhk goal verify --goal " + goal.goalKey());
    }

    private GoalCheck find(List<GoalCheck> checks, String key) {
        for (GoalCheck check : checks == null ? new ArrayList<GoalCheck>() : checks) {
            if (key.equals(check.checkKey())) {
                return check;
            }
        }
        return null;
    }

    private GoalArtifact findArtifact(List<GoalArtifact> artifacts, String type) {
        for (GoalArtifact artifact : artifacts == null ? new ArrayList<GoalArtifact>() : artifacts) {
            if (type.equals(artifact.artifactType())) {
                return artifact;
            }
        }
        return null;
    }

    private Path path(String raw) {
        return new java.io.File(raw).toPath();
    }

    private String fileHash(Path path) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] data = Files.readAllBytes(path);
            byte[] hash = digest.digest(data);
            StringBuilder builder = new StringBuilder("sha256:");
            for (byte b : hash) {
                builder.append(String.format("%02x", b & 0xff));
            }
            return builder.toString();
        } catch (Exception ex) {
            return "";
        }
    }
}
