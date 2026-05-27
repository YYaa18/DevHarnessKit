package com.devharnesskit.dhk.service.brief;

import com.devharnesskit.dhk.model.brief.WorkBrief;

public final class WorkBriefRenderer {
    public String render(WorkBrief brief) {
        StringBuilder builder = new StringBuilder();
        builder.append("# Work Brief\n\n");
        builder.append("## 本次工作\n\n");
        builder.append(brief.taskSummary()).append("\n\n");
        builder.append("## 建议模式\n\n");
        builder.append("- mode_id: ").append(brief.modeId()).append('\n');
        builder.append("- recommendation: ").append(brief.recommendation()).append('\n');
        builder.append("- confidence: ").append(brief.confidence()).append('\n');
        builder.append("- risk_score: ").append(brief.riskScore()).append("\n\n");
        builder.append("## 为什么这样建议\n\n");
        appendList(builder, brief.why(), "暂无额外原因。");
        builder.append("\n## 风险提示\n\n");
        appendList(builder, brief.riskFlags(), "未发现明显高风险信号。");
        builder.append("\n## 预计会做\n\n");
        appendList(builder, brief.expectedWork(), "先确认任务边界。");
        builder.append("\n## 不会做\n\n");
        appendList(builder, brief.willNotDo(), "不会越过用户确认扩大范围。");
        builder.append("\n## 你可以选择\n\n");
        appendList(builder, brief.userChoices(), "确认后再开始。");
        builder.append("\n## 是否需要确认\n\n");
        builder.append("- confirmation_required: ").append(brief.confirmationRequired()).append('\n');
        builder.append("- requires_user_confirmation_reason: ").append(brief.confirmationReason()).append('\n');
        builder.append("- safe_to_start: ").append(brief.safeToStart()).append('\n');
        builder.append("\n<!-- Work Brief 面向用户；底层 Harness 命令只给 Agent Brief 使用。 -->\n");
        return builder.toString();
    }

    private void appendList(StringBuilder builder, String[] values, String empty) {
        if (values == null || values.length == 0) {
            builder.append("- ").append(empty).append('\n');
            return;
        }
        for (String value : values) {
            builder.append("- ").append(value).append('\n');
        }
    }
}
