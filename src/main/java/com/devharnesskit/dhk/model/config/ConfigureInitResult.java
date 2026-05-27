package com.devharnesskit.dhk.model.config;

import java.nio.file.Path;

public final class ConfigureInitResult {
    private final Path configPath;
    private final String preset;
    private final boolean configCreated;
    private final DevHarnessConfig config;
    private final boolean dryRun;
    private final boolean wouldWrite;
    private final boolean forceRequired;

    public ConfigureInitResult(Path configPath, String preset, boolean configCreated, DevHarnessConfig config) {
        this(configPath, preset, configCreated, config, false, configCreated, false);
    }

    public ConfigureInitResult(Path configPath, String preset, boolean configCreated, DevHarnessConfig config,
                               boolean dryRun, boolean wouldWrite, boolean forceRequired) {
        this.configPath = configPath;
        this.preset = preset == null ? "" : preset;
        this.configCreated = configCreated;
        this.config = config;
        this.dryRun = dryRun;
        this.wouldWrite = wouldWrite;
        this.forceRequired = forceRequired;
    }

    public Path configPath() { return configPath; }
    public String preset() { return preset; }
    public boolean configCreated() { return configCreated; }
    public DevHarnessConfig config() { return config; }
    public boolean dryRun() { return dryRun; }
    public boolean wouldWrite() { return wouldWrite; }
    public boolean forceRequired() { return forceRequired; }
}
