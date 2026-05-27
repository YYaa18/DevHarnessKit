package com.devharnesskit.dhk.command.configure;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.model.config.DevHarnessConfig;
import com.devharnesskit.dhk.service.config.DevHarnessConfigService;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;

public final class ConfigureShowCommand implements Command {
    private final DevHarnessConfigService service;

    public ConfigureShowCommand() {
        this(new DevHarnessConfigService());
    }

    ConfigureShowCommand(DevHarnessConfigService service) {
        this.service = service;
    }

    public int run(CommandContext context, Args args) {
        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        try {
            Path configPath = PathUtil.devharnessConfig(projectRoot);
            if (!service.hasConfig(projectRoot)) {
                context.err().println("ERROR configure show failed: config missing: " + configPath);
                return ExitCodes.NOT_FOUND;
            }
            DevHarnessConfig config = service.load(projectRoot);
            if (JsonOutput.enabled(args)) {
                context.out().print(ConfigureCommandSupport.configJson(config,
                        "configure show", configPath.toString()));
            } else {
                ConfigureCommandSupport.printConfig(context, "configure show",
                        configPath.toString(), config);
            }
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR configure show failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
