package com.devharnesskit.dhk.command;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.cli.VersionInfo;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.config.DevHarnessConfig;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.repository.goal.GoalRunRepository;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.service.config.DevHarnessConfigService;
import com.devharnesskit.dhk.service.goal.WorkspaceFingerprintService;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public final class StatusCommand implements Command {
    private final String commandName;
    private final DbConnectionFactory connectionFactory;
    private final ProjectService projectService;
    private final GoalRunRepository goalRunRepository;
    private final DevHarnessConfigService configService;
    private final WorkspaceFingerprintService fingerprintService;

    public StatusCommand(String commandName) {
        this(commandName, new DbConnectionFactory(), new ProjectService(), new GoalRunRepository(),
                new DevHarnessConfigService(), new WorkspaceFingerprintService());
    }

    StatusCommand(String commandName, DbConnectionFactory connectionFactory, ProjectService projectService,
                  GoalRunRepository goalRunRepository, DevHarnessConfigService configService,
                  WorkspaceFingerprintService fingerprintService) {
        this.commandName = commandName == null || commandName.length() == 0 ? "status" : commandName;
        this.connectionFactory = connectionFactory;
        this.projectService = projectService;
        this.goalRunRepository = goalRunRepository;
        this.configService = configService;
        this.fingerprintService = fingerprintService;
    }

    public int run(CommandContext context, Args args) {
        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        Snapshot snapshot = snapshot(projectRoot);
        String rendered = render(args, snapshot);
        String writePath = args.option("write", "");
        if (writePath.length() > 0) {
            try {
                Path out = PathUtil.resolvePath(writePath, context.workingDirectory());
                if (out.getParent() != null) {
                    Files.createDirectories(out.getParent());
                }
                Files.write(out, rendered.getBytes(StandardCharsets.UTF_8));
            } catch (Exception ex) {
                context.err().println("ERROR " + commandName + " write failed: " + ex.getMessage());
                return ExitCodes.RUNTIME_ERROR;
            }
        }
        context.out().print(rendered);
        if (args.hasFlag("exit-code") && !"ready".equals(snapshot.readiness)) {
            return ExitCodes.VALIDATION_ERROR;
        }
        return ExitCodes.SUCCESS;
    }

    private Snapshot snapshot(Path projectRoot) {
        Path configPath = PathUtil.devharnessConfig(projectRoot);
        Path installState = projectRoot.resolve(".agents/devharness/install-state.json");
        Path policyPath = PathUtil.devharnessPolicy(projectRoot);
        Path dbPath = PathUtil.memoryDb(projectRoot);
        Path projectJson = PathUtil.projectJson(projectRoot);

        Snapshot snapshot = new Snapshot();
        snapshot.projectRoot = projectRoot.toString();
        snapshot.version = VersionInfo.version();
        snapshot.configStatus = Files.isRegularFile(configPath) ? "ok" : "missing";
        snapshot.installStateStatus = Files.isRegularFile(installState) ? "ok" : "missing";
        snapshot.policyStatus = Files.isRegularFile(policyPath) ? "ok" : "default";
        snapshot.memoryDbStatus = Files.isRegularFile(dbPath) ? "ok" : "missing";
        snapshot.schemaVersion = "missing";
        snapshot.graphSnapshotStatus = Files.isRegularFile(PathUtil.graphSnapshotJson(projectRoot)) ? "ok" : "missing";
        snapshot.graphContextStatus = Files.isRegularFile(PathUtil.graphContext(projectRoot)) ? "ok" : "missing";
        snapshot.impactMapStatus = Files.isRegularFile(PathUtil.graphImpactMap(projectRoot)) ? "ok" : "missing";
        snapshot.bddContextStatus = Files.isRegularFile(PathUtil.bddContext(projectRoot)) ? "ok" : "missing";
        snapshot.graphStale = graphStale(projectRoot);

        if (Files.isRegularFile(configPath)) {
            try {
                DevHarnessConfig config = configService.load(projectRoot);
                snapshot.preset = config.preset();
                snapshot.compileMode = config.compileMode();
                snapshot.testMode = config.testMode();
                snapshot.graphMode = config.graphMode();
            } catch (Exception ex) {
                snapshot.configStatus = "invalid";
                snapshot.preset = "unknown";
            }
        }

        if (Files.isRegularFile(dbPath)) {
            try (Connection connection = connectionFactory.open(projectRoot)) {
                snapshot.schemaVersion = String.valueOf(MigrationRunner.currentSchemaVersion(connection));
                if (Files.isRegularFile(projectJson) && MigrationRunner.hasTable(connection, "goal_run")) {
                    Project project = projectService.readProject(projectJson);
                    GoalRun goal = goalRunRepository.latestOpen(connection, project.projectKey());
                    if (goal != null) {
                        snapshot.activeGoal = goal.goalKey();
                        snapshot.activeGoalStatus = goal.status();
                        snapshot.currentAction = goal.currentAction();
                    }
                }
            } catch (Exception ex) {
                snapshot.memoryDbStatus = "error";
                snapshot.schemaVersion = "error";
                snapshot.errors.add("memory_db: " + ex.getMessage());
            }
        }

        if (!"ok".equals(snapshot.configStatus)) {
            snapshot.missing.add("config");
        }
        if (!"ok".equals(snapshot.installStateStatus)) {
            snapshot.missing.add("install_state");
        }
        if (!"ok".equals(snapshot.memoryDbStatus)) {
            snapshot.missing.add("memory_db");
        }
        snapshot.readiness = snapshot.missing.isEmpty() ? "ready" : "not_ready";
        snapshot.nextCommand = nextCommand(projectRoot, snapshot);
        return snapshot;
    }

    private String graphStale(Path projectRoot) {
        Path snapshot = PathUtil.graphSnapshotJson(projectRoot);
        if (!Files.isRegularFile(snapshot)) {
            return "unknown";
        }
        try {
            String text = new String(Files.readAllBytes(snapshot), StandardCharsets.UTF_8);
            String stored = extractJsonString(text, "workspace_fingerprint");
            if (stored.length() == 0) {
                return "unknown";
            }
            return String.valueOf(!stored.equals(fingerprintService.workspaceFingerprint(projectRoot)));
        } catch (Exception ex) {
            return "unknown";
        }
    }

    private String extractJsonString(String json, String key) {
        String pattern = "\"" + key + "\"";
        int keyIndex = json.indexOf(pattern);
        if (keyIndex < 0) {
            return "";
        }
        int colon = json.indexOf(':', keyIndex + pattern.length());
        if (colon < 0) {
            return "";
        }
        int firstQuote = json.indexOf('"', colon + 1);
        if (firstQuote < 0) {
            return "";
        }
        int secondQuote = json.indexOf('"', firstQuote + 1);
        if (secondQuote < 0) {
            return "";
        }
        return json.substring(firstQuote + 1, secondQuote);
    }

    private String nextCommand(Path projectRoot, Snapshot snapshot) {
        if (!"ok".equals(snapshot.configStatus)) {
            return "dhk configure init --project-root " + quotePath(projectRoot)
                    + " --preset springboot-manual-ide-test --dry-run";
        }
        if (!"ok".equals(snapshot.installStateStatus)) {
            return "scripts/devharness-control-panel.sh plan --project-root "
                    + quotePath(projectRoot) + " --target all --dry-run";
        }
        if (!"ok".equals(snapshot.memoryDbStatus)) {
            return "dhk memory init --project-root " + quotePath(projectRoot);
        }
        if (!"none".equals(snapshot.activeGoal)) {
            return "dhk goal next --project-root " + quotePath(projectRoot)
                    + " --goal " + snapshot.activeGoal;
        }
        return "dhk goal start --project-root " + quotePath(projectRoot)
                + " --profile java-api-change --task \"<task>\"";
    }

    private String quotePath(Path path) {
        return "\"" + path.toString() + "\"";
    }

    private String render(Args args, Snapshot snapshot) {
        if (JsonOutput.enabled(args)) {
            return renderJson(snapshot);
        }
        if (args.hasFlag("markdown") || "markdown".equalsIgnoreCase(args.option("format", ""))) {
            return renderMarkdown(snapshot);
        }
        return renderText(snapshot);
    }

    private String renderText(Snapshot snapshot) {
        StringBuilder builder = new StringBuilder();
        builder.append("dhk ").append(commandName).append('\n');
        appendLines(builder, snapshot, false);
        return builder.toString();
    }

    private String renderMarkdown(Snapshot snapshot) {
        StringBuilder builder = new StringBuilder();
        builder.append("# DevHarnessKit ").append(commandName).append("\n\n");
        appendLines(builder, snapshot, true);
        return builder.toString();
    }

    private void appendLines(StringBuilder builder, Snapshot snapshot, boolean markdown) {
        String prefix = markdown ? "- " : "";
        builder.append(prefix).append("readiness: ").append(snapshot.readiness).append('\n');
        builder.append(prefix).append("project_root: ").append(snapshot.projectRoot).append('\n');
        builder.append(prefix).append("version: ").append(snapshot.version).append('\n');
        builder.append(prefix).append("memory_db: ").append(snapshot.memoryDbStatus).append('\n');
        builder.append(prefix).append("schema_version: ").append(snapshot.schemaVersion).append('\n');
        builder.append(prefix).append("config: ").append(snapshot.configStatus).append('\n');
        builder.append(prefix).append("preset: ").append(snapshot.preset).append('\n');
        builder.append(prefix).append("compile_mode: ").append(snapshot.compileMode).append('\n');
        builder.append(prefix).append("test_mode: ").append(snapshot.testMode).append('\n');
        builder.append(prefix).append("graph_mode: ").append(snapshot.graphMode).append('\n');
        builder.append(prefix).append("install_state: ").append(snapshot.installStateStatus).append('\n');
        builder.append(prefix).append("policy: ").append(snapshot.policyStatus).append('\n');
        builder.append(prefix).append("graph_snapshot: ").append(snapshot.graphSnapshotStatus).append('\n');
        builder.append(prefix).append("graph_context: ").append(snapshot.graphContextStatus).append('\n');
        builder.append(prefix).append("impact_map: ").append(snapshot.impactMapStatus).append('\n');
        builder.append(prefix).append("graph_stale: ").append(snapshot.graphStale).append('\n');
        builder.append(prefix).append("bdd_context: ").append(snapshot.bddContextStatus).append('\n');
        builder.append(prefix).append("active_goal: ").append(snapshot.activeGoal).append('\n');
        builder.append(prefix).append("active_goal_status: ").append(snapshot.activeGoalStatus).append('\n');
        builder.append(prefix).append("current_action: ").append(snapshot.currentAction).append('\n');
        builder.append(prefix).append("missing: ").append(join(snapshot.missing)).append('\n');
        builder.append(prefix).append("next_command: ").append(snapshot.nextCommand).append('\n');
    }

    private String renderJson(Snapshot snapshot) {
        return JsonOutput.object(
                JsonOutput.stringField("command", commandName),
                JsonOutput.stringField("readiness", snapshot.readiness),
                JsonOutput.stringField("project_root", snapshot.projectRoot),
                JsonOutput.stringField("version", snapshot.version),
                JsonOutput.stringField("memory_db", snapshot.memoryDbStatus),
                JsonOutput.stringField("schema_version", snapshot.schemaVersion),
                JsonOutput.stringField("config", snapshot.configStatus),
                JsonOutput.stringField("preset", snapshot.preset),
                JsonOutput.stringField("compile_mode", snapshot.compileMode),
                JsonOutput.stringField("test_mode", snapshot.testMode),
                JsonOutput.stringField("graph_mode", snapshot.graphMode),
                JsonOutput.stringField("install_state", snapshot.installStateStatus),
                JsonOutput.stringField("policy", snapshot.policyStatus),
                JsonOutput.stringField("graph_snapshot", snapshot.graphSnapshotStatus),
                JsonOutput.stringField("graph_context", snapshot.graphContextStatus),
                JsonOutput.stringField("impact_map", snapshot.impactMapStatus),
                JsonOutput.stringField("graph_stale", snapshot.graphStale),
                JsonOutput.stringField("bdd_context", snapshot.bddContextStatus),
                JsonOutput.stringField("active_goal", snapshot.activeGoal),
                JsonOutput.stringField("active_goal_status", snapshot.activeGoalStatus),
                JsonOutput.stringField("current_action", snapshot.currentAction),
                JsonOutput.rawField("missing", JsonOutput.stringArray(snapshot.missing.toArray(new String[snapshot.missing.size()]))),
                JsonOutput.stringField("next_command", snapshot.nextCommand)
        );
    }

    private String join(List<String> values) {
        if (values.isEmpty()) {
            return "none";
        }
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private static final class Snapshot {
        private String projectRoot = "";
        private String version = "";
        private String readiness = "not_ready";
        private String memoryDbStatus = "missing";
        private String schemaVersion = "missing";
        private String configStatus = "missing";
        private String preset = "none";
        private String compileMode = "unknown";
        private String testMode = "unknown";
        private String graphMode = "unknown";
        private String installStateStatus = "missing";
        private String policyStatus = "default";
        private String graphSnapshotStatus = "missing";
        private String graphContextStatus = "missing";
        private String impactMapStatus = "missing";
        private String graphStale = "unknown";
        private String bddContextStatus = "missing";
        private String activeGoal = "none";
        private String activeGoalStatus = "none";
        private String currentAction = "none";
        private String nextCommand = "";
        private final List<String> missing = new ArrayList<String>();
        private final List<String> errors = new ArrayList<String>();
    }
}
