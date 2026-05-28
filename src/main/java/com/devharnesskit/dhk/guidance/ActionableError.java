package com.devharnesskit.dhk.guidance;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ActionableError {
    private final String errorCode;
    private final String message;
    private final String reason;
    private final String[] missing;
    private final String[] validValues;
    private final String[] aliases;
    private final String nextCommand;
    private final String nextAction;
    private final String docs;
    private final Map<String, String> details;

    private ActionableError(Builder builder) {
        this.errorCode = value(builder.errorCode);
        this.message = value(builder.message);
        this.reason = value(builder.reason);
        this.missing = array(builder.missing);
        this.validValues = array(builder.validValues);
        this.aliases = array(builder.aliases);
        this.nextCommand = value(builder.nextCommand);
        this.nextAction = value(builder.nextAction);
        this.docs = value(builder.docs);
        this.details = new LinkedHashMap<String, String>(builder.details);
    }

    public String errorCode() { return errorCode; }
    public String message() { return message; }
    public String reason() { return reason; }
    public String[] missing() { return array(missing); }
    public String[] validValues() { return array(validValues); }
    public String[] aliases() { return array(aliases); }
    public String nextCommand() { return nextCommand; }
    public String nextAction() { return nextAction; }
    public String docs() { return docs; }
    public Map<String, String> details() { return new LinkedHashMap<String, String>(details); }

    public static Builder builder(String errorCode, String message) {
        return new Builder(errorCode, message);
    }

    private static String value(String value) {
        return value == null ? "" : value.trim();
    }

    private static String[] array(String[] values) {
        if (values == null) {
            return new String[0];
        }
        String[] copy = new String[values.length];
        System.arraycopy(values, 0, copy, 0, values.length);
        return copy;
    }

    public static final class Builder {
        private final String errorCode;
        private final String message;
        private String reason = "";
        private String[] missing = new String[0];
        private String[] validValues = new String[0];
        private String[] aliases = new String[0];
        private String nextCommand = "";
        private String nextAction = "";
        private String docs = "";
        private final Map<String, String> details = new LinkedHashMap<String, String>();

        private Builder(String errorCode, String message) {
            this.errorCode = errorCode;
            this.message = message;
        }

        public Builder reason(String value) {
            this.reason = value;
            return this;
        }

        public Builder missing(String[] values) {
            this.missing = array(values);
            return this;
        }

        public Builder validValues(String[] values) {
            this.validValues = array(values);
            return this;
        }

        public Builder aliases(String[] values) {
            this.aliases = array(values);
            return this;
        }

        public Builder nextCommand(String value) {
            this.nextCommand = value;
            return this;
        }

        public Builder nextAction(String value) {
            this.nextAction = value;
            return this;
        }

        public Builder docs(String value) {
            this.docs = value;
            return this;
        }

        public Builder detail(String key, String value) {
            if (key != null && key.trim().length() > 0 && value != null && value.trim().length() > 0) {
                this.details.put(key.trim(), value.trim());
            }
            return this;
        }

        public ActionableError build() {
            return new ActionableError(this);
        }
    }
}
