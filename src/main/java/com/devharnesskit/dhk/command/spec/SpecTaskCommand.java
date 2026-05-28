package com.devharnesskit.dhk.command.spec;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.db.TransactionTemplate;
import com.devharnesskit.dhk.guidance.CommandErrorGuidance;
import com.devharnesskit.dhk.guidance.EnumGuidance;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.spec.SpecChange;
import com.devharnesskit.dhk.model.spec.SpecTask;
import com.devharnesskit.dhk.repository.ProjectRepository;
import com.devharnesskit.dhk.repository.spec.SpecChangeRepository;
import com.devharnesskit.dhk.repository.spec.SpecEventRepository;
import com.devharnesskit.dhk.repository.spec.SpecTaskRepository;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.service.SensitiveDataGuard;
import com.devharnesskit.dhk.service.spec.SpecTaskService;

import java.nio.file.Path;
import java.sql.Connection;

public final class SpecTaskCommand implements Command {
    private final DbConnectionFactory connectionFactory = new DbConnectionFactory();
    private final MigrationRunner migrationRunner = new MigrationRunner();
    private final ProjectService projectService = new ProjectService();
    private final ProjectRepository projectRepository = new ProjectRepository();
    private final SpecChangeRepository changeRepository = new SpecChangeRepository();
    private final SpecTaskRepository taskRepository = new SpecTaskRepository();
    private final SensitiveDataGuard sensitiveDataGuard = new SensitiveDataGuard();
    private final TransactionTemplate transactionTemplate = new TransactionTemplate();
    private final SpecTaskService taskService = new SpecTaskService(taskRepository, new SpecEventRepository());

    public int run(CommandContext context, Args args) {
        String action = args.positional(2);
        if ("add".equals(action)) {
            return add(context, args);
        }
        if ("update".equals(action)) {
            return update(context, args);
        }
        return EnumGuidance.printInvalid(context, args, "INVALID_SPEC_TASK_ACTION", "spec task action",
                action, new String[]{"add", "update"}, new String[0],
                "dhk spec task add --change <change> --task <task> --title \"<title>\"",
                "docs/GOAL_CONFIGURATION.md");
    }

    private int add(CommandContext context, Args args) {
        String changeKey = args.option("change").trim();
        String taskKey = args.option("task").trim();
        String title = args.option("title").trim();
        if (changeKey.length() == 0 || taskKey.length() == 0 || title.length() == 0) {
            return CommandErrorGuidance.missing(context, args, "SPEC_TASK_ADD_ARGUMENTS_MISSING",
                    new String[]{"--change", "--task", "--title"},
                    "dhk spec task add --change <change> --task <task> --title \"<title>\"",
                    "docs/GOAL_CONFIGURATION.md");
        }
        if (!SpecCommandSupport.isKeyAllowed(taskKey)) {
            return CommandErrorGuidance.invalidKey(context, args, "INVALID_SPEC_TASK_KEY",
                    "spec task key", taskKey,
                    "dhk spec task add --change <change> --task task-1 --title \"<title>\"",
                    "docs/GOAL_CONFIGURATION.md");
        }
        String description = args.option("description", "").trim();
        String phase = args.option("phase", "").trim();
        if (SpecCommandSupport.rejectSensitive(context, sensitiveDataGuard, "spec task add",
                changeKey, taskKey, title, description, phase)) {
            return ExitCodes.VALIDATION_ERROR;
        }
        Path projectRoot = SpecCommandSupport.projectRoot(args, context);
        try (Connection connection = connectionFactory.open(projectRoot)) {
            Project project = SpecCommandSupport.ensureProject(context, projectRoot, connection,
                    projectService, projectRepository, migrationRunner);
            SpecChange change = changeRepository.findByKey(connection, changeKey);
            if (!SpecCommandSupport.belongsToProject(change, project)) {
                return CommandErrorGuidance.notFound(context, args, "SPEC_CHANGE_NOT_FOUND",
                        "spec change", changeKey, "dhk spec status --change <change>",
                        "docs/GOAL_CONFIGURATION.md");
            }
            final SpecChange selectedChange = change;
            final String selectedTask = taskKey;
            final String selectedTitle = title;
            final String selectedDescription = description;
            final String selectedPhase = phase;
            SpecTask task = transactionTemplate.execute(connection, new TransactionTemplate.Work<SpecTask>() {
                public SpecTask execute() throws Exception {
                    return taskService.addTask(connection, selectedChange, selectedTask, selectedTitle,
                            selectedDescription, selectedPhase, context.clock().now().toString());
                }
            });
            context.out().println("task_key: " + task.taskKey());
            context.out().println("status: " + task.status());
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR spec task add failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private int update(CommandContext context, Args args) {
        String changeKey = args.option("change").trim();
        String taskKey = args.option("task").trim();
        String rawStatus = args.option("status").trim();
        String status = EnumGuidance.normalizeSpecTaskStatus(rawStatus);
        if (changeKey.length() == 0 || taskKey.length() == 0 || status.length() == 0) {
            return CommandErrorGuidance.missing(context, args, "SPEC_TASK_UPDATE_ARGUMENTS_MISSING",
                    new String[]{"--change", "--task", "--status"},
                    "dhk spec task update --change <change> --task <task> --status done --evidence \"<evidence>\"",
                    "docs/GOAL_CONFIGURATION.md");
        }
        if (!SpecTaskService.isTaskStatusAllowed(status)) {
            return EnumGuidance.printInvalid(context, args, "INVALID_SPEC_TASK_STATUS", "spec task status",
                    rawStatus, EnumGuidance.SPEC_TASK_STATUSES, EnumGuidance.SPEC_TASK_ALIASES,
                    "dhk spec task update --change <change> --task <task> --status done --evidence \"<evidence>\"",
                    "docs/GOAL_CONFIGURATION.md");
        }
        String evidence = args.option("evidence", "").trim();
        if (SpecCommandSupport.rejectSensitive(context, sensitiveDataGuard, "spec task update",
                changeKey, taskKey, status, evidence)) {
            return ExitCodes.VALIDATION_ERROR;
        }
        Path projectRoot = SpecCommandSupport.projectRoot(args, context);
        try (Connection connection = connectionFactory.open(projectRoot)) {
            Project project = SpecCommandSupport.ensureProject(context, projectRoot, connection,
                    projectService, projectRepository, migrationRunner);
            SpecChange change = changeRepository.findByKey(connection, changeKey);
            if (!SpecCommandSupport.belongsToProject(change, project)) {
                return CommandErrorGuidance.notFound(context, args, "SPEC_CHANGE_NOT_FOUND",
                        "spec change", changeKey, "dhk spec status --change <change>",
                        "docs/GOAL_CONFIGURATION.md");
            }
            SpecTask task = taskRepository.findByKey(connection, changeKey, taskKey);
            if (task == null) {
                return CommandErrorGuidance.notFound(context, args, "SPEC_TASK_NOT_FOUND",
                        "spec task", taskKey,
                        "dhk spec task add --change " + changeKey + " --task <task> --title \"<title>\"",
                        "docs/GOAL_CONFIGURATION.md");
            }
            final SpecChange selectedChange = change;
            final SpecTask selectedTask = task;
            final String selectedStatus = status;
            final String selectedEvidence = evidence.length() == 0 ? task.evidence() : evidence;
            transactionTemplate.execute(connection, new TransactionTemplate.Work<Void>() {
                public Void execute() throws Exception {
                    taskService.updateTask(connection, selectedChange, selectedTask,
                            selectedStatus, selectedEvidence, context.clock().now().toString());
                    return null;
                }
            });
            context.out().println("task_key: " + taskKey);
            context.out().println("status: " + status);
            if (!rawStatus.equals(status)) {
                context.out().println("normalized_from: " + rawStatus);
            }
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR spec task update failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
