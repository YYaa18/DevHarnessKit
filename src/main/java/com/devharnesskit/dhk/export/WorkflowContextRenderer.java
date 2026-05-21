package com.devharnesskit.dhk.export;

import com.devharnesskit.dhk.model.workflow.WorkflowGateRun;
import com.devharnesskit.dhk.model.workflow.WorkflowPhaseRun;
import com.devharnesskit.dhk.model.workflow.WorkflowRun;

import java.util.List;

public final class WorkflowContextRenderer {
    private static final int MAX_CHARS = 12 * 1024;

    public String render(WorkflowRun run, List<WorkflowPhaseRun> phases, List<WorkflowGateRun> gates,
                         String generatedAt) {
        StringBuilder builder = new StringBuilder();
        builder.append("# WORKFLOW_CONTEXT\n\n");
        builder.append("<generated-at>").append(generatedAt).append("</generated-at>\n\n");
        builder.append("<workflow-run>\n");
        builder.append("run_key: ").append(run.runKey()).append('\n');
        builder.append("workflow: ").append(run.workflowKey()).append('\n');
        builder.append("task: ").append(run.taskName()).append('\n');
        builder.append("module: ").append(run.moduleName()).append('\n');
        builder.append("mode: ").append(run.mode()).append('\n');
        builder.append("status: ").append(run.status()).append('\n');
        builder.append("current_phase: ").append(run.currentPhaseKey()).append('\n');
        builder.append("</workflow-run>\n\n");
        WorkflowPhaseRun current = currentPhase(run, phases);
        builder.append("<current-phase>\n");
        if (current == null) {
            builder.append("none\n");
        } else {
            builder.append("- key: ").append(current.phaseKey()).append('\n');
            builder.append("- name: ").append(current.phaseName()).append('\n');
            builder.append("- status: ").append(current.status()).append('\n');
            if (current.outputSummary().length() > 0) {
                builder.append("- output_summary: ").append(current.outputSummary()).append('\n');
            }
        }
        builder.append("</current-phase>\n\n");
        builder.append("<phases>\n");
        for (WorkflowPhaseRun phase : phases) {
            builder.append("[").append(phase.status()).append("] ").append(phase.phaseKey()).append('\n');
        }
        builder.append("</phases>\n\n");
        builder.append("<pending-hard-gates>\n");
        int pendingHard = 0;
        for (WorkflowGateRun gate : gates) {
            if ("hard".equals(gate.severity()) && "pending".equals(gate.status())) {
                builder.append("- ").append(gate.gateKey()).append(" (phase: ")
                        .append(gate.phaseKey()).append(")\n");
                pendingHard++;
            }
        }
        if (pendingHard == 0) {
            builder.append("none\n");
        }
        builder.append("</pending-hard-gates>\n\n");
        builder.append("<agent-instructions>\n");
        builder.append("1. Continue from current_phase; do not skip pending hard gates.\n");
        builder.append("2. Record phase/gate outcomes with dhk workflow phase/gate commands.\n");
        builder.append("3. Use memory checkpoint after meaningful task progress.\n");
        builder.append("</agent-instructions>\n");
        return limit(builder.toString());
    }

    public String renderInline(WorkflowRun run, List<WorkflowGateRun> gates) {
        StringBuilder builder = new StringBuilder();
        builder.append("<workflow-context>\n");
        builder.append("workflow: ").append(run.workflowKey()).append('\n');
        builder.append("run_key: ").append(run.runKey()).append('\n');
        builder.append("status: ").append(run.status()).append('\n');
        builder.append("current_phase: ").append(run.currentPhaseKey()).append("\n\n");
        builder.append("Pending hard gates:\n");
        int pending = 0;
        for (WorkflowGateRun gate : gates) {
            if ("hard".equals(gate.severity()) && "pending".equals(gate.status())) {
                builder.append("- ").append(gate.gateKey()).append('\n');
                pending++;
            }
        }
        if (pending == 0) {
            builder.append("- none\n");
        }
        builder.append("</workflow-context>\n");
        return builder.toString();
    }

    private WorkflowPhaseRun currentPhase(WorkflowRun run, List<WorkflowPhaseRun> phases) {
        for (WorkflowPhaseRun phase : phases) {
            if (phase.phaseKey().equals(run.currentPhaseKey())) {
                return phase;
            }
        }
        return null;
    }

    private String limit(String text) {
        if (text.length() <= MAX_CHARS) {
            return text;
        }
        return text.substring(0, MAX_CHARS - 80) + "\n\n<!-- truncated: workflow context exceeded budget -->\n";
    }
}
