package com.devharnesskit.dhk.model;

public final class SearchResult {
    private final MemoryItem item;
    private final int score;
    private final String match;

    public SearchResult(MemoryItem item, int score, String match) {
        this.item = item;
        this.score = score;
        this.match = match == null ? "" : match;
    }

    public MemoryItem item() {
        return item;
    }

    public int score() {
        return score;
    }

    public String match() {
        return match;
    }
}
