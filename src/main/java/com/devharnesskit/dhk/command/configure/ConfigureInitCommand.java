package com.devharnesskit.dhk.command.configure;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.guidance.EnumGuidance;
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
        String preset = args.option("preset", "springboot-manual-ide-test");
        String compile = args.option("compile", "");
        String test = args.option("test", "");
        String graph = args.option("graph", "");
        if (!EnumGuidance.isConfigurePresetAllowed(preset)) {
            return EnumGuidance.printInvalid(context, args, "INVALID_CONFIGURE_PRESET",
                    "configure preset", preset, EnumGuidance.CONFIGURE_PRESETS, new String[]{"auto -> springboot-manual-ide-test"},
                    "dhk configure init --preset springboot-manual-ide-test", "docs/GOAL_CONFIGURATION.md");
        }
        if (!EnumGuidance.isVerificationModeAllowed(compile)) {
            return EnumGuidance.printInvalid(context, args, "INVALID_VERIFICATION_MODE",
                    "verification compile mode", compile, EnumGuidance.VERIFICATION_MODES, new String[0],
                    "dhk configure init --compile manual", "docs/GOAL_CONFIGURATION.md");
        }
        if (!EnumGuidance.isVerificationModeAllowed(test)) {
            return EnumGuidance.printInvalid(context, args, "INVALID_VERIFICATION_MODE",
                    "verification test mode", test, EnumGuidance.VERIFICATION_MODES, new String[0],
                    "dhk configure init --test manual", "docs/GOAL_CONFIGURATION.md");
        }
        if (!EnumGuidance.isGraphModeAllowed(graph)) {
            return EnumGuidance.printInvalid(context, args, "INVALID_GRAPH_MODE", "graph mode", graph,
                    EnumGuidance.GRAPH_MODES, new String[]{"optional -> advisory", "disabled -> off"},
                    "dhk configure init --graph advisory", "docs/GOAL_CONFIGURATION.md");
        }
        try {
            ConfigureInitResult result = service.init(projectRoot,
                    preset,
                    args.hasFlag("force"),
                    compile,
                    test,
                    graph,
                    args.option("target", ""),
                    args.hasFlag("dry-run"));
            if (JsonOutput.enabled(args)) {
                context.out().print(configureInitJson(result));
            } else {
                ConfigureCommandSupport.printConfig(context,
                        result.dryRun() ? "configure init dry-run" : "configure init complete",
                        result.configPath().toString(), result.config());
                context.out().println("dry_run: " + result.dryRun());
                context.out().println("would_write: " + result.wouldWrite());
                context.out().println("force_required: " + result.forceRequired());
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

    private String configureInitJson(ConfigureInitResult result) {
        String base = ConfigureCommandSupport.configJson(result.config(),
                result.dryRun() ? "configure init dry-run" : "configure init",
                result.configPath().toString());
        String trimmed = base.trim();
        if (trimmed.endsWith("}")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed + ",\n"
                + "  \"dry_run\": " + result.dryRun() + ",\n"
                + "  \"would_write\": " + result.wouldWrite() + ",\n"
                + "  \"force_required\": " + result.forceRequired() + ",\n"
                + "  \"config_created\": " + result.configCreated() + "\n"
                + "}\n";
    }
}
