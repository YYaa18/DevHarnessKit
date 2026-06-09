package com.devharnesskit.dhk.context.compress;

public final class ShellOutputCompressor extends LineSelectingLogCompressor {
    public CompressResult compressShell(String command, int exitCode, String output) {
        return compress(command, exitCode, output);
    }

    protected String sourceType() {
        return "shell-output";
    }

    protected boolean isSignal(String lowerLine) {
        return lowerLine.indexOf("error") >= 0
                || lowerLine.indexOf("failed") >= 0
                || lowerLine.indexOf("exception") >= 0
                || lowerLine.indexOf("traceback") >= 0
                || lowerLine.indexOf("permission denied") >= 0
                || lowerLine.indexOf("not found") >= 0;
    }

    protected String signalReason(String lowerLine) {
        if (lowerLine.indexOf("traceback") >= 0) {
            return "traceback";
        }
        if (lowerLine.indexOf("permission denied") >= 0) {
            return "permission";
        }
        if (lowerLine.indexOf("not found") >= 0) {
            return "not-found";
        }
        return "shell-signal";
    }
}
