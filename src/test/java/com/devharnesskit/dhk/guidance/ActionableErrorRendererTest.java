package com.devharnesskit.dhk.guidance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ActionableErrorRendererTest {
    @Test
    void rendersTextAndJsonWithActionableFields() {
        ActionableError error = ActionableError.builder("INVALID_SAMPLE_MODE", "Invalid sample mode: robot")
                .reason("sample mode must be documented")
                .missing(new String[]{"--mode"})
                .validValues(new String[]{"auto", "manual"})
                .aliases(new String[]{"default -> auto"})
                .nextCommand("dhk sample --mode auto")
                .docs("docs/GOAL_CONFIGURATION.md")
                .detail("field", "sample.mode")
                .build();

        ActionableErrorRenderer renderer = new ActionableErrorRenderer();
        String text = renderer.renderText(error);
        String json = renderer.renderJson(error);

        assertTrue(text.contains("ERROR: Invalid sample mode: robot"));
        assertTrue(text.contains("error_code: INVALID_SAMPLE_MODE"));
        assertTrue(text.contains("valid_values:"));
        assertTrue(text.contains("default -> auto"));
        assertTrue(text.contains("next_command: dhk sample --mode auto"));

        assertTrue(json.contains("\"ok\": false"));
        assertTrue(json.contains("\"error_code\": \"INVALID_SAMPLE_MODE\""));
        assertTrue(json.contains("\"valid_values\": [\"auto\", \"manual\"]"));
        assertTrue(json.contains("\"aliases\": [\"default -> auto\"]"));
        assertTrue(json.contains("\"next_command\": \"dhk sample --mode auto\""));
    }
}
