package com.devharnesskit.dhk.service.bdd;

import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.bdd.BddEvidence;
import com.devharnesskit.dhk.model.bdd.BddScenarioView;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.Collections;
import java.util.Locale;

public final class JsonReportBddEvidenceService {
    public ReportEvidenceImportResult importReport(Connection connection, Project project, BddService bddService,
                                                   Path projectRoot, String adapterKey, Path reportPath,
                                                   String scenarioKey, String goalKey, String now)
            throws Exception {
        if (!isAdapterAllowed(adapterKey)) {
            throw new IllegalArgumentException("Unsupported BDD report adapter: " + adapterKey);
        }
        BddScenarioView scenario = bddService.findScenario(connection, project, scenarioKey);
        if (scenario == null) {
            return new ReportEvidenceImportResult(adapterKey, 0, null, "missing", "");
        }
        Path resolvedReport = reportPath.isAbsolute() ? reportPath : projectRoot.resolve(reportPath).normalize();
        if (!Files.isRegularFile(resolvedReport)) {
            BddEvidence evidence = bddService.addEvidence(connection, project, scenario.scenario().scenarioKey(),
                    goalKey, "test", "pending", "", adapterKey + " report missing: " + reportPath,
                    adapterKey + ":" + reportPath.toString(), now);
            return new ReportEvidenceImportResult(adapterKey, 1, evidence, "pending", "");
        }
        String json = new String(Files.readAllBytes(resolvedReport), "UTF-8");
        String status = status(adapterKey, json);
        String relativePath = relativize(projectRoot, resolvedReport);
        BddEvidence evidence = bddService.addEvidence(connection, project, scenario.scenario().scenarioKey(),
                goalKey, "test", status, relativePath,
                adapterKey + " report " + status + " for " + scenario.scenario().scenarioKey(),
                adapterKey + ":" + relativePath, now);
        return new ReportEvidenceImportResult(adapterKey, 1, evidence, status, relativePath);
    }

    public boolean isAdapterAllowed(String adapterKey) {
        return "cucumber".equals(adapterKey) || "postman".equals(adapterKey)
                || "playwright".equals(adapterKey);
    }

    private String status(String adapterKey, String json) {
        String compact = json.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
        if ("cucumber".equals(adapterKey)) {
            return cucumberStatus(compact);
        }
        if ("postman".equals(adapterKey)) {
            return postmanStatus(compact);
        }
        if ("playwright".equals(adapterKey)) {
            return playwrightStatus(compact);
        }
        return "pending";
    }

    private String cucumberStatus(String compact) {
        if (compact.contains("\"status\":\"failed\"")
                || compact.contains("\"status\":\"undefined\"")
                || compact.contains("\"status\":\"ambiguous\"")) {
            return "failed";
        }
        if (compact.contains("\"status\":\"pending\"")) {
            return "pending";
        }
        if (compact.contains("\"status\":\"skipped\"")) {
            return "skipped";
        }
        return compact.contains("\"status\":\"passed\"") ? "passed" : "pending";
    }

    private String postmanStatus(String compact) {
        if (compact.contains("\"failures\":[{") || compact.matches(".*\"failed\":[1-9][0-9]*.*")) {
            return "failed";
        }
        if (compact.contains("\"skipped\":[1-9]") || compact.contains("\"pending\":[1-9]")) {
            return "skipped";
        }
        if (compact.contains("\"failures\":[]") || compact.contains("\"failed\":0")) {
            return "passed";
        }
        return "pending";
    }

    private String playwrightStatus(String compact) {
        if (compact.contains("\"status\":\"failed\"")
                || compact.contains("\"status\":\"timedout\"")
                || compact.contains("\"status\":\"interrupted\"")) {
            return "failed";
        }
        if (compact.contains("\"status\":\"skipped\"")) {
            return "skipped";
        }
        return compact.contains("\"status\":\"passed\"") ? "passed" : "pending";
    }

    private String relativize(Path projectRoot, Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        Path root = projectRoot.toAbsolutePath().normalize();
        if (normalized.startsWith(root)) {
            return root.relativize(normalized).toString();
        }
        return normalized.toString();
    }

    public static final class ReportEvidenceImportResult {
        private final String adapterKey;
        private final int scenarioCount;
        private final BddEvidence evidence;
        private final String status;
        private final String reportPath;

        public ReportEvidenceImportResult(String adapterKey, int scenarioCount, BddEvidence evidence,
                                          String status, String reportPath) {
            this.adapterKey = adapterKey;
            this.scenarioCount = scenarioCount;
            this.evidence = evidence;
            this.status = status;
            this.reportPath = reportPath == null ? "" : reportPath;
        }

        public String adapterKey() { return adapterKey; }
        public int scenarioCount() { return scenarioCount; }
        public java.util.List<BddEvidence> evidence() {
            return evidence == null ? Collections.<BddEvidence>emptyList() : Collections.singletonList(evidence);
        }
        public int evidenceCount() { return evidence == null ? 0 : 1; }
        public String status() { return status; }
        public String reportPath() { return reportPath; }
    }
}
