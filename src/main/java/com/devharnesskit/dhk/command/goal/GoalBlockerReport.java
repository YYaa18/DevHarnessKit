package com.devharnesskit.dhk.command.goal;

import com.devharnesskit.dhk.model.goal.GoalEvaluation;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.util.JsonOutput;

import java.util.ArrayList;
import java.util.List;

final class GoalBlockerReport {
    private GoalBlockerReport() {
    }

    static List<Blocker> blockers(GoalRun goal, GoalEvaluation evaluation) {
        List<Blocker> blockers = new ArrayList<Blocker>();
        for (String missing : evaluation.missing()) {
            blockers.add(blocker(goal, missing));
        }
        for (String stale : evaluation.staleChecks()) {
            boolean alreadyPresent = false;
            for (Blocker blocker : blockers) {
                if (stale.equals(blocker.key)) {
                    alreadyPresent = true;
                    break;
                }
            }
            if (!alreadyPresent) {
                blockers.add(new Blocker(stale, "已有检查结果过期，需要重新验证最新代码。",
                        "required check is stale: " + stale,
                        "dhk goal check --goal " + goal.goalKey() + " --check " + stale));
            }
        }
        return blockers;
    }

    static String renderText(GoalRun goal, GoalEvaluation evaluation) {
        List<Blocker> blockers = blockers(goal, evaluation);
        StringBuilder builder = new StringBuilder();
        builder.append("goal complete: not ready\n");
        builder.append("decision: ").append(evaluation.decision()).append('\n');
        builder.append("ready_to_complete: ").append(evaluation.readyToComplete()).append('\n');
        builder.append("blockers:\n");
        if (blockers.isEmpty()) {
            builder.append("  - none\n");
        } else {
            int index = 1;
            for (Blocker blocker : blockers) {
                builder.append("  - key: ").append(blocker.key).append('\n');
                builder.append("    user_message: ").append(blocker.userMessage).append('\n');
                builder.append("    technical_reason: ").append(blocker.technicalReason).append('\n');
                builder.append("    next_command: ").append(blocker.nextCommand).append('\n');
                index++;
            }
        }
        builder.append("missing:\n");
        appendArray(builder, evaluation.missing());
        builder.append("stale_checks:\n");
        appendArray(builder, evaluation.staleChecks());
        builder.append("next_action: ").append(evaluation.nextAction()).append('\n');
        builder.append("next_command: ").append(evaluation.nextCommand()).append('\n');
        return builder.toString();
    }

    static String renderJson(GoalRun goal, GoalEvaluation evaluation) {
        List<String> raw = new ArrayList<String>();
        for (Blocker blocker : blockers(goal, evaluation)) {
            raw.add(JsonOutput.object(
                    JsonOutput.stringField("key", blocker.key),
                    JsonOutput.stringField("user_message", blocker.userMessage),
                    JsonOutput.stringField("technical_reason", blocker.technicalReason),
                    JsonOutput.stringField("next_command", blocker.nextCommand)
            ).trim());
        }
        return JsonOutput.object(
                JsonOutput.stringField("command", "goal complete"),
                JsonOutput.stringField("status", "not_ready"),
                JsonOutput.stringField("decision", evaluation.decision()),
                JsonOutput.booleanField("ready_to_complete", evaluation.readyToComplete()),
                JsonOutput.rawField("blockers", JsonOutput.array(raw)),
                JsonOutput.numberField("missing_count", evaluation.missing().length),
                JsonOutput.rawField("missing", JsonOutput.stringArray(evaluation.missing())),
                JsonOutput.numberField("stale_count", evaluation.staleChecks().length),
                JsonOutput.rawField("stale_checks", JsonOutput.stringArray(evaluation.staleChecks())),
                JsonOutput.stringField("next_action", evaluation.nextAction()),
                JsonOutput.stringField("next_command", evaluation.nextCommand())
        );
    }

    private static Blocker blocker(GoalRun goal, String missing) {
        String checkKey = checkKey(missing);
        String lower = missing.toLowerCase(java.util.Locale.ROOT);
        if (missing.startsWith("goal steps incomplete")) {
            return new Blocker("goal_steps", "还有目标步骤没有完成，需要继续执行当前 action 并记录证据。",
                    missing, "dhk goal next --goal " + goal.goalKey());
        }
        if (missing.startsWith("context export is not ready")) {
            return new Blocker("context_export", "GOAL_CONTEXT 尚未恢复，需要先 resume。",
                    missing, "dhk goal resume --goal " + goal.goalKey());
        }
        if (checkKey.length() > 0) {
            String userMessage = "检查 `" + checkKey + "` 尚未满足完成条件。";
            if (lower.contains("stale")) {
                userMessage = "检查 `" + checkKey + "` 已过期，需要重新验证。";
            } else if (lower.contains("pending")) {
                userMessage = "检查 `" + checkKey + "` 还没有执行。";
            } else if (lower.contains("skipped")) {
                userMessage = "当前 profile 不接受 `" + checkKey + "` 被跳过。";
            } else if (lower.contains("failed")) {
                userMessage = "检查 `" + checkKey + "` 失败，需要修复后重跑。";
            }
            return new Blocker(checkKey, userMessage, missing,
                    "dhk goal check --goal " + goal.goalKey() + " --check " + checkKey);
        }
        return new Blocker("goal_blocker", "当前还有完成阻塞项需要处理。", missing,
                "dhk goal verify --goal " + goal.goalKey());
    }

    private static String checkKey(String missing) {
        if (!missing.startsWith("check ")) {
            return "";
        }
        int marker = missing.indexOf(" is ", 6);
        if (marker <= 6) {
            return "";
        }
        return missing.substring(6, marker);
    }

    private static void appendArray(StringBuilder builder, String[] values) {
        if (values.length == 0) {
            builder.append("  - none\n");
            return;
        }
        for (String value : values) {
            builder.append("  - ").append(value).append('\n');
        }
    }

    static final class Blocker {
        private final String key;
        private final String userMessage;
        private final String technicalReason;
        private final String nextCommand;

        private Blocker(String key, String userMessage, String technicalReason, String nextCommand) {
            this.key = key;
            this.userMessage = userMessage;
            this.technicalReason = technicalReason;
            this.nextCommand = nextCommand;
        }
    }
}
