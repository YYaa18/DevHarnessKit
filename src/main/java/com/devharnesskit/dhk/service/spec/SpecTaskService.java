package com.devharnesskit.dhk.service.spec;

import com.devharnesskit.dhk.model.spec.SpecChange;
import com.devharnesskit.dhk.model.spec.SpecEvent;
import com.devharnesskit.dhk.model.spec.SpecTask;
import com.devharnesskit.dhk.repository.spec.SpecEventRepository;
import com.devharnesskit.dhk.repository.spec.SpecTaskRepository;

import java.sql.Connection;
import java.sql.SQLException;

public final class SpecTaskService {
    private final SpecTaskRepository taskRepository;
    private final SpecEventRepository eventRepository;

    public SpecTaskService(SpecTaskRepository taskRepository, SpecEventRepository eventRepository) {
        this.taskRepository = taskRepository;
        this.eventRepository = eventRepository;
    }

    public SpecTask addTask(Connection connection, SpecChange change, String taskKey,
                            String title, String description, String phaseKey,
                            String now) throws SQLException {
        if (taskRepository.findByKey(connection, change.changeKey(), taskKey) != null) {
            throw new SQLException("Spec task already exists: " + taskKey);
        }
        SpecTask task = new SpecTask(0L, change.changeKey(), taskKey,
                taskRepository.nextOrder(connection, change.changeKey()), title, description,
                "pending", phaseKey, "", now, now, "");
        long id = taskRepository.insert(connection, task);
        eventRepository.insert(connection, new SpecEvent(0L, change.projectKey(), change.changeKey(),
                "task_added", "info", "Spec task added: " + taskKey, title, now));
        return new SpecTask(id, task.changeKey(), task.taskKey(), task.taskOrder(), task.title(),
                task.description(), task.status(), task.phaseKey(), task.evidence(),
                task.createdAt(), task.updatedAt(), task.completedAt());
    }

    public void updateTask(Connection connection, SpecChange change, SpecTask task,
                           String status, String evidence, String now) throws SQLException {
        String completedAt = "done".equals(status) || "skipped".equals(status) ? now : "";
        taskRepository.updateStatus(connection, change.changeKey(), task.taskKey(),
                status, evidence, completedAt, now);
        eventRepository.insert(connection, new SpecEvent(0L, change.projectKey(), change.changeKey(),
                "task_updated", "info", "Spec task updated: " + task.taskKey(),
                status, now));
    }

    public static boolean isTaskStatusAllowed(String status) {
        return "pending".equals(status) || "in_progress".equals(status)
                || "done".equals(status) || "blocked".equals(status)
                || "skipped".equals(status);
    }
}
