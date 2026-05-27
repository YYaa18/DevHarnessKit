package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.goal.GoalCheck;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.repository.goal.GoalCheckRepository;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;

final class GoalCheckRecorder {
    private final GoalCheckRepository checkRepository;
    private final WorkspaceFingerprintService fingerprintService;

    GoalCheckRecorder(GoalCheckRepository checkRepository, WorkspaceFingerprintService fingerprintService) {
        this.checkRepository = checkRepository;
        this.fingerprintService = fingerprintService;
    }

    GoalCheck save(Connection connection, Path projectRoot, GoalRun goal, String checkKey, String checkType,
                   String command, String status, String summary, Path evidencePath, String now) throws Exception {
        String evidence = evidencePath == null ? "" : evidencePath.toString();
        String workspaceFingerprint = fingerprintService.workspaceFingerprint(projectRoot);
        String contextFingerprint = fingerprintService.contextFingerprint(projectRoot);
        String checkFingerprint = fingerprintService.checkFingerprint(checkKey, status, summary, evidence,
                workspaceFingerprint, contextFingerprint);
        GoalCheck check = new GoalCheck(0L, goal.goalKey(), checkKey, checkType, true, goal.stepCount(),
                command, status, summary, evidence, workspaceFingerprint, contextFingerprint,
                checkFingerprint, now, now, now);
        checkRepository.upsert(connection, check);
        return checkRepository.find(connection, goal.goalKey(), checkKey);
    }

    Path logPath(Path projectRoot, GoalRun goal, String checkKey) {
        return PathUtil.goalCheckArtifactsDirectory(projectRoot, goal.goalKey()).resolve(checkKey + ".log");
    }

    void writeLog(Path path, String content) throws Exception {
        Files.createDirectories(path.getParent());
        Files.write(path, content.getBytes("UTF-8"));
    }
}
