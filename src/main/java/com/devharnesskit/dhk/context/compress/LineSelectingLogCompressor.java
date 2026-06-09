package com.devharnesskit.dhk.context.compress;

import com.devharnesskit.dhk.context.token.CharsOverFourTokenEstimator;
import com.devharnesskit.dhk.context.token.TokenEstimator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

abstract class LineSelectingLogCompressor {
    private static final int EDGE_LINE_COUNT = 20;
    private static final int MAX_RETAINED_LINES = 140;

    private final TokenEstimator estimator;

    LineSelectingLogCompressor() {
        this(new CharsOverFourTokenEstimator());
    }

    LineSelectingLogCompressor(TokenEstimator estimator) {
        this.estimator = estimator == null ? new CharsOverFourTokenEstimator() : estimator;
    }

    CompressResult compress(String command, int exitCode, String output) {
        String text = output == null ? "" : output;
        String[] lines = splitLines(text);
        Map<Integer, RetainedSpan> retained = new LinkedHashMap<Integer, RetainedSpan>();
        retainEdges(lines, retained);
        retainSignals(lines, retained);
        List<RetainedSpan> spans = limit(new ArrayList<RetainedSpan>(retained.values()));
        boolean degraded = degraded(lines.length, spans);
        String compressed = render(command, exitCode, lines.length, spans, text, degraded);
        return new CompressResult(sourceType(), compressed, spans,
                Math.max(0, lines.length - spans.size()), estimator.estimate(text),
                estimator.estimate(compressed), degraded);
    }

    protected abstract String sourceType();

    protected abstract boolean isSignal(String lowerLine);

    protected abstract String signalReason(String lowerLine);

    private void retainEdges(String[] lines, Map<Integer, RetainedSpan> retained) {
        for (int i = 0; i < lines.length && i < EDGE_LINE_COUNT; i++) {
            retain(retained, i + 1, "head", lines[i]);
        }
        int start = Math.max(0, lines.length - EDGE_LINE_COUNT);
        for (int i = start; i < lines.length; i++) {
            retain(retained, i + 1, "tail", lines[i]);
        }
    }

    private void retainSignals(String[] lines, Map<Integer, RetainedSpan> retained) {
        for (int i = 0; i < lines.length; i++) {
            String lower = lines[i].toLowerCase(Locale.ROOT);
            if (isSignal(lower)) {
                retain(retained, i + 1, signalReason(lower), lines[i]);
            }
        }
    }

    private void retain(Map<Integer, RetainedSpan> retained, int line, String reason, String text) {
        if (retained.containsKey(line)) {
            return;
        }
        retained.put(line, new RetainedSpan(line, reason, text));
    }

    private List<RetainedSpan> limit(List<RetainedSpan> spans) {
        if (spans.size() <= MAX_RETAINED_LINES) {
            return spans;
        }
        List<RetainedSpan> limited = new ArrayList<RetainedSpan>();
        for (int i = 0; i < spans.size() && i < MAX_RETAINED_LINES; i++) {
            limited.add(spans.get(i));
        }
        return limited;
    }

    private boolean degraded(int lineCount, List<RetainedSpan> spans) {
        if (lineCount <= spans.size()) {
            return false;
        }
        for (RetainedSpan span : spans) {
            if (!"head".equals(span.reason()) && !"tail".equals(span.reason())) {
                return false;
            }
        }
        return true;
    }

    private String render(String command, int exitCode, int lineCount, List<RetainedSpan> spans, String original,
                          boolean degraded) {
        StringBuilder builder = new StringBuilder();
        builder.append("context_digest:\n");
        builder.append("  source_type: ").append(sourceType()).append('\n');
        builder.append("  command: ").append(command == null ? "" : command).append('\n');
        builder.append("  exit_code: ").append(exitCode).append('\n');
        builder.append("  status: ").append(exitCode == 0 ? "passed" : "failed").append('\n');
        builder.append("  degraded: ").append(degraded).append('\n');
        builder.append("  original_lines: ").append(lineCount).append('\n');
        builder.append("  retained_lines: ").append(spans.size()).append('\n');
        builder.append("  omitted_lines: ").append(Math.max(0, lineCount - spans.size())).append('\n');
        builder.append("  root_cause_candidates:\n");
        int causes = 0;
        for (RetainedSpan span : spans) {
            if (!"head".equals(span.reason()) && !"tail".equals(span.reason())) {
                builder.append("    - line ").append(span.lineNumber()).append(": ")
                        .append(trim(span.text(), 240)).append('\n');
                causes++;
                if (causes >= 12) {
                    break;
                }
            }
        }
        if (causes == 0) {
            builder.append("    - none\n");
        }
        builder.append("  retained_output:\n");
        if (spans.isEmpty() && original.length() == 0) {
            builder.append("    (empty output)\n");
        } else {
            for (RetainedSpan span : spans) {
                builder.append("    [line ").append(span.lineNumber()).append(" ")
                        .append(span.reason()).append("] ").append(span.text()).append('\n');
            }
        }
        return builder.toString();
    }

    private String trim(String value, int max) {
        if (value == null || value.length() <= max) {
            return value == null ? "" : value;
        }
        return value.substring(0, max) + "...";
    }

    private String[] splitLines(String text) {
        if (text == null || text.length() == 0) {
            return new String[0];
        }
        return text.split("\\r?\\n", -1);
    }
}
