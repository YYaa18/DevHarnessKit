package com.devharnesskit.dhk.cli;

import com.devharnesskit.dhk.Main;
import com.devharnesskit.dhk.db.MigrationRunner;

import java.io.InputStream;
import java.util.Properties;

public final class VersionInfo {
    public static final String FALLBACK_VERSION = "dev";
    public static final String RELEASE_CHANNEL = "stable";
    public static final int CURRENT_SCHEMA_VERSION = MigrationRunner.V15;

    private VersionInfo() {
    }

    public static String version() {
        Package pkg = Main.class.getPackage();
        String manifestVersion = pkg == null ? null : pkg.getImplementationVersion();
        if (manifestVersion == null || manifestVersion.trim().length() == 0) {
            return resourceVersion();
        }
        return manifestVersion;
    }

    private static String resourceVersion() {
        InputStream input = VersionInfo.class.getResourceAsStream("/com/devharnesskit/dhk/version.properties");
        if (input == null) {
            return FALLBACK_VERSION;
        }
        try {
            Properties properties = new Properties();
            properties.load(input);
            String version = properties.getProperty("version", "").trim();
            if (version.length() == 0 || version.indexOf("${") >= 0) {
                return FALLBACK_VERSION;
            }
            return version;
        } catch (Exception ex) {
            return FALLBACK_VERSION;
        } finally {
            try {
                input.close();
            } catch (Exception ignored) {
                // Best-effort close only; version fallback must stay safe.
            }
        }
    }
}
