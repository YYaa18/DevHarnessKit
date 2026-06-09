package com.devharnesskit.dhk.context.compress;

import com.devharnesskit.dhk.util.JsonOutput;

public final class RetainedSpan {
    private final int lineNumber;
    private final String reason;
    private final String text;

    public RetainedSpan(int lineNumber, String reason, String text) {
        this.lineNumber = lineNumber;
        this.reason = reason == null ? "" : reason;
        this.text = text == null ? "" : text;
    }

    public int lineNumber() {
        return lineNumber;
    }

    public String reason() {
        return reason;
    }

    public String text() {
        return text;
    }

    public String toJson() {
        return JsonOutput.object(
                JsonOutput.numberField("line", lineNumber),
                JsonOutput.stringField("reason", reason),
                JsonOutput.stringField("text", text)
        ).trim();
    }
}
