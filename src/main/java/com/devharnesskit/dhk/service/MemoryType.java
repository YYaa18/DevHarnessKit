package com.devharnesskit.dhk.service;

public final class MemoryType {
    private static final String[] VALUES = new String[]{
            "project_fact",
            "api_convention",
            "mvc_convention",
            "gateway_convention",
            "database_convention",
            "code_pattern",
            "module_pattern",
            "exception_convention",
            "logging_convention",
            "security_convention",
            "testing_convention",
            "decision",
            "risk",
            "todo"
    };

    private MemoryType() {
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
