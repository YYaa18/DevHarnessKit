package com.devharnesskit.dhk.service.graph;

import com.devharnesskit.dhk.model.graph.GraphFileEntry;
import com.devharnesskit.dhk.model.graph.GraphScanReport;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class GraphWorkspaceFingerprintService {
    public String fingerprint(Path projectRoot, GraphScanReport report) {
        List<GraphFileEntry> entries = new ArrayList<GraphFileEntry>(report.entries());
        Collections.sort(entries, new Comparator<GraphFileEntry>() {
            public int compare(GraphFileEntry left, GraphFileEntry right) {
                return left.relativePath().compareTo(right.relativePath());
            }
        });
        StringBuilder builder = new StringBuilder();
        builder.append("graph-workspace-v2\n");
        for (GraphFileEntry entry : entries) {
            builder.append(entry.relativePath()).append('\t')
                    .append(entry.language()).append('\t')
                    .append(entry.fileKind()).append('\t')
                    .append(entry.sizeBytes()).append('\t')
                    .append(entry.indexed()).append('\t')
                    .append(entry.skipReason()).append('\t')
                    .append(entry.contentHash()).append('\t')
                    .append(lastModified(projectRoot, entry.relativePath()))
                    .append('\n');
        }
        return "graph:" + sha256(builder.toString());
    }

    private String lastModified(Path projectRoot, String relativePath) {
        try {
            Path file = projectRoot.resolve(relativePath);
            if (!Files.exists(file)) {
                return "missing";
            }
            return Files.getLastModifiedTime(file).toString();
        } catch (Exception ex) {
            return "unavailable";
        }
    }

    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(text.getBytes("UTF-8"));
            StringBuilder builder = new StringBuilder();
            for (byte value : bytes) {
                String hex = Integer.toHexString(value & 0xff);
                if (hex.length() == 1) {
                    builder.append('0');
                }
                builder.append(hex);
            }
            return builder.toString();
        } catch (Exception ex) {
            return "";
        }
    }
}
