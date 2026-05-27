package com.devharnesskit.dhk.cli;

import com.devharnesskit.dhk.command.AdviseCommand;
import com.devharnesskit.dhk.command.HelpCommand;
import com.devharnesskit.dhk.command.CompletionCommand;
import com.devharnesskit.dhk.command.DoctorCommand;
import com.devharnesskit.dhk.command.QuickstartCommand;
import com.devharnesskit.dhk.command.StatusCommand;
import com.devharnesskit.dhk.command.VersionCommand;
import com.devharnesskit.dhk.command.artifact.ArtifactCommand;
import com.devharnesskit.dhk.command.bdd.BddCommand;
import com.devharnesskit.dhk.command.checkpoint.CheckpointCommand;
import com.devharnesskit.dhk.command.configure.ConfigureCommand;
import com.devharnesskit.dhk.command.projectdb.DbCommand;
import com.devharnesskit.dhk.command.graph.GraphCommand;
import com.devharnesskit.dhk.command.memory.MemoryCommand;
import com.devharnesskit.dhk.command.goal.GoalCommand;
import com.devharnesskit.dhk.command.skill.SkillCommand;
import com.devharnesskit.dhk.command.spec.SpecCommand;
import com.devharnesskit.dhk.command.workflow.WorkflowCommand;
import com.devharnesskit.dhk.service.SensitiveDataGuard;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public final class CommandRouter {
    private final Map<String, Supplier<Command>> commands;

    public CommandRouter() {
        this.commands = new LinkedHashMap<String, Supplier<Command>>();
        this.commands.put("help", new Supplier<Command>() {
            public Command get() {
                return new HelpCommand();
            }
        });
        this.commands.put("version", new Supplier<Command>() {
            public Command get() {
                return new VersionCommand();
            }
        });
        this.commands.put("doctor", new Supplier<Command>() {
            public Command get() {
                return new DoctorCommand();
            }
        });
        this.commands.put("status", new Supplier<Command>() {
            public Command get() {
                return new StatusCommand("status");
            }
        });
        this.commands.put("readiness", new Supplier<Command>() {
            public Command get() {
                return new StatusCommand("readiness");
            }
        });
        this.commands.put("quickstart", new Supplier<Command>() {
            public Command get() {
                return new QuickstartCommand();
            }
        });
        this.commands.put("advise", new Supplier<Command>() {
            public Command get() {
                return new AdviseCommand();
            }
        });
        this.commands.put("completion", new Supplier<Command>() {
            public Command get() {
                return new CompletionCommand();
            }
        });
        this.commands.put("memory", new Supplier<Command>() {
            public Command get() {
                return new MemoryCommand();
            }
        });
        this.commands.put("db", new Supplier<Command>() {
            public Command get() {
                return new DbCommand();
            }
        });
        this.commands.put("goal", new Supplier<Command>() {
            public Command get() {
                return new GoalCommand();
            }
        });
        this.commands.put("graph", new Supplier<Command>() {
            public Command get() {
                return new GraphCommand();
            }
        });
        this.commands.put("bdd", new Supplier<Command>() {
            public Command get() {
                return new BddCommand();
            }
        });
        this.commands.put("skill", new Supplier<Command>() {
            public Command get() {
                return new SkillCommand();
            }
        });
        this.commands.put("artifact", new Supplier<Command>() {
            public Command get() {
                return new ArtifactCommand();
            }
        });
        this.commands.put("checkpoint", new Supplier<Command>() {
            public Command get() {
                return new CheckpointCommand();
            }
        });
        this.commands.put("configure", new Supplier<Command>() {
            public Command get() {
                return new ConfigureCommand();
            }
        });
        this.commands.put("workflow", new Supplier<Command>() {
            public Command get() {
                return new WorkflowCommand();
            }
        });
        this.commands.put("spec", new Supplier<Command>() {
            public Command get() {
                return new SpecCommand();
            }
        });
    }

    public int run(String[] rawArgs, CommandContext context) {
        Args parsedArgs = Args.parse(rawArgs);
        Path projectRoot = PathUtil.resolveProjectRoot(parsedArgs, context.workingDirectory());
        SensitiveDataGuard.useProjectPolicy(projectRoot);
        try {
            if (isVersionRequest(parsedArgs)) {
                return command("version").run(context, parsedArgs);
            }
            if (isHelpRequest(parsedArgs)) {
                return command("help").run(context, parsedArgs);
            }
            if (rejectSensitiveCommandArguments(parsedArgs, context)) {
                return ExitCodes.VALIDATION_ERROR;
            }
            Args args = argsForCommand(parsedArgs);

            Supplier<Command> command = commands.get(args.primaryCommand());
            if (command == null) {
                context.err().println("Unknown command: " + args.primaryCommand());
                context.err().println("Run `dhk help` for usage.");
                return ExitCodes.USAGE_ERROR;
            }

            return command.get().run(context, args);
        } finally {
            SensitiveDataGuard.clearProjectPolicy();
        }
    }

    private boolean rejectSensitiveCommandArguments(Args parsedArgs, CommandContext context) {
        if ("db".equals(parsedArgs.primaryCommand())) {
            return false;
        }
        SensitiveDataGuard guard = new SensitiveDataGuard();
        String originalText = parsedArgs.sensitiveScanText(redactionExcludedOptions());
        List<String> originalMatches = guard.findMatches(originalText);
        if (originalMatches.isEmpty()) {
            return false;
        }
        List<String> redactedMatches = guard.findMatches(guard.redact(originalText));
        if (redactedMatches.containsAll(originalMatches)) {
            return false;
        }
        context.err().println("Sensitive data rejected in command arguments before redaction: "
                + missingMatches(originalMatches, redactedMatches));
        return true;
    }

    private List<String> missingMatches(List<String> originalMatches, List<String> redactedMatches) {
        java.util.ArrayList<String> missing = new java.util.ArrayList<String>();
        for (String match : originalMatches) {
            if (!redactedMatches.contains(match)) {
                missing.add(match);
            }
        }
        return missing;
    }

    private Args argsForCommand(Args parsedArgs) {
        if ("db".equals(parsedArgs.primaryCommand())) {
            return parsedArgs;
        }
        return parsedArgs.redacted(new SensitiveDataGuard(), redactionExcludedOptions());
    }

    private Set<String> redactionExcludedOptions() {
        Set<String> excluded = new HashSet<String>();
        excluded.add("project-root");
        excluded.add("out");
        excluded.add("content-file");
        excluded.add("summary-file");
        excluded.add("sql-file");
        excluded.add("file");
        excluded.add("password-env");
        excluded.add("jdbc-url");
        excluded.add("host");
        excluded.add("database");
        excluded.add("user");
        excluded.add("port");
        excluded.add("path");
        excluded.add("write");
        return excluded;
    }

    private Command command(String name) {
        return commands.get(name).get();
    }

    private boolean isHelpRequest(Args args) {
        return args.positionals().isEmpty()
                || "help".equals(args.primaryCommand())
                || args.hasFlag("help");
    }

    private boolean isVersionRequest(Args args) {
        return "version".equals(args.primaryCommand()) || args.hasFlag("version");
    }
}
