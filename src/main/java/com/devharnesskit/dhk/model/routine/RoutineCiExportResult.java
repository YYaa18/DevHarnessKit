package com.devharnesskit.dhk.model.routine;

import java.nio.file.Path;

public final class RoutineCiExportResult {
    private final Path readmePath;
    private final Path jsonPath;
    private final Path checksPath;
    private final Path profilesPath;

    public RoutineCiExportResult(Path readmePath, Path jsonPath, Path checksPath, Path profilesPath) {
        this.readmePath = readmePath;
        this.jsonPath = jsonPath;
        this.checksPath = checksPath;
        this.profilesPath = profilesPath;
    }

    public Path readmePath() { return readmePath; }
    public Path jsonPath() { return jsonPath; }
    public Path checksPath() { return checksPath; }
    public Path profilesPath() { return profilesPath; }
}
