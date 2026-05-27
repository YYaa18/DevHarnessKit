package com.devharnesskit.dhk.model.brief;

import java.nio.file.Path;

public final class BriefRequest {
    private final Path projectRoot;
    private final String task;
    private final String module;
    private final String target;
    private final String requestedMode;
    private final String profileKey;
    private final String preset;
    private final String graphOverride;

    public BriefRequest(Path projectRoot, String task, String module, String target,
                        String requestedMode, String profileKey, String preset,
                        String graphOverride) {
        this.projectRoot = projectRoot;
        this.task = value(task);
        this.module = value(module);
        this.target = value(target);
        this.requestedMode = value(requestedMode);
        this.profileKey = value(profileKey);
        this.preset = value(preset);
        this.graphOverride = value(graphOverride);
    }

    public Path projectRoot() { return projectRoot; }
    public String task() { return task; }
    public String module() { return module; }
    public String target() { return target; }
    public String requestedMode() { return requestedMode; }
    public String profileKey() { return profileKey; }
    public String preset() { return preset; }
    public String graphOverride() { return graphOverride; }

    private static String value(String value) {
        return value == null ? "" : value.trim();
    }
}
