package com.devharnesskit.dhk.command.memory;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;

public final class MemoryCommand implements Command {
    private final InitCommand initCommand;
    private final AddCommand addCommand;
    private final ConfirmCommand confirmCommand;
    private final ListCommand listCommand;
    private final SearchCommand searchCommand;
    private final ExportCommand exportCommand;
    private final CheckpointCommand checkpointCommand;
    private final RecoverCommand recoverCommand;
    private final BackupCommand backupCommand;
    private final SuggestCommand suggestCommand;
    private final CandidatesCommand candidatesCommand;
    private final DedupeCommand dedupeCommand;
    private final ConflictsCommand conflictsCommand;
    private final SupersedeCommand supersedeCommand;
    private final StaleCommand staleCommand;
    private final ExpireCommand expireCommand;
    private final RefreshCommand refreshCommand;
    private final PackCommand packCommand;

    public MemoryCommand() {
        this(new InitCommand(), new AddCommand(), new ConfirmCommand(), new ListCommand(), new SearchCommand(),
                new ExportCommand(), new CheckpointCommand(), new RecoverCommand(), new BackupCommand(),
                new SuggestCommand(), new CandidatesCommand(), new DedupeCommand(), new ConflictsCommand(),
                new SupersedeCommand(), new StaleCommand(), new ExpireCommand(), new RefreshCommand(),
                new PackCommand());
    }

    MemoryCommand(InitCommand initCommand, AddCommand addCommand, ConfirmCommand confirmCommand,
                  ListCommand listCommand, SearchCommand searchCommand, ExportCommand exportCommand,
                  CheckpointCommand checkpointCommand, RecoverCommand recoverCommand,
                  BackupCommand backupCommand, SuggestCommand suggestCommand,
                  CandidatesCommand candidatesCommand, DedupeCommand dedupeCommand,
                  ConflictsCommand conflictsCommand, SupersedeCommand supersedeCommand,
                  StaleCommand staleCommand, ExpireCommand expireCommand,
                  RefreshCommand refreshCommand, PackCommand packCommand) {
        this.initCommand = initCommand;
        this.addCommand = addCommand;
        this.confirmCommand = confirmCommand;
        this.listCommand = listCommand;
        this.searchCommand = searchCommand;
        this.exportCommand = exportCommand;
        this.checkpointCommand = checkpointCommand;
        this.recoverCommand = recoverCommand;
        this.backupCommand = backupCommand;
        this.suggestCommand = suggestCommand;
        this.candidatesCommand = candidatesCommand;
        this.dedupeCommand = dedupeCommand;
        this.conflictsCommand = conflictsCommand;
        this.supersedeCommand = supersedeCommand;
        this.staleCommand = staleCommand;
        this.expireCommand = expireCommand;
        this.refreshCommand = refreshCommand;
        this.packCommand = packCommand;
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
        if ("list".equals(subCommand)) {
            return listCommand.run(context, args);
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
        if ("backup".equals(subCommand)) {
            return backupCommand.run(context, args);
        }
        if ("suggest".equals(subCommand)) {
            return suggestCommand.run(context, args);
        }
        if ("candidates".equals(subCommand)) {
            return candidatesCommand.run(context, args);
        }
        if ("dedupe".equals(subCommand)) {
            return dedupeCommand.run(context, args);
        }
        if ("conflicts".equals(subCommand)) {
            return conflictsCommand.run(context, args);
        }
        if ("supersede".equals(subCommand)) {
            return supersedeCommand.run(context, args);
        }
        if ("stale".equals(subCommand)) {
            return staleCommand.run(context, args);
        }
        if ("expire".equals(subCommand)) {
            return expireCommand.run(context, args);
        }
        if ("refresh".equals(subCommand)) {
            return refreshCommand.run(context, args);
        }
        if ("pack".equals(subCommand)) {
            return packCommand.run(context, args);
        }
        context.err().println("Unknown memory command: " + subCommand);
        context.err().println("Run `dhk help` for usage.");
        return ExitCodes.USAGE_ERROR;
    }
}
