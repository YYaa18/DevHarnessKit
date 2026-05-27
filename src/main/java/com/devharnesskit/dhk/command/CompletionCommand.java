package com.devharnesskit.dhk.command;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.CliCommandCatalog;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;

import java.util.Map;

public final class CompletionCommand implements Command {
    public int run(CommandContext context, Args args) {
        String shell = args.subCommand();
        if ("bash".equals(shell)) {
            context.out().print(bash());
            return ExitCodes.SUCCESS;
        }
        if ("zsh".equals(shell)) {
            context.out().print(zsh());
            return ExitCodes.SUCCESS;
        }
        if ("fish".equals(shell)) {
            context.out().print(fish());
            return ExitCodes.SUCCESS;
        }
        context.err().println("Usage: dhk completion bash|zsh|fish");
        return ExitCodes.USAGE_ERROR;
    }

    private String bash() {
        StringBuilder builder = new StringBuilder();
        builder.append("_dhk_completion() {\n");
        builder.append("  local cur command words\n");
        builder.append("  COMPREPLY=()\n");
        builder.append("  cur=\"${COMP_WORDS[COMP_CWORD]}\"\n");
        builder.append("  command=\"${COMP_WORDS[1]}\"\n");
        builder.append("  if [[ ${COMP_CWORD} -eq 1 ]]; then\n");
        builder.append("    COMPREPLY=( $(compgen -W \"")
                .append(CliCommandCatalog.join(CliCommandCatalog.topLevelCommands()))
                .append("\" -- \"$cur\") )\n");
        builder.append("    return 0\n");
        builder.append("  fi\n");
        builder.append("  case \"$command\" in\n");
        for (Map.Entry<String, String[]> entry : CliCommandCatalog.subcommands().entrySet()) {
            builder.append("    ").append(entry.getKey()).append(") words=\"")
                    .append(CliCommandCatalog.join(entry.getValue())).append(' ')
                    .append(CliCommandCatalog.join(CliCommandCatalog.commonCommandOptions(entry.getKey())))
                    .append("\" ;;\n");
        }
        builder.append("    *) words=\"")
                .append(CliCommandCatalog.join(CliCommandCatalog.commonOptions())).append("\" ;;\n");
        builder.append("  esac\n");
        builder.append("  COMPREPLY=( $(compgen -W \"$words\" -- \"$cur\") )\n");
        builder.append("}\n");
        builder.append("complete -F _dhk_completion dhk\n");
        return builder.toString();
    }

    private String zsh() {
        StringBuilder builder = new StringBuilder();
        builder.append("#compdef dhk\n");
        builder.append("_dhk() {\n");
        builder.append("  local -a commands common\n");
        builder.append("  commands=(").append(zshWords(CliCommandCatalog.topLevelCommands())).append(")\n");
        builder.append("  common=(").append(zshWords(CliCommandCatalog.commonOptions())).append(")\n");
        builder.append("  if (( CURRENT == 2 )); then\n");
        builder.append("    _describe 'dhk command' commands\n");
        builder.append("    return\n");
        builder.append("  fi\n");
        builder.append("  case $words[2] in\n");
        for (Map.Entry<String, String[]> entry : CliCommandCatalog.subcommands().entrySet()) {
            builder.append("    ").append(entry.getKey()).append(")\n");
            builder.append("      local -a subcommands options\n");
            builder.append("      subcommands=(").append(zshWords(entry.getValue())).append(")\n");
            builder.append("      options=(").append(zshWords(CliCommandCatalog.commonCommandOptions(entry.getKey()))).append(")\n");
            builder.append("      _describe 'subcommand' subcommands && return\n");
            builder.append("      _describe 'option' options\n");
            builder.append("      ;;\n");
        }
        builder.append("    *) _describe 'option' common ;;\n");
        builder.append("  esac\n");
        builder.append("}\n");
        builder.append("compdef _dhk dhk\n");
        return builder.toString();
    }

    private String fish() {
        StringBuilder builder = new StringBuilder();
        builder.append("complete -c dhk -f\n");
        builder.append("complete -c dhk -n '__fish_use_subcommand' -a '")
                .append(CliCommandCatalog.join(CliCommandCatalog.topLevelCommands())).append("'\n");
        for (Map.Entry<String, String[]> entry : CliCommandCatalog.subcommands().entrySet()) {
            builder.append("complete -c dhk -n '__fish_seen_subcommand_from ")
                    .append(entry.getKey()).append("' -a '")
                    .append(CliCommandCatalog.join(entry.getValue())).append("'\n");
        }
        for (String option : CliCommandCatalog.commonOptions()) {
            builder.append("complete -c dhk -l ").append(option.substring(2)).append('\n');
        }
        return builder.toString();
    }

    private String zshWords(String[] values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append("'").append(value).append("'");
        }
        return builder.toString();
    }
}
