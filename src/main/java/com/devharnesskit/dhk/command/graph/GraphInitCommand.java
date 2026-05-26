package com.devharnesskit.dhk.command.graph;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.model.graph.GraphInitResult;
import com.devharnesskit.dhk.service.graph.GraphService;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;

import java.io.IOException;
import java.nio.file.Path;

public final class GraphInitCommand implements Command {
    private final GraphService graphService;

    public GraphInitCommand() {
        this(new GraphService());
    }

    GraphInitCommand(GraphService graphService) {
        this.graphService = graphService;
    }

    public int run(CommandContext context, Args args) {
        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        try {
            GraphInitResult result = graphService.init(projectRoot);
            if (JsonOutput.enabled(args)) {
                context.out().print(JsonOutput.object(
                        JsonOutput.stringField("command", "graph init"),
                        JsonOutput.stringField("config_path", result.configPath().toString()),
                        JsonOutput.stringField("exports_dir", result.exportsDirectory().toString()),
                        JsonOutput.booleanField("config_created", result.configCreated())
                ));
            } else {
                context.out().println("graph init complete");
                context.out().println("config_path: " + result.configPath());
                context.out().println("exports_dir: " + result.exportsDirectory());
                context.out().println("config_created: " + result.configCreated());
            }
            return ExitCodes.SUCCESS;
        } catch (IOException | RuntimeException ex) {
            context.err().println("ERROR graph init failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
