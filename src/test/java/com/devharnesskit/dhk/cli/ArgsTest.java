package com.devharnesskit.dhk.cli;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ArgsTest {
    @Test
    void parsesPositionalsAndKeyValueOptions() {
        Args args = Args.parse(new String[]{
                "memory", "add",
                "--type", "project_fact",
                "--title", "Gateway rule"
        });

        assertEquals("memory", args.primaryCommand());
        assertEquals("add", args.subCommand());
        assertEquals("project_fact", args.option("type"));
        assertEquals("Gateway rule", args.option("title"));
    }

    @Test
    void parsesFlags() {
        Args args = Args.parse(new String[]{"memory", "recover", "--latest"});

        assertTrue(args.hasFlag("latest"));
        assertFalse(args.hasOption("latest"));
    }

    @Test
    void repeatedOptionsUseLastValue() {
        Args args = Args.parse(new String[]{"memory", "search", "--limit", "10", "--limit", "20"});

        assertEquals("20", args.option("limit"));
    }
}
