package com.devharnesskit.dhk.command.graph;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.model.graph.GraphDoctorResult;
import com.devharnesskit.dhk.service.graph.GraphDoctorService;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;

public final class GraphDoctorCommand implements Command {
    private final GraphDoctorService doctorService;

    public GraphDoctorCommand() {
        this(new GraphDoctorService());
    }

    GraphDoctorCommand(GraphDoctorService doctorService) {
        this.doctorService = doctorService;
    }

    public int run(CommandContext context, Args args) {
        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        try {
            GraphDoctorResult result = doctorService.diagnose(projectRoot);
            if (JsonOutput.enabled(args)) {
                context.out().print(JsonOutput.object(
                        JsonOutput.stringField("command", "graph doctor"),
                        JsonOutput.stringField("config_path", result.configPath().toString()),
                        JsonOutput.stringField("config_source", result.configSource()),
                        JsonOutput.stringField("provider", result.provider()),
                        JsonOutput.stringField("cgc_command", result.cgcCommand()),
                        JsonOutput.booleanField("cgc_required", result.cgcRequired()),
                        JsonOutput.booleanField("cgc_available", result.cgcAvailable()),
                        JsonOutput.stringField("cgc_status", result.cgcStatus()),
                        JsonOutput.stringField("cgc_detail", result.cgcDetail()),
                        JsonOutput.stringField("adapter_contract", "optional alpha normalization into DevHarness graph model"),
                        JsonOutput.booleanField("default_provider_unaffected", result.defaultProviderUnaffected())
                ));
            } else {
                context.out().println("graph doctor");
                context.out().println("config_path: " + result.configPath());
                context.out().println("config_source: " + result.configSource());
                context.out().println("provider: " + result.provider());
                context.out().println("cgc_command: " + result.cgcCommand());
                context.out().println("cgc_required: " + result.cgcRequired());
                context.out().println("cgc_available: " + result.cgcAvailable());
                context.out().println("cgc_status: " + result.cgcStatus());
                context.out().println("cgc_detail: " + result.cgcDetail());
                context.out().println("adapter_contract: optional alpha normalization into DevHarness graph model");
                context.out().println("default_provider_unaffected: " + result.defaultProviderUnaffected());
            }
            return ExitCodes.SUCCESS;
        } catch (RuntimeException ex) {
            context.err().println("ERROR graph doctor failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
