package com.devharnesskit.dhk.model.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GraphArchitectureCheckResult {
    private final String status;
    private final String summary;
    private final String output;
    private final List<GraphArchitectureViolation> violations;
    private final List<String> publicApiImpactFiles;

    public GraphArchitectureCheckResult(String status, String summary, String output,
                                        List<GraphArchitectureViolation> violations,
                                        List<String> publicApiImpactFiles) {
        this.status = value(status);
        this.summary = value(summary);
        this.output = value(output);
        this.violations = violations == null
                ? Collections.<GraphArchitectureViolation>emptyList()
                : Collections.unmodifiableList(new ArrayList<GraphArchitectureViolation>(violations));
        this.publicApiImpactFiles = publicApiImpactFiles == null
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<String>(publicApiImpactFiles));
    }

    public String status() { return status; }
    public String summary() { return summary; }
    public String output() { return output; }
    public List<GraphArchitectureViolation> violations() { return violations; }
    public List<String> publicApiImpactFiles() { return publicApiImpactFiles; }
    public boolean publicApiImpact() { return !publicApiImpactFiles.isEmpty(); }

    private static String value(String input) {
        return input == null ? "" : input;
    }
}
