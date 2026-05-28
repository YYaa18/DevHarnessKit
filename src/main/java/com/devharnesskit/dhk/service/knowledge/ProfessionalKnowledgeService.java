package com.devharnesskit.dhk.service.knowledge;

import com.devharnesskit.dhk.model.brief.BriefRequest;
import com.devharnesskit.dhk.model.brief.ModeAdvice;
import com.devharnesskit.dhk.model.goal.GoalPlan;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.model.knowledge.KnowledgePack;
import com.devharnesskit.dhk.model.knowledge.KnowledgePackEntry;
import com.devharnesskit.dhk.model.knowledge.KnowledgeSnippet;
import com.devharnesskit.dhk.model.knowledge.ProfessionalKnowledgeContext;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ProfessionalKnowledgeService {
    private static final int DEFAULT_MAX_SNIPPETS = 5;
    private final KnowledgePackRegistry registry;
    private final KnowledgeInjectionSelector selector;
    private final KnowledgeSnippetLoader loader;

    public ProfessionalKnowledgeService() {
        this(new KnowledgePackRegistry(), new KnowledgeInjectionSelector(), new KnowledgeSnippetLoader());
    }

    ProfessionalKnowledgeService(KnowledgePackRegistry registry, KnowledgeInjectionSelector selector,
                                 KnowledgeSnippetLoader loader) {
        this.registry = registry;
        this.selector = selector;
        this.loader = loader;
    }

    public ProfessionalKnowledgeContext forGoal(Path projectRoot, GoalRun goal, GoalPlan plan) {
        return build(projectRoot, goal.profileKey(), plan.currentAction(), goal.taskName(), goal.moduleName(),
                DEFAULT_MAX_SNIPPETS);
    }

    public ProfessionalKnowledgeContext forBrief(BriefRequest request, ModeAdvice advice) {
        String action = ModeAdvice.PATCH.equals(advice.recommendation())
                ? "implement_minimal_change" : "create_change_plan";
        return build(request.projectRoot(), advice.profileKey(), action, request.task(), request.module(), 4);
    }

    public ProfessionalKnowledgeContext build(Path projectRoot, String profileKey, String action,
                                              String task, String module, int maxSnippets) {
        List<KnowledgePack> packs = registry.load(projectRoot);
        if (packs.isEmpty()) {
            return ProfessionalKnowledgeContext.disabled();
        }
        List<KnowledgePackEntry> entries = selector.select(packs, profileKey, action, task, module, maxSnippets);
        List<KnowledgeSnippet> snippets = new ArrayList<KnowledgeSnippet>();
        Set<String> refs = new LinkedHashSet<String>();
        for (KnowledgePack pack : packs) {
            refs.add(pack.packKey() + "@" + pack.version());
        }
        for (KnowledgePackEntry entry : entries) {
            snippets.add(loader.load(PathUtil.knowledgePacksDirectory(projectRoot).resolve(entry.packKey()), entry));
        }
        return new ProfessionalKnowledgeContext(!snippets.isEmpty(),
                refs.toArray(new String[refs.size()]),
                snippets.toArray(new KnowledgeSnippet[snippets.size()]),
                "Professional knowledge is advisory. If it conflicts with current code or confirmed memory, inspect the project and prefer current evidence.");
    }
}
