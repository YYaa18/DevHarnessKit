package com.devharnesskit.dhk.context.compress;

import com.devharnesskit.dhk.context.token.CharsOverFourTokenEstimator;
import com.devharnesskit.dhk.context.token.TokenEstimator;

import java.util.Collections;

public final class PassthroughCompressor {
    private final TokenEstimator estimator;

    public PassthroughCompressor() {
        this(new CharsOverFourTokenEstimator());
    }

    PassthroughCompressor(TokenEstimator estimator) {
        this.estimator = estimator == null ? new CharsOverFourTokenEstimator() : estimator;
    }

    public CompressResult compress(String sourceType, String text) {
        String output = text == null ? "" : text;
        int tokens = estimator.estimate(output);
        return new CompressResult(sourceType, output, Collections.<RetainedSpan>emptyList(),
                0, tokens, tokens, true);
    }
}
