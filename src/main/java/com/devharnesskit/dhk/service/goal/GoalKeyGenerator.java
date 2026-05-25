package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.repository.goal.GoalRunRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class GoalKeyGenerator {
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);

    public String uniqueGoalKey(Connection connection, GoalRunRepository repository,
                                String module, Instant now) throws SQLException {
        String base = TIME.format(now) + "-" + slug(module) + "-goal";
        String candidate = base;
        int suffix = 2;
        while (repository.findByKey(connection, candidate) != null) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    public String uniqueChangeKey(Connection connection,
                                  com.devharnesskit.dhk.repository.spec.SpecChangeRepository repository,
                                  String module, String task, Instant now) throws SQLException {
        String base = slug(module) + "-" + slug(task);
        if (base.length() > 80) {
            base = base.substring(0, 80).replaceAll("-+$", "");
        }
        if (base.length() == 0 || "global".equals(base)) {
            base = TIME.format(now) + "-change";
        }
        String candidate = base;
        int suffix = 2;
        while (repository.findByKey(connection, candidate) != null) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private String slug(String value) {
        String lower = value == null ? "" : value.toLowerCase(Locale.ROOT);
        String slug = lower.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return slug.length() == 0 ? "global" : slug;
    }
}
