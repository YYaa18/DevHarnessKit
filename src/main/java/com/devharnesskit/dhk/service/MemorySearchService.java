package com.devharnesskit.dhk.service;

import com.devharnesskit.dhk.model.MemoryItem;
import com.devharnesskit.dhk.model.SearchResult;
import com.devharnesskit.dhk.repository.FtsRepository;
import com.devharnesskit.dhk.repository.MemoryRepository;
import com.devharnesskit.dhk.util.TextUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MemorySearchService {
    private final MemoryRepository memoryRepository;
    private final FtsRepository ftsRepository;

    public MemorySearchService(MemoryRepository memoryRepository, FtsRepository ftsRepository) {
        this.memoryRepository = memoryRepository;
        this.ftsRepository = ftsRepository;
    }

    public List<SearchResult> search(Connection connection, String projectKey, String query,
                                     String module, String status, int limit) throws SQLException {
        List<String> tokens = TextUtil.tokens(query);
        Set<Long> ftsIds = ftsRepository.searchIds(connection, query, Math.max(limit * 10, 100));
        int candidateLimit = Math.max(100, limit * 10);
        Map<Long, MemoryItem> candidates = new LinkedHashMap<Long, MemoryItem>();
        for (MemoryItem item : memoryRepository.searchLike(connection, projectKey, tokens, module, status, candidateLimit)) {
            candidates.put(Long.valueOf(item.id()), item);
        }
        for (MemoryItem item : memoryRepository.findByIds(connection, projectKey, new ArrayList<Long>(ftsIds))) {
            if (status.length() > 0 && !status.equals(item.status())) {
                continue;
            }
            if (module.length() > 0 && !module.equals(item.moduleName())) {
                continue;
            }
            candidates.put(Long.valueOf(item.id()), item);
        }
        for (MemoryItem item : memoryRepository.listCandidates(connection, projectKey, module, status, Math.max(limit, 20))) {
            candidates.put(Long.valueOf(item.id()), item);
        }

        List<SearchResult> scored = new ArrayList<SearchResult>();
        for (MemoryItem item : candidates.values()) {
            SearchResult result = score(item, tokens, module, ftsIds.contains(Long.valueOf(item.id())));
            if (result.score() > 0) {
                scored.add(result);
            }
        }
        Collections.sort(scored, new Comparator<SearchResult>() {
            public int compare(SearchResult left, SearchResult right) {
                int byScore = right.score() - left.score();
                if (byScore != 0) {
                    return byScore;
                }
                return Long.valueOf(right.item().id()).compareTo(Long.valueOf(left.item().id()));
            }
        });
        if (scored.size() <= limit) {
            return scored;
        }
        return new ArrayList<SearchResult>(scored.subList(0, limit));
    }

    private SearchResult score(MemoryItem item, List<String> tokens, String requestedModule, boolean ftsMatch) {
        int score = 0;
        List<String> matches = new ArrayList<String>();
        boolean queryMatched = false;
        if (matchesAny(item.tags(), tokens)) {
            score += 8;
            matches.add("tags");
            queryMatched = true;
        }
        if (matchesAny(item.title(), tokens)) {
            score += 5;
            matches.add("title");
            queryMatched = true;
        }
        if (matchesAny(item.content(), tokens)) {
            score += 3;
            matches.add("content");
            queryMatched = true;
        }
        if (ftsMatch) {
            queryMatched = true;
        }
        if (!queryMatched) {
            return new SearchResult(item, 0, "");
        }
        if (ftsMatch && matches.isEmpty()) {
            score += 1;
            matches.add("fts");
        }
        if (requestedModule.length() > 0 && requestedModule.equals(item.moduleName())) {
            score += 10;
            matches.add("module");
        }
        if ("confirmed".equals(item.status())) {
            score += 3;
            matches.add("confirmed");
        }
        if (item.confidence() >= 90) {
            score += 2;
            matches.add("confidence>=90");
        } else if (item.confidence() >= 70) {
            score += 1;
            matches.add("confidence>=70");
        }
        return new SearchResult(item, score, join(matches));
    }

    private boolean matchesAny(String text, List<String> tokens) {
        String lower = text == null ? "" : text.toLowerCase();
        for (String token : tokens) {
            if (lower.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private String join(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(values.get(i));
        }
        return builder.toString();
    }
}
