package com.devharnesskit.dhk.command.bdd;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.repository.ProjectRepository;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.service.bdd.BddService;
import com.devharnesskit.dhk.repository.bdd.BddFeatureRepository;
import com.devharnesskit.dhk.repository.bdd.BddScenarioRepository;
import com.devharnesskit.dhk.repository.bdd.BddStepRepository;
import com.devharnesskit.dhk.util.JsonOutput;

import java.nio.file.Path;
import java.sql.Connection;

public final class BddInitCommand implements Command {
    private final DbConnectionFactory connectionFactory = new DbConnectionFactory();
    private final MigrationRunner migrationRunner = new MigrationRunner();
    private final ProjectService projectService = new ProjectService();
    private final ProjectRepository projectRepository = new ProjectRepository();
    private final BddService bddService = new BddService(new BddFeatureRepository(),
            new BddScenarioRepository(), new BddStepRepository());

    public int run(CommandContext context, Args args) {
        Path projectRoot = BddCommandSupport.projectRoot(args, context);
        try (Connection connection = connectionFactory.open(projectRoot)) {
            BddCommandSupport.ensureProject(context, projectRoot, connection,
                    projectService, projectRepository, migrationRunner);
            BddService.BddInitResult result = bddService.init(projectRoot);
            if (JsonOutput.enabled(args)) {
                context.out().print(JsonOutput.object(
                        JsonOutput.stringField("command", "bdd init"),
                        JsonOutput.stringField("bdd_dir", result.bddDirectory().toString()),
                        JsonOutput.stringField("features_dir", result.featuresDirectory().toString()),
                        JsonOutput.stringField("evidence_dir", result.evidenceDirectory().toString()),
                        JsonOutput.stringField("exports_dir", result.exportsDirectory().toString())
                ));
            } else {
                context.out().println("bdd init complete");
                context.out().println("bdd_dir: " + result.bddDirectory());
                context.out().println("features_dir: " + result.featuresDirectory());
                context.out().println("evidence_dir: " + result.evidenceDirectory());
                context.out().println("exports_dir: " + result.exportsDirectory());
            }
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR bdd init failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
