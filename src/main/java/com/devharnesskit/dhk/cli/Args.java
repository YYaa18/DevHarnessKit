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
                String key = token.substring(2);
                if (i + 1 < rawArgs.length && !rawArgs[i + 1].startsWith("--")) {
                    options.put(key, rawArgs[++i]);
                    flags.remove(key);
                } else {
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
}
