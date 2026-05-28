package com.devharnesskit.dhk.guidance;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.util.JsonOutput;

public final class CommandErrorGuidance {
    private static final ActionableErrorRenderer RENDERER = new ActionableErrorRenderer();

    private CommandErrorGuidance() {
    }

    public static int missing(CommandContext context, Args args, String errorCode, String[] missing,
                              String nextCommand, String docs) {
        ActionableError error = ActionableError.builder(errorCode, "Missing required parameter")
                .reason("The command cannot run until the required parameter is provided.")
                .missing(missing)
                .nextCommand(nextCommand)
                .docs(docs)
                .build();
        render(context, args, error);
        return ExitCodes.USAGE_ERROR;
    }

    public static int invalidNumber(CommandContext context, Args args, String errorCode, String label,
                                    String value, String reason, String nextCommand, String docs) {
        ActionableError error = ActionableError.builder(errorCode, "Invalid " + label + ": " + safe(value))
                .reason(reason)
                .nextCommand(nextCommand)
                .docs(docs)
                .build();
        render(context, args, error);
        return ExitCodes.VALIDATION_ERROR;
    }

    public static int invalidKey(CommandContext context, Args args, String errorCode, String label,
                                 String value, String nextCommand, String docs) {
        ActionableError error = ActionableError.builder(errorCode, "Invalid " + label + ": " + safe(value))
                .reason(label + " may only contain letters, numbers, dot, underscore, or dash.")
                .nextCommand(nextCommand)
                .docs(docs)
                .build();
        render(context, args, error);
        return ExitCodes.VALIDATION_ERROR;
    }

    public static int notFound(CommandContext context, Args args, String errorCode, String label,
                               String value, String nextCommand, String docs) {
        ActionableError error = ActionableError.builder(errorCode, label + " not found: " + safe(value))
                .reason("The referenced " + label + " does not exist in the current project state.")
                .nextCommand(nextCommand)
                .nextAction("Create it first, choose an existing key, or inspect the current project state.")
                .docs(docs)
                .build();
        render(context, args, error);
        return ExitCodes.NOT_FOUND;
    }

    public static int invalidUsage(CommandContext context, Args args, String errorCode, String message,
                                   String reason, String nextCommand, String docs) {
        ActionableError error = ActionableError.builder(errorCode, message)
                .reason(reason)
                .nextCommand(nextCommand)
                .docs(docs)
                .build();
        render(context, args, error);
        return ExitCodes.USAGE_ERROR;
    }

    public static void render(CommandContext context, Args args, ActionableError error) {
        if (JsonOutput.enabled(args)) {
            context.err().print(RENDERER.renderJson(error));
        } else {
            context.err().print(RENDERER.renderText(error));
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
