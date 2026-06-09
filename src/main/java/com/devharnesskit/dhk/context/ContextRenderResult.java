package com.devharnesskit.dhk.context;

public final class ContextRenderResult {
    private final String text;
    private final int tokenBefore;
    private final int tokenAfter;
    private final int omittedItems;
    private final String[] risks;

    public ContextRenderResult(String text, int tokenBefore, int tokenAfter, int omittedItems, String[] risks) {
        this.text = text == null ? "" : text;
        this.tokenBefore = Math.max(0, tokenBefore);
        this.tokenAfter = Math.max(0, tokenAfter);
        this.omittedItems = Math.max(0, omittedItems);
        this.risks = risks == null ? new String[0] : copy(risks);
    }

    public String text() {
        return text;
    }

    public int tokenBefore() {
        return tokenBefore;
    }

    public int tokenAfter() {
        return tokenAfter;
    }

    public int omittedItems() {
        return omittedItems;
    }

    public String[] risks() {
        return copy(risks);
    }

    private String[] copy(String[] values) {
        String[] result = new String[values.length];
        System.arraycopy(values, 0, result, 0, values.length);
        return result;
    }
}
