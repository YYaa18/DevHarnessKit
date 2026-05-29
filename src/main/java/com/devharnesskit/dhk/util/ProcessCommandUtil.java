package com.devharnesskit.dhk.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ProcessCommandUtil {
    private ProcessCommandUtil() {
    }

    public static String[] resolveExecutable(String[] command) {
        return resolveExecutable(command, System.getProperty("os.name", ""));
    }

    static String[] resolveExecutable(String[] command, String osName) {
        if (command == null || command.length == 0) {
            return command;
        }
        String[] resolved = command.clone();
        resolved[0] = resolveExecutableName(resolved[0], osName);
        return resolved;
    }

    public static List<String> resolveExecutable(List<String> command) {
        if (command == null || command.isEmpty()) {
            return command;
        }
        List<String> resolved = new ArrayList<String>(command);
        resolved.set(0, resolveExecutableName(resolved.get(0), System.getProperty("os.name", "")));
        return resolved;
    }

    private static String resolveExecutableName(String executable, String osName) {
        if (executable == null || !isWindows(osName)) {
            return executable;
        }
        if ("mvn".equalsIgnoreCase(executable)) {
            return "mvn.cmd";
        }
        if ("mvnw".equalsIgnoreCase(executable)) {
            return "mvnw.cmd";
        }
        return executable;
    }

    private static boolean isWindows(String osName) {
        return osName != null && osName.toLowerCase(Locale.ROOT).contains("win");
    }
}
