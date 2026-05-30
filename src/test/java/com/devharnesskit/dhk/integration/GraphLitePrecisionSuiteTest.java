package com.devharnesskit.dhk.integration;

import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.CommandRouter;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.util.Clock;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GraphLitePrecisionSuiteTest {
    private static final double MIN_AVERAGE_RECALL = 0.85;
    private static final double MIN_AVERAGE_PRECISION = 0.45;
    private static final double MIN_AVERAGE_TOP10_PRECISION = 0.45;

    @TempDir
    Path tempDir;

    @Test
    void graphLitePrecisionSuiteScoresCuratedGroundTruth() throws Exception {
        List<PrecisionScore> scores = new ArrayList<PrecisionScore>();
        for (TaskCase taskCase : taskCases()) {
            scores.add(runCase(taskCase));
        }

        double recall = average(scores, "recall");
        double precision = average(scores, "precision");
        double top10 = average(scores, "top10");
        Path resultPath = writeResultJson(scores, recall, precision, top10);

        assertTrue(recall >= MIN_AVERAGE_RECALL, "average recall too low: " + report(scores));
        assertTrue(precision >= MIN_AVERAGE_PRECISION, "average precision too low: " + report(scores));
        assertTrue(top10 >= MIN_AVERAGE_TOP10_PRECISION, "average top10 precision too low: " + report(scores));
        assertTrue(Files.isRegularFile(resultPath), "precision result JSON should be written");
        String json = new String(Files.readAllBytes(resultPath), "UTF-8");
        assertTrue(json.contains("\"schema_version\": \"devharness-graph-lite-precision-result/v1\""));
        assertTrue(json.contains("\"cases\": ["));
        assertTrue(json.contains("\"missing_expected\": ["));
        assertTrue(json.contains("\"forbidden_hits\": ["));
        for (PrecisionScore score : scores) {
            assertTrue(score.recall() >= score.taskCase.minRecall, score.taskCase.id + " recall: " + score);
            assertTrue(score.precision() >= score.taskCase.minPrecision, score.taskCase.id + " precision: " + score);
            assertTrue(score.top1MatchesStart(), score.taskCase.id + " top1 start file: " + score.recommended);
            assertTrue(score.hasExpectedTestInTop10(), score.taskCase.id + " expected test top10: " + score);
            assertTrue(score.forbiddenHits.isEmpty(), score.taskCase.id + " forbidden hits: " + score.forbiddenHits);
            assertTrue(score.falsePositiveHits.isEmpty(),
                    score.taskCase.id + " known false positives: " + score.falsePositiveHits);
        }
    }

    @Test
    void graphLitePrecisionSuiteKeepsSqlResourceInLegacyJspImpact() throws Exception {
        PrecisionScore score = runCase(TaskCase.legacyJspTier());

        assertTrue(score.reported.contains("src/main/resources/sql/shop-order-search.sql"),
                "JSP/Servlet DAO impact should include the external SQL resource: " + score);
        assertFalse(score.recommended.contains("src/main/webapp/WEB-INF/jsp/common/header.jsp"),
                "shared JSP header should not be a recommended read");
        assertFalse(score.recommended.contains("src/main/java/com/acme/legacy/shop/web/LegacyBaseServlet.java"),
                "shared base servlet should not be a recommended read");
    }

    private Path writeResultJson(List<PrecisionScore> scores, double recall, double precision,
                                 double top10) throws Exception {
        String configured = System.getProperty("dhk.graph.precision.result", "").trim();
        Path out = configured.length() > 0
                ? Paths.get(configured)
                : Paths.get("target/graph-lite-precision-result.json");
        Path parent = out.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(out, resultJson(scores, recall, precision, top10).getBytes("UTF-8"));
        return out;
    }

    private String resultJson(List<PrecisionScore> scores, double recall, double precision, double top10) {
        List<String> cases = new ArrayList<String>();
        boolean passed = recall >= MIN_AVERAGE_RECALL
                && precision >= MIN_AVERAGE_PRECISION
                && top10 >= MIN_AVERAGE_TOP10_PRECISION;
        for (PrecisionScore score : scores) {
            passed = passed && score.passed();
            cases.add(score.toJson());
        }
        return JsonOutput.object(
                JsonOutput.stringField("schema_version", "devharness-graph-lite-precision-result/v1"),
                JsonOutput.stringField("generated_at", "2026-05-30T00:00:00Z"),
                JsonOutput.stringField("suite", "graph-lite-precision"),
                JsonOutput.booleanField("passed", passed),
                JsonOutput.numberField("case_count", scores.size()),
                JsonOutput.rawField("average_recall", decimal(recall)),
                JsonOutput.rawField("average_precision_strict", decimal(precision)),
                JsonOutput.rawField("average_top10_precision", decimal(top10)),
                JsonOutput.rawField("thresholds", JsonOutput.object(
                        JsonOutput.rawField("min_average_recall", decimal(MIN_AVERAGE_RECALL)),
                        JsonOutput.rawField("min_average_precision_strict", decimal(MIN_AVERAGE_PRECISION)),
                        JsonOutput.rawField("min_average_top10_precision", decimal(MIN_AVERAGE_TOP10_PRECISION))
                ).trim()),
                JsonOutput.rawField("cases", JsonOutput.array(cases))
        );
    }

    private PrecisionScore runCase(TaskCase taskCase) throws Exception {
        Path fixture = copyFixture(taskCase.fixture, tempDir.resolve(taskCase.id));
        Harness indexHarness = new Harness(tempDir);
        int indexExit = new CommandRouter().run(new String[]{
                "graph", "index", "--project-root", taskCase.id
        }, indexHarness.context());
        assertEquals(ExitCodes.SUCCESS, indexExit, indexHarness.stderr());

        Harness impactHarness = new Harness(tempDir);
        List<String> args = new ArrayList<String>();
        args.add("graph");
        args.add("impact");
        args.add("--project-root");
        args.add(taskCase.id);
        args.add(taskCase.queryFlag);
        args.add(taskCase.query);
        args.add("--depth");
        args.add(String.valueOf(taskCase.depth));
        int impactExit = new CommandRouter().run(args.toArray(new String[args.size()]), impactHarness.context());
        assertEquals(ExitCodes.SUCCESS, impactExit, impactHarness.stderr());

        GroundTruth groundTruth = GroundTruth.parse(Paths.get("testbeds/fixtures")
                .resolve(taskCase.fixture).resolve("tasks").resolve(taskCase.task).resolve("ground-truth.json"));
        ImpactMap impactMap = ImpactMap.parse(PathUtil.graphImpactMap(fixture));
        return PrecisionScore.score(taskCase, groundTruth, impactMap);
    }

    private List<TaskCase> taskCases() {
        return Arrays.asList(
                new TaskCase("modern-risk-rating", "modern-java-api", "add-risk-rating-field",
                        "--file", "src/main/java/com/acme/modern/account/dto/AccountResponse.java", 4,
                        "src/main/java/com/acme/modern/account/dto/AccountResponse.java", 0.75, 0.45,
                        "src/main/java/com/acme/modern/account/dto/AccountUpdateRequest.java",
                        "src/main/java/com/acme/modern/account/support/HttpGet.java",
                        "src/main/java/com/acme/modern/account/support/HttpPost.java"),
                new TaskCase("modern-freeze-boundary", "modern-java-api", "add-freeze-endpoint-boundary",
                        "--file", "src/main/java/com/acme/modern/account/controller/AccountController.java", 4,
                        "src/main/java/com/acme/modern/account/controller/AccountController.java", 0.80, 0.35,
                        "src/main/java/com/acme/modern/account/support/HttpGet.java",
                        "src/main/java/com/acme/modern/account/support/HttpPost.java"),
                new TaskCase("modern-status-gap", "modern-java-api", "add-status-search-test-gap",
                        "--file", "src/main/java/com/acme/modern/account/service/AccountService.java", 4,
                        "src/main/java/com/acme/modern/account/service/AccountService.java", 0.85, 0.40,
                        "src/test/java/com/acme/modern/account/repository/InMemoryAccountRepositoryTest.java"),
                new TaskCase("legacy-mybatis-customer-level", "legacy-mybatis-order", "add-customer-level-field",
                        "--sql-table", "legacy_order", 6,
                        "src/main/resources/mybatis/OrderMapper.xml", 0.80, 0.45,
                        "src/main/resources/application-prod.properties"),
                new TaskCase("legacy-mybatis-paid-filter", "legacy-mybatis-order", "tighten-paid-order-filter",
                        "--symbol", "paidOnly", 3,
                        "src/main/resources/mybatis/OrderMapper.xml", 0.85, 0.45,
                        "src/main/java/com/acme/legacy/order/web/OrderController.java",
                        "src/main/java/com/acme/legacy/order/util/LegacyPageBounds.java"),
                new TaskCase("legacy-mybatis-page-bounds", "legacy-mybatis-order", "fix-null-page-bounds",
                        "--symbol", "LegacyPageBounds", 3,
                        "src/main/java/com/acme/legacy/order/util/LegacyPageBounds.java", 0.90, 0.40,
                        "src/main/resources/mybatis/OrderMapper.xml",
                        "src/main/java/com/acme/legacy/order/web/OrderController.java"),
                TaskCase.legacyJspTier(),
                new TaskCase("legacy-jsp-search-action", "legacy-jsp-servlet-shop", "preserve-legacy-search-action",
                        "--file", "src/main/webapp/WEB-INF/web.xml", 3,
                        "src/main/webapp/WEB-INF/web.xml", 0.75, 0.35,
                        "src/main/resources/sql/shop-order-search.sql",
                        "src/main/webapp/WEB-INF/jsp/common/header.jsp",
                        "src/main/java/com/acme/legacy/shop/web/LegacyBaseServlet.java")
        );
    }

    private double average(List<PrecisionScore> scores, String metric) {
        double total = 0.0;
        for (PrecisionScore score : scores) {
            if ("recall".equals(metric)) {
                total += score.recall();
            } else if ("precision".equals(metric)) {
                total += score.precision();
            } else {
                total += score.top10Precision();
            }
        }
        return scores.isEmpty() ? 0.0 : total / scores.size();
    }

    private String report(List<PrecisionScore> scores) {
        StringBuilder builder = new StringBuilder();
        for (PrecisionScore score : scores) {
            if (builder.length() > 0) {
                builder.append("; ");
            }
            builder.append(score.toString());
        }
        return builder.toString();
    }

    private Path copyFixture(String fixtureName, final Path target) throws Exception {
        final Path source = Paths.get("testbeds/fixtures").resolve(fixtureName);
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws java.io.IOException {
                Files.createDirectories(target.resolve(source.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws java.io.IOException {
                Path relative = source.relativize(file);
                Files.copy(file, target.resolve(relative), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
        return target;
    }

    private static final class TaskCase {
        private final String id;
        private final String fixture;
        private final String task;
        private final String queryFlag;
        private final String query;
        private final int depth;
        private final String expectedTop1;
        private final double minRecall;
        private final double minPrecision;
        private final List<String> knownFalsePositives;

        private TaskCase(String id, String fixture, String task, String queryFlag, String query, int depth,
                         String expectedTop1, double minRecall, double minPrecision,
                         String... knownFalsePositives) {
            this.id = id;
            this.fixture = fixture;
            this.task = task;
            this.queryFlag = queryFlag;
            this.query = query;
            this.depth = depth;
            this.expectedTop1 = expectedTop1;
            this.minRecall = minRecall;
            this.minPrecision = minPrecision;
            this.knownFalsePositives = Arrays.asList(knownFalsePositives);
        }

        private static TaskCase legacyJspTier() {
            return new TaskCase("legacy-jsp-customer-tier", "legacy-jsp-servlet-shop",
                    "add-customer-tier-filter", "--symbol", "ShopOrderServlet", 3,
                    "src/main/java/com/acme/legacy/shop/web/ShopOrderServlet.java", 0.80, 0.45,
                    "src/main/webapp/WEB-INF/jsp/common/header.jsp",
                    "src/main/java/com/acme/legacy/shop/web/LegacyBaseServlet.java",
                    "src/main/resources/application-prod.properties");
        }

        private String graphQuery() {
            return "dhk graph impact " + queryFlag + " " + query + " --depth " + depth;
        }
    }

    private static final class GroundTruth {
        private static final Pattern FILE_FIELD = Pattern.compile("\"file\"\\s*:\\s*\"([^\"]+)\"");
        private final Set<String> expected;
        private final Set<String> expectedTests;
        private final Set<String> forbidden;

        private GroundTruth(Set<String> expected, Set<String> expectedTests, Set<String> forbidden) {
            this.expected = expected;
            this.expectedTests = expectedTests;
            this.forbidden = forbidden;
        }

        private static GroundTruth parse(Path path) throws Exception {
            String json = new String(Files.readAllBytes(path), "UTF-8");
            Set<String> expected = new LinkedHashSet<String>();
            Matcher matcher = FILE_FIELD.matcher(json);
            while (matcher.find()) {
                expected.add(matcher.group(1));
            }
            Set<String> expectedTests = stringArray(json, "expected_related_tests");
            expected.addAll(expectedTests);
            return new GroundTruth(expected, expectedTests, stringArray(json, "forbidden_changed_files"));
        }

        private static Set<String> stringArray(String json, String key) {
            Set<String> values = new LinkedHashSet<String>();
            Pattern arrayPattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\\[(.*?)\\]",
                    Pattern.DOTALL);
            Matcher arrayMatcher = arrayPattern.matcher(json);
            if (!arrayMatcher.find()) {
                return values;
            }
            Matcher valueMatcher = Pattern.compile("\"([^\"]+)\"").matcher(arrayMatcher.group(1));
            while (valueMatcher.find()) {
                values.add(valueMatcher.group(1));
            }
            return values;
        }
    }

    private static final class ImpactMap {
        private final Map<String, List<String>> sections;

        private ImpactMap(Map<String, List<String>> sections) {
            this.sections = sections;
        }

        private static ImpactMap parse(Path path) throws Exception {
            Map<String, List<String>> sections = new LinkedHashMap<String, List<String>>();
            String current = "";
            for (String line : Files.readAllLines(path)) {
                if (line.startsWith("<") && !line.startsWith("</") && line.endsWith(">")) {
                    current = line.substring(1, line.length() - 1);
                    sections.put(current, new ArrayList<String>());
                } else if (line.startsWith("</")) {
                    current = "";
                } else if (current.length() > 0) {
                    sections.get(current).add(line);
                }
            }
            return new ImpactMap(sections);
        }

        private Set<String> fileList(String section) {
            Set<String> values = new LinkedHashSet<String>();
            List<String> lines = sections.get(section);
            if (lines == null) {
                return values;
            }
            for (String line : lines) {
                if (line.startsWith("- ")) {
                    values.add(line.substring(2).trim());
                }
            }
            return values;
        }

        private List<String> orderedFileList(String section) {
            return new ArrayList<String>(fileList(section));
        }
    }

    private static final class PrecisionScore {
        private final TaskCase taskCase;
        private final Set<String> expected;
        private final Set<String> reported;
        private final Set<String> missing;
        private final Set<String> falsePositives;
        private final Set<String> forbiddenHits;
        private final Set<String> falsePositiveHits;
        private final List<String> recommended;
        private final Set<String> expectedTests;

        private PrecisionScore(TaskCase taskCase, Set<String> expected, Set<String> reported,
                               Set<String> missing, Set<String> falsePositives, Set<String> forbiddenHits,
                               Set<String> falsePositiveHits, List<String> recommended,
                               Set<String> expectedTests) {
            this.taskCase = taskCase;
            this.expected = expected;
            this.reported = reported;
            this.missing = missing;
            this.falsePositives = falsePositives;
            this.forbiddenHits = forbiddenHits;
            this.falsePositiveHits = falsePositiveHits;
            this.recommended = recommended;
            this.expectedTests = expectedTests;
        }

        private static PrecisionScore score(TaskCase taskCase, GroundTruth groundTruth, ImpactMap impactMap) {
            Set<String> reported = new LinkedHashSet<String>();
            reported.addAll(impactMap.fileList("related-files"));
            reported.addAll(impactMap.fileList("related-tests"));
            reported.addAll(impactMap.fileList("missing-related-tests"));
            List<String> recommended = impactMap.orderedFileList("recommended-read-files");
            Set<String> missing = difference(groundTruth.expected, reported);
            Set<String> falsePositives = difference(reported, groundTruth.expected);
            Set<String> forbiddenNegatives = difference(groundTruth.forbidden, groundTruth.expected);
            Set<String> forbiddenHits = intersection(union(reported, new LinkedHashSet<String>(recommended)),
                    forbiddenNegatives);
            Set<String> falsePositiveHits = intersection(new LinkedHashSet<String>(recommended),
                    new LinkedHashSet<String>(taskCase.knownFalsePositives));
            return new PrecisionScore(taskCase, groundTruth.expected, reported, missing, falsePositives,
                    forbiddenHits, falsePositiveHits, recommended, groundTruth.expectedTests);
        }

        private double recall() {
            return expected.isEmpty() ? 1.0 : (expected.size() - missing.size()) / (double) expected.size();
        }

        private double precision() {
            return reported.isEmpty() ? 0.0 : (reported.size() - falsePositives.size()) / (double) reported.size();
        }

        private double top10Precision() {
            int limit = Math.min(10, recommended.size());
            if (limit == 0) {
                return 0.0;
            }
            int hits = 0;
            for (int index = 0; index < limit; index++) {
                if (expected.contains(recommended.get(index))) {
                    hits++;
                }
            }
            return hits / (double) limit;
        }

        private boolean top1MatchesStart() {
            return !recommended.isEmpty() && taskCase.expectedTop1.equals(recommended.get(0));
        }

        private boolean hasExpectedTestInTop10() {
            if (expectedTests.isEmpty()) {
                return true;
            }
            int limit = Math.min(10, recommended.size());
            for (int index = 0; index < limit; index++) {
                if (expectedTests.contains(recommended.get(index))) {
                    return true;
                }
            }
            return false;
        }

        private boolean passed() {
            return recall() >= taskCase.minRecall
                    && precision() >= taskCase.minPrecision
                    && top1MatchesStart()
                    && hasExpectedTestInTop10()
                    && forbiddenHits.isEmpty()
                    && falsePositiveHits.isEmpty();
        }

        private String toJson() {
            return JsonOutput.object(
                    JsonOutput.stringField("fixture", taskCase.fixture),
                    JsonOutput.stringField("task_id", taskCase.id),
                    JsonOutput.stringField("task", taskCase.task),
                    JsonOutput.stringField("graph_query", taskCase.graphQuery()),
                    JsonOutput.rawField("impact_recall", decimal(recall())),
                    JsonOutput.rawField("impact_precision_strict", decimal(precision())),
                    JsonOutput.rawField("top10_precision", decimal(top10Precision())),
                    JsonOutput.booleanField("top1_matches_start", top1MatchesStart()),
                    JsonOutput.booleanField("expected_test_in_top10", hasExpectedTestInTop10()),
                    JsonOutput.rawField("missing_expected", stringArray(missing)),
                    JsonOutput.rawField("false_positives", stringArray(falsePositives)),
                    JsonOutput.rawField("forbidden_hits", stringArray(forbiddenHits)),
                    JsonOutput.rawField("known_false_positive_hits", stringArray(falsePositiveHits)),
                    JsonOutput.numberField("sensitive_leaks", 0),
                    JsonOutput.booleanField("snapshot_stale", false),
                    JsonOutput.numberField("duration_ms", 0),
                    JsonOutput.booleanField("passed", passed())
            ).trim();
        }

        public String toString() {
            return taskCase.id + " recall=" + recall() + " precision=" + precision()
                    + " top10=" + top10Precision() + " missing=" + missing
                    + " false_positives=" + falsePositives;
        }

        private static Set<String> difference(Set<String> left, Set<String> right) {
            Set<String> result = new LinkedHashSet<String>(left);
            result.removeAll(right);
            return result;
        }

        private static Set<String> intersection(Set<String> left, Set<String> right) {
            Set<String> result = new LinkedHashSet<String>(left);
            result.retainAll(right);
            return result;
        }

        private static Set<String> union(Set<String> left, Set<String> right) {
            Set<String> result = new LinkedHashSet<String>(left);
            result.addAll(right);
            return result;
        }
    }

    private static String decimal(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }

    private static String stringArray(Iterable<String> values) {
        List<String> encoded = new ArrayList<String>();
        for (String value : values) {
            encoded.add(JsonOutput.quote(value));
        }
        return JsonOutput.array(encoded);
    }

    private static final class Harness {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();
        private final ByteArrayOutputStream err = new ByteArrayOutputStream();
        private final Path workingDirectory;

        private Harness(Path workingDirectory) {
            this.workingDirectory = workingDirectory;
        }

        CommandContext context() {
            return new CommandContext(
                    workingDirectory,
                    com.devharnesskit.dhk.testsupport.Utf8HarnessSupport.printStream(out),
                    com.devharnesskit.dhk.testsupport.Utf8HarnessSupport.printStream(err),
                    new FixedClock()
            );
        }

        String stderr() {
            return com.devharnesskit.dhk.testsupport.Utf8HarnessSupport.text(err);
        }
    }

    private static final class FixedClock implements Clock {
        public Instant now() {
            return Instant.parse("2026-05-30T00:00:00Z");
        }
    }
}
