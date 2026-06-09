package com.devharnesskit.dhk.context.compress;

public final class TestLogCompressor extends LineSelectingLogCompressor {
    public CompressResult compressTest(String command, int exitCode, String output) {
        return compress(command, exitCode, output);
    }

    protected String sourceType() {
        return "test-log";
    }

    protected boolean isSignal(String lowerLine) {
        return lowerLine.indexOf("tests run:") >= 0
                || lowerLine.indexOf("failures:") >= 0
                || lowerLine.indexOf("errors:") >= 0
                || lowerLine.indexOf("<<< failure") >= 0
                || lowerLine.indexOf("<<< error") >= 0
                || lowerLine.indexOf("assertionerror") >= 0
                || lowerLine.indexOf("surefire-reports") >= 0
                || lowerLine.indexOf("[error]") >= 0
                || lowerLine.indexOf("exception") >= 0
                || lowerLine.startsWith("failed ")
                || lowerLine.startsWith("error ")
                || lowerLine.startsWith("e   ")
                || lowerLine.indexOf("short test summary") >= 0;
    }

    protected String signalReason(String lowerLine) {
        if (lowerLine.indexOf("tests run:") >= 0) {
            return "test-summary";
        }
        if (lowerLine.indexOf("<<< failure") >= 0 || lowerLine.indexOf("assertionerror") >= 0) {
            return "test-failure";
        }
        if (lowerLine.indexOf("<<< error") >= 0 || lowerLine.indexOf("exception") >= 0) {
            return "test-error";
        }
        if (lowerLine.startsWith("failed ") || lowerLine.startsWith("error ")
                || lowerLine.startsWith("e   ") || lowerLine.indexOf("short test summary") >= 0) {
            return "pytest-signal";
        }
        return "test-signal";
    }
}
