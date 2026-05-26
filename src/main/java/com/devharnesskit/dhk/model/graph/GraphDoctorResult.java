package com.devharnesskit.dhk.model.graph;

import java.nio.file.Path;

public final class GraphDoctorResult {
    private final Path configPath;
    private final String configSource;
    private final String provider;
    private final String cgcCommand;
    private final boolean cgcRequired;
    private final boolean cgcAvailable;
    private final String cgcStatus;
    private final String cgcDetail;
    private final boolean defaultProviderUnaffected;

    public GraphDoctorResult(Path configPath, String configSource, String provider, String cgcCommand,
                             boolean cgcRequired, boolean cgcAvailable, String cgcStatus, String cgcDetail,
                             boolean defaultProviderUnaffected) {
        this.configPath = configPath;
        this.configSource = configSource;
        this.provider = provider;
        this.cgcCommand = cgcCommand;
        this.cgcRequired = cgcRequired;
        this.cgcAvailable = cgcAvailable;
        this.cgcStatus = cgcStatus;
        this.cgcDetail = cgcDetail;
        this.defaultProviderUnaffected = defaultProviderUnaffected;
    }

    public Path configPath() {
        return configPath;
    }

    public String configSource() {
        return configSource;
    }

    public String provider() {
        return provider;
    }

    public String cgcCommand() {
        return cgcCommand;
    }

    public boolean cgcRequired() {
        return cgcRequired;
    }

    public boolean cgcAvailable() {
        return cgcAvailable;
    }

    public String cgcStatus() {
        return cgcStatus;
    }

    public String cgcDetail() {
        return cgcDetail;
    }

    public boolean defaultProviderUnaffected() {
        return defaultProviderUnaffected;
    }
}
