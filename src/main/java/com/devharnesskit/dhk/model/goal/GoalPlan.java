package com.devharnesskit.dhk.model.goal;

public final class GoalPlan {
    private final String currentAction;
    private final String instruction;
    private final String[] requiredEvidence;
    private final String[] forbiddenActions;
    private final String nextCommand;

    public GoalPlan(String currentAction, String instruction, String[] requiredEvidence,
                    String[] forbiddenActions, String nextCommand) {
        this.currentAction = currentAction == null ? "" : currentAction;
        this.instruction = instruction == null ? "" : instruction;
        this.requiredEvidence = requiredEvidence == null ? new String[0] : requiredEvidence;
        this.forbiddenActions = forbiddenActions == null ? new String[0] : forbiddenActions;
        this.nextCommand = nextCommand == null ? "" : nextCommand;
    }

    public String currentAction() { return currentAction; }
    public String instruction() { return instruction; }
    public String[] requiredEvidence() { return requiredEvidence; }
    public String[] forbiddenActions() { return forbiddenActions; }
    public String nextCommand() { return nextCommand; }
}
