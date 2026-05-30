package com.devharnesskit.dhk.command.spec;

import com.devharnesskit.dhk.model.spec.SpecAcceptance;
import com.devharnesskit.dhk.model.spec.SpecChange;
import com.devharnesskit.dhk.model.spec.SpecTask;
import com.devharnesskit.dhk.model.spec.WorkflowSpecBinding;
import com.devharnesskit.dhk.util.JsonOutput;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class SpecJsonSupport {
    private SpecJsonSupport() {
    }

    static String create(SpecChange change) {
        return JsonOutput.object(
                JsonOutput.stringField("command", "spec create"),
                JsonOutput.stringField("change_key", change.changeKey()),
                JsonOutput.stringField("title", change.title()),
                JsonOutput.stringField("status", change.status()),
                JsonOutput.stringField("module", change.moduleName()),
                JsonOutput.stringField("mode", change.mode()),
                JsonOutput.stringField("priority", change.priority()),
                JsonOutput.rawField("tasks", JsonOutput.array(new ArrayList<String>())),
                JsonOutput.rawField("acceptance", JsonOutput.array(new ArrayList<String>())),
                JsonOutput.rawField("bound_workflows", JsonOutput.array(new ArrayList<String>()))
        );
    }

    static String status(SpecChange change, List<SpecTask> tasks,
                         List<SpecAcceptance> acceptances,
                         List<WorkflowSpecBinding> bindings) {
        return JsonOutput.object(
                JsonOutput.stringField("command", "spec status"),
                JsonOutput.stringField("change_key", change.changeKey()),
                JsonOutput.stringField("title", change.title()),
                JsonOutput.stringField("status", change.status()),
                JsonOutput.stringField("module", change.moduleName()),
                JsonOutput.stringField("mode", change.mode()),
                JsonOutput.stringField("priority", change.priority()),
                JsonOutput.rawField("tasks", tasks(tasks)),
                JsonOutput.rawField("acceptance", acceptance(acceptances)),
                JsonOutput.rawField("bound_workflows", boundWorkflows(bindings))
        );
    }

    static String export(SpecChange change, List<SpecTask> tasks,
                         List<SpecAcceptance> acceptances,
                         List<WorkflowSpecBinding> bindings, Path out) {
        return JsonOutput.object(
                JsonOutput.stringField("command", "spec export"),
                JsonOutput.stringField("change_key", change.changeKey()),
                JsonOutput.stringField("title", change.title()),
                JsonOutput.stringField("status", change.status()),
                JsonOutput.stringField("module", change.moduleName()),
                JsonOutput.stringField("mode", change.mode()),
                JsonOutput.stringField("priority", change.priority()),
                JsonOutput.stringField("export_path", out.toString()),
                JsonOutput.rawField("tasks", tasks(tasks)),
                JsonOutput.rawField("acceptance", acceptance(acceptances)),
                JsonOutput.rawField("bound_workflows", boundWorkflows(bindings))
        );
    }

    static String bindWorkflow(long id, SpecChange change, WorkflowSpecBinding binding) {
        List<WorkflowSpecBinding> bindings = new ArrayList<WorkflowSpecBinding>();
        bindings.add(binding);
        return JsonOutput.object(
                JsonOutput.stringField("command", "spec bind-workflow"),
                JsonOutput.numberField("workflow_spec_binding_id", id),
                JsonOutput.stringField("change_key", change.changeKey()),
                JsonOutput.stringField("title", change.title()),
                JsonOutput.stringField("status", change.status()),
                JsonOutput.stringField("module", change.moduleName()),
                JsonOutput.stringField("mode", change.mode()),
                JsonOutput.stringField("run_key", binding.runKey()),
                JsonOutput.stringField("binding_type", binding.bindingType()),
                JsonOutput.rawField("bound_workflows", boundWorkflows(bindings))
        );
    }

    private static String tasks(List<SpecTask> tasks) {
        List<String> values = new ArrayList<String>();
        if (tasks != null) {
            for (SpecTask task : tasks) {
                values.add(JsonOutput.object(
                        JsonOutput.stringField("task_key", task.taskKey()),
                        JsonOutput.stringField("title", task.title()),
                        JsonOutput.stringField("status", task.status()),
                        JsonOutput.stringField("phase", task.phaseKey())
                ).trim());
            }
        }
        return JsonOutput.array(values);
    }

    private static String acceptance(List<SpecAcceptance> acceptances) {
        List<String> values = new ArrayList<String>();
        if (acceptances != null) {
            for (SpecAcceptance acceptance : acceptances) {
                values.add(JsonOutput.object(
                        JsonOutput.stringField("acceptance_key", acceptance.acceptanceKey()),
                        JsonOutput.stringField("description", acceptance.description()),
                        JsonOutput.stringField("expected", acceptance.expectedResult()),
                        JsonOutput.stringField("status", acceptance.status())
                ).trim());
            }
        }
        return JsonOutput.array(values);
    }

    private static String boundWorkflows(List<WorkflowSpecBinding> bindings) {
        List<String> values = new ArrayList<String>();
        if (bindings != null) {
            for (WorkflowSpecBinding binding : bindings) {
                values.add(JsonOutput.object(
                        JsonOutput.stringField("run_key", binding.runKey()),
                        JsonOutput.stringField("binding_type", binding.bindingType())
                ).trim());
            }
        }
        return JsonOutput.array(values);
    }
}
