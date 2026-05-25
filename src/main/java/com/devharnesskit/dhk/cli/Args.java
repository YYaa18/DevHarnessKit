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
    private final Set<String> flags;

    private Args(List<String> positionals, Map<String, String> options, Set<String> flags) {
        this.positionals = Collections.unmodifiableList(positionals);
        this.options = Collections.unmodifiableMap(options);
        this.flags = Collections.unmodifiableSet(flags);
    }

    public static Args parse(String[] rawArgs) {
        List<String> positionals = new ArrayList<String>();
        Map<String, String> options = new LinkedHashMap<String, String>();
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
                        flags.remove(key);
                    }
                } else if (i + 1 < rawArgs.length && !rawArgs[i + 1].startsWith("--")) {
                    String key = option;
                    options.put(key, rawArgs[++i]);
                    flags.remove(key);
                } else {
                    String key = option;
                    flags.add(key);
                    options.remove(key);
                }
            } else {
                positionals.add(token);
            }
        }

        return new Args(positionals, options, flags);
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

    public Set<String> flags() {
        return flags;
    }

    public Args redacted(com.devharnesskit.dhk.service.SensitiveDataGuard guard, Set<String> excludedOptions) {
        List<String> redactedPositionals = new ArrayList<String>();
        for (String positional : positionals) {
            redactedPositionals.add(guard.redact(positional));
        }
        Map<String, String> redactedOptions = new LinkedHashMap<String, String>();
        for (Map.Entry<String, String> entry : options.entrySet()) {
            if (excludedOptions.contains(entry.getKey())) {
                redactedOptions.put(entry.getKey(), entry.getValue());
            } else {
                redactedOptions.put(entry.getKey(), guard.redact(entry.getValue()));
            }
        }
        return new Args(redactedPositionals, redactedOptions, new LinkedHashSet<String>(flags));
    }
}
