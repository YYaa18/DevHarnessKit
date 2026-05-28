package com.devharnesskit.dhk.command.brief;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.model.brief.InteractionRequest;
import com.devharnesskit.dhk.service.brief.BriefLifecycleService;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;

public final class BriefShowCommand implements Command {
    private final BriefLifecycleService lifecycleService = new BriefLifecycleService();

    public int run(CommandContext context, Args args) {
        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        String requestId = args.option("request", "").trim();
        if (requestId.length() == 0) {
            context.err().println("Missing required parameter: --request");
            context.err().println("next_command: dhk brief show --request <request-id>");
            return ExitCodes.USAGE_ERROR;
        }
        InteractionRequest request = lifecycleService.findInteraction(projectRoot, requestId);
        if (request == null) {
            context.err().println("Interaction request not found: " + requestId);
            return ExitCodes.NOT_FOUND;
        }
        String[] choices = splitChoices(request.choices());
        if (JsonOutput.enabled(args)) {
            context.out().print(JsonOutput.object(
                    JsonOutput.stringField("command", "brief show"),
                    JsonOutput.stringField("request_id", request.requestId()),
                    JsonOutput.stringField("goal_key", request.goalKey()),
                    JsonOutput.stringField("type", request.type()),
                    JsonOutput.stringField("priority", request.priority()),
                    JsonOutput.stringField("question", request.question()),
                    JsonOutput.stringField("why", request.why()),
                    JsonOutput.rawField("valid_choices", JsonOutput.stringArray(choices)),
                    JsonOutput.stringField("default_choice", request.defaultChoice()),
                    JsonOutput.booleanField("blocks_progress", request.blocksProgress()),
                    JsonOutput.stringField("status", request.status()),
                    JsonOutput.stringField("answer", request.answer()),
                    JsonOutput.stringField("next_command",
                            "dhk brief answer --request " + request.requestId() + " --choice \"<choice>\"")
            ));
        } else {
            context.out().println("request_id: " + request.requestId());
            context.out().println("goal_key: " + valueOrNone(request.goalKey()));
            context.out().println("type: " + request.type());
            context.out().println("priority: " + request.priority());
            context.out().println("question: " + request.question());
            context.out().println("why: " + request.why());
            context.out().println("valid_choices:");
            for (String choice : choices) {
                context.out().println("  - " + choice);
            }
            context.out().println("default_choice: " + request.defaultChoice());
            context.out().println("blocks_progress: " + request.blocksProgress());
            context.out().println("status: " + request.status());
            context.out().println("answer: " + valueOrNone(request.answer()));
            context.out().println("next_command: dhk brief answer --request "
                    + request.requestId() + " --choice \"<choice>\"");
        }
        return ExitCodes.SUCCESS;
    }

    private String[] splitChoices(String text) {
        if (text == null || text.trim().length() == 0) {
            return new String[0];
        }
        String[] raw = text.split("\\|");
        java.util.List<String> result = new java.util.ArrayList<String>();
        for (String item : raw) {
            String value = item.trim();
            if (value.length() > 0) {
                result.add(value);
            }
        }
        return result.toArray(new String[result.size()]);
    }

    private String valueOrNone(String value) {
        return value == null || value.length() == 0 ? "none" : value;
    }
}
