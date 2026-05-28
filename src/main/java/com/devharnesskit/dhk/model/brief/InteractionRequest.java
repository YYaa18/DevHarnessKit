package com.devharnesskit.dhk.model.brief;

public final class InteractionRequest {
    private final String requestId;
    private final String goalKey;
    private final String phase;
    private final String type;
    private final String priority;
    private final String question;
    private final String why;
    private final String choices;
    private final String defaultChoice;
    private final boolean blocksProgress;
    private final String status;
    private final String answer;

    public InteractionRequest(String requestId, String goalKey, String phase, String type,
                              String priority, String question, String why, String choices,
                              String defaultChoice, boolean blocksProgress, String status, String answer) {
        this.requestId = value(requestId);
        this.goalKey = value(goalKey);
        this.phase = value(phase);
        this.type = value(type);
        this.priority = value(priority);
        this.question = value(question);
        this.why = value(why);
        this.choices = value(choices);
        this.defaultChoice = value(defaultChoice);
        this.blocksProgress = blocksProgress;
        this.status = value(status);
        this.answer = value(answer);
    }

    public String requestId() { return requestId; }
    public String goalKey() { return goalKey; }
    public String phase() { return phase; }
    public String type() { return type; }
    public String priority() { return priority; }
    public String question() { return question; }
    public String why() { return why; }
    public String choices() { return choices; }
    public String defaultChoice() { return defaultChoice; }
    public boolean blocksProgress() { return blocksProgress; }
    public String status() { return status; }
    public String answer() { return answer; }
    public boolean openBlockingFor(String goalKey) {
        return blocksProgress && !"answered".equals(status) && !"rejected".equals(status)
                && (this.goalKey.length() == 0 || this.goalKey.equals(goalKey));
    }

    public InteractionRequest answered(String answer) {
        return new InteractionRequest(requestId, goalKey, phase, type, priority, question, why,
                choices, defaultChoice, blocksProgress, "answered", answer);
    }

    private static String value(String value) {
        return value == null ? "" : value.trim();
    }
}
