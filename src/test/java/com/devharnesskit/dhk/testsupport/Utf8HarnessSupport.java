package com.devharnesskit.dhk.testsupport;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

public final class Utf8HarnessSupport {
    private Utf8HarnessSupport() {
    }

    public static PrintStream printStream(ByteArrayOutputStream output) {
        try {
            return new PrintStream(output, true, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException("UTF-8 is not available", ex);
        }
    }

    public static String text(ByteArrayOutputStream output) {
        try {
            return output.toString("UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException("UTF-8 is not available", ex);
        }
    }
}
