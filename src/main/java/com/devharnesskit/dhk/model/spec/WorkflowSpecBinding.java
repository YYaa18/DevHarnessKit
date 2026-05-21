package com.devharnesskit.dhk.model.spec;

public final class WorkflowSpecBinding {
    private final long id;
    private final String runKey;
    private final String changeKey;
    private final String bindingType;
    private final String createdAt;

    public WorkflowSpecBinding(long id, String runKey, String changeKey,
                               String bindingType, String createdAt) {
        this.id = id;
        this.runKey = runKey;
        this.changeKey = changeKey;
        this.bindingType = bindingType;
        this.createdAt = createdAt;
    }

    public long id() {
        return id;
    }

    public String runKey() {
        return runKey;
    }

    public String changeKey() {
        return changeKey;
    }

    public String bindingType() {
        return bindingType;
    }

    public String createdAt() {
        return createdAt;
    }
}
