package com.devharnesskit.dhk.command;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;

public final class HelpCommand implements Command {
    public int run(CommandContext context, Args args) {
        context.out().println("DevHarness Kit CLI (dhk)");
        context.out().println();
        context.out().println("Usage:");
        context.out().println("  dhk help");
        context.out().println("  dhk doctor --project-root <path>");
        context.out().println("  dhk memory init --project-root <path>");
        context.out().println("  dhk memory add --type <type> --title <title> --content <content>");
        context.out().println("  dhk memory confirm --id <id>");
        context.out().println("  dhk memory search --q <query>");
        context.out().println("  dhk memory export --task <task>");
        context.out().println("  dhk memory checkpoint --task <task> --summary <summary>");
        context.out().println("  dhk memory recover --latest");
        context.out().println("  dhk db test --jdbc-url <url> --user <user> --password-env <env>");
        context.out().println("  dhk db sql --sql <sql> --password-env <env>");
        context.out().println("  dhk workflow template seed --project-root <path>");
        context.out().println("  dhk workflow template list --project-root <path>");
        context.out().println("  dhk workflow template show --key <workflow>");
        context.out().println("  dhk workflow start --workflow <workflow> --task <task>");
        context.out().println("  dhk workflow status --run <run-key>");
        context.out().println("  dhk workflow phase pass|fail --run <run-key> --phase <phase>");
        context.out().println("  dhk workflow gate pass|fail|waive --run <run-key> [--phase <phase>] --gate <gate>");
        context.out().println("  dhk workflow export --run <run-key>");
        context.out().println("  dhk workflow artifact list --run <run-key>");
        context.out().println("  dhk workflow bind-memory --run <run-key> --memory-id <id>");
        context.out().println("  dhk workflow bind-checkpoint --run <run-key> --checkpoint <id>");
        context.out().println("  dhk workflow summary --run <run-key>");
        context.out().println("  dhk spec create --change <key> --title <title>");
        context.out().println("  dhk spec document set --change <key> --type <type> --content <text>|--file <path>");
        context.out().println("  dhk spec task add|update --change <key> --task <task>");
        context.out().println("  dhk spec acceptance add|update --change <key> --acceptance <acceptance>");
        context.out().println("  dhk spec status --change <key>");
        context.out().println("  dhk spec export --change <key>");
        context.out().println("  dhk spec bind-workflow --change <key> --run <run-key>");
        context.out().println("  dhk spec archive --change <key> --reason <reason>");
        context.out().println();
        context.out().println("Exit codes:");
        context.out().println("  0 success");
        context.out().println("  1 runtime error");
        context.out().println("  2 usage error");
        context.out().println("  3 validation or safety rejection");
        context.out().println("  4 target not found");
        return ExitCodes.SUCCESS;
    }
}
