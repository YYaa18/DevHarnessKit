package com.devharnesskit.dhk.integration;

import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.CommandRouter;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.util.Clock;
import com.devharnesskit.dhk.util.PathUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GraphCommandIntegrationTest {
    @TempDir
    Path tempDir;

    @Test
    void graphInitAndStatusCreateConfigAndReport() throws Exception {
        Path root = tempDir.resolve("demo");
        write(root, "src/main/java/App.java", "class App {}\n");
        write(root, "src/main/resources/mybatis/Mapper.xml",
                "<mapper namespace=\"com.example.Mapper\"><select id=\"findAll\">SELECT id FROM example_table</select></mapper>\n");
        write(root, "src/main/webapp/WEB-INF/view.jsp", "<html />\n");
        write(root, "src/main/resources/application.properties", "app=true\n");
        write(root, "src/main/resources/query.sql", "select 1\n");
        write(root, "src/main/resources/secret.properties", "token=example\n");
        write(root, "target/classes/App.class", "fake\n");

        Harness initHarness = new Harness(tempDir);
        int initExit = new CommandRouter().run(new String[]{"graph", "init", "--project-root", "demo"},
                initHarness.context());

        Harness statusHarness = new Harness(tempDir);
        int statusExit = new CommandRouter().run(new String[]{"graph", "status", "--project-root", "demo"},
                statusHarness.context());

        assertEquals(ExitCodes.SUCCESS, initExit);
        assertEquals(ExitCodes.SUCCESS, statusExit);
        assertTrue(Files.isRegularFile(PathUtil.graphConfig(root)));
        assertTrue(Files.isRegularFile(PathUtil.graphIndexReport(root)));
        assertTrue(initHarness.stdout().contains("graph init complete"));
        assertTrue(statusHarness.stdout().contains("indexed_files: 5"));
        assertTrue(statusHarness.stdout().contains("skipped_files: 1"));
        assertTrue(statusHarness.stdout().contains("graph_nodes: "));
        assertTrue(statusHarness.stdout().contains("graph_edges: "));

        String report = new String(Files.readAllBytes(PathUtil.graphIndexReport(root)), "UTF-8");
        assertTrue(report.contains("GRAPH_INDEX_REPORT"));
        assertTrue(report.contains("src/main/java/App.java"));
        assertTrue(report.contains("src/main/resources/secret.properties [sensitive_filename]"));
        assertTrue(report.contains("sql_statement com.example.Mapper.findAll"));
        assertTrue(report.contains("db_table example_table"));
    }

    @Test
    void graphStatusSupportsJson() throws Exception {
        Path root = tempDir.resolve("demo-json");
        write(root, "src/main/java/App.java", "class App {}\n");

        Harness harness = new Harness(tempDir);
        int exit = new CommandRouter().run(new String[]{
                "graph", "status", "--project-root", "demo-json", "--json"
        }, harness.context());

        assertEquals(ExitCodes.SUCCESS, exit);
        assertTrue(harness.stdout().contains("\"command\": \"graph status\""));
        assertTrue(harness.stdout().contains("\"indexed_files\": 1"));
        assertTrue(harness.stdout().contains("\"graph_nodes\":"));
    }

    @Test
    void graphDoctorReportsLiteProviderWithoutCgcRequirement() throws Exception {
        Path root = tempDir.resolve("demo-doctor-lite");
        Files.createDirectories(root);

        Harness harness = new Harness(tempDir);
        int exit = new CommandRouter().run(new String[]{
                "graph", "doctor", "--project-root", "demo-doctor-lite", "--json"
        }, harness.context());

        assertEquals(ExitCodes.SUCCESS, exit);
        assertTrue(harness.stdout().contains("\"command\": \"graph doctor\""));
        assertTrue(harness.stdout().contains("\"config_source\": \"default\""));
        assertTrue(harness.stdout().contains("\"provider\": \"lite\""));
        assertTrue(harness.stdout().contains("\"cgc_required\": false"));
        assertTrue(harness.stdout().contains("\"cgc_available\": false"));
        assertTrue(harness.stdout().contains("\"cgc_status\": \"not_required\""));
        assertTrue(harness.stdout().contains("\"default_provider_unaffected\": true"));
    }

    @Test
    void graphDoctorReportsCgcUnavailableWhenConfigured() throws Exception {
        Path root = tempDir.resolve("demo-doctor-cgc");
        write(root, ".agents/graph/config.json",
                "{\n"
                        + "  \"schema_version\": \"devharness-graph-config/v1-alpha\",\n"
                        + "  \"provider\": \"cgc\",\n"
                        + "  \"cgc_command\": \"definitely-missing-cgc-command\"\n"
                        + "}\n");

        Harness harness = new Harness(tempDir);
        int exit = new CommandRouter().run(new String[]{
                "graph", "doctor", "--project-root", "demo-doctor-cgc", "--json"
        }, harness.context());

        assertEquals(ExitCodes.SUCCESS, exit);
        assertTrue(harness.stdout().contains("\"config_source\": \"file\""));
        assertTrue(harness.stdout().contains("\"provider\": \"cgc\""));
        assertTrue(harness.stdout().contains("\"cgc_command\": \"definitely-missing-cgc-command\""));
        assertTrue(harness.stdout().contains("\"cgc_required\": true"));
        assertTrue(harness.stdout().contains("\"cgc_available\": false"));
        assertTrue(harness.stdout().contains("\"cgc_status\": \"unavailable\""));
        assertTrue(harness.stdout().contains("\"default_provider_unaffected\": false"));
    }

    @Test
    void graphReportsLimitsTruncationAndProtectedFileSkips() throws Exception {
        Path root = tempDir.resolve("demo-limits");
        write(root, "src/main/java/com/example/App.java",
                "package com.example;\npublic class App { public void run() {} }\n");
        write(root, "src/main/java/com/example/Extra.java",
                "package com.example;\npublic class Extra { public void run() {} }\n");
        write(root, "src/main/resources/application-prod.yml", "password: raw-secret\n");
        write(root, ".agents/graph/config.json",
                "{\n"
                        + "  \"limits\": {\n"
                        + "    \"max_indexed_files\": 1,\n"
                        + "    \"max_impact_depth\": 1,\n"
                        + "    \"max_export_nodes\": 1\n"
                        + "  }\n"
                        + "}\n");
        write(root, ".agents/devharness/policy.json",
                "{\n"
                        + "  \"protected_files\": \"src/main/resources/application-prod.yml\"\n"
                        + "}\n");

        Harness indexHarness = new Harness(tempDir);
        int indexExit = new CommandRouter().run(new String[]{"graph", "index", "--project-root", "demo-limits"},
                indexHarness.context());
        Harness exportHarness = new Harness(tempDir);
        int exportExit = new CommandRouter().run(new String[]{"graph", "export", "--project-root", "demo-limits"},
                exportHarness.context());

        assertEquals(ExitCodes.SUCCESS, indexExit);
        assertEquals(ExitCodes.SUCCESS, exportExit);
        assertTrue(indexHarness.stdout().contains("max_indexed_files: 1"));
        assertTrue(indexHarness.stdout().contains("max_impact_depth: 1"));
        assertTrue(indexHarness.stdout().contains("max_export_nodes: 1"));

        String report = new String(Files.readAllBytes(PathUtil.graphIndexReport(root)), "UTF-8");
        assertTrue(report.contains("<limits>"));
        assertTrue(report.contains("src/main/resources/application-prod.yml [protected_file]"));
        assertTrue(report.contains("- skipped_max_indexed_files: 1"));
        assertTrue(report.contains("- skipped_protected_file: 1"));
        assertTrue(report.contains("<truncation-report>"));
        assertFalse(report.contains("raw-secret"));

        String context = new String(Files.readAllBytes(PathUtil.graphContext(root)), "UTF-8");
        String snapshot = new String(Files.readAllBytes(PathUtil.graphSnapshotJson(root)), "UTF-8");
        assertTrue(context.contains("<limits>"));
        assertTrue(context.contains("<truncation-report>"));
        assertTrue(snapshot.contains("\"max_export_nodes\": 1"));
        assertFalse(context.contains("raw-secret"));
        assertFalse(snapshot.contains("raw-secret"));
    }

    @Test
    void graphIndexPersistsSnapshotsAndStatusShowsLatestSnapshot() throws Exception {
        Path root = tempDir.resolve("demo-index");
        write(root, "src/main/java/com/example/App.java",
                "package com.example;\npublic class App { public void run() { Helper.call(); } }\n");
        write(root, "src/main/java/com/example/Helper.java",
                "package com.example;\npublic class Helper { public static void call() {} }\n");
        write(root, "src/main/resources/mybatis/Mapper.xml",
                "<mapper namespace=\"com.example.Mapper\"><select id=\"findAll\">SELECT id FROM example_table</select></mapper>\n");
        write(root, "src/main/resources/application.properties", "app.route=/demo\n");

        Harness firstHarness = new Harness(tempDir);
        int firstExit = new CommandRouter().run(new String[]{"graph", "index", "--project-root", "demo-index"},
                firstHarness.context());
        Harness secondHarness = new Harness(tempDir);
        int secondExit = new CommandRouter().run(new String[]{"graph", "index", "--project-root", "demo-index"},
                secondHarness.context());
        Harness statusHarness = new Harness(tempDir);
        int statusExit = new CommandRouter().run(new String[]{"graph", "status", "--project-root", "demo-index"},
                statusHarness.context());

        assertEquals(ExitCodes.SUCCESS, firstExit);
        assertEquals(ExitCodes.SUCCESS, secondExit);
        assertEquals(ExitCodes.SUCCESS, statusExit);
        assertTrue(firstHarness.stdout().contains("graph index complete"));
        assertTrue(firstHarness.stdout().contains("snapshot_key: graph-"));
        assertTrue(statusHarness.stdout().contains("latest_snapshot_key: graph-"));
        assertTrue(statusHarness.stdout().contains("latest_snapshot_status: completed"));
        assertEquals(2, countRows(root, "code_graph_snapshot"));
        assertEquals(8, countRows(root, "code_graph_file"));
        assertTrue(countRows(root, "code_graph_node") > 0);
        assertTrue(countRows(root, "code_graph_edge") > 0);
        assertTrue(countRowsWhere(root, "code_graph_node", "node_kind = 'sql_statement'") > 0);
        assertTrue(countRowsWhere(root, "code_graph_edge", "edge_kind = 'reads'") > 0);
    }

    @Test
    void graphExportWritesContextAndSnapshotContractsWithoutSensitiveValues() throws Exception {
        Path root = tempDir.resolve("demo-export");
        write(root, "src/main/java/com/example/App.java",
                "package com.example;\npublic class App { public void run() {} }\n");
        write(root, "src/main/resources/application.properties", "app.name=demo\n");
        write(root, "src/main/resources/secret.properties", "token=top-secret-value\n");

        Harness indexHarness = new Harness(tempDir);
        int indexExit = new CommandRouter().run(new String[]{"graph", "index", "--project-root", "demo-export"},
                indexHarness.context());
        Harness exportHarness = new Harness(tempDir);
        int exportExit = new CommandRouter().run(new String[]{"graph", "export", "--project-root", "demo-export"},
                exportHarness.context());

        assertEquals(ExitCodes.SUCCESS, indexExit);
        assertEquals(ExitCodes.SUCCESS, exportExit);
        assertTrue(exportHarness.stdout().contains("graph export complete"));
        assertTrue(Files.isRegularFile(PathUtil.graphContext(root)));
        assertTrue(Files.isRegularFile(PathUtil.graphSnapshotJson(root)));

        String context = new String(Files.readAllBytes(PathUtil.graphContext(root)), "UTF-8");
        String snapshot = new String(Files.readAllBytes(PathUtil.graphSnapshotJson(root)), "UTF-8");
        assertTrue(context.contains("graph facts are snapshot-bound machine facts"));
        assertTrue(context.contains("<file-hashes>"));
        assertTrue(context.contains("<agent-instructions>"));
        assertTrue(snapshot.contains("\"schema_version\": \"devharness-graph-snapshot/v1\""));
        assertTrue(snapshot.contains("\"snapshot_id\":"));
        assertTrue(snapshot.contains("\"git_commit\":"));
        assertTrue(snapshot.contains("\"git_dirty\":"));
        assertTrue(snapshot.contains("\"file_hashes\":"));
        assertTrue(snapshot.contains("\"node_count\":"));
        assertTrue(snapshot.contains("\"edge_count\":"));
        assertFalse(context.contains("top-secret-value"));
        assertFalse(snapshot.contains("top-secret-value"));
    }

    @Test
    void graphImpactCoversLegacyMybatisOrderSearchFlow() throws Exception {
        Path fixture = copyFixture("legacy-mybatis-order", tempDir.resolve("legacy-impact"));
        write(fixture, ".agents/graph/config.json",
                "{\n"
                        + "  \"limits\": {\n"
                        + "    \"max_impact_depth\": 6\n"
                        + "  }\n"
                        + "}\n");

        Harness indexHarness = new Harness(tempDir);
        int indexExit = new CommandRouter().run(new String[]{"graph", "index", "--project-root", "legacy-impact"},
                indexHarness.context());
        Harness impactHarness = new Harness(tempDir);
        int impactExit = new CommandRouter().run(new String[]{
                "graph", "impact", "--project-root", "legacy-impact", "--sql-table", "legacy_order", "--depth", "6"
        }, impactHarness.context());

        assertEquals(ExitCodes.SUCCESS, indexExit);
        assertEquals(ExitCodes.SUCCESS, impactExit);
        assertTrue(impactHarness.stdout().contains("graph impact"));
        assertTrue(impactHarness.stdout().contains("related_tests:"));
        String impactMap = new String(Files.readAllBytes(PathUtil.graphImpactMap(fixture)), "UTF-8");
        assertTrue(impactMap.contains("src/main/java/com/acme/legacy/order/web/OrderController.java"));
        assertTrue(impactMap.contains("src/main/java/com/acme/legacy/order/service/OrderService.java"));
        assertTrue(impactMap.contains("src/main/java/com/acme/legacy/order/mapper/OrderMapper.java"));
        assertTrue(impactMap.contains("src/main/resources/mybatis/OrderMapper.xml"));
        assertTrue(impactMap.contains("src/main/java/com/acme/legacy/order/dto/OrderQuery.java"));
        assertTrue(impactMap.contains("src/main/java/com/acme/legacy/order/dto/OrderRow.java"));
        assertTrue(impactMap.contains("src/main/java/com/acme/legacy/order/dto/OrderView.java"));
        assertTrue(impactMap.contains("src/test/java/com/acme/legacy/order/service/OrderServiceTest.java"));
        assertTrue(impactMap.contains("confidence="));
        assertTrue(impactMap.contains("source=lite"));
        assertTrue(impactMap.contains("evidence="));
        assertTrue(impactMap.contains("<scoring-data>"));
    }

    @Test
    void graphImpactCoversLegacyJspServletShopFlow() throws Exception {
        Path fixture = copyFixture("legacy-jsp-servlet-shop", tempDir.resolve("legacy-jsp-impact"));
        write(fixture, ".agents/graph/config.json",
                "{\n"
                        + "  \"limits\": {\n"
                        + "    \"max_impact_depth\": 8\n"
                        + "  }\n"
                        + "}\n");

        Harness indexHarness = new Harness(tempDir);
        int indexExit = new CommandRouter().run(new String[]{"graph", "index", "--project-root", "legacy-jsp-impact"},
                indexHarness.context());
        Harness impactHarness = new Harness(tempDir);
        int impactExit = new CommandRouter().run(new String[]{
                "graph", "impact", "--project-root", "legacy-jsp-impact",
                "--file", "src/main/webapp/WEB-INF/jsp/shop/order-list.jsp", "--depth", "8"
        }, impactHarness.context());

        assertEquals(ExitCodes.SUCCESS, indexExit);
        assertEquals(ExitCodes.SUCCESS, impactExit);
        assertTrue(impactHarness.stdout().contains("graph impact"));
        String impactMap = new String(Files.readAllBytes(PathUtil.graphImpactMap(fixture)), "UTF-8");
        assertTrue(impactMap.contains("src/main/webapp/WEB-INF/jsp/shop/order-list.jsp"));
        assertTrue(impactMap.contains("src/main/webapp/WEB-INF/jsp/common/header.jsp"));
        assertTrue(impactMap.contains("src/main/webapp/WEB-INF/web.xml"));
        assertTrue(impactMap.contains("src/main/java/com/acme/legacy/shop/web/ShopOrderServlet.java"));
        assertTrue(impactMap.contains("src/main/java/com/acme/legacy/shop/service/ShopOrderService.java"));
        assertTrue(impactMap.contains("src/main/java/com/acme/legacy/shop/dao/ShopOrderDao.java"));
        assertTrue(impactMap.contains("src/main/java/com/acme/legacy/shop/dao/JdbcShopOrderDao.java"));
        assertTrue(impactMap.contains("form_field customerNo"));
        assertTrue(impactMap.contains("form_field status"));
        assertTrue(impactMap.contains("submits_to jsp_form:src/main/webapp/WEB-INF/jsp/shop/order-list.jsp#orderSearchForm"
                + " -> route:ANY:/shop/orders/search"));
        assertTrue(impactMap.contains("route /shop/orders/search"));
    }

    @Test
    void graphImpactMarksMissingRelatedTestsForModernFixture() throws Exception {
        Path fixture = copyFixture("modern-java-api", tempDir.resolve("modern-test-gap"));

        Harness indexHarness = new Harness(tempDir);
        int indexExit = new CommandRouter().run(new String[]{"graph", "index", "--project-root", "modern-test-gap"},
                indexHarness.context());
        Harness impactHarness = new Harness(tempDir);
        int impactExit = new CommandRouter().run(new String[]{
                "graph", "impact", "--project-root", "modern-test-gap",
                "--file", "src/main/java/com/acme/modern/account/service/AccountService.java",
                "--depth", "4"
        }, impactHarness.context());

        assertEquals(ExitCodes.SUCCESS, indexExit);
        assertEquals(ExitCodes.SUCCESS, impactExit);
        assertTrue(impactHarness.stdout().contains("related_tests:"));
        String impactMap = new String(Files.readAllBytes(PathUtil.graphImpactMap(fixture)), "UTF-8");
        assertTrue(impactMap.contains("- missing_related_tests: 1"));
        assertTrue(impactMap.contains("<missing-related-tests>"));
        assertTrue(impactMap.contains("src/test/java/com/acme/modern/account/repository/AccountRepositoryTest.java"));
        assertTrue(impactMap.contains("src/test/java/com/acme/modern/account/service/AccountServiceTest.java"));
        assertFalse(impactMap.contains("src/test/java/com/acme/modern/account/repository/InMemoryAccountRepositoryTest.java"));
        assertTrue(impactMap.contains("- missing_related_test_count: 1"));
    }

    @Test
    void graphImpactReturnsCandidateSuggestionsWhenSymbolIsMissing() throws Exception {
        Path fixture = copyFixture("legacy-mybatis-order", tempDir.resolve("legacy-candidates"));

        Harness indexHarness = new Harness(tempDir);
        int indexExit = new CommandRouter().run(new String[]{"graph", "index", "--project-root", "legacy-candidates"},
                indexHarness.context());
        Harness impactHarness = new Harness(tempDir);
        int impactExit = new CommandRouter().run(new String[]{
                "graph", "impact", "--project-root", "legacy-candidates", "--symbol", "OrderServiceCandidate"
        }, impactHarness.context());

        assertEquals(ExitCodes.SUCCESS, indexExit);
        assertEquals(ExitCodes.NOT_FOUND, impactExit);
        assertTrue(impactHarness.stderr().contains("candidate_suggestions"));
        assertTrue(impactHarness.stderr().contains("OrderService"));
        String impactMap = new String(Files.readAllBytes(PathUtil.graphImpactMap(fixture)), "UTF-8");
        assertTrue(impactMap.contains("<candidate-suggestions>"));
        assertTrue(impactMap.contains("OrderService"));
    }

    @Test
    void graphImpactReportsConfiguredDepthLimit() throws Exception {
        Path fixture = copyFixture("legacy-mybatis-order", tempDir.resolve("legacy-depth-limit"));
        write(fixture, ".agents/graph/config.json",
                "{\n"
                        + "  \"limits\": {\n"
                        + "    \"max_impact_depth\": 1\n"
                        + "  }\n"
                        + "}\n");

        Harness indexHarness = new Harness(tempDir);
        int indexExit = new CommandRouter().run(new String[]{"graph", "index", "--project-root", "legacy-depth-limit"},
                indexHarness.context());
        Harness impactHarness = new Harness(tempDir);
        int impactExit = new CommandRouter().run(new String[]{
                "graph", "impact", "--project-root", "legacy-depth-limit", "--sql-table", "legacy_order", "--depth", "8"
        }, impactHarness.context());

        assertEquals(ExitCodes.SUCCESS, indexExit);
        assertEquals(ExitCodes.SUCCESS, impactExit);
        assertTrue(impactHarness.stdout().contains("depth: 1"));
        assertTrue(impactHarness.stdout().contains("requested_depth: 8"));
        assertTrue(impactHarness.stdout().contains("max_impact_depth: 1"));
        assertTrue(impactHarness.stdout().contains("depth_limited: true"));
        String impactMap = new String(Files.readAllBytes(PathUtil.graphImpactMap(fixture)), "UTF-8");
        assertTrue(impactMap.contains("- requested_depth: 8"));
        assertTrue(impactMap.contains("- max_impact_depth: 1"));
        assertTrue(impactMap.contains("- depth_limited: true"));
    }

    private void write(Path root, String relativePath, String content) throws Exception {
        Path file = root.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes("UTF-8"));
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

    private int countRows(Path root, String table) throws Exception {
        return countRowsWhere(root, table, "1 = 1");
    }

    private int countRowsWhere(Path root, String table, String where) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PathUtil.memoryDb(root));
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + table + " WHERE " + where)) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
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
                    new PrintStream(out),
                    new PrintStream(err),
                    new FixedClock()
            );
        }

        String stdout() {
            return out.toString();
        }

        String stderr() {
            return err.toString();
        }
    }

    private static final class FixedClock implements Clock {
        public Instant now() {
            return Instant.parse("2026-05-26T00:00:00Z");
        }
    }
}
