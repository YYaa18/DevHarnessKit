package com.devharnesskit.dhk.sql;

public final class SqlColumn {
    private final String name;
    private final String type;

    public SqlColumn(String name, String type) {
        this.name = name == null ? "" : name;
        this.type = type == null ? "" : type;
    }

    public String name() {
        return name;
    }

    public String type() {
        return type;
    }
}
