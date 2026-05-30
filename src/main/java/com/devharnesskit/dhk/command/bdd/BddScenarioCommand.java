package com.devharnesskit.dhk.command.bdd;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.guidance.CommandErrorGuidance;

import java.util.ArrayList;
import java.util.List;

public final class BddScenarioCommand implements Command {
    public int run(CommandContext context, Args args) {
        String action = args.positional(2);
        if ("create".equals(action) || "add".equals(action)) {
            return create(context, args);
        }
        if ("list".equals(action)) {
            return new BddListCommand().run(context, args);
        }
        if ("show".equals(action)) {
            return show(context, args);
        }
        if (action.length() == 0 || "help".equals(action)) {
            printHelp(context);
            return ExitCodes.SUCCESS;
        }
        context.err().println("Unknown bdd scenario action: " + action);
        context.err().println("Run `dhk bdd scenario help` for usage.");
        return ExitCodes.USAGE_ERROR;
    }

    private int create(CommandContext context, Args args) {
        String scenario = value(args.option("scenario"), args.positional(3));
        String title = args.option("title").trim();
        if (scenario.length() == 0 || title.length() == 0) {
            return CommandErrorGuidance.missing(context, args, "BDD_SCENARIO_CREATE_ARGUMENTS_MISSING",
                    new String[]{"--scenario", "--title"},
                    "dhk bdd scenario create --scenario <scenario> --title \"<title>\"",
                    "docs/GOAL_CONFIGURATION.md");
        }

        String feature = value(args.option("feature"), derivedFeature(scenario, args.option("module", "")));
        String featureTitle = value(args.option("feature-title"), title);
        List<String> raw = new ArrayList<String>();
        raw.add("bdd");
        raw.add("add");
        addOption(raw, "project-root", args.option("project-root", ""));
        addOption(raw, "feature", feature);
        addOption(raw, "title", featureTitle);
        addOption(raw, "description", args.option("feature-description", ""));
        addOption(raw, "scenario", scenario);
        addOption(raw, "scenario-title", title);
        addOption(raw, "scenario-description", value(args.option("scenario-description", ""),
                args.option("description", "")));
        addOption(raw, "module", args.option("module", ""));
        addOption(raw, "type", args.option("type", ""));
        addOption(raw, "priority", args.option("priority", ""));
        addOption(raw, "status", args.option("status", ""));
        addOption(raw, "tags", args.option("tags", ""));
        addOption(raw, "given", args.option("given", ""));
        addOption(raw, "when", args.option("when", ""));
        addOption(raw, "then", args.option("then", ""));
        addOption(raw, "and", args.option("and", ""));
        if (args.hasFlag("json")) {
            raw.add("--json");
        }
        return new BddAddCommand().run(context, Args.parse(raw.toArray(new String[raw.size()])));
    }

    private int show(CommandContext context, Args args) {
        if (args.option("scenario", "").trim().length() > 0
                || args.option("feature", "").trim().length() > 0) {
            return new BddShowCommand().run(context, args);
        }
        String scenario = args.positional(3);
        if (scenario.length() == 0) {
            return CommandErrorGuidance.missing(context, args, "BDD_SCENARIO_SHOW_TARGET_MISSING",
                    new String[]{"--scenario"},
                    "dhk bdd scenario show --scenario <scenario>",
                    "docs/GOAL_CONFIGURATION.md");
        }
        List<String> raw = new ArrayList<String>();
        raw.add("bdd");
        raw.add("show");
        addOption(raw, "project-root", args.option("project-root", ""));
        addOption(raw, "scenario", scenario);
        if (args.hasFlag("json")) {
            raw.add("--json");
        }
        return new BddShowCommand().run(context, Args.parse(raw.toArray(new String[raw.size()])));
    }

    private void printHelp(CommandContext context) {
        context.out().println("BDD scenario commands:");
        context.out().println("  dhk bdd scenario create --scenario <key> --title <title> [--given <text>] [--when <text>] [--then <text>]");
        context.out().println("  dhk bdd scenario list [--feature <key>]");
        context.out().println("  dhk bdd scenario show --scenario <key>");
        context.out().println("  Optional create fields: --feature <key>, --feature-title <title>, --module <module>, --tags <tags>");
    }

    private void addOption(List<String> raw, String key, String value) {
        String text = value == null ? "" : value.trim();
        if (text.length() == 0) {
            return;
        }
        raw.add("--" + key);
        raw.add(text);
    }

    private String value(String preferred, String fallback) {
        String text = preferred == null ? "" : preferred.trim();
        if (text.length() > 0) {
            return text;
        }
        return fallback == null ? "" : fallback.trim();
    }

    private String derivedFeature(String scenario, String module) {
        String moduleText = module == null ? "" : module.trim();
        if (moduleText.length() > 0 && !"global".equals(moduleText)) {
            return moduleText;
        }
        int index = scenario.lastIndexOf('-');
        if (index > 0) {
            return scenario.substring(0, index);
        }
        return "bdd";
    }
}
