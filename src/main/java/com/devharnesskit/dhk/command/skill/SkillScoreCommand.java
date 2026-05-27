package com.devharnesskit.dhk.command.skill;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.model.skill.SkillQualityScore;
import com.devharnesskit.dhk.repository.goal.GoalCheckRepository;
import com.devharnesskit.dhk.repository.goal.GoalRunRepository;
import com.devharnesskit.dhk.repository.goal.GoalStepRepository;
import com.devharnesskit.dhk.service.skill.SkillQualityScoreService;
import com.devharnesskit.dhk.util.JsonOutput;

import java.nio.file.Path;
import java.sql.Connection;

public final class SkillScoreCommand implements Command {
    private final DbConnectionFactory connectionFactory = new DbConnectionFactory();
    private final MigrationRunner migrationRunner = new MigrationRunner();
    private final GoalRunRepository goalRunRepository = new GoalRunRepository();
    private final GoalStepRepository goalStepRepository = new GoalStepRepository();
    private final GoalCheckRepository goalCheckRepository = new GoalCheckRepository();
    private final SkillQualityScoreService scoreService = new SkillQualityScoreService();

    public int run(CommandContext context, Args args) {
        String goalKey = args.option("goal", "").trim();
        if (goalKey.length() == 0) {
            context.err().println("Missing required parameter: --goal <goal-key>");
            return ExitCodes.USAGE_ERROR;
        }
        Path projectRoot = SkillCommandSupport.projectRoot(args, context);
        try (Connection connection = connectionFactory.open(projectRoot)) {
            migrationRunner.migrate(connection, context.clock());
            GoalRun goal = goalRunRepository.findByKey(connection, goalKey);
            if (goal == null) {
                context.err().println("Goal not found: " + goalKey);
                return ExitCodes.NOT_FOUND;
            }
            SkillQualityScore score = scoreService.score(goal.goalKey(),
                    goalStepRepository.listByGoal(connection, goal.goalKey()),
                    goalCheckRepository.listByGoal(connection, goal.goalKey()));
            if (JsonOutput.enabled(args)) {
                printJson(context, score);
            } else {
                printText(context, score);
            }
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR skill score failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private void printText(CommandContext context, SkillQualityScore score) {
        context.out().println("skill quality score complete");
        context.out().println("goal_key: " + score.goalKey());
        context.out().println("score: " + score.score());
        context.out().println("decision: " + score.decision());
        context.out().println("gate_pass_rate: " + score.gatePassRate());
        context.out().println("evidence_completeness: " + score.evidenceCompleteness());
        context.out().println("rollback_quality: " + score.rollbackQuality());
        context.out().println("summary: " + score.summary());
    }

    private void printJson(CommandContext context, SkillQualityScore score) {
        context.out().print(JsonOutput.object(
                JsonOutput.stringField("command", "skill score"),
                JsonOutput.stringField("goal_key", score.goalKey()),
                JsonOutput.numberField("score", score.score()),
                JsonOutput.stringField("decision", score.decision()),
                JsonOutput.numberField("gate_pass_rate", score.gatePassRate()),
                JsonOutput.numberField("evidence_completeness", score.evidenceCompleteness()),
                JsonOutput.numberField("rollback_quality", score.rollbackQuality()),
                JsonOutput.stringField("summary", score.summary())
        ));
    }
}
