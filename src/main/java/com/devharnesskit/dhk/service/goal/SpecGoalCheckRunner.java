package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.goal.GoalCheck;
import com.devharnesskit.dhk.model.spec.SpecAcceptance;
import com.devharnesskit.dhk.model.spec.SpecTask;
import com.devharnesskit.dhk.repository.spec.SpecAcceptanceRepository;
import com.devharnesskit.dhk.repository.spec.SpecTaskRepository;

import java.util.ArrayList;
import java.util.List;

final class SpecGoalCheckRunner extends AbstractGoalCheckRunner {
    private final GoalCheckRecorder recorder;
    private final SpecTaskRepository taskRepository;
    private final SpecAcceptanceRepository acceptanceRepository;

    SpecGoalCheckRunner(GoalCheckRecorder recorder, SpecTaskRepository taskRepository,
                        SpecAcceptanceRepository acceptanceRepository) {
        super("spec");
        this.recorder = recorder;
        this.taskRepository = taskRepository;
        this.acceptanceRepository = acceptanceRepository;
    }

    public GoalCheck run(GoalCheckContext context) throws Exception {
        if (context.goal().specChangeKey().length() == 0) {
            if (context.profile() != null && context.profile().specRequired()) {
                return recorder.save(context.connection(), context.projectRoot(), context.goal(), key(), "spec",
                        "", "failed", "spec required but goal has no spec change", null, context.now());
            }
            return recorder.save(context.connection(), context.projectRoot(), context.goal(), key(), "spec",
                    "", "skipped", "goal has no spec change", null, context.now());
        }
        List<String> missing = new ArrayList<String>();
        List<SpecTask> tasks = taskRepository.listByChange(context.connection(), context.goal().specChangeKey());
        List<SpecAcceptance> acceptances = acceptanceRepository.listByChange(context.connection(),
                context.goal().specChangeKey());
        if (context.profile() != null && context.profile().specRequired()
                && context.profile().specRequireNonEmptyTasks() && tasks.isEmpty()) {
            missing.add("spec has no tasks");
        }
        if (context.profile() != null && context.profile().specRequired()
                && context.profile().specRequireNonEmptyAcceptance() && acceptances.isEmpty()) {
            missing.add("spec has no acceptance criteria");
        }
        for (SpecTask task : tasks) {
            if (!"done".equals(task.status()) && !"skipped".equals(task.status())) {
                missing.add("task " + task.taskKey() + " is " + task.status());
            }
        }
        for (SpecAcceptance acceptance : acceptances) {
            if (!"passed".equals(acceptance.status()) && !"waived".equals(acceptance.status())) {
                missing.add("acceptance " + acceptance.acceptanceKey() + " is " + acceptance.status());
            }
        }
        String status = missing.isEmpty() ? "passed" : "failed";
        String summary = missing.isEmpty()
                ? "spec tasks and acceptance are closed"
                : "spec incomplete: " + missing;
        return recorder.save(context.connection(), context.projectRoot(), context.goal(), key(), "spec",
                "", status, summary, null, context.now());
    }
}
