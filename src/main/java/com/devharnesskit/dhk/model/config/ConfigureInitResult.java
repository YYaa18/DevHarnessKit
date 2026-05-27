package com.devharnesskit.dhk.model.config;

import java.nio.file.Path;

public final class ConfigureInitResult {
    private final Path configPath;
    private final String preset;
    private final boolean configCreated;
    private final DevHarnessConfig config;

    public ConfigureInitResult(Path configPath, String preset, boolean configCreated, DevHarnessConfig config) {
        this.configPath = configPath;
        this.preset = preset == null ? "" : preset;
        this.configCreated = configCreated;
        this.config = config;
    }

    public Path configPath() { return configPath; }
    public String preset() { return preset; }
    public boolean configCreated() { return configCreated; }
    public DevHarnessConfig config() { return config; }
}
