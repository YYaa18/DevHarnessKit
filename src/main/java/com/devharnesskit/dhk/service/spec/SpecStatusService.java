package com.devharnesskit.dhk.service.spec;

import com.devharnesskit.dhk.model.spec.SpecAcceptance;
import com.devharnesskit.dhk.model.spec.SpecTask;

import java.util.List;

public final class SpecStatusService {
    public int openTaskCount(List<SpecTask> tasks) {
        int count = 0;
        for (SpecTask task : tasks) {
            if (!"done".equals(task.status()) && !"skipped".equals(task.status())) {
                count++;
            }
        }
        return count;
    }

    public int openAcceptanceCount(List<SpecAcceptance> acceptances) {
        int count = 0;
        for (SpecAcceptance acceptance : acceptances) {
            if (!"passed".equals(acceptance.status()) && !"waived".equals(acceptance.status())) {
                count++;
            }
        }
        return count;
    }
}
