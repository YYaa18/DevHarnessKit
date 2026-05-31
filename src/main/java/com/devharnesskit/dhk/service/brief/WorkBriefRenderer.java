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
        builder.append("- risk_score: ").append(brief.riskScore()).append('\n');
        builder.append("- 建议流程：").append(modeLabel(brief.recommendation())).append('\n');
        builder.append("- 用户说明：").append(modeReason(brief.recommendation(), brief.riskScore())).append("\n\n");
        builder.append("## 为什么这样建议\n\n");
        appendList(builder, brief.why(), "暂无额外原因。");
        builder.append("\n## 风险提示\n\n");
        appendList(builder, brief.riskFlags(), "未发现明显高风险信号。");
        builder.append("\n## 专业注意事项\n\n");
        appendList(builder, brief.professionalNotes(), "暂无额外专业知识提示。");
        builder.append("\n## 预计会做\n\n");
        appendList(builder, brief.expectedWork(), "先确认任务边界。");
        builder.append("\n## 不会做\n\n");
        appendList(builder, brief.willNotDo(), "不会越过用户确认扩大范围。");
        builder.append("\n## 你可以选择\n\n");
        appendList(builder, brief.userChoices(), "确认后再开始。");
        builder.append("\n## 是否需要确认\n\n");
        builder.append("- confirmation_required: ").append(brief.confirmationRequired()).append('\n');
        builder.append("- requires_user_confirmation_reason: ").append(brief.confirmationReason()).append('\n');
        builder.append("- safe_to_start: ").append(brief.safeToStart()).append("\n\n");
        if (brief.confirmationRequired()) {
            builder.append("请确认以下事项后，告诉 Agent「按工作说明开始」：\n\n");
            builder.append("- [ ] 任务描述准确，和你的预期一致\n");
            builder.append("- [ ] 了解验证证据需要人工在 IDE 中操作\n");
            if (brief.riskScore() > 50) {
                builder.append("- [ ] 已知晓这是较高风险变更，留意 Agent 的影响面分析\n");
            }
        } else {
            builder.append("- 可以直接开始，无需额外确认。\n");
            builder.append("- 告诉 Agent「按工作说明开始」即可。\n");
        }
        builder.append("\n<!-- Work Brief 面向用户；底层 Harness 命令只给 Agent Brief 使用。 -->\n");
        return builder.toString();
    }

    private String modeLabel(String mode) {
        if (mode == null || mode.length() == 0) return "待确认";
        if ("patch".equals(mode)) return "轻流程（改动范围小，快速推进）";
        if ("standard".equals(mode)) return "标准流程（分析 → 计划 → 实现 → 验证）";
        if ("strict".equals(mode)) return "严格流程（完整审计，适合高风险变更）";
        if ("analyze_only".equals(mode)) return "只分析，不开始";
        if ("ask".equals(mode)) return "需要先澄清任务";
        return mode;
    }

    private String modeReason(String mode, int riskScore) {
        if ("patch".equals(mode)) return "改动范围较小，但仍会记录关键证据";
        if ("standard".equals(mode)) return "中等影响面，建议按分析、计划、实现、验证推进";
        if ("strict".equals(mode)) return "风险或影响面较高，需要完整验证和审计记录";
        if ("analyze_only".equals(mode)) return "当前只做分析，不进入代码修改";
        if ("ask".equals(mode)) return "任务边界还不够明确，需要先确认";
        if (riskScore <= 20) return "影响面小，适合轻量推进";
        if (riskScore <= 50) return "中等影响面，建议留下验证证据";
        return "影响面较大，需完整验证和审计记录";
    }

    private void appendList(StringBuilder builder, String[] values, String empty) {
        if (values == null || values.length == 0) {
            builder.append("- ").append(empty).append('\n');
            return;
        }
        boolean wrote = false;
        for (String value : values) {
            if (isInternalDiagnostic(value)) {
                continue;
            }
            builder.append("- ").append(value).append('\n');
            wrote = true;
        }
        if (!wrote) {
            builder.append("- ").append(empty).append('\n');
        }
    }

    private boolean isInternalDiagnostic(String value) {
        if (value == null) return false;
        // suppress internal "Knowledge file missing" notes — not actionable for end users
        return value.contains("Knowledge file missing:");
    }
}
