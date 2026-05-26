package com.acme.legacy.shop.web;

import java.util.Map;

public class LegacyBaseServlet {
    protected String first(Map<String, String[]> parameters, String name) {
        if (parameters == null || !parameters.containsKey(name)) {
            return null;
        }
        String[] values = parameters.get(name);
        if (values == null || values.length == 0) {
            return null;
        }
        return values[0];
    }

    protected Integer intParam(Map<String, String[]> parameters, String name) {
        String value = first(parameters, name);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
