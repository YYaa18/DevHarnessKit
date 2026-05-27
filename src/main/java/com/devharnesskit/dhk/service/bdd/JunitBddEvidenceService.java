package com.devharnesskit.dhk.service.bdd;

import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.bdd.BddBinding;
import com.devharnesskit.dhk.model.bdd.BddEvidence;
import com.devharnesskit.dhk.model.bdd.BddScenario;
import com.devharnesskit.dhk.model.bdd.BddScenarioView;
import com.devharnesskit.dhk.service.bdd.adapter.JunitBddEvidenceAdapter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public final class JunitBddEvidenceService {
    public JunitEvidenceImportResult importReports(Connection connection, Project project, BddService bddService,
                                                   Path projectRoot, List<Path> reportRoots,
                                                   String scenarioKey, String goalKey, String now)
            throws Exception {
        Map<String, JunitCaseResult> testResults = loadReports(projectRoot, reportRoots);
        List<BddScenarioView> scenarios = selectedScenarios(connection, project, bddService, scenarioKey);
        List<BddEvidence> evidence = new ArrayList<BddEvidence>();
        int bindingCount = 0;
        int passedCount = 0;
        int failedCount = 0;
        int skippedCount = 0;
        int pendingCount = 0;
        for (BddScenarioView scenario : scenarios) {
            for (BddBinding binding : scenario.bindings()) {
                if (!"test".equals(binding.bindingType())) {
                    continue;
                }
                bindingCount++;
                JunitCaseResult result = testResults.get(binding.bindingKey());
                String status = result == null ? "pending" : result.status();
                if ("passed".equals(status)) {
                    passedCount++;
                } else if ("failed".equals(status)) {
                    failedCount++;
                } else if ("skipped".equals(status)) {
                    skippedCount++;
                } else {
                    pendingCount++;
                }
                String path = result == null ? "" : relativize(projectRoot, result.reportPath());
                String summary = summary(binding.bindingKey(), status, result);
                evidence.add(bddService.addEvidence(connection, project, scenario.scenario().scenarioKey(),
                        goalKey, "test", status, path, summary,
                        JunitBddEvidenceAdapter.COMMAND_PREFIX + binding.bindingKey(), now));
            }
        }
        return new JunitEvidenceImportResult(scenarios.size(), bindingCount, evidence,
                passedCount, failedCount, skippedCount, pendingCount, testResults.size());
    }

    private Map<String, JunitCaseResult> loadReports(Path projectRoot, List<Path> reportRoots)
            throws ParserConfigurationException, IOException, SAXException {
        Map<String, JunitCaseResult> results = new LinkedHashMap<String, JunitCaseResult>();
        for (Path reportRoot : reportRoots) {
            Path root = reportRoot.isAbsolute() ? reportRoot : projectRoot.resolve(reportRoot).normalize();
            if (!Files.isDirectory(root)) {
                continue;
            }
            List<Path> xmlFiles = xmlFiles(root);
            for (Path xmlFile : xmlFiles) {
                parseReport(xmlFile, results);
            }
        }
        return results;
    }

    private List<Path> xmlFiles(Path root) throws IOException {
        List<Path> files = new ArrayList<Path>();
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(path -> Files.isRegularFile(path)
                    && path.getFileName().toString().endsWith(".xml"))
                    .forEach(files::add);
        }
        Collections.sort(files);
        return files;
    }

    private void parseReport(Path xmlFile, Map<String, JunitCaseResult> results)
            throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = secureFactory();
        Document document = factory.newDocumentBuilder().parse(xmlFile.toFile());
        Element suite = document.getDocumentElement();
        String suiteName = suite == null ? "" : suite.getAttribute("name");
        NodeList testCases = document.getElementsByTagName("testcase");
        for (int i = 0; i < testCases.getLength(); i++) {
            Element testCase = (Element) testCases.item(i);
            String className = value(testCase.getAttribute("classname"));
            if (className.length() == 0) {
                className = value(suiteName);
            }
            String methodName = value(testCase.getAttribute("name"));
            if (className.length() == 0 || methodName.length() == 0) {
                continue;
            }
            String status = status(testCase);
            JunitCaseResult exact = new JunitCaseResult(className, methodName,
                    className + "#" + methodName, status, xmlFile);
            putDominant(results, exact.bindingKey(), exact);
            JunitCaseResult classResult = new JunitCaseResult(className, "",
                    className, status, xmlFile);
            putDominant(results, classResult.bindingKey(), classResult);
        }
    }

    private DocumentBuilderFactory secureFactory() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory;
    }

    private void putDominant(Map<String, JunitCaseResult> results, String key, JunitCaseResult next) {
        JunitCaseResult current = results.get(key);
        if (current == null || rank(next.status()) > rank(current.status())) {
            results.put(key, next);
        }
    }

    private int rank(String status) {
        if ("failed".equals(status)) {
            return 3;
        }
        if ("skipped".equals(status)) {
            return 2;
        }
        if ("passed".equals(status)) {
            return 1;
        }
        return 0;
    }

    private String status(Element testCase) {
        if (testCase.getElementsByTagName("failure").getLength() > 0
                || testCase.getElementsByTagName("error").getLength() > 0) {
            return "failed";
        }
        if (testCase.getElementsByTagName("skipped").getLength() > 0) {
            return "skipped";
        }
        return "passed";
    }

    private List<BddScenarioView> selectedScenarios(Connection connection, Project project,
                                                    BddService bddService, String scenarioKey)
            throws SQLException {
        List<BddScenarioView> views = new ArrayList<BddScenarioView>();
        if (scenarioKey != null && scenarioKey.length() > 0) {
            BddScenarioView view = bddService.findScenario(connection, project, scenarioKey);
            if (view != null) {
                views.add(view);
            }
            return views;
        }
        for (BddScenario scenario : bddService.listScenarios(connection, project, "")) {
            if ("archived".equals(scenario.status()) || "deprecated".equals(scenario.status())) {
                continue;
            }
            BddScenarioView view = bddService.findScenario(connection, project, scenario.scenarioKey());
            if (view != null) {
                views.add(view);
            }
        }
        return views;
    }

    private String summary(String bindingKey, String status, JunitCaseResult result) {
        if (result == null) {
            return "JUnit result missing for " + bindingKey;
        }
        return "JUnit result " + status + " for " + bindingKey;
    }

    private String relativize(Path projectRoot, Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        Path root = projectRoot.toAbsolutePath().normalize();
        if (normalized.startsWith(root)) {
            return root.relativize(normalized).toString();
        }
        return normalized.toString();
    }

    private String value(String raw) {
        return raw == null ? "" : raw.trim();
    }

    public static final class JunitEvidenceImportResult {
        private final int scenarioCount;
        private final int bindingCount;
        private final List<BddEvidence> evidence;
        private final int passedCount;
        private final int failedCount;
        private final int skippedCount;
        private final int pendingCount;
        private final int reportResultCount;

        public JunitEvidenceImportResult(int scenarioCount, int bindingCount, List<BddEvidence> evidence,
                                         int passedCount, int failedCount, int skippedCount,
                                         int pendingCount, int reportResultCount) {
            this.scenarioCount = scenarioCount;
            this.bindingCount = bindingCount;
            this.evidence = Collections.unmodifiableList(evidence);
            this.passedCount = passedCount;
            this.failedCount = failedCount;
            this.skippedCount = skippedCount;
            this.pendingCount = pendingCount;
            this.reportResultCount = reportResultCount;
        }

        public int scenarioCount() { return scenarioCount; }
        public int bindingCount() { return bindingCount; }
        public List<BddEvidence> evidence() { return evidence; }
        public int evidenceCount() { return evidence.size(); }
        public int passedCount() { return passedCount; }
        public int failedCount() { return failedCount; }
        public int skippedCount() { return skippedCount; }
        public int pendingCount() { return pendingCount; }
        public int reportResultCount() { return reportResultCount; }
    }

    private static final class JunitCaseResult {
        private final String className;
        private final String methodName;
        private final String bindingKey;
        private final String status;
        private final Path reportPath;

        private JunitCaseResult(String className, String methodName, String bindingKey,
                                String status, Path reportPath) {
            this.className = className;
            this.methodName = methodName;
            this.bindingKey = bindingKey;
            this.status = status;
            this.reportPath = reportPath;
        }

        public String className() { return className; }
        public String methodName() { return methodName; }
        public String bindingKey() { return bindingKey; }
        public String status() { return status; }
        public Path reportPath() { return reportPath; }
    }
}
