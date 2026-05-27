package com.devharnesskit.dhk.command.bdd;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.db.TransactionTemplate;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.bdd.BddQualityIssue;
import com.devharnesskit.dhk.repository.ProjectRepository;
import com.devharnesskit.dhk.repository.bdd.BddFeatureRepository;
import com.devharnesskit.dhk.repository.bdd.BddQualityIssueRepository;
import com.devharnesskit.dhk.repository.bdd.BddScenarioRepository;
import com.devharnesskit.dhk.repository.bdd.BddStepRepository;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.service.bdd.BddLintService;
import com.devharnesskit.dhk.service.bdd.BddService;
import com.devharnesskit.dhk.util.JsonOutput;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public final class BddLintCommand implements Command {
    private final DbConnectionFactory connectionFactory = new DbConnectionFactory();
    private final MigrationRunner migrationRunner = new MigrationRunner();
    private final ProjectService projectService = new ProjectService();
    private final ProjectRepository projectRepository = new ProjectRepository();
    private final TransactionTemplate transactionTemplate = new TransactionTemplate();
    private final BddLintService lintService = new BddLintService(
            new BddService(new BddFeatureRepository(), new BddScenarioRepository(), new BddStepRepository()),
            new BddQualityIssueRepository());

    public int run(CommandContext context, Args args) {
        final String featureFilter = args.option("feature", "").trim();
        Path projectRoot = BddCommandSupport.projectRoot(args, context);
        try (Connection connection = connectionFactory.open(projectRoot)) {
            final Project project = BddCommandSupport.ensureProject(context, projectRoot, connection,
                    projectService, projectRepository, migrationRunner);
            List<BddQualityIssue> issues = transactionTemplate.execute(connection,
                    new TransactionTemplate.Work<List<BddQualityIssue>>() {
                        public List<BddQualityIssue> execute() throws Exception {
                            return lintService.lint(connection, project, featureFilter,
                                    context.clock().now().toString());
                        }
                    });
            if (JsonOutput.enabled(args)) {
                context.out().print(JsonOutput.object(
                        JsonOutput.stringField("command", "bdd lint"),
                        JsonOutput.numberField("issue_count", issues.size()),
                        JsonOutput.rawField("issues", issuesJson(issues))
                ));
            } else {
                context.out().println("bdd lint complete");
                context.out().println("issues: " + issues.size());
                for (BddQualityIssue issue : issues) {
                    context.out().println("- [" + issue.severity() + "] "
                            + issue.scenarioKey() + " " + issue.issueType()
                            + " - " + issue.message());
                }
            }
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR bdd lint failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private String issuesJson(List<BddQualityIssue> issues) {
        List<String> values = new ArrayList<String>();
        for (BddQualityIssue issue : issues) {
            values.add(JsonOutput.object(
                    JsonOutput.stringField("scenario_key", issue.scenarioKey()),
                    JsonOutput.stringField("issue_type", issue.issueType()),
                    JsonOutput.stringField("severity", issue.severity()),
                    JsonOutput.stringField("message", issue.message())
            ).trim());
        }
        return JsonOutput.array(values);
    }
}
