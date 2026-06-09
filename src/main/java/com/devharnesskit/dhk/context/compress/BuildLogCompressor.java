package com.devharnesskit.dhk.context.compress;

public final class BuildLogCompressor extends LineSelectingLogCompressor {
    public CompressResult compressBuild(String command, int exitCode, String output) {
        return compress(command, exitCode, output);
    }

    protected String sourceType() {
        return "build-log";
    }

    protected boolean isSignal(String lowerLine) {
        return lowerLine.indexOf("[error]") >= 0
                || lowerLine.indexOf("build failure") >= 0
                || lowerLine.indexOf("compilation failure") >= 0
                || lowerLine.indexOf("compilation error") >= 0
                || lowerLine.indexOf("failed to execute goal") >= 0
                || lowerLine.indexOf("cannot find symbol") >= 0
                || lowerLine.indexOf("package ") >= 0 && lowerLine.indexOf(" does not exist") >= 0
                || lowerLine.indexOf("exception") >= 0
                || lowerLine.indexOf("assertionerror") >= 0
                || lowerLine.indexOf("caused by:") >= 0
                || lowerLine.indexOf("build success") >= 0;
    }

    protected String signalReason(String lowerLine) {
        if (lowerLine.indexOf("build success") >= 0) {
            return "build-success";
        }
        if (lowerLine.indexOf("cannot find symbol") >= 0) {
            return "compile-symbol";
        }
        if (lowerLine.indexOf("failed to execute goal") >= 0) {
            return "maven-goal";
        }
        if (lowerLine.indexOf("exception") >= 0) {
            return "exception";
        }
        if (lowerLine.indexOf("assertionerror") >= 0) {
            return "assertion";
        }
        if (lowerLine.indexOf("caused by:") >= 0) {
            return "caused-by";
        }
        return "build-signal";
    }
}
