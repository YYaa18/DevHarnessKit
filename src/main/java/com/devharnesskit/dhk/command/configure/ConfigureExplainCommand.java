package com.devharnesskit.dhk.command.configure;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.service.config.DevHarnessConfigService;
import com.devharnesskit.dhk.util.JsonOutput;

public final class ConfigureExplainCommand implements Command {
    private final DevHarnessConfigService service;

    public ConfigureExplainCommand() {
        this(new DevHarnessConfigService());
    }

    ConfigureExplainCommand(DevHarnessConfigService service) {
        this.service = service;
    }

    public int run(CommandContext context, Args args) {
        String key = args.option("key", "");
        if (key.length() == 0 && args.positionals().size() > 2) {
            key = args.positionals().get(2);
        }
        String explanation = service.explain(key);
        if (JsonOutput.enabled(args)) {
            context.out().print(JsonOutput.object(
                    JsonOutput.stringField("command", "configure explain"),
                    JsonOutput.stringField("key", key),
                    JsonOutput.stringField("explanation", explanation)
            ));
        } else {
            context.out().println("configure explain");
            context.out().println("key: " + (key.length() == 0 ? "all" : key));
            context.out().println("explanation: " + explanation);
        }
        return key.length() == 0 || explanation.indexOf("Unknown configure key") < 0
                ? ExitCodes.SUCCESS : ExitCodes.USAGE_ERROR;
    }
}
