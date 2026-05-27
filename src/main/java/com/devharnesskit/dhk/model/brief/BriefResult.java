package com.devharnesskit.dhk.model.brief;

import java.nio.file.Path;

public final class BriefResult {
    private final WorkBrief workBrief;
    private final AgentBrief agentBrief;
    private final Path workBriefPath;
    private final Path agentBriefPath;

    public BriefResult(WorkBrief workBrief, AgentBrief agentBrief,
                       Path workBriefPath, Path agentBriefPath) {
        this.workBrief = workBrief;
        this.agentBrief = agentBrief;
        this.workBriefPath = workBriefPath;
        this.agentBriefPath = agentBriefPath;
    }

    public WorkBrief workBrief() { return workBrief; }
    public AgentBrief agentBrief() { return agentBrief; }
    public Path workBriefPath() { return workBriefPath; }
    public Path agentBriefPath() { return agentBriefPath; }
}
