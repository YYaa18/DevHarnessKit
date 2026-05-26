package com.devharnesskit.dhk.model.graph;

import java.nio.file.Path;

public final class GraphInitResult {
    private final Path configPath;
    private final Path exportsDirectory;
    private final boolean configCreated;

    public GraphInitResult(Path configPath, Path exportsDirectory, boolean configCreated) {
        this.configPath = configPath;
        this.exportsDirectory = exportsDirectory;
        this.configCreated = configCreated;
    }

    public Path configPath() {
        return configPath;
    }

    public Path exportsDirectory() {
        return exportsDirectory;
    }

    public boolean configCreated() {
        return configCreated;
    }
}
