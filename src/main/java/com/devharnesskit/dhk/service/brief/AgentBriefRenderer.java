package com.devharnesskit.dhk.service.brief;

import com.devharnesskit.dhk.model.brief.AgentBrief;
import com.devharnesskit.dhk.model.brief.HarnessCommand;
import com.devharnesskit.dhk.model.knowledge.KnowledgeSnippet;
import com.devharnesskit.dhk.model.knowledge.ProfessionalKnowledgeContext;
import com.devharnesskit.dhk.util.JsonOutput;

import java.util.ArrayList;
import java.util.List;

public final class AgentBriefRenderer {
    public String render(AgentBrief brief) {
        return JsonOutput.object(
                JsonOutput.stringField("schema_version", AgentBrief.SCHEMA_VERSION),
                JsonOutput.stringField("brief_id", brief.briefId()),
                JsonOutput.stringField("recommendation_id", brief.recommendationId()),
                JsonOutput.stringField("task_key", brief.taskKey()),
                JsonOutput.stringField("goal_key", brief.goalKey()),
                JsonOutput.stringField("mode", brief.mode()),
                JsonOutput.stringField("profile_key", brief.profileKey()),
                JsonOutput.stringField("current_action", brief.currentAction()),
                JsonOutput.rawField("allowed_actions", JsonOutput.stringArray(brief.allowedActions())),
                JsonOutput.rawField("forbidden_actions", JsonOutput.stringArray(brief.forbiddenActions())),
                JsonOutput.rawField("required_evidence", JsonOutput.stringArray(brief.requiredEvidence())),
                JsonOutput.rawField("escalation_rules", escalationRules(brief)),
                JsonOutput.rawField("verification_policy", verificationPolicy(brief)),
                JsonOutput.stringField("user_brief_path", brief.userBriefPath()),
                JsonOutput.stringField("user_visible_summary_ref", brief.userBriefPath()),
                JsonOutput.rawField("growth_context", growthContext(brief)),
                JsonOutput.rawField("knowledge_context", knowledgeContext(brief.knowledgeContext())),
                JsonOutput.rawField("execution_policy", executionPolicy(brief)),
                JsonOutput.rawField("harness_commands", commands(brief.harnessCommands()))
        );
    }

    private String escalationRules(AgentBrief brief) {
        return JsonOutput.object(
                JsonOutput.rawField("soft", JsonOutput.stringArray(brief.softEscalationRules())),
                JsonOutput.rawField("hard", JsonOutput.stringArray(brief.hardEscalationRules()))
        );
    }

    private String verificationPolicy(AgentBrief brief) {
        return JsonOutput.object(
                JsonOutput.stringField("compile_mode", brief.compileMode()),
                JsonOutput.stringField("test_mode", brief.testMode()),
                JsonOutput.stringField("graph_mode", brief.graphMode()),
                JsonOutput.booleanField("manual_evidence_required", brief.manualEvidenceRequired())
        );
    }

    private String executionPolicy(AgentBrief brief) {
        return JsonOutput.object(
                JsonOutput.booleanField("show_commands_to_user", false),
                JsonOutput.booleanField("require_user_confirmation", brief.requireUserConfirmation())
        );
    }

    private String growthContext(AgentBrief brief) {
        String path = brief.userBriefPath().replace("WORK_BRIEF.md", "GROWTH_CONTEXT.md");
        return JsonOutput.object(
                JsonOutput.stringField("path", path),
                JsonOutput.booleanField("advisory_only", true)
        );
    }

    private String knowledgeContext(ProfessionalKnowledgeContext context) {
        List<String> snippets = new ArrayList<String>();
        for (KnowledgeSnippet snippet : context.snippets()) {
            snippets.add(JsonOutput.object(
                    JsonOutput.stringField("pack_ref", snippet.packRef()),
                    JsonOutput.stringField("rule_id", snippet.ruleId()),
                    JsonOutput.stringField("domain", snippet.domain()),
                    JsonOutput.stringField("severity", snippet.severity()),
                    JsonOutput.booleanField("advisory", true),
                    JsonOutput.stringField("full_ref", snippet.fullRef()),
                    JsonOutput.rawField("summary", JsonOutput.stringArray(snippet.summaryLines())),
                    JsonOutput.rawField("evidence_checklist", JsonOutput.stringArray(snippet.evidenceChecklist()))
            ).trim());
        }
        return JsonOutput.object(
                JsonOutput.booleanField("enabled", context.enabled()),
                JsonOutput.rawField("pack_refs", JsonOutput.stringArray(context.packRefs())),
                JsonOutput.stringField("agent_instruction", context.advisory()),
                JsonOutput.rawField("snippets", JsonOutput.array(snippets))
        );
    }

    private String commands(HarnessCommand[] commands) {
        List<String> raw = new ArrayList<String>();
        for (HarnessCommand command : commands) {
            raw.add(JsonOutput.object(
                    JsonOutput.stringField("name", command.name()),
                    JsonOutput.rawField("argv", JsonOutput.stringArray(command.argv())),
                    JsonOutput.stringField("when", command.when()),
                    JsonOutput.booleanField("user_visible", command.userVisible()),
                    JsonOutput.booleanField("agent_internal_only", command.agentInternalOnly())
            ).trim());
        }
        return JsonOutput.array(raw);
    }
}
