package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.goal.GoalCheck;

public interface GoalCheckRunner {
    String key();

    GoalCheck run(GoalCheckContext context) throws Exception;
}
