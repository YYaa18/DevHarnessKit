package com.devharnesskit.dhk.cli;

import java.util.LinkedHashMap;
import java.util.Map;

public final class CliCommandCatalog {
    private static final String[] COMMON_OPTIONS = new String[]{
            "--project-root", "--json", "--format", "--write", "--dry-run", "--force", "--help", "--version"
    };
    private static final CliCommandDescriptor[] COMMANDS = new CliCommandDescriptor[]{
            command("help", "Show CLI help"),
            command("version", "Print build and stability metadata"),
            command("doctor", "Run local environment diagnostics"),
            command("status", "Show a read-only project snapshot"),
            command("readiness", "Run status with CI/release-friendly readiness semantics"),
            commandWithOptions("advise", "Generate a Work Brief and Agent Brief without starting a goal",
                    options("--project-root", "--task", "--module", "--target", "--preset", "--profile",
                            "--mode", "--graph", "--json")),
            command("brief", "Answer interaction requests and manage knowledge candidates",
                    subcommands("answer", "show", "knowledge"),
                    options("--project-root", "--request", "--choice", "--candidate", "--destination", "--json")),
            command("growth", "Review and export advisory personal growth lessons",
                    subcommands("review", "confirm", "export"),
                    options("--project-root", "--lesson", "--json")),
            commandWithOptions("quickstart", "Configure the project and create the first goal",
                    options("--project-root", "--preset", "--task", "--module", "--target", "--graph",
                            "--profile", "--mode", "--resume-existing", "--force", "--dry-run", "--json")),
            command("completion", "Generate static shell completion", subcommands("bash", "zsh", "fish")),
            command("memory", "Manage durable local memory",
                    subcommands("init", "add", "confirm", "list", "search", "export", "checkpoint", "recover", "backup")),
            command("db", "Inspect business databases with guarded SQL", subcommands("test", "sql")),
            command("goal", "Run goal orchestration",
                    subcommands("start", "resume", "next", "step", "check", "evaluate", "verify",
                            "audit", "recheck", "complete", "status", "export", "evidence-template"),
                    options("--project-root", "--goal", "--profile", "--task", "--module", "--mode",
                            "--summary", "--changed-files", "--evidence", "--check", "--all", "--level",
                            "--auto", "--markdown", "--force-new", "--json")),
            command("graph", "Index and inspect Graph Lite snapshots",
                    subcommands("init", "doctor", "status", "index", "impact", "export", "prune"),
                    options("--project-root", "--file", "--symbol", "--sql-table", "--scenario", "--depth",
                            "--allow-stale", "--allow-stale-evidence", "--keep", "--dry-run", "--json")),
            command("bdd", "Manage BDD acceptance evidence",
                    subcommands("init", "add", "list", "show", "export", "lint", "evidence",
                            "verify", "coverage", "bind-spec", "bind-goal", "bind-graph", "bind-test")),
            command("skill", "Audit and gate agent skill contracts",
                    subcommands("lint", "verify", "trust", "audit", "score", "report", "gate")),
            command("artifact", "Verify generated audit artifacts", subcommands("passport")),
            command("checkpoint", "Request and approve human checkpoints",
                    subcommands("request", "approve", "list")),
            command("configure", "Configure project verification policy",
                    subcommands("init", "show", "doctor", "explain"),
                    options("--project-root", "--preset", "--compile", "--test", "--graph", "--target",
                            "--force", "--dry-run", "--json")),
            command("workflow", "Record workflow audit state",
                    subcommands("template", "start", "status", "phase", "gate", "export",
                            "artifact", "bind-memory", "bind-checkpoint", "summary")),
            command("spec", "Record specification tasks and acceptance state",
                    subcommands("create", "document", "task", "acceptance", "status", "export",
                            "bind-workflow", "archive"))
    };

    private CliCommandCatalog() {
    }

    public static CliCommandDescriptor[] descriptors() {
        CliCommandDescriptor[] result = new CliCommandDescriptor[COMMANDS.length];
        System.arraycopy(COMMANDS, 0, result, 0, COMMANDS.length);
        return result;
    }

    public static CliCommandDescriptor find(String command) {
        for (CliCommandDescriptor descriptor : COMMANDS) {
            if (descriptor.name().equals(command)) {
                return descriptor;
            }
        }
        return null;
    }

    public static String[] topLevelCommands() {
        String[] result = new String[COMMANDS.length];
        for (int i = 0; i < COMMANDS.length; i++) {
            result[i] = COMMANDS[i].name();
        }
        return result;
    }

    public static String[] commonOptions() {
        return copy(COMMON_OPTIONS);
    }

    public static Map<String, String[]> subcommands() {
        Map<String, String[]> values = new LinkedHashMap<String, String[]>();
        for (CliCommandDescriptor descriptor : COMMANDS) {
            if (descriptor.hasSubcommands()) {
                values.put(descriptor.name(), descriptor.subcommands());
            }
        }
        return values;
    }

    public static String[] commonCommandOptions(String command) {
        CliCommandDescriptor descriptor = find(command);
        if (descriptor != null && descriptor.options().length > 0) {
            return descriptor.options();
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

    private static CliCommandDescriptor command(String name, String description) {
        return command(name, description, new String[0], COMMON_OPTIONS);
    }

    private static CliCommandDescriptor command(String name, String description, String[] subcommands) {
        return command(name, description, subcommands, COMMON_OPTIONS);
    }

    private static CliCommandDescriptor commandWithOptions(String name, String description, String[] options) {
        return command(name, description, new String[0], options);
    }

    private static CliCommandDescriptor command(String name, String description, String[] subcommands,
                                                String[] options) {
        return new CliCommandDescriptor(name, description, subcommands, options);
    }

    private static String[] subcommands(String... values) {
        return values;
    }

    private static String[] options(String... values) {
        return values;
    }
}
