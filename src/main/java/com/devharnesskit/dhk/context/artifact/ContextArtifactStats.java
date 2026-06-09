package com.devharnesskit.dhk.context.artifact;

import java.util.ArrayList;
import java.util.List;

public final class ContextArtifactStats {
    private final int artifactCount;
    private final long tokenBefore;
    private final long tokenAfter;
    private final long omittedLines;
    private final List<SourceTypeStats> sourceTypes;

    public ContextArtifactStats(int artifactCount, long tokenBefore, long tokenAfter, long omittedLines) {
        this(artifactCount, tokenBefore, tokenAfter, omittedLines, new ArrayList<SourceTypeStats>());
    }

    public ContextArtifactStats(int artifactCount, long tokenBefore, long tokenAfter, long omittedLines,
                                List<SourceTypeStats> sourceTypes) {
        this.artifactCount = Math.max(0, artifactCount);
        this.tokenBefore = Math.max(0L, tokenBefore);
        this.tokenAfter = Math.max(0L, tokenAfter);
        this.omittedLines = Math.max(0L, omittedLines);
        this.sourceTypes = sourceTypes == null
                ? new ArrayList<SourceTypeStats>() : new ArrayList<SourceTypeStats>(sourceTypes);
    }

    public int artifactCount() {
        return artifactCount;
    }

    public long tokenBefore() {
        return tokenBefore;
    }

    public long tokenAfter() {
        return tokenAfter;
    }

    public long omittedLines() {
        return omittedLines;
    }

    public List<SourceTypeStats> sourceTypes() {
        return new ArrayList<SourceTypeStats>(sourceTypes);
    }

    public int reductionPercent() {
        if (tokenBefore <= 0L) {
            return 0;
        }
        long reduced = Math.max(0L, tokenBefore - tokenAfter);
        return (int) Math.min(100L, (reduced * 100L) / tokenBefore);
    }

    public static final class SourceTypeStats {
        private final String sourceType;
        private final int artifactCount;
        private final long tokenBefore;
        private final long tokenAfter;
        private final long omittedLines;
        private final int degradedCount;

        public SourceTypeStats(String sourceType, int artifactCount, long tokenBefore, long tokenAfter,
                               long omittedLines, int degradedCount) {
            this.sourceType = sourceType == null ? "" : sourceType;
            this.artifactCount = Math.max(0, artifactCount);
            this.tokenBefore = Math.max(0L, tokenBefore);
            this.tokenAfter = Math.max(0L, tokenAfter);
            this.omittedLines = Math.max(0L, omittedLines);
            this.degradedCount = Math.max(0, degradedCount);
        }

        public String sourceType() {
            return sourceType;
        }

        public int artifactCount() {
            return artifactCount;
        }

        public long tokenBefore() {
            return tokenBefore;
        }

        public long tokenAfter() {
            return tokenAfter;
        }

        public long omittedLines() {
            return omittedLines;
        }

        public int degradedCount() {
            return degradedCount;
        }
    }
}
