package com.devharnesskit.dhk.service;

import com.devharnesskit.dhk.model.Project;

public final class ProjectIndexRenderer {
    public String render(Project project) {
        StringBuilder builder = new StringBuilder();
        builder.append("# PROJECT_INDEX\n\n");
        builder.append("<project>\n");
        builder.append("- project_key: ").append(project.projectKey()).append('\n');
        builder.append("- project_name: ").append(project.projectName()).append('\n');
        builder.append("- project_type: ").append(project.projectType()).append('\n');
        builder.append("- language: ").append(project.language()).append('\n');
        builder.append("- framework: ").append(project.framework()).append('\n');
        builder.append("- database_type: ").append(project.databaseType()).append('\n');
        builder.append("</project>\n\n");
        builder.append("<exports>\n");
        builder.append("- CURRENT_CONTEXT.md: short confirmed memory for the current task.\n");
        builder.append("- RECOVERY_CONTEXT.md: latest checkpoint and relevant memory for resuming work.\n");
        builder.append("- SQL_RESULT.md: temporary readonly SQL query output; not long-term memory.\n");
        builder.append("</exports>\n\n");
        builder.append("<usage>\n");
        builder.append("- Before development: dhk memory export --task \"...\"\n");
        builder.append("- Resume work: dhk memory recover --latest\n");
        builder.append("- Verify business SQL only when explicitly requested: dhk db sql --sql \"...\"\n");
        builder.append("</usage>\n");
        return builder.toString();
    }
}
