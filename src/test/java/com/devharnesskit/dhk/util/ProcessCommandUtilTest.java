package com.devharnesskit.dhk.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

final class ProcessCommandUtilTest {
    @Test
    void resolvesMavenCommandOnWindows() {
        assertArrayEquals(new String[]{"mvn.cmd", "-q", "test"},
                ProcessCommandUtil.resolveExecutable(new String[]{"mvn", "-q", "test"}, "Windows 11"));
        assertArrayEquals(new String[]{"mvn.cmd", "-q", "test"},
                ProcessCommandUtil.resolveExecutable(new String[]{"mvn.cmd", "-q", "test"}, "Windows 11"));
        assertArrayEquals(new String[]{"mvnw.cmd", "-q", "test"},
                ProcessCommandUtil.resolveExecutable(new String[]{"mvnw", "-q", "test"}, "Windows 11"));
    }

    @Test
    void leavesMavenCommandUnchangedOnUnix() {
        assertArrayEquals(new String[]{"mvn", "-q", "test"},
                ProcessCommandUtil.resolveExecutable(new String[]{"mvn", "-q", "test"}, "Mac OS X"));
    }
}
