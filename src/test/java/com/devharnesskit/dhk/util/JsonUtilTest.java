package com.devharnesskit.dhk.util;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class JsonUtilTest {
    @Test
    void roundTripsSimpleStringObject() {
        Map<String, String> fields = new LinkedHashMap<String, String>();
        fields.put("project_key", "demo");
        fields.put("root_path", "/tmp/path with spaces");
        fields.put("description", "quote \" and slash \\ and newline\n");

        Map<String, String> parsed = JsonUtil.parseObject(JsonUtil.toObject(fields));

        assertEquals(fields, parsed);
    }

    @Test
    void rejectsUnsupportedJsonShape() {
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                JsonUtil.parseObject("{\"a\": 1}");
            }
        });
    }
}
