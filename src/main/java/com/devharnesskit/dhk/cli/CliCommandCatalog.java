package com.devharnesskit.dhk.cli;

import java.util.LinkedHashMap;
import java.util.Map;

public final class CliCommandCatalog {
    private static final String[] TOP_LEVEL = new String[]{
            "help", "version", "doctor", "status", "readiness", "advise", "quickstart", "completion",
            "memory", "db", "goal", "graph", "bdd", "skill", "artifact", "checkpoint",
            "configure", "workflow", "spec"
    };
    private static final String[] COMMON_OPTIONS = new String[]{
            "--project-root", "--json", "--format", "--write", "--dry-run", "--force", "--help", "--version"
    };

    private CliCommandCatalog() {
    }

    public static String[] topLevelCommands() {
        return copy(TOP_LEVEL);
    }

    public static String[] commonOptions() {
        return copy(COMMON_OPTIONS);
    }

    public static Map<String, String[]> subcommands() {
        Map<String, String[]> values = new LinkedHashMap<String, String[]>();
        values.put("completion", new String[]{"bash", "zsh", "fish"});
        values.put("memory", new String[]{"init", "add", "confirm", "search", "export", "checkpoint", "recover", "backup"});
        values.put("db", new String[]{"test", "sql"});
        values.put("goal", new String[]{"start", "resume", "next", "step", "check", "evaluate", "verify",
                "audit", "recheck", "complete", "status", "export"});
        values.put("graph", new String[]{"init", "doctor", "status", "index", "impact", "export", "prune"});
        values.put("bdd", new String[]{"init", "add", "list", "show", "export", "lint", "evidence",
                "verify", "coverage", "bind-spec", "bind-goal", "bind-graph", "bind-test"});
        values.put("skill", new String[]{"lint", "verify", "trust", "audit", "score", "report", "gate"});
        values.put("artifact", new String[]{"passport"});
        values.put("checkpoint", new String[]{"request", "approve", "list"});
        values.put("configure", new String[]{"init", "show", "doctor", "explain"});
        values.put("workflow", new String[]{"template", "start", "status", "phase", "gate", "export",
                "artifact", "bind-memory", "bind-checkpoint", "summary"});
        values.put("spec", new String[]{"create", "document", "task", "acceptance", "status", "export",
                "bind-workflow", "archive"});
        return values;
    }

    public static String[] commonCommandOptions(String command) {
        if ("goal".equals(command)) {
            return new String[]{"--project-root", "--goal", "--profile", "--task", "--module", "--mode",
                    "--summary", "--changed-files", "--evidence", "--check", "--all", "--level", "--auto",
                    "--markdown", "--json"};
        }
        if ("graph".equals(command)) {
            return new String[]{"--project-root", "--file", "--symbol", "--sql-table", "--scenario", "--depth",
                    "--allow-stale", "--allow-stale-evidence", "--keep", "--dry-run", "--json"};
        }
        if ("configure".equals(command)) {
            return new String[]{"--project-root", "--preset", "--compile", "--test", "--graph", "--target",
                    "--force", "--dry-run", "--json"};
        }
        if ("quickstart".equals(command)) {
            return new String[]{"--project-root", "--preset", "--task", "--module", "--target", "--graph",
                    "--profile", "--mode", "--force", "--dry-run", "--json"};
        }
        if ("advise".equals(command)) {
            return new String[]{"--project-root", "--task", "--module", "--target", "--preset", "--profile",
                    "--mode", "--graph", "--json"};
        }
        return commonOptions();
    }

    public static String join(String[] values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private static String[] copy(String[] values) {
        String[] result = new String[values.length];
        System.arraycopy(values, 0, result, 0, values.length);
        return result;
    }
}
