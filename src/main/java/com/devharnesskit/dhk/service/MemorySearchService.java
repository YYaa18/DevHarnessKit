package com.devharnesskit.dhk.service;

import com.devharnesskit.dhk.model.MemoryItem;
import com.devharnesskit.dhk.model.SearchResult;
import com.devharnesskit.dhk.repository.FtsRepository;
import com.devharnesskit.dhk.repository.MemoryRepository;
import com.devharnesskit.dhk.util.TextUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
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
            SearchResult result = score(item, tokens, module, status, ftsIds.contains(Long.valueOf(item.id())));
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

    private SearchResult score(MemoryItem item, List<String> tokens, String requestedModule,
                               String requestedStatus, boolean ftsMatch) {
        int score = 0;
        List<String> matches = new ArrayList<String>();
        List<String> explanation = new ArrayList<String>();
        boolean queryMatched = false;
        if (matchesAny(item.tags(), tokens)) {
            score += 8;
            matches.add("tags");
            explanation.add("tags:+8");
            queryMatched = true;
        }
        if (matchesAny(item.title(), tokens)) {
            score += 5;
            matches.add("title");
            explanation.add("title:+5");
            queryMatched = true;
        }
        if (matchesAny(item.content(), tokens)) {
            score += 3;
            matches.add("content");
            explanation.add("content:+3");
            queryMatched = true;
        }
        if (ftsMatch) {
            queryMatched = true;
            score += 1;
            matches.add("fts");
            explanation.add("fts:+1");
        }
        if (!queryMatched) {
            return new SearchResult(item, 0, "");
        }
        if (requestedModule.length() > 0 && requestedModule.equals(item.moduleName())) {
            score += 10;
            matches.add("module");
            explanation.add("module:+10");
        }
        if ("confirmed".equals(item.status())) {
            score += 3;
            matches.add("confirmed");
            explanation.add("confirmed:+3");
        }
        if (item.confidence() >= 90) {
            score += 2;
            matches.add("confidence>=90");
            explanation.add("confidence>=90:+2");
        } else if (item.confidence() >= 70) {
            score += 1;
            matches.add("confidence>=70");
            explanation.add("confidence>=70:+1");
        }
        if (item.lastVerifiedAt().length() > 0) {
            score += 1;
            matches.add("verified");
            explanation.add("verified:+1");
        }
        if (item.lastUsedAt().length() > 0) {
            score += 1;
            matches.add("used");
            explanation.add("used:+1");
        }
        if (isStale(item)) {
            score -= 8;
            matches.add("stale");
            explanation.add("stale:-8");
        }
        if (item.supersededBy() > 0) {
            score -= 12;
            matches.add("superseded");
            explanation.add("superseded:-12");
        }
        if ("deprecated".equals(item.status())) {
            score -= 6;
            matches.add("deprecated");
            explanation.add("deprecated:-6");
        }
        if ("archived".equals(item.status()) && requestedStatus.length() == 0) {
            return new SearchResult(item, 0, "");
        }
        if (score < 1) {
            score = 1;
        }
        return new SearchResult(item, score, join(matches), join(explanation));
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

    private boolean isStale(MemoryItem item) {
        if (item.staleReason().length() > 0) {
            return true;
        }
        if (item.effectiveTo().length() == 0) {
            return false;
        }
        try {
            return Instant.parse(item.effectiveTo()).isBefore(Instant.now());
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
