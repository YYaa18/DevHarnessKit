package com.devharnesskit.dhk.util;

import java.time.Instant;

public final class SystemClock implements Clock {
    public Instant now() {
        return Instant.now();
    }
}
