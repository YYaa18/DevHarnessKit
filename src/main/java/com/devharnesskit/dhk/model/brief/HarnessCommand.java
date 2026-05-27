package com.devharnesskit.dhk.model.brief;

public final class HarnessCommand {
    private final String name;
    private final String[] argv;
    private final String when;
    private final boolean userVisible;
    private final boolean agentInternalOnly;

    public HarnessCommand(String name, String[] argv, String when,
                          boolean userVisible, boolean agentInternalOnly) {
        this.name = value(name);
        this.argv = argv == null ? new String[0] : argv;
        this.when = value(when);
        this.userVisible = userVisible;
        this.agentInternalOnly = agentInternalOnly;
    }

    public String name() { return name; }
    public String[] argv() { return argv; }
    public String when() { return when; }
    public boolean userVisible() { return userVisible; }
    public boolean agentInternalOnly() { return agentInternalOnly; }

    private static String value(String value) {
        return value == null ? "" : value.trim();
    }
}
