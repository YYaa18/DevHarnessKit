package com.devharnesskit.dhk.context.token;

public final class CharsOverFourTokenEstimator implements TokenEstimator {
    public int estimate(String text) {
        if (text == null || text.length() == 0) {
            return 0;
        }
        return (text.length() + 3) / 4;
    }
}
