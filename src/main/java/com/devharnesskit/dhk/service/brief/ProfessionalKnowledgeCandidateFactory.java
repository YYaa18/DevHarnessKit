package com.devharnesskit.dhk.service.brief;

import com.devharnesskit.dhk.model.brief.KnowledgeCandidate;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.model.knowledge.KnowledgeSnippet;
import com.devharnesskit.dhk.model.knowledge.ProfessionalKnowledgeContext;
import com.devharnesskit.dhk.service.knowledge.ProfessionalKnowledgeService;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class ProfessionalKnowledgeCandidateFactory {
    private final ProfessionalKnowledgeService knowledgeService;

    ProfessionalKnowledgeCandidateFactory() {
        this(new ProfessionalKnowledgeService());
    }

    ProfessionalKnowledgeCandidateFactory(ProfessionalKnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    List<KnowledgeCandidate> candidates(Path projectRoot, GoalRun goal) {
        List<KnowledgeCandidate> result = new ArrayList<KnowledgeCandidate>();
        ProfessionalKnowledgeContext knowledge = knowledgeService.build(projectRoot,
                goal.profileKey(), "verify", goal.taskName(), goal.moduleName(), 4);
        for (KnowledgeSnippet snippet : knowledge.snippets()) {
            if ("database".equals(snippet.domain())) {
                result.add(candidate(goal, snippet, "java-db-verification",
                        "verification_rule_candidate",
                        "SQL 或数据库变更需要验证证据",
                        "本任务匹配数据库专业知识规则；若触碰 SQL、Mapper 或事务边界，建议沉淀验证规则。",
                        "task/profile/module mentions database, SQL, mapper, MyBatis, or transaction",
                        "not applicable when no data access behavior changed",
                        "database verification conventions are reusable project memory"));
            } else if ("security".equals(snippet.domain())) {
                result.add(candidate(goal, snippet, "java-security-convention",
                        "project_convention_candidate",
                        "安全敏感变更需要确认本地授权与脱敏约定",
                        "本任务匹配安全专业知识规则；若涉及权限、支付、身份或日志，建议确认项目约定。",
                        "task/profile/module mentions security, auth, permission, token, payment, or risk",
                        "not applicable when no security-sensitive behavior changed",
                        "security conventions should be confirmed before future reuse"));
            }
        }
        return result;
    }

    private KnowledgeCandidate candidate(GoalRun goal, KnowledgeSnippet snippet, String suffix,
                                         String type, String title, String summary,
                                         String applicableWhen, String notApplicableWhen,
                                         String destinationReason) {
        return new KnowledgeCandidate("kc-" + goal.goalKey() + "-" + suffix,
                goal.goalKey(), type, title, summary,
                "goal:" + goal.goalKey() + ";rule:" + snippet.ruleId(),
                "project_memory", "high", true, "passed", "draft",
                snippet.ruleId(), packKey(snippet.packRef()), snippet.domain(), snippet.severity(),
                applicableWhen, notApplicableWhen, destinationReason, "full_ref=" + snippet.fullRef());
    }

    private String packKey(String packRef) {
        int at = packRef.indexOf('@');
        return at < 0 ? packRef : packRef.substring(0, at);
    }
}
