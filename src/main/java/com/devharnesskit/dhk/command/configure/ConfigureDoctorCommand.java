package com.devharnesskit.dhk.command.configure;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.service.config.DevHarnessConfigService;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ConfigureDoctorCommand implements Command {
    private final DevHarnessConfigService service;

    public ConfigureDoctorCommand() {
        this(new DevHarnessConfigService());
    }

    ConfigureDoctorCommand(DevHarnessConfigService service) {
        this.service = service;
    }

    public int run(CommandContext context, Args args) {
        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        Path configPath = PathUtil.devharnessConfig(projectRoot);
        List<String> warnings = service.diagnose(projectRoot);
        boolean missing = !service.hasConfig(projectRoot);
        if (JsonOutput.enabled(args)) {
            List<String> quoted = new ArrayList<String>();
            for (String warning : warnings) {
                quoted.add(JsonOutput.quote(warning));
            }
            context.out().print(JsonOutput.object(
                    JsonOutput.stringField("command", "configure doctor"),
                    JsonOutput.stringField("config_path", configPath.toString()),
                    JsonOutput.stringField("status", warnings.isEmpty() ? "ok" : (missing ? "missing" : "warning")),
                    JsonOutput.rawField("warnings", JsonOutput.array(quoted))
            ));
        } else {
            context.out().println("configure doctor");
            context.out().println("config_path: " + configPath);
            context.out().println("status: " + (warnings.isEmpty() ? "ok" : (missing ? "missing" : "warning")));
            if (warnings.isEmpty()) {
                context.out().println("warnings: none");
            } else {
                context.out().println("warnings:");
                for (String warning : warnings) {
                    context.out().println("  - " + warning);
                }
            }
        }
        if (warnings.isEmpty()) {
            return ExitCodes.SUCCESS;
        }
        return missing ? ExitCodes.NOT_FOUND : ExitCodes.VALIDATION_ERROR;
    }
}
