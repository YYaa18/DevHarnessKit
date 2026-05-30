package com.devharnesskit.dhk.command.workflow;

import com.devharnesskit.dhk.model.workflow.WorkflowGateRun;
import com.devharnesskit.dhk.model.workflow.WorkflowPhaseRun;
import com.devharnesskit.dhk.model.workflow.WorkflowRun;
import com.devharnesskit.dhk.util.JsonOutput;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class WorkflowJsonSupport {
    private WorkflowJsonSupport() {
    }

    static String run(String command, WorkflowRun run, List<WorkflowPhaseRun> phases,
                      List<WorkflowGateRun> gates) {
        return JsonOutput.object(
                JsonOutput.stringField("command", command),
                JsonOutput.stringField("run_key", run.runKey()),
                JsonOutput.stringField("workflow", run.workflowKey()),
                JsonOutput.stringField("task", run.taskName()),
                JsonOutput.stringField("module", run.moduleName()),
                JsonOutput.stringField("mode", run.mode()),
                JsonOutput.stringField("status", run.status()),
                JsonOutput.stringField("current_phase", run.currentPhaseKey()),
                JsonOutput.rawField("phases", phases(phases)),
                JsonOutput.rawField("pending_hard_gates", pendingHardGates(gates))
        );
    }

    static String export(WorkflowRun run, List<WorkflowPhaseRun> phases,
                         List<WorkflowGateRun> gates, Path out) {
        return JsonOutput.object(
                JsonOutput.stringField("command", "workflow export"),
                JsonOutput.stringField("run_key", run.runKey()),
                JsonOutput.stringField("workflow", run.workflowKey()),
                JsonOutput.stringField("task", run.taskName()),
                JsonOutput.stringField("module", run.moduleName()),
                JsonOutput.stringField("mode", run.mode()),
                JsonOutput.stringField("status", run.status()),
                JsonOutput.stringField("current_phase", run.currentPhaseKey()),
                JsonOutput.stringField("workflow_context_path", out.toString()),
                JsonOutput.rawField("phases", phases(phases)),
                JsonOutput.rawField("pending_hard_gates", pendingHardGates(gates))
        );
    }

    static String summary(WorkflowRun run, long exportedMemoryCount, long artifactCount,
                          long checkpointCount, long boundSpecCount,
                          long pendingHardGateCount, long blockingHardGateCount) {
        return JsonOutput.object(
                JsonOutput.stringField("command", "workflow summary"),
                JsonOutput.stringField("run_key", run.runKey()),
                JsonOutput.stringField("workflow", run.workflowKey()),
                JsonOutput.stringField("task", run.taskName()),
                JsonOutput.stringField("module", run.moduleName()),
                JsonOutput.stringField("mode", run.mode()),
                JsonOutput.stringField("status", run.status()),
                JsonOutput.stringField("current_phase", run.currentPhaseKey()),
                JsonOutput.numberField("exported_memory_count", exportedMemoryCount),
                JsonOutput.numberField("artifact_count", artifactCount),
                JsonOutput.numberField("checkpoint_count", checkpointCount),
                JsonOutput.numberField("bound_spec_count", boundSpecCount),
                JsonOutput.numberField("pending_hard_gate_count", pendingHardGateCount),
                JsonOutput.numberField("blocking_hard_gate_count", blockingHardGateCount)
        );
    }

    private static String phases(List<WorkflowPhaseRun> phases) {
        List<String> values = new ArrayList<String>();
        if (phases != null) {
            for (WorkflowPhaseRun phase : phases) {
                values.add(JsonOutput.object(
                        JsonOutput.stringField("phase_key", phase.phaseKey()),
                        JsonOutput.stringField("name", phase.phaseName()),
                        JsonOutput.stringField("status", phase.status()),
                        JsonOutput.numberField("phase_order", phase.phaseOrder())
                ).trim());
            }
        }
        return JsonOutput.array(values);
    }

    private static String pendingHardGates(List<WorkflowGateRun> gates) {
        List<String> values = new ArrayList<String>();
        if (gates != null) {
            for (WorkflowGateRun gate : gates) {
                if ("hard".equals(gate.severity()) && "pending".equals(gate.status())) {
                    values.add(JsonOutput.object(
                            JsonOutput.stringField("gate_key", gate.gateKey()),
                            JsonOutput.stringField("phase_key", gate.phaseKey()),
                            JsonOutput.stringField("status", gate.status())
                    ).trim());
                }
            }
        }
        return JsonOutput.array(values);
    }
}
