package com.devharnesskit.dhk.service;

import com.devharnesskit.dhk.model.MemoryItem;
import com.devharnesskit.dhk.repository.MemoryRepository;
import com.devharnesskit.dhk.util.TextUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ExportSelectionService {
    private final MemoryRepository memoryRepository;

    public ExportSelectionService(MemoryRepository memoryRepository) {
        this.memoryRepository = memoryRepository;
    }

    public List<MemoryItem> select(Connection connection, String projectKey, String module,
                                   String mode, String task, String keywords, int limit) throws SQLException {
        String requestedModule = normalizeModule(module);
        List<String> tokens = TextUtil.tokens(task + " " + keywords + " " + mode);
        int candidateLimit = Math.max(100, limit * 10);
        Map<Long, MemoryItem> candidates = new LinkedHashMap<Long, MemoryItem>();
        for (MemoryItem item : memoryRepository.searchConfirmedForExport(connection, projectKey, requestedModule,
                tokens, candidateLimit)) {
            candidates.put(Long.valueOf(item.id()), item);
        }
        for (MemoryItem item : memoryRepository.listConfirmedForExport(connection, projectKey, requestedModule,
                candidateLimit)) {
            candidates.put(Long.valueOf(item.id()), item);
        }

        List<ScoredMemory> scored = new ArrayList<ScoredMemory>();
        for (MemoryItem item : candidates.values()) {
            int score = score(item, requestedModule, mode, tokens);
            if (score > 0) {
                scored.add(new ScoredMemory(item, score));
            }
        }
        Collections.sort(scored, new Comparator<ScoredMemory>() {
            public int compare(ScoredMemory left, ScoredMemory right) {
                int byScore = right.score - left.score;
                if (byScore != 0) {
                    return byScore;
                }
                return Long.valueOf(right.item.id()).compareTo(Long.valueOf(left.item.id()));
            }
        });

        List<MemoryItem> selected = new ArrayList<MemoryItem>();
        for (ScoredMemory value : scored) {
            selected.add(value.item);
            if (selected.size() >= limit) {
                break;
            }
        }
        return selected;
    }

    private int score(MemoryItem item, String requestedModule, String mode, List<String> tokens) {
        int score = 0;
        if (requestedModule.length() > 0 && requestedModule.equals(item.moduleName())) {
            score += 10;
        } else if ("global".equals(item.moduleName())) {
            score += 3;
        }
        if (typeMatchesMode(item.memoryType(), mode) || matchesAny(item.tags(), TextUtil.tokens(mode))) {
            score += 8;
        }
        if (matchesAny(item.tags(), tokens)) {
            score += 6;
        }
        if (matchesAny(item.title(), tokens)) {
            score += 4;
        }
        if (matchesAny(item.content(), tokens)) {
            score += 2;
        }
        if (item.confidence() >= 90) {
            score += 5;
        }
        if (item.useCount() > 0) {
            score += Math.min(3, item.useCount());
        }
        return score;
    }

    private boolean typeMatchesMode(String memoryType, String mode) {
        String normalizedMode = mode == null ? "" : mode.toLowerCase(Locale.ROOT).trim();
        if (normalizedMode.length() == 0 || "auto".equals(normalizedMode)) {
            return false;
        }
        String type = memoryType == null ? "" : memoryType.toLowerCase(Locale.ROOT);
        if (type.contains(normalizedMode)) {
            return true;
        }
        if ("api".equals(normalizedMode)) {
            return "api_convention".equals(type) || "gateway_convention".equals(type)
                    || "security_convention".equals(type) || "exception_convention".equals(type);
        }
        if ("mvc".equals(normalizedMode)) {
            return "mvc_convention".equals(type);
        }
        if ("db".equals(normalizedMode) || "database".equals(normalizedMode) || "sql".equals(normalizedMode)) {
            return "database_convention".equals(type);
        }
        return false;
    }

    private boolean matchesAny(String text, List<String> tokens) {
        String lower = text == null ? "" : text.toLowerCase(Locale.ROOT);
        for (String token : tokens) {
            if (token.length() > 0 && lower.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeModule(String module) {
        String normalized = module == null ? "" : module.trim();
        return normalized.length() == 0 ? "global" : normalized;
    }

    private static final class ScoredMemory {
        private final MemoryItem item;
        private final int score;

        private ScoredMemory(MemoryItem item, int score) {
            this.item = item;
            this.score = score;
        }
    }
}
