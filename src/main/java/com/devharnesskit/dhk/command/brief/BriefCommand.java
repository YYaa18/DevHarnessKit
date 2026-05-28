package com.devharnesskit.dhk.command.brief;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;

public final class BriefCommand implements Command {
    private final BriefAnswerCommand answerCommand;
    private final BriefShowCommand showCommand;
    private final BriefKnowledgeCommand knowledgeCommand;

    public BriefCommand() {
        this(new BriefAnswerCommand(), new BriefShowCommand(), new BriefKnowledgeCommand());
    }

    BriefCommand(BriefAnswerCommand answerCommand, BriefShowCommand showCommand,
                 BriefKnowledgeCommand knowledgeCommand) {
        this.answerCommand = answerCommand;
        this.showCommand = showCommand;
        this.knowledgeCommand = knowledgeCommand;
    }

    public int run(CommandContext context, Args args) {
        String subCommand = args.subCommand();
        if ("answer".equals(subCommand)) {
            return answerCommand.run(context, args);
        }
        if ("show".equals(subCommand)) {
            return showCommand.run(context, args);
        }
        if ("knowledge".equals(subCommand)) {
            return knowledgeCommand.run(context, args);
        }
        context.err().println("Unknown brief command: " + subCommand);
        context.err().println("Supported brief commands: answer, show, knowledge");
        return ExitCodes.USAGE_ERROR;
    }
}
