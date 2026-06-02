package com.devharnesskit.dhk.model;

public final class SearchResult {
    private final MemoryItem item;
    private final int score;
    private final String match;
    private final String explanation;

    public SearchResult(MemoryItem item, int score, String match) {
        this(item, score, match, "");
    }

    public SearchResult(MemoryItem item, int score, String match, String explanation) {
        this.item = item;
        this.score = score;
        this.match = match == null ? "" : match;
        this.explanation = explanation == null ? "" : explanation;
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

    public String explanation() {
        return explanation;
    }
}
