package com.devharnesskit.dhk.command.context;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.context.ContextBudget;
import com.devharnesskit.dhk.context.ContextBudgetPolicy;
import com.devharnesskit.dhk.context.artifact.ContextArtifactRepository;
import com.devharnesskit.dhk.context.artifact.ContextArtifactStats;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.service.config.DevHarnessConfigService;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public final class ContextStatsCommand implements Command {
    private final DbConnectionFactory connectionFactory = new DbConnectionFactory();
    private final ContextArtifactRepository repository = new ContextArtifactRepository();
    private final DevHarnessConfigService configService = new DevHarnessConfigService();

    public int run(CommandContext context, Args args) {
        Path projectRoot = ContextCommandSupport.projectRoot(args, context);
        String goalKey = args.option("goal", "").trim();
        if (!ContextCommandSupport.requireProjectJson(context, projectRoot)) {
            return ExitCodes.NOT_FOUND;
        }
        try (Connection connection = connectionFactory.open(projectRoot)) {
            Project project = ContextCommandSupport.requireProject(context, projectRoot, connection);
            if (project == null) {
                return ExitCodes.NOT_FOUND;
            }
            ContextArtifactStats stats = repository.stats(connection, project.projectKey(), goalKey);
            ContextBudget budget = ContextBudgetPolicy.fromConfig(configService.load(projectRoot));
            if (JsonOutput.enabled(args)) {
                context.out().print(JsonOutput.object(
                        JsonOutput.stringField("command", "context stats"),
                        JsonOutput.stringField("project_key", project.projectKey()),
                        JsonOutput.stringField("goal_key", goalKey),
                        JsonOutput.numberField("artifact_count", stats.artifactCount()),
                        JsonOutput.numberField("token_before", stats.tokenBefore()),
                        JsonOutput.numberField("token_after", stats.tokenAfter()),
                        JsonOutput.numberField("token_reduction_percent", stats.reductionPercent()),
                        JsonOutput.numberField("omitted_lines", stats.omittedLines()),
                        JsonOutput.rawField("sections", JsonOutput.array(sectionJson(budget))),
                        JsonOutput.rawField("compressed_artifacts", JsonOutput.array(sourceTypeJson(stats))),
                        JsonOutput.rawField("risks", JsonOutput.stringArray(risks(stats))),
                        JsonOutput.stringField("artifacts_dir", PathUtil.displayPath(PathUtil.contextArtifactsDirectory(projectRoot)))
                ));
                return ExitCodes.SUCCESS;
            }
            context.out().println("CURRENT_CONTEXT.md");
            context.out().println("  before: " + stats.tokenBefore() + " est tokens");
            context.out().println("  after: " + stats.tokenAfter() + " est tokens");
            context.out().println("  reduction: " + stats.reductionPercent() + "%");
            context.out().println("sections:");
            printSection(context, "goal", budget.goalTokens());
            printSection(context, "current_step", budget.currentStepTokens());
            printSection(context, "memory", budget.memoryTokens());
            printSection(context, "evidence", budget.evidenceTokens());
            printSection(context, "graph", budget.graphTokens());
            printSection(context, "bdd", budget.bddTokens());
            printSection(context, "risks", budget.risksTokens());
            context.out().println("compressed_artifacts:");
            if (stats.sourceTypes().isEmpty()) {
                context.out().println("  none");
            } else {
                for (ContextArtifactStats.SourceTypeStats source : stats.sourceTypes()) {
                    context.out().println("  " + source.sourceType() + ": " + source.artifactCount()
                            + " (" + source.tokenBefore() + " -> " + source.tokenAfter()
                            + ", omitted_lines=" + source.omittedLines() + ")");
                }
            }
            context.out().println("risks:");
            printRisks(context, risks(stats));
            context.out().println("context artifacts: " + stats.artifactCount());
            context.out().println("goal_key: " + (goalKey.length() == 0 ? "all" : goalKey));
            context.out().println("token_before: " + stats.tokenBefore());
            context.out().println("token_after: " + stats.tokenAfter());
            context.out().println("token_reduction_percent: " + stats.reductionPercent());
            context.out().println("omitted_lines: " + stats.omittedLines());
            context.out().println("artifacts_dir: " + PathUtil.contextArtifactsDirectory(projectRoot));
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR context stats failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private void printSection(CommandContext context, String name, int budgetTokens) {
        context.out().println("  " + name + ": estimated/ " + budgetTokens + " tokens");
    }

    private void printRisks(CommandContext context, String[] risks) {
        if (risks.length == 0) {
            context.out().println("  - none");
            return;
        }
        for (String risk : risks) {
            context.out().println("  - " + risk);
        }
    }

    private List<String> sectionJson(ContextBudget budget) {
        List<String> sections = new ArrayList<String>();
        sections.add(section("goal", budget.goalTokens()));
        sections.add(section("current_step", budget.currentStepTokens()));
        sections.add(section("memory", budget.memoryTokens()));
        sections.add(section("evidence", budget.evidenceTokens()));
        sections.add(section("graph", budget.graphTokens()));
        sections.add(section("bdd", budget.bddTokens()));
        sections.add(section("risks", budget.risksTokens()));
        return sections;
    }

    private String section(String section, int budgetTokens) {
        return JsonOutput.object(
                JsonOutput.stringField("section", section),
                JsonOutput.numberField("budget_tokens", budgetTokens),
                JsonOutput.booleanField("estimated", true)
        ).trim();
    }

    private List<String> sourceTypeJson(ContextArtifactStats stats) {
        List<String> raw = new ArrayList<String>();
        for (ContextArtifactStats.SourceTypeStats source : stats.sourceTypes()) {
            raw.add(JsonOutput.object(
                    JsonOutput.stringField("source_type", source.sourceType()),
                    JsonOutput.numberField("artifact_count", source.artifactCount()),
                    JsonOutput.numberField("token_before", source.tokenBefore()),
                    JsonOutput.numberField("token_after", source.tokenAfter()),
                    JsonOutput.numberField("omitted_lines", source.omittedLines()),
                    JsonOutput.numberField("degraded_count", source.degradedCount())
            ).trim());
        }
        return raw;
    }

    private String[] risks(ContextArtifactStats stats) {
        List<String> risks = new ArrayList<String>();
        for (ContextArtifactStats.SourceTypeStats source : stats.sourceTypes()) {
            if (source.omittedLines() > 0) {
                risks.add(source.artifactCount() + " " + source.sourceType()
                        + " artifact(s) compressed; omitted_lines=" + source.omittedLines());
            }
            if (source.degradedCount() > 0) {
                risks.add(source.degradedCount() + " " + source.sourceType()
                        + " artifact(s) used degraded fallback compression");
            }
        }
        return risks.toArray(new String[risks.size()]);
    }
}
