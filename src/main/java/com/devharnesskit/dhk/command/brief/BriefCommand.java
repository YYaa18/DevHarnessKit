package com.devharnesskit.dhk.command.brief;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;

public final class BriefCommand implements Command {
    private final BriefAnswerCommand answerCommand;
    private final BriefKnowledgeCommand knowledgeCommand;

    public BriefCommand() {
        this(new BriefAnswerCommand(), new BriefKnowledgeCommand());
    }

    BriefCommand(BriefAnswerCommand answerCommand, BriefKnowledgeCommand knowledgeCommand) {
        this.answerCommand = answerCommand;
        this.knowledgeCommand = knowledgeCommand;
    }

    public int run(CommandContext context, Args args) {
        String subCommand = args.subCommand();
        if ("answer".equals(subCommand)) {
            return answerCommand.run(context, args);
        }
        if ("knowledge".equals(subCommand)) {
            return knowledgeCommand.run(context, args);
        }
        context.err().println("Unknown brief command: " + subCommand);
        context.err().println("Supported brief commands: answer, knowledge");
        return ExitCodes.USAGE_ERROR;
    }
}
