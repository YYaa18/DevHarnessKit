package com.devharnesskit.dhk.service;

public final class MemoryStatus {
    private static final String[] VALUES = new String[]{"draft", "confirmed", "deprecated", "archived"};

    private MemoryStatus() {
    }

    public static boolean isAllowed(String value) {
        for (String allowed : VALUES) {
            if (allowed.equals(value)) {
                return true;
            }
        }
        return false;
    }
}
