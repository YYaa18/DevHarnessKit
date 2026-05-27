package com.devharnesskit.dhk.cli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Args {
    private final List<String> positionals;
    private final Map<String, String> options;
    private final Map<String, List<String>> optionValues;
    private final Set<String> flags;

    private Args(List<String> positionals, Map<String, String> options,
                 Map<String, List<String>> optionValues, Set<String> flags) {
        this.positionals = Collections.unmodifiableList(positionals);
        this.options = Collections.unmodifiableMap(options);
        this.optionValues = immutableOptionValues(optionValues);
        this.flags = Collections.unmodifiableSet(flags);
    }

    public static Args parse(String[] rawArgs) {
        List<String> positionals = new ArrayList<String>();
        Map<String, String> options = new LinkedHashMap<String, String>();
        Map<String, List<String>> optionValues = new LinkedHashMap<String, List<String>>();
        Set<String> flags = new LinkedHashSet<String>();

        for (int i = 0; i < rawArgs.length; i++) {
            String token = rawArgs[i];
            if (token.startsWith("--") && token.length() > 2) {
                String option = token.substring(2);
                int equalsIndex = option.indexOf('=');
                if (equalsIndex >= 0) {
                    String key = option.substring(0, equalsIndex);
                    String value = option.substring(equalsIndex + 1);
                    if (key.length() == 0) {
                        positionals.add(token);
                    } else {
                        options.put(key, value);
                        appendOptionValue(optionValues, key, value);
                        flags.remove(key);
                    }
                } else if (i + 1 < rawArgs.length && !rawArgs[i + 1].startsWith("--")) {
                    String key = option;
                    String value = rawArgs[++i];
                    options.put(key, value);
                    appendOptionValue(optionValues, key, value);
                    flags.remove(key);
                } else {
                    String key = option;
                    flags.add(key);
                    options.remove(key);
                    optionValues.remove(key);
                }
            } else {
                positionals.add(token);
            }
        }

        return new Args(positionals, options, optionValues, flags);
    }

    public List<String> positionals() {
        return positionals;
    }

    public String primaryCommand() {
        return positional(0);
    }

    public String subCommand() {
        return positional(1);
    }

    public String positional(int index) {
        if (index < 0 || index >= positionals.size()) {
            return "";
        }
        return positionals.get(index);
    }

    public boolean hasOption(String key) {
        return options.containsKey(key);
    }

    public String option(String key) {
        return option(key, "");
    }

    public String option(String key, String defaultValue) {
        String value = options.get(key);
        return value == null ? defaultValue : value;
    }

    public boolean hasFlag(String key) {
        return flags.contains(key);
    }

    public Map<String, String> options() {
        return options;
    }

    public List<String> optionValues(String key) {
        List<String> values = optionValues.get(key);
        return values == null ? Collections.<String>emptyList() : values;
    }

    public Set<String> flags() {
        return flags;
    }

    public Args redacted(com.devharnesskit.dhk.service.SensitiveDataGuard guard, Set<String> excludedOptions) {
        List<String> redactedPositionals = new ArrayList<String>();
        for (String positional : positionals) {
            redactedPositionals.add(guard.redact(positional));
        }
        Map<String, String> redactedOptions = new LinkedHashMap<String, String>();
        Map<String, List<String>> redactedOptionValues = new LinkedHashMap<String, List<String>>();
        for (Map.Entry<String, String> entry : options.entrySet()) {
            if (excludedOptions.contains(entry.getKey())) {
                redactedOptions.put(entry.getKey(), entry.getValue());
            } else {
                redactedOptions.put(entry.getKey(), guard.redact(entry.getValue()));
            }
        }
        for (Map.Entry<String, List<String>> entry : optionValues.entrySet()) {
            List<String> values = new ArrayList<String>();
            for (String value : entry.getValue()) {
                values.add(excludedOptions.contains(entry.getKey()) ? value : guard.redact(value));
            }
            redactedOptionValues.put(entry.getKey(), values);
        }
        return new Args(redactedPositionals, redactedOptions, redactedOptionValues,
                new LinkedHashSet<String>(flags));
    }

    private static void appendOptionValue(Map<String, List<String>> optionValues, String key, String value) {
        List<String> values = optionValues.get(key);
        if (values == null) {
            values = new ArrayList<String>();
            optionValues.put(key, values);
        }
        values.add(value);
    }

    private static Map<String, List<String>> immutableOptionValues(Map<String, List<String>> optionValues) {
        Map<String, List<String>> result = new LinkedHashMap<String, List<String>>();
        for (Map.Entry<String, List<String>> entry : optionValues.entrySet()) {
            result.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<String>(entry.getValue())));
        }
        return Collections.unmodifiableMap(result);
    }
}
