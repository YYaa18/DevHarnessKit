package com.devharnesskit.dhk.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TagUtilTest {
    @Test
    void normalizesTags() {
        assertEquals("api,gateway,user-id", TagUtil.normalize(" API, gateway, api, User-Id "));
    }

    @Test
    void handlesEmptyTags() {
        assertEquals("", TagUtil.normalize(" , "));
    }
}
