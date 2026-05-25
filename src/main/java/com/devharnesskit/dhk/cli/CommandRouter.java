package com.devharnesskit.dhk.cli;

import com.devharnesskit.dhk.command.HelpCommand;
import com.devharnesskit.dhk.command.DoctorCommand;
import com.devharnesskit.dhk.command.VersionCommand;
import com.devharnesskit.dhk.command.projectdb.DbCommand;
import com.devharnesskit.dhk.command.memory.MemoryCommand;
import com.devharnesskit.dhk.command.goal.GoalCommand;
import com.devharnesskit.dhk.command.spec.SpecCommand;
import com.devharnesskit.dhk.command.workflow.WorkflowCommand;
import com.devharnesskit.dhk.service.SensitiveDataGuard;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
            Args args = argsForCommand(parsedArgs);
            if (isVersionRequest(args)) {
                return command("version").run(context, args);
            }
            if (isHelpRequest(args)) {
                return command("help").run(context, args);
            }

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
