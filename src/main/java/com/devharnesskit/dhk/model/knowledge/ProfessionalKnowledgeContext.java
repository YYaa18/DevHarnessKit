package com.devharnesskit.dhk.model.knowledge;

public final class ProfessionalKnowledgeContext {
    private static final ProfessionalKnowledgeContext DISABLED =
            new ProfessionalKnowledgeContext(false, new String[0], new KnowledgeSnippet[0],
                    "Professional knowledge is advisory. Current project code and confirmed memory remain source of truth.");

    private final boolean enabled;
    private final String[] packRefs;
    private final KnowledgeSnippet[] snippets;
    private final String advisory;

    public ProfessionalKnowledgeContext(boolean enabled, String[] packRefs,
                                        KnowledgeSnippet[] snippets, String advisory) {
        this.enabled = enabled;
        this.packRefs = packRefs == null ? new String[0] : packRefs;
        this.snippets = snippets == null ? new KnowledgeSnippet[0] : snippets;
        this.advisory = advisory == null || advisory.trim().length() == 0
                ? DISABLED.advisory() : advisory.trim();
    }

    public static ProfessionalKnowledgeContext disabled() {
        return DISABLED;
    }

    public boolean enabled() { return enabled; }
    public String[] packRefs() { return packRefs; }
    public KnowledgeSnippet[] snippets() { return snippets; }
    public String advisory() { return advisory; }
}
