package com.devharnesskit.dhk.command.memory;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;

public final class MemoryCommand implements Command {
    private final InitCommand initCommand;
    private final AddCommand addCommand;
    private final ConfirmCommand confirmCommand;
    private final SearchCommand searchCommand;
    private final ExportCommand exportCommand;
    private final CheckpointCommand checkpointCommand;
    private final RecoverCommand recoverCommand;

    public MemoryCommand() {
        this(new InitCommand(), new AddCommand(), new ConfirmCommand(), new SearchCommand(),
                new ExportCommand(), new CheckpointCommand(), new RecoverCommand());
    }

    MemoryCommand(InitCommand initCommand, AddCommand addCommand, ConfirmCommand confirmCommand,
                  SearchCommand searchCommand, ExportCommand exportCommand,
                  CheckpointCommand checkpointCommand, RecoverCommand recoverCommand) {
        this.initCommand = initCommand;
        this.addCommand = addCommand;
        this.confirmCommand = confirmCommand;
        this.searchCommand = searchCommand;
        this.exportCommand = exportCommand;
        this.checkpointCommand = checkpointCommand;
        this.recoverCommand = recoverCommand;
    }

    public int run(CommandContext context, Args args) {
        String subCommand = args.subCommand();
        if ("init".equals(subCommand)) {
            return initCommand.run(context, args);
        }
        if ("add".equals(subCommand)) {
            return addCommand.run(context, args);
        }
        if ("confirm".equals(subCommand)) {
            return confirmCommand.run(context, args);
        }
        if ("search".equals(subCommand)) {
            return searchCommand.run(context, args);
        }
        if ("export".equals(subCommand)) {
            return exportCommand.run(context, args);
        }
        if ("checkpoint".equals(subCommand)) {
            return checkpointCommand.run(context, args);
        }
        if ("recover".equals(subCommand)) {
            return recoverCommand.run(context, args);
        }
        context.err().println("Unknown memory command: " + subCommand);
        context.err().println("Run `dhk help` for usage.");
        return ExitCodes.USAGE_ERROR;
    }
}
