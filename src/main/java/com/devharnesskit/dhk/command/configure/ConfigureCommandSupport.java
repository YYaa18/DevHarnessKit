package com.devharnesskit.dhk.command.configure;

import com.devharnesskit.dhk.model.config.DevHarnessConfig;
import com.devharnesskit.dhk.util.JsonOutput;

final class ConfigureCommandSupport {
    private ConfigureCommandSupport() {
    }

    static String configJson(DevHarnessConfig config, String command, String configPath) {
        return JsonOutput.object(
                JsonOutput.stringField("command", command),
                JsonOutput.stringField("config_path", configPath),
                JsonOutput.stringField("schema_version", config.schemaVersion()),
                JsonOutput.stringField("preset", config.preset()),
                JsonOutput.stringField("project_type", config.projectType()),
                JsonOutput.stringField("module_style", config.moduleStyle()),
                JsonOutput.stringField("runtime", config.runtime()),
                JsonOutput.stringField("compile_mode", config.compileMode()),
                JsonOutput.stringField("compile_command", config.compileCommand()),
                JsonOutput.stringField("compile_trigger", config.compileTrigger()),
                JsonOutput.stringField("test_mode", config.testMode()),
                JsonOutput.stringField("test_command", config.testCommand()),
                JsonOutput.stringField("test_trigger", config.testTrigger()),
                JsonOutput.stringField("test_cost", config.testCost()),
                JsonOutput.booleanField("graph_required", config.graphRequired()),
                JsonOutput.booleanField("graph_fresh_snapshot_required", config.graphFreshSnapshotRequired()),
                JsonOutput.booleanField("impact_map_required", config.impactMapRequired()),
                JsonOutput.booleanField("allow_stale_requires_approval", config.allowStaleRequiresApproval())
        );
    }

    static void printConfig(com.devharnesskit.dhk.cli.CommandContext context,
                            String heading, String configPath, DevHarnessConfig config) {
        context.out().println(heading);
        context.out().println("config_path: " + configPath);
        context.out().println("schema_version: " + config.schemaVersion());
        context.out().println("preset: " + config.preset());
        context.out().println("project_type: " + config.projectType());
        context.out().println("module_style: " + config.moduleStyle());
        context.out().println("runtime: " + config.runtime());
        context.out().println("compile_mode: " + config.compileMode());
        context.out().println("compile_command: " + config.compileCommand());
        context.out().println("compile_trigger: " + config.compileTrigger());
        context.out().println("test_mode: " + config.testMode());
        context.out().println("test_command: " + config.testCommand());
        context.out().println("test_trigger: " + config.testTrigger());
        context.out().println("test_cost: " + config.testCost());
        context.out().println("graph_required: " + config.graphRequired());
        context.out().println("graph_fresh_snapshot_required: " + config.graphFreshSnapshotRequired());
        context.out().println("impact_map_required: " + config.impactMapRequired());
        context.out().println("allow_stale_requires_approval: " + config.allowStaleRequiresApproval());
    }
}
