package com.devharnesskit.dhk.context;

public final class ContextItem {
    private final ContextItemType type;
    private final ContextPriority priority;
    private final String rawText;
    private final String sourceRef;
    private final boolean compressible;

    public ContextItem(ContextItemType type, ContextPriority priority, String rawText,
                       String sourceRef, boolean compressible) {
        this.type = type == null ? ContextItemType.SHELL_OUTPUT : type;
        this.priority = priority == null ? ContextPriority.NORMAL : priority;
        this.rawText = rawText == null ? "" : rawText;
        this.sourceRef = sourceRef == null ? "" : sourceRef;
        this.compressible = compressible;
    }

    public ContextItemType type() {
        return type;
    }

    public ContextPriority priority() {
        return priority;
    }

    public String rawText() {
        return rawText;
    }

    public String sourceRef() {
        return sourceRef;
    }

    public boolean compressible() {
        return compressible;
    }
}
