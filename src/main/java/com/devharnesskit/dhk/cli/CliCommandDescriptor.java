package com.devharnesskit.dhk.cli;

public final class CliCommandDescriptor {
    private final String name;
    private final String description;
    private final String[] subcommands;
    private final String[] options;

    CliCommandDescriptor(String name, String description, String[] subcommands, String[] options) {
        this.name = name;
        this.description = description;
        this.subcommands = copy(subcommands);
        this.options = copy(options);
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public String[] subcommands() {
        return copy(subcommands);
    }

    public String[] options() {
        return copy(options);
    }

    public boolean hasSubcommands() {
        return subcommands.length > 0;
    }

    private static String[] copy(String[] values) {
        if (values == null || values.length == 0) {
            return new String[0];
        }
        String[] result = new String[values.length];
        System.arraycopy(values, 0, result, 0, values.length);
        return result;
    }
}
