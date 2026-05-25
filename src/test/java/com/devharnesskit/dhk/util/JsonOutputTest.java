package com.devharnesskit.dhk.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class JsonOutputTest {
    @Test
    void rendersEscapedObjectAndArrays() {
        String json = JsonOutput.object(
                JsonOutput.stringField("message", "quote \" slash \\ newline\n"),
                JsonOutput.numberField("count", 2),
                JsonOutput.booleanField("ok", true),
                JsonOutput.rawField("items", JsonOutput.array(Arrays.asList(
                        JsonOutput.quote("a"),
                        JsonOutput.quote("b")
                )))
        );

        assertTrue(json.contains("\"message\": \"quote \\\" slash \\\\ newline\\n\""));
        assertTrue(json.contains("\"count\": 2"));
        assertTrue(json.contains("\"ok\": true"));
        assertTrue(json.contains("\"items\": [\"a\", \"b\"]"));
    }
}
