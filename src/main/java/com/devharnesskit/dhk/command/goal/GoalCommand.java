package com.devharnesskit.dhk.command.goal;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class GoalCommand implements Command {
    private final Map<String, Supplier<Command>> commands = new LinkedHashMap<String, Supplier<Command>>();

    public GoalCommand() {
        commands.put("start", new Supplier<Command>() {
            public Command get() { return new GoalStartCommand(); }
        });
        commands.put("next", new Supplier<Command>() {
            public Command get() { return new GoalNextCommand(); }
        });
        commands.put("step", new Supplier<Command>() {
            public Command get() { return new GoalStepCommand(); }
        });
        commands.put("status", new Supplier<Command>() {
            public Command get() { return new GoalStatusCommand(); }
        });
        commands.put("export", new Supplier<Command>() {
            public Command get() { return new GoalExportCommand(); }
        });
        commands.put("check", new Supplier<Command>() {
            public Command get() { return new GoalCheckCommand(); }
        });
        commands.put("evaluate", new Supplier<Command>() {
            public Command get() { return new GoalEvaluateCommand(); }
        });
        commands.put("verify", new Supplier<Command>() {
            public Command get() { return new GoalVerifyCommand(); }
        });
        commands.put("complete", new Supplier<Command>() {
            public Command get() { return new GoalCompleteCommand(); }
        });
        commands.put("resume", new Supplier<Command>() {
            public Command get() { return new GoalResumeCommand(); }
        });
    }

    public int run(CommandContext context, Args args) {
        Supplier<Command> command = commands.get(args.subCommand());
        if (command == null) {
            context.err().println("Unknown goal command: " + args.subCommand());
            context.err().println("Run `dhk help` for usage.");
            return ExitCodes.USAGE_ERROR;
        }
        return command.get().run(context, args);
    }
}
