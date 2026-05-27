package com.devharnesskit.dhk.command.checkpoint;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.model.checkpoint.HumanCheckpoint;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.repository.goal.GoalRunRepository;
import com.devharnesskit.dhk.service.SensitiveDataGuard;
import com.devharnesskit.dhk.service.checkpoint.HumanCheckpointService;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;

final class HumanCheckpointCommand implements Command {
    private final DbConnectionFactory connectionFactory = new DbConnectionFactory();
    private final MigrationRunner migrationRunner = new MigrationRunner();
    private final GoalRunRepository goalRunRepository = new GoalRunRepository();
    private final HumanCheckpointService service = new HumanCheckpointService();
    private final SensitiveDataGuard sensitiveDataGuard = new SensitiveDataGuard();

    public int run(CommandContext context, Args args) {
        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        String command = args.subCommand();
        try (Connection connection = connectionFactory.open(projectRoot)) {
            migrationRunner.migrate(connection, context.clock());
            String goalKey = required(args, "goal");
            GoalRun goal = goalRunRepository.findByKey(connection, goalKey);
            if (goal == null) {
                context.err().println("Goal not found: " + goalKey);
                return ExitCodes.NOT_FOUND;
            }
            if ("request".equals(command)) {
                return request(context, args, connection, goal);
            }
            if ("approve".equals(command)) {
                return approve(context, args, connection);
            }
            if ("list".equals(command)) {
                return list(context, args, connection, goal);
            }
            return ExitCodes.USAGE_ERROR;
        } catch (IllegalArgumentException ex) {
            context.err().println(ex.getMessage());
            return ExitCodes.VALIDATION_ERROR;
        } catch (Exception ex) {
            context.err().println("ERROR checkpoint command failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private int request(CommandContext context, Args args, Connection connection, GoalRun goal)
            throws Exception {
        String reason = required(args, "reason");
        rejectSensitive("checkpoint reason", reason);
        String type = args.option("type", "before_complete").trim();
        String requestedBy = args.option("requested-by", "manual").trim();
        HumanCheckpoint checkpoint = service.request(connection, goal, type, reason, requestedBy,
                context.clock().now().toString());
        print(context, args, "human checkpoint requested", checkpoint);
        return ExitCodes.SUCCESS;
    }

    private int approve(CommandContext context, Args args, Connection connection) throws Exception {
        String idText = required(args, "id");
        String approver = required(args, "approver");
        String reason = args.option("reason", "").trim();
        rejectSensitive("checkpoint approval reason", reason);
        long id = Long.parseLong(idText);
        HumanCheckpoint checkpoint = service.approve(connection, id, approver, reason,
                context.clock().now().toString());
        print(context, args, "human checkpoint approved", checkpoint);
        return ExitCodes.SUCCESS;
    }

    private int list(CommandContext context, Args args, Connection connection, GoalRun goal)
            throws Exception {
        List<HumanCheckpoint> checkpoints = service.list(connection, goal.goalKey());
        if (JsonOutput.enabled(args)) {
            java.util.List<String> raw = new java.util.ArrayList<String>();
            for (HumanCheckpoint checkpoint : checkpoints) {
                raw.add(json(checkpoint).trim());
            }
            context.out().print(JsonOutput.object(
                    JsonOutput.stringField("command", "checkpoint list"),
                    JsonOutput.stringField("goal_key", goal.goalKey()),
                    JsonOutput.rawField("checkpoints", JsonOutput.array(raw))
            ));
            return ExitCodes.SUCCESS;
        }
        context.out().println("human checkpoints:");
        if (checkpoints.isEmpty()) {
            context.out().println("  - none");
            return ExitCodes.SUCCESS;
        }
        for (HumanCheckpoint checkpoint : checkpoints) {
            context.out().println("  - id: " + checkpoint.id());
            context.out().println("    type: " + checkpoint.checkpointType());
            context.out().println("    status: " + checkpoint.status());
            context.out().println("    reason: " + checkpoint.reason());
            context.out().println("    requested_by: " + checkpoint.requestedBy());
            context.out().println("    requested_at: " + checkpoint.requestedAt());
            if (checkpoint.approver().length() > 0) {
                context.out().println("    approver: " + checkpoint.approver());
                context.out().println("    approved_at: " + checkpoint.approvedAt());
                context.out().println("    decision_reason: " + checkpoint.decisionReason());
            }
        }
        return ExitCodes.SUCCESS;
    }

    private void print(CommandContext context, Args args, String title, HumanCheckpoint checkpoint) {
        if (JsonOutput.enabled(args)) {
            context.out().print(JsonOutput.object(
                    JsonOutput.stringField("command", title),
                    JsonOutput.rawField("checkpoint", json(checkpoint))
            ));
            return;
        }
        context.out().println(title);
        context.out().println("id: " + checkpoint.id());
        context.out().println("goal_key: " + checkpoint.goalKey());
        context.out().println("type: " + checkpoint.checkpointType());
        context.out().println("status: " + checkpoint.status());
        context.out().println("reason: " + checkpoint.reason());
        context.out().println("requested_by: " + checkpoint.requestedBy());
        context.out().println("requested_at: " + checkpoint.requestedAt());
        if (checkpoint.approver().length() > 0) {
            context.out().println("approver: " + checkpoint.approver());
            context.out().println("approved_at: " + checkpoint.approvedAt());
            context.out().println("decision_reason: " + checkpoint.decisionReason());
        }
    }

    private String json(HumanCheckpoint checkpoint) {
        return JsonOutput.object(
                JsonOutput.numberField("id", checkpoint.id()),
                JsonOutput.stringField("goal_key", checkpoint.goalKey()),
                JsonOutput.stringField("type", checkpoint.checkpointType()),
                JsonOutput.stringField("status", checkpoint.status()),
                JsonOutput.stringField("reason", checkpoint.reason()),
                JsonOutput.stringField("requested_by", checkpoint.requestedBy()),
                JsonOutput.stringField("requested_at", checkpoint.requestedAt()),
                JsonOutput.stringField("approver", checkpoint.approver()),
                JsonOutput.stringField("approved_at", checkpoint.approvedAt()),
                JsonOutput.stringField("decision_reason", checkpoint.decisionReason())
        );
    }

    private String required(Args args, String key) {
        String value = args.option(key).trim();
        if (value.length() == 0) {
            throw new IllegalArgumentException("--" + key + " is required");
        }
        return value;
    }

    private void rejectSensitive(String label, String value) {
        if (value.length() > 0 && sensitiveDataGuard.containsSensitiveData(value)) {
            throw new IllegalArgumentException("Sensitive data rejected in " + label + ": "
                    + sensitiveDataGuard.findMatches(value));
        }
    }
}
