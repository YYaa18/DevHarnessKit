package com.devharnesskit.dhk.cli;

import com.devharnesskit.dhk.Main;
import com.devharnesskit.dhk.db.MigrationRunner;

public final class VersionInfo {
    public static final String FALLBACK_VERSION = "0.1.0-alpha";
    public static final String RELEASE_CHANNEL = "alpha / developer preview";
    public static final int CURRENT_SCHEMA_VERSION = MigrationRunner.V6;

    private VersionInfo() {
    }

    public static String version() {
        Package pkg = Main.class.getPackage();
        String manifestVersion = pkg == null ? null : pkg.getImplementationVersion();
        if (manifestVersion == null || manifestVersion.trim().length() == 0) {
            return FALLBACK_VERSION;
        }
        return manifestVersion;
    }
}
