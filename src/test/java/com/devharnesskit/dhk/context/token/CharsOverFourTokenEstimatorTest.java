package com.devharnesskit.dhk.context.token;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class CharsOverFourTokenEstimatorTest {
    @Test
    void estimatesWithCeilingCharsOverFour() {
        CharsOverFourTokenEstimator estimator = new CharsOverFourTokenEstimator();

        assertEquals(0, estimator.estimate(null));
        assertEquals(0, estimator.estimate(""));
        assertEquals(1, estimator.estimate("a"));
        assertEquals(1, estimator.estimate("abcd"));
        assertEquals(2, estimator.estimate("abcde"));
    }
}
