package com.devharnesskit.dhk.service;

import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.util.Clock;
import com.devharnesskit.dhk.util.JsonUtil;
import com.devharnesskit.dhk.util.PathUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class ProjectService {
    public Project ensureProject(Path projectRoot, Clock clock) {
        Path projectJson = PathUtil.projectJson(projectRoot);
        Project existing = null;
        if (Files.isRegularFile(projectJson)) {
            existing = readProject(projectJson);
        }

        String now = clock.now().toString();
        Project project;
        if (existing == null) {
            project = new Project(
                    "dhk-" + UUID.randomUUID().toString(),
                    projectName(projectRoot),
                    projectRoot.toAbsolutePath().normalize().toString(),
                    "unknown",
                    "java",
                    "unknown",
                    "unknown",
                    now,
                    now
            );
        } else {
            project = new Project(
                    existing.projectKey(),
                    emptyToDefault(existing.projectName(), projectName(projectRoot)),
                    projectRoot.toAbsolutePath().normalize().toString(),
                    emptyToDefault(existing.projectType(), "unknown"),
                    emptyToDefault(existing.language(), "java"),
                    emptyToDefault(existing.framework(), "unknown"),
                    emptyToDefault(existing.databaseType(), "unknown"),
                    emptyToDefault(existing.createdAt(), now),
                    now
            );
        }
        writeProject(projectJson, project);
        return project;
    }

    public Project readProject(Path projectJson) {
        try {
            Map<String, String> fields = JsonUtil.parseObject(new String(Files.readAllBytes(projectJson), "UTF-8"));
            String now = "";
            String projectKey = fields.get("project_key");
            if (projectKey == null || projectKey.trim().length() == 0) {
                throw new IllegalStateException("project.json is missing project_key");
            }
            return new Project(
                    projectKey,
                    emptyToDefault(fields.get("project_name"), "unknown"),
                    emptyToDefault(fields.get("root_path"), ""),
                    emptyToDefault(fields.get("project_type"), "unknown"),
                    emptyToDefault(fields.get("language"), "java"),
                    emptyToDefault(fields.get("framework"), "unknown"),
                    emptyToDefault(fields.get("database_type"), "unknown"),
                    emptyToDefault(fields.get("created_at"), now),
                    emptyToDefault(fields.get("updated_at"), now)
            );
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read project.json: " + ex.getMessage(), ex);
        }
    }

    public void writeProject(Path projectJson, Project project) {
        Map<String, String> fields = new LinkedHashMap<String, String>();
        fields.put("project_key", project.projectKey());
        fields.put("project_name", project.projectName());
        fields.put("root_path", project.rootPath());
        fields.put("project_type", project.projectType());
        fields.put("language", project.language());
        fields.put("framework", project.framework());
        fields.put("database_type", project.databaseType());
        fields.put("created_at", project.createdAt());
        fields.put("updated_at", project.updatedAt());
        try {
            Files.write(projectJson, JsonUtil.toObject(fields).getBytes("UTF-8"));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write project.json: " + ex.getMessage(), ex);
        }
    }

    private String projectName(Path projectRoot) {
        Path fileName = projectRoot.getFileName();
        if (fileName == null || fileName.toString().length() == 0) {
            return "unknown";
        }
        return fileName.toString();
    }

    private String emptyToDefault(String value, String defaultValue) {
        if (value == null || value.trim().length() == 0) {
            return defaultValue;
        }
        return value;
    }
}
