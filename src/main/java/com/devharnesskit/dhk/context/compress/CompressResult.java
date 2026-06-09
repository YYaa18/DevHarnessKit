package com.devharnesskit.dhk.context.compress;

import com.devharnesskit.dhk.util.JsonOutput;

import java.util.ArrayList;
import java.util.List;

public final class CompressResult {
    private final String sourceType;
    private final String compressedText;
    private final List<RetainedSpan> retainedSpans;
    private final int omittedLines;
    private final int tokenBefore;
    private final int tokenAfter;
    private final boolean degraded;

    public CompressResult(String sourceType, String compressedText, List<RetainedSpan> retainedSpans,
                          int omittedLines, int tokenBefore, int tokenAfter, boolean degraded) {
        this.sourceType = sourceType == null ? "" : sourceType;
        this.compressedText = compressedText == null ? "" : compressedText;
        this.retainedSpans = retainedSpans == null
                ? new ArrayList<RetainedSpan>() : new ArrayList<RetainedSpan>(retainedSpans);
        this.omittedLines = Math.max(0, omittedLines);
        this.tokenBefore = Math.max(0, tokenBefore);
        this.tokenAfter = Math.max(0, tokenAfter);
        this.degraded = degraded;
    }

    public String sourceType() {
        return sourceType;
    }

    public String compressedText() {
        return compressedText;
    }

    public List<RetainedSpan> retainedSpans() {
        return new ArrayList<RetainedSpan>(retainedSpans);
    }

    public String retainedSpansJson() {
        List<String> raw = new ArrayList<String>();
        for (RetainedSpan span : retainedSpans) {
            raw.add(span.toJson());
        }
        return JsonOutput.array(raw);
    }

    public int omittedLines() {
        return omittedLines;
    }

    public int tokenBefore() {
        return tokenBefore;
    }

    public int tokenAfter() {
        return tokenAfter;
    }

    public boolean degraded() {
        return degraded;
    }
}
