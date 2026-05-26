package com.devharnesskit.dhk.service.graph;

import com.devharnesskit.dhk.export.GraphIndexReportRenderer;
import com.devharnesskit.dhk.model.graph.GraphConfig;
import com.devharnesskit.dhk.model.graph.GraphEdge;
import com.devharnesskit.dhk.model.graph.GraphFileEntry;
import com.devharnesskit.dhk.model.graph.GraphNode;
import com.devharnesskit.dhk.model.graph.GraphParseResult;
import com.devharnesskit.dhk.model.graph.GraphScanReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GraphLiteParserTest {
    @TempDir
    Path tempDir;

    @Test
    void parsesLegacyMybatisFixtureNodesAndEdges() {
        Path fixture = Paths.get("testbeds/fixtures/legacy-mybatis-order");
        GraphConfig config = GraphConfig.defaults();
        GraphScanReport scan = new GraphFileScanner().scan(fixture, config);

        GraphParseResult result = new GraphLiteParser().parse(fixture, scan.entries());

        assertNode(result, "class", "com.acme.legacy.order.web.OrderController");
        assertNode(result, "class", "com.acme.legacy.order.service.OrderService");
        assertNode(result, "interface", "com.acme.legacy.order.mapper.OrderMapper");
        assertNode(result, "class", "com.acme.legacy.order.dto.OrderQuery");
        assertNode(result, "test_case", "com.acme.legacy.order.service.OrderServiceTest#filtersByStatus");
        assertNode(result, "route", "GET /legacy/orders");
        assertNode(result, "xml_mapper", "com.acme.legacy.order.mapper.OrderMapper");
        assertNode(result, "sql_statement", "com.acme.legacy.order.mapper.OrderMapper.findOrders");
        assertNode(result, "sql_statement", "com.acme.legacy.order.mapper.OrderMapper.countOrders");
        assertNode(result, "db_table", "legacy_order");
        assertNode(result, "config_key", "legacy.order.default-page-size");
        assertEdge(result, "handles_route", "route:GET:/legacy/orders");
        assertEdge(result, "maps_to", "java_method:com.acme.legacy.order.mapper.OrderMapper#findOrders");
        assertEdge(result, "reads", "db_table:legacy_order");
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void parseErrorsDoNotStopOtherFiles() throws Exception {
        write("src/main/java/com/example/App.java", "package com.example;\npublic class App { public void run() {} }\n");
        write("src/main/resources/mybatis/BrokenMapper.xml",
                "<mapper namespace=\"com.example.BrokenMapper\"><select id=\"broken\">SELECT id FROM broken_table\n");

        GraphConfig config = GraphConfig.defaults();
        GraphScanReport scan = new GraphFileScanner().scan(tempDir, config);
        GraphParseResult result = new GraphLiteParser().parse(tempDir, scan.entries());

        assertNode(result, "class", "com.example.App");
        assertNode(result, "xml_mapper", "com.example.BrokenMapper");
        assertFalse(result.errors().isEmpty());
    }

    @Test
    void propertiesParserAndReportDoNotExportSensitiveValues() throws Exception {
        write("src/main/resources/application.properties",
                "db.password=super-secret-value\napp.route=/legacy/orders\n");

        GraphConfig config = GraphConfig.defaults();
        GraphScanReport scan = new GraphFileScanner().scan(tempDir, config);
        GraphParseResult result = new GraphLiteParser().parse(tempDir, scan.entries());
        GraphScanReport report = new GraphScanReport(tempDir, config, scan.entries(), result);

        String markdown = new GraphIndexReportRenderer().render(report, Instant.parse("2026-05-26T00:00:00Z"));

        assertNode(result, "config_key", "db.password");
        assertTrue(markdown.contains("db.password"));
        assertTrue(markdown.contains("app.route"));
        assertFalse(markdown.contains("super-secret-value"));
    }

    private void write(String relativePath, String content) throws Exception {
        Path file = tempDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes("UTF-8"));
    }

    private void assertNode(GraphParseResult result, String kind, String qualifiedName) {
        for (GraphNode node : result.nodes()) {
            if (kind.equals(node.nodeKind()) && qualifiedName.equals(node.qualifiedName())) {
                return;
            }
        }
        throw new AssertionError("Missing node: " + kind + " " + qualifiedName);
    }

    private void assertEdge(GraphParseResult result, String kind, String targetNodeKey) {
        for (GraphEdge edge : result.edges()) {
            if (kind.equals(edge.edgeKind()) && targetNodeKey.equals(edge.targetNodeKey())) {
                return;
            }
        }
        throw new AssertionError("Missing edge: " + kind + " -> " + targetNodeKey);
    }
}
