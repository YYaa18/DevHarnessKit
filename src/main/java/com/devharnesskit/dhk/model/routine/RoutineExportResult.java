package com.devharnesskit.dhk.model.routine;

import java.nio.file.Path;

public final class RoutineExportResult {
    private final Path markdownPath;
    private final Path jsonPath;
    private final Path goalsPath;
    private final Path replayDirectory;
    private final int replayFiles;

    public RoutineExportResult(Path markdownPath, Path jsonPath, Path goalsPath,
                               Path replayDirectory, int replayFiles) {
        this.markdownPath = markdownPath;
        this.jsonPath = jsonPath;
        this.goalsPath = goalsPath;
        this.replayDirectory = replayDirectory;
        this.replayFiles = replayFiles;
    }

    public Path markdownPath() { return markdownPath; }
    public Path jsonPath() { return jsonPath; }
    public Path goalsPath() { return goalsPath; }
    public Path replayDirectory() { return replayDirectory; }
    public int replayFiles() { return replayFiles; }
}
