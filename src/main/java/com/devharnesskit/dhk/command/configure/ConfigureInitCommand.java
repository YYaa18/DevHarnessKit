package com.devharnesskit.dhk.command.configure;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.model.config.ConfigureInitResult;
import com.devharnesskit.dhk.service.config.DevHarnessConfigService;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;

public final class ConfigureInitCommand implements Command {
    private final DevHarnessConfigService service;

    public ConfigureInitCommand() {
        this(new DevHarnessConfigService());
    }

    ConfigureInitCommand(DevHarnessConfigService service) {
        this.service = service;
    }

    public int run(CommandContext context, Args args) {
        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        try {
            ConfigureInitResult result = service.init(projectRoot,
                    args.option("preset", "springboot-manual-ide-test"),
                    args.hasFlag("force"),
                    args.option("compile", ""),
                    args.option("test", ""),
                    args.option("graph", ""),
                    args.option("target", ""));
            if (JsonOutput.enabled(args)) {
                context.out().print(ConfigureCommandSupport.configJson(result.config(),
                        "configure init", result.configPath().toString()));
            } else {
                ConfigureCommandSupport.printConfig(context, "configure init complete",
                        result.configPath().toString(), result.config());
                context.out().println("config_created: " + result.configCreated());
            }
            return ExitCodes.SUCCESS;
        } catch (IllegalArgumentException ex) {
            context.err().println("ERROR configure init rejected: " + ex.getMessage());
            return ExitCodes.USAGE_ERROR;
        } catch (Exception ex) {
            context.err().println("ERROR configure init failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
