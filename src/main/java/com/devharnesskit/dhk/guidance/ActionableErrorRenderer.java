package com.devharnesskit.dhk.guidance;

import com.devharnesskit.dhk.util.JsonOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ActionableErrorRenderer {
    public String renderText(ActionableError error) {
        StringBuilder builder = new StringBuilder();
        builder.append("ERROR: ").append(error.message()).append('\n');
        appendScalar(builder, "error_code", error.errorCode());
        appendScalar(builder, "reason", error.reason());
        appendArray(builder, "missing", error.missing());
        appendArray(builder, "valid_values", error.validValues());
        appendArray(builder, "aliases", error.aliases());
        for (Map.Entry<String, String> entry : error.details().entrySet()) {
            appendScalar(builder, entry.getKey(), entry.getValue());
        }
        appendScalar(builder, "next_command", error.nextCommand());
        appendScalar(builder, "next_action", error.nextAction());
        appendScalar(builder, "docs", error.docs());
        return builder.toString();
    }

    public String renderJson(ActionableError error) {
        List<String> details = new ArrayList<String>();
        for (Map.Entry<String, String> entry : error.details().entrySet()) {
            details.add(JsonOutput.object(
                    JsonOutput.stringField("key", entry.getKey()),
                    JsonOutput.stringField("value", entry.getValue())).trim());
        }
        return JsonOutput.object(
                JsonOutput.booleanField("ok", false),
                JsonOutput.stringField("error_code", error.errorCode()),
                JsonOutput.stringField("message", error.message()),
                JsonOutput.stringField("reason", error.reason()),
                JsonOutput.rawField("missing", JsonOutput.stringArray(error.missing())),
                JsonOutput.rawField("valid_values", JsonOutput.stringArray(error.validValues())),
                JsonOutput.rawField("aliases", JsonOutput.stringArray(error.aliases())),
                JsonOutput.rawField("details", JsonOutput.array(details)),
                JsonOutput.stringField("next_command", error.nextCommand()),
                JsonOutput.stringField("next_action", error.nextAction()),
                JsonOutput.stringField("docs", error.docs())
        );
    }

    private void appendScalar(StringBuilder builder, String label, String value) {
        if (value == null || value.length() == 0) {
            return;
        }
        builder.append(label).append(": ").append(value).append('\n');
    }

    private void appendArray(StringBuilder builder, String label, String[] values) {
        if (values == null || values.length == 0) {
            return;
        }
        builder.append(label).append(":\n");
        for (String value : values) {
            builder.append("  - ").append(value).append('\n');
        }
    }
}
