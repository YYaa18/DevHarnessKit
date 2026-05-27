package com.devharnesskit.dhk.command.skill;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.model.skill.SkillEvaluationReport;
import com.devharnesskit.dhk.repository.goal.GoalCheckRepository;
import com.devharnesskit.dhk.repository.goal.GoalRunRepository;
import com.devharnesskit.dhk.repository.goal.GoalStepRepository;
import com.devharnesskit.dhk.service.skill.SkillEvaluationReportService;
import com.devharnesskit.dhk.util.JsonOutput;

import java.nio.file.Path;
import java.sql.Connection;

public final class SkillReportCommand implements Command {
    private final DbConnectionFactory connectionFactory = new DbConnectionFactory();
    private final MigrationRunner migrationRunner = new MigrationRunner();
    private final GoalRunRepository goalRunRepository = new GoalRunRepository();
    private final GoalStepRepository goalStepRepository = new GoalStepRepository();
    private final GoalCheckRepository goalCheckRepository = new GoalCheckRepository();
    private final SkillEvaluationReportService reportService = new SkillEvaluationReportService();

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
            SkillEvaluationReport report = reportService.report(connection, projectRoot, goal,
                    goalStepRepository.listByGoal(connection, goal.goalKey()),
                    goalCheckRepository.listByGoal(connection, goal.goalKey()),
                    args.option("group", "F"), parseInt(args.option("baseline-score", "-1"), -1));
            if (JsonOutput.enabled(args)) {
                printJson(context, report);
            } else {
                printText(context, report);
            }
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR skill report failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private void printText(CommandContext context, SkillEvaluationReport report) {
        context.out().println("skill evaluation report complete");
        context.out().println("goal_key: " + report.goalKey());
        context.out().println("group: " + report.group());
        context.out().println("skill_key: " + report.skillKey());
        context.out().println("skill_contract_present: " + report.skillContractPresent());
        context.out().println("skill_trusted: " + report.trusted());
        context.out().println("trust_status: " + report.trustStatus());
        context.out().println("skill_quality_score: " + report.qualityScore().score());
        context.out().println("dqi_score: " + report.dqiScore());
        context.out().println("baseline_score: " + report.baselineScore());
        context.out().println("dqi_delta: " + report.dqiDelta());
        context.out().println("gate_pass_rate: " + report.qualityScore().gatePassRate());
        context.out().println("evidence_completeness: " + report.qualityScore().evidenceCompleteness());
        context.out().println("rollback_quality: " + report.qualityScore().rollbackQuality());
    }

    private void printJson(CommandContext context, SkillEvaluationReport report) {
        context.out().print(JsonOutput.object(
                JsonOutput.stringField("command", "skill report"),
                JsonOutput.stringField("goal_key", report.goalKey()),
                JsonOutput.stringField("group", report.group()),
                JsonOutput.stringField("skill_key", report.skillKey()),
                JsonOutput.booleanField("skill_contract_present", report.skillContractPresent()),
                JsonOutput.booleanField("skill_trusted", report.trusted()),
                JsonOutput.stringField("trust_status", report.trustStatus()),
                JsonOutput.numberField("skill_quality_score", report.qualityScore().score()),
                JsonOutput.numberField("dqi_score", report.dqiScore()),
                JsonOutput.numberField("baseline_score", report.baselineScore()),
                JsonOutput.numberField("dqi_delta", report.dqiDelta()),
                JsonOutput.numberField("gate_pass_rate", report.qualityScore().gatePassRate()),
                JsonOutput.numberField("evidence_completeness", report.qualityScore().evidenceCompleteness()),
                JsonOutput.numberField("rollback_quality", report.qualityScore().rollbackQuality())
        ));
    }

    private int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw);
        } catch (Exception ex) {
            return fallback;
        }
    }
}
