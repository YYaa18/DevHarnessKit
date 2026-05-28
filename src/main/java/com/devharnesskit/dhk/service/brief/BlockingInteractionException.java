package com.devharnesskit.dhk.service.brief;

public final class BlockingInteractionException extends IllegalArgumentException {
    private final String requestId;
    private final String question;
    private final String[] choices;

    BlockingInteractionException(String requestId, String question, String choices) {
        super("blocking interaction requires user answer: " + value(requestId) + " - " + value(question));
        this.requestId = value(requestId);
        this.question = value(question);
        this.choices = split(choices);
    }

    public String requestId() { return requestId; }
    public String question() { return question; }
    public String[] choices() { return copy(choices); }

    private static String value(String value) {
        return value == null ? "" : value.trim();
    }

    private static String[] split(String value) {
        String text = value(value);
        if (text.length() == 0) {
            return new String[0];
        }
        String[] parts = text.split("\\|");
        java.util.List<String> result = new java.util.ArrayList<String>();
        for (String part : parts) {
            String item = value(part);
            if (item.length() > 0) {
                result.add(item);
            }
        }
        return result.toArray(new String[result.size()]);
    }

    private static String[] copy(String[] values) {
        String[] copy = new String[values.length];
        System.arraycopy(values, 0, copy, 0, values.length);
        return copy;
    }
}
