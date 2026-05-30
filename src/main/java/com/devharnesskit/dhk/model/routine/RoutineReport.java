package com.devharnesskit.dhk.model.routine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RoutineReport {
    public static final String SCHEMA_VERSION = "routine-summary/v1";

    private final String generatedAt;
    private final String since;
    private final String until;
    private final String sourceSchema;
    private final String metricsSchema;
    private final String replaySchema;
    private final RoutineOutcomeSummary outcomes;
    private final List<RoutineProfileSummary> profiles;
    private final List<RoutineCheckSummary> checks;
    private final RoutineInterventionSummary interventions;

    public RoutineReport(String generatedAt, String since, String until,
                         String sourceSchema, String metricsSchema, String replaySchema,
                         RoutineOutcomeSummary outcomes, List<RoutineProfileSummary> profiles,
                         List<RoutineCheckSummary> checks, RoutineInterventionSummary interventions) {
        this.generatedAt = value(generatedAt);
        this.since = value(since);
        this.until = value(until);
        this.sourceSchema = value(sourceSchema);
        this.metricsSchema = value(metricsSchema);
        this.replaySchema = value(replaySchema);
        this.outcomes = outcomes == null ? new RoutineOutcomeSummary(0, 0, 0, 0, 0) : outcomes;
        this.profiles = immutable(profiles);
        this.checks = immutable(checks);
        this.interventions = interventions == null
                ? new RoutineInterventionSummary(0, 0, 0, 0, 0)
                : interventions;
    }

    public String schemaVersion() { return SCHEMA_VERSION; }
    public String generatedAt() { return generatedAt; }
    public String since() { return since; }
    public String until() { return until; }
    public String sourceSchema() { return sourceSchema; }
    public String metricsSchema() { return metricsSchema; }
    public String replaySchema() { return replaySchema; }
    public RoutineOutcomeSummary outcomes() { return outcomes; }
    public List<RoutineProfileSummary> profiles() { return profiles; }
    public List<RoutineCheckSummary> checks() { return checks; }
    public RoutineInterventionSummary interventions() { return interventions; }

    private String value(String raw) {
        return raw == null ? "" : raw;
    }

    private <T> List<T> immutable(List<T> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<T>(values));
    }
}
