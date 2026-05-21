package com.devharnesskit.dhk.service.workflow;

import com.devharnesskit.dhk.model.Checkpoint;
import com.devharnesskit.dhk.model.MemoryItem;
import com.devharnesskit.dhk.model.workflow.WorkflowArtifact;
import com.devharnesskit.dhk.model.workflow.WorkflowCheckpointBinding;
import com.devharnesskit.dhk.model.workflow.WorkflowEvent;
import com.devharnesskit.dhk.model.workflow.WorkflowMemoryBinding;
import com.devharnesskit.dhk.model.workflow.WorkflowRun;
import com.devharnesskit.dhk.repository.workflow.WorkflowArtifactRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowCheckpointBindingRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowEventRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowMemoryBindingRepository;

import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public final class WorkflowArtifactService {
    private final WorkflowArtifactRepository artifactRepository;
    private final WorkflowMemoryBindingRepository memoryBindingRepository;
    private final WorkflowCheckpointBindingRepository checkpointBindingRepository;
    private final WorkflowEventRepository eventRepository;

    public WorkflowArtifactService(WorkflowArtifactRepository artifactRepository,
                                   WorkflowMemoryBindingRepository memoryBindingRepository,
                                   WorkflowCheckpointBindingRepository checkpointBindingRepository,
                                   WorkflowEventRepository eventRepository) {
        this.artifactRepository = artifactRepository;
        this.memoryBindingRepository = memoryBindingRepository;
        this.checkpointBindingRepository = checkpointBindingRepository;
        this.eventRepository = eventRepository;
    }

    public long recordArtifact(Connection connection, WorkflowRun run, String artifactType, String title,
                               String filePath, String content, String summary, String now) throws SQLException {
        long id = artifactRepository.insert(connection, new WorkflowArtifact(0L, run.projectKey(), run.runKey(),
                artifactType, title, filePath, sha256(content), "confirmed", run.currentPhaseKey(),
                summary, "auto", now, now));
        eventRepository.insert(connection, new WorkflowEvent(0L, run.projectKey(), run.runKey(),
                "artifact_created", run.currentPhaseKey(), "", "info",
                "Artifact recorded: " + artifactType, filePath, now));
        return id;
    }

    public int recordExportedMemory(Connection connection, WorkflowRun run, List<MemoryItem> items,
                                    String reason, String now) throws SQLException {
        int count = 0;
        for (MemoryItem item : items) {
            memoryBindingRepository.insert(connection, new WorkflowMemoryBinding(0L, run.runKey(),
                    item.id(), "exported", run.currentPhaseKey(), reason, now));
            count++;
        }
        eventRepository.insert(connection, new WorkflowEvent(0L, run.projectKey(), run.runKey(),
                "memory_exported", run.currentPhaseKey(), "", "info",
                "Memory exported: " + count, reason, now));
        return count;
    }

    public long bindMemory(Connection connection, WorkflowRun run, long memoryId, String bindingType,
                           String phaseKey, String reason, String now) throws SQLException {
        return memoryBindingRepository.insert(connection, new WorkflowMemoryBinding(0L, run.runKey(),
                memoryId, bindingType, phaseKey, reason, now));
    }

    public long bindCheckpoint(Connection connection, WorkflowRun run, Checkpoint checkpoint,
                               String bindingType, String now) throws SQLException {
        long bindingId = checkpointBindingRepository.insert(connection, new WorkflowCheckpointBinding(0L,
                run.runKey(), checkpoint.id(), bindingType, now));
        artifactRepository.insert(connection, new WorkflowArtifact(0L, run.projectKey(), run.runKey(),
                "checkpoint", "checkpoint_id=" + checkpoint.id(), "", "", "confirmed",
                run.currentPhaseKey(), checkpoint.summary(), "checkpoint", now, now));
        eventRepository.insert(connection, new WorkflowEvent(0L, run.projectKey(), run.runKey(),
                "checkpoint_created", run.currentPhaseKey(), "", "info",
                "Checkpoint bound: " + checkpoint.id(), bindingType, now));
        return bindingId;
    }

    private String sha256(String text) {
        if (text == null || text.length() == 0) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes("UTF-8"));
            StringBuilder builder = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(b & 0xff);
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
