package com.devharnesskit.dhk.service.knowledge;

import com.devharnesskit.dhk.model.knowledge.KnowledgePack;
import com.devharnesskit.dhk.model.knowledge.KnowledgePackEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class KnowledgeInjectionSelector {
    List<KnowledgePackEntry> select(List<KnowledgePack> packs, String profileKey, String currentAction,
                                    String task, String module, int maxSnippets) {
        List<KnowledgePackEntry> selected = new ArrayList<KnowledgePackEntry>();
        String haystack = text(profileKey) + " " + text(currentAction) + " " + text(task) + " " + text(module);
        for (KnowledgePack pack : packs) {
            for (KnowledgePackEntry entry : pack.entries()) {
                if (selected.size() >= maxSnippets) {
                    return selected;
                }
                if (matchesAction(entry, currentAction) && matchesEntry(entry, profileKey, haystack)) {
                    selected.add(entry);
                }
            }
        }
        return selected;
    }

    private boolean matchesEntry(KnowledgePackEntry entry, String profileKey, String haystack) {
        if ("methodology".equals(text(entry.domain()))) {
            return matchesProfile(entry, profileKey);
        }
        if ("code-pattern".equals(text(entry.domain())) && matchesProfile(entry, profileKey)) {
            return true;
        }
        return matchesProfile(entry, profileKey) && matchesRisk(entry, haystack);
    }

    private boolean matchesAction(KnowledgePackEntry entry, String currentAction) {
        String action = text(currentAction);
        for (String value : entry.appliesToActions()) {
            String candidate = text(value);
            if ("*".equals(candidate) || candidate.equals(action)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesProfile(KnowledgePackEntry entry, String profileKey) {
        String profile = text(profileKey);
        if (entry.appliesToProfiles().length == 0) {
            return true;
        }
        for (String value : entry.appliesToProfiles()) {
            String candidate = text(value);
            if ("*".equals(candidate) || profile.indexOf(candidate) >= 0 || candidate.indexOf(profile) >= 0) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesRisk(KnowledgePackEntry entry, String haystack) {
        if (entry.riskFlags().length == 0) {
            return false;
        }
        for (String risk : entry.riskFlags()) {
            if (haystack.indexOf(text(risk)) >= 0) {
                return true;
            }
        }
        return false;
    }

    private String text(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
