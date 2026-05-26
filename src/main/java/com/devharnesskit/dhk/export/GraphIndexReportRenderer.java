package com.devharnesskit.dhk.export;

import com.devharnesskit.dhk.model.graph.GraphFileEntry;
import com.devharnesskit.dhk.model.graph.GraphScanReport;

import java.time.Instant;
import java.util.Map;

public final class GraphIndexReportRenderer {
    public String render(GraphScanReport report, Instant generatedAt) {
        StringBuilder builder = new StringBuilder();
        builder.append("# GRAPH_INDEX_REPORT\n\n");
        builder.append("<generated-at>").append(generatedAt.toString()).append("</generated-at>\n\n");
        builder.append("<summary>\n");
        builder.append("- provider: ").append(report.config().provider()).append('\n');
        builder.append("- config_source: ").append(report.config().loadedFromFile() ? "file" : "default").append('\n');
        builder.append("- files_considered: ").append(report.filesConsidered()).append('\n');
        builder.append("- indexed_files: ").append(report.indexedFiles()).append('\n');
        builder.append("- skipped_files: ").append(report.skippedFiles()).append('\n');
        builder.append("- max_file_bytes: ").append(report.config().maxFileBytes()).append('\n');
        builder.append("- max_indexed_files: ").append(report.config().maxIndexedFiles()).append('\n');
        builder.append("</summary>\n\n");

        builder.append("<languages>\n");
        for (Map.Entry<String, Integer> entry : report.languageCounts().entrySet()) {
            builder.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append('\n');
        }
        builder.append("</languages>\n\n");

        builder.append("<indexed-files>\n");
        for (GraphFileEntry entry : report.entries()) {
            if (entry.indexed()) {
                builder.append("- ").append(entry.relativePath())
                        .append(" [").append(entry.language()).append(", ")
                        .append(entry.fileKind()).append(", ")
                        .append(entry.contentHash()).append("]\n");
            }
        }
        builder.append("</indexed-files>\n\n");

        builder.append("<skipped-files>\n");
        for (GraphFileEntry entry : report.entries()) {
            if (!entry.indexed()) {
                builder.append("- ").append(entry.relativePath())
                        .append(" [").append(entry.skipReason()).append("]\n");
            }
        }
        builder.append("</skipped-files>\n");
        return builder.toString();
    }
}
