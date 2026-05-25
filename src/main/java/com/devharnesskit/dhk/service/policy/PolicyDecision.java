package com.devharnesskit.dhk.service.policy;

public final class PolicyDecision {
    private final boolean allowed;
    private final String reason;

    private PolicyDecision(boolean allowed, String reason) {
        this.allowed = allowed;
        this.reason = reason == null ? "" : reason;
    }

    public static PolicyDecision allow() {
        return new PolicyDecision(true, "");
    }

    public static PolicyDecision block(String reason) {
        return new PolicyDecision(false, reason);
    }

    public boolean allowed() { return allowed; }
    public String reason() { return reason; }
}
