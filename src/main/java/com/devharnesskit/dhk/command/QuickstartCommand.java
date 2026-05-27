package com.devharnesskit.dhk.command;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.cli.VersionInfo;
import com.devharnesskit.dhk.model.config.ConfigureInitResult;
import com.devharnesskit.dhk.model.config.DevHarnessConfig;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.model.spec.SpecChange;
import com.devharnesskit.dhk.service.config.DevHarnessConfigService;
import com.devharnesskit.dhk.service.goal.GoalOrchestrator;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;

public final class QuickstartCommand implements Command {
    private final DevHarnessConfigService configService;
    private final GoalOrchestrator orchestrator;

    public QuickstartCommand() {
        this(new DevHarnessConfigService(), new GoalOrchestrator());
    }

    QuickstartCommand(DevHarnessConfigService configService, GoalOrchestrator orchestrator) {
        this.configService = configService;
        this.orchestrator = orchestrator;
    }

    public int run(CommandContext context, Args args) {
        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        String task = args.option("task", "").trim();
        if (task.length() == 0) {
            printFailure(context, args, projectRoot, "missing_task",
                    "dhk quickstart --project-root " + quotePath(projectRoot)
                            + " --task \"<task>\"");
            return ExitCodes.USAGE_ERROR;
        }

        String preset = args.option("preset", "springboot-manual-ide-test").trim();
        String profile = args.option("profile", "java-api-change").trim();
        String module = args.option("module", "global").trim();
        String target = args.option("target", "all").trim();
        String graph = args.option("graph", "").trim();
        String mode = args.option("mode", "auto").trim();
        String condition = args.option("condition", "").trim();
        boolean force = args.hasFlag("force");
        boolean dryRun = args.hasFlag("dry-run");
        if (module.length() == 0) {
            module = "global";
        }
        if (target.length() == 0) {
            target = "all";
        }
        if (profile.length() == 0) {
            profile = "java-api-change";
        }

        try {
            if (dryRun) {
                ConfigureInitResult plan = configService.init(projectRoot, preset, force,
                        args.option("compile", ""), args.option("test", ""), graph, target, true);
                QuickstartResult result = QuickstartResult.dryRun(projectRoot, plan, profile, task, module, mode,
                        installStateStatus(projectRoot), PathUtil.goalContext(projectRoot).toString());
                printResult(context, args, result);
                return ExitCodes.SUCCESS;
            }

            ConfigureInitResult configResult = ensureConfig(projectRoot, preset, force,
                    args.option("compile", ""), args.option("test", ""), graph, target);
            GoalRun existingGoal = latestOpenGoal(context, projectRoot);
            QuickstartResult result;
            if (existingGoal != null) {
                result = QuickstartResult.existing(projectRoot, configResult, existingGoal,
                        installStateStatus(projectRoot), PathUtil.goalContext(projectRoot).toString());
            } else {
                GoalOrchestrator.GoalStartResult start = orchestrator.start(context, projectRoot, profile, task,
                        module, mode, condition);
                SpecChange spec = start.specChange();
                result = QuickstartResult.started(projectRoot, configResult, start.goal(), start.contextPath().toString(),
                        start.workflowRun().runKey(), spec == null ? "" : spec.changeKey(),
                        installStateStatus(projectRoot));
            }
            printResult(context, args, result);
            return ExitCodes.SUCCESS;
        } catch (IllegalArgumentException ex) {
            printFailure(context, args, projectRoot, "rejected",
                    "dhk quickstart --project-root " + quotePath(projectRoot)
                            + " --preset springboot-manual-ide-test --task \"<task>\"");
            context.err().println("ERROR quickstart rejected: " + ex.getMessage());
            return ExitCodes.USAGE_ERROR;
        } catch (Exception ex) {
            printFailure(context, args, projectRoot, "failed",
                    "dhk status --project-root " + quotePath(projectRoot));
            context.err().println("ERROR quickstart failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private ConfigureInitResult ensureConfig(Path projectRoot, String preset, boolean force,
                                             String compileOverride, String testOverride,
                                             String graphOverride, String target) throws Exception {
        if (configService.hasConfig(projectRoot) && !force) {
            DevHarnessConfig config = configService.load(projectRoot);
            return new ConfigureInitResult(PathUtil.devharnessConfig(projectRoot), config.preset(), false, config);
        }
        return configService.init(projectRoot, preset, force, compileOverride, testOverride, graphOverride, target, false);
    }

    private GoalRun latestOpenGoal(CommandContext context, Path projectRoot) {
        if (!Files.isRegularFile(PathUtil.memoryDb(projectRoot)) || !Files.isRegularFile(PathUtil.projectJson(projectRoot))) {
            return null;
        }
        try {
            return orchestrator.latestOpen(context, projectRoot);
        } catch (Exception ex) {
            return null;
        }
    }

    private String installStateStatus(Path projectRoot) {
        return Files.isRegularFile(PathUtil.devharnessDirectory(projectRoot).resolve("install-state.json"))
                ? "ok" : "missing";
    }

    private void printFailure(CommandContext context, Args args, Path projectRoot,
                              String quickstart, String nextCommand) {
        QuickstartResult result = QuickstartResult.failure(projectRoot, quickstart, nextCommand);
        printResult(context, args, result);
    }

    private void printResult(CommandContext context, Args args, QuickstartResult result) {
        if (JsonOutput.enabled(args)) {
            context.out().print(renderJson(result));
        } else {
            context.out().print(renderText(result));
        }
    }

    private String renderText(QuickstartResult result) {
        StringBuilder builder = new StringBuilder();
        builder.append("quickstart: ").append(result.quickstart).append('\n');
        builder.append("readiness: ").append(result.readiness).append('\n');
        builder.append("project_root: ").append(result.projectRoot).append('\n');
        builder.append("version: ").append(result.version).append('\n');
        builder.append("preset: ").append(result.preset).append('\n');
        builder.append("profile: ").append(result.profile).append('\n');
        builder.append("module: ").append(result.module).append('\n');
        builder.append("config: ").append(result.configStatus).append('\n');
        builder.append("config_path: ").append(result.configPath).append('\n');
        builder.append("install_state: ").append(result.installState).append('\n');
        builder.append("goal_key: ").append(result.goalKey).append('\n');
        builder.append("workflow_run: ").append(result.workflowRun).append('\n');
        builder.append("spec_change: ").append(result.specChange).append('\n');
        builder.append("current_action: ").append(result.currentAction).append('\n');
        builder.append("context_path: ").append(result.contextPath).append('\n');
        builder.append("next_command: ").append(result.nextCommand).append('\n');
        if (result.adapterNextCommand.length() > 0) {
            builder.append("adapter_next_command: ").append(result.adapterNextCommand).append('\n');
        }
        return builder.toString();
    }

    private String renderJson(QuickstartResult result) {
        return JsonOutput.object(
                JsonOutput.stringField("quickstart", result.quickstart),
                JsonOutput.stringField("readiness", result.readiness),
                JsonOutput.stringField("project_root", result.projectRoot),
                JsonOutput.stringField("version", result.version),
                JsonOutput.stringField("preset", result.preset),
                JsonOutput.stringField("profile", result.profile),
                JsonOutput.stringField("module", result.module),
                JsonOutput.stringField("config", result.configStatus),
                JsonOutput.stringField("config_path", result.configPath),
                JsonOutput.stringField("install_state", result.installState),
                JsonOutput.stringField("goal_key", result.goalKey),
                JsonOutput.stringField("workflow_run", result.workflowRun),
                JsonOutput.stringField("spec_change", result.specChange),
                JsonOutput.stringField("current_action", result.currentAction),
                JsonOutput.stringField("context_path", result.contextPath),
                JsonOutput.stringField("next_command", result.nextCommand),
                JsonOutput.stringField("adapter_next_command", result.adapterNextCommand)
        );
    }

    private String quotePath(Path path) {
        return "\"" + path.toString() + "\"";
    }

    private static final class QuickstartResult {
        private String quickstart = "";
        private String readiness = "";
        private String projectRoot = "";
        private String version = VersionInfo.version();
        private String preset = "";
        private String profile = "";
        private String module = "";
        private String configStatus = "";
        private String configPath = "";
        private String installState = "";
        private String goalKey = "";
        private String workflowRun = "";
        private String specChange = "";
        private String currentAction = "";
        private String contextPath = "";
        private String nextCommand = "";
        private String adapterNextCommand = "";

        private static QuickstartResult dryRun(Path projectRoot, ConfigureInitResult plan, String profile,
                                               String task, String module, String mode,
                                               String installState, String contextPath) {
            QuickstartResult result = new QuickstartResult();
            result.quickstart = "dry_run";
            result.readiness = "dry_run";
            result.projectRoot = projectRoot.toString();
            result.preset = plan.preset();
            result.profile = profile;
            result.module = module;
            result.configStatus = plan.forceRequired() ? "exists_force_required"
                    : (plan.wouldWrite() ? "would_create" : "exists");
            result.configPath = plan.configPath().toString();
            result.installState = installState;
            result.goalKey = "pending";
            result.currentAction = "pending";
            result.contextPath = contextPath;
            result.nextCommand = "dhk quickstart --project-root \"" + projectRoot + "\" --preset "
                    + plan.preset() + " --profile " + profile + " --task \"" + task
                    + "\" --module " + module + " --mode " + mode;
            result.adapterNextCommand = adapterNextCommand(projectRoot, installState);
            return result;
        }

        private static QuickstartResult started(Path projectRoot, ConfigureInitResult config,
                                                GoalRun goal, String contextPath, String workflowRun,
                                                String specChange, String installState) {
            QuickstartResult result = baseStarted(projectRoot, config, goal, contextPath, installState);
            result.quickstart = "ready";
            result.workflowRun = workflowRun;
            result.specChange = specChange;
            return result;
        }

        private static QuickstartResult existing(Path projectRoot, ConfigureInitResult config,
                                                 GoalRun goal, String installState, String contextPath) {
            QuickstartResult result = baseStarted(projectRoot, config, goal, contextPath, installState);
            result.quickstart = "existing_goal";
            result.workflowRun = goal.workflowRunKey();
            result.specChange = goal.specChangeKey();
            return result;
        }

        private static QuickstartResult baseStarted(Path projectRoot, ConfigureInitResult config,
                                                    GoalRun goal, String contextPath, String installState) {
            QuickstartResult result = new QuickstartResult();
            result.readiness = "ok".equals(installState) ? "ready" : "ready_with_warnings";
            result.projectRoot = projectRoot.toString();
            result.preset = config.preset();
            result.profile = goal.profileKey();
            result.module = goal.moduleName();
            result.configStatus = config.configCreated() ? "created" : "existing";
            result.configPath = config.configPath().toString();
            result.installState = installState;
            result.goalKey = goal.goalKey();
            result.currentAction = goal.currentAction();
            result.contextPath = contextPath;
            result.nextCommand = "dhk goal next --project-root \"" + projectRoot + "\" --goal " + goal.goalKey();
            result.adapterNextCommand = adapterNextCommand(projectRoot, installState);
            return result;
        }

        private static QuickstartResult failure(Path projectRoot, String quickstart, String nextCommand) {
            QuickstartResult result = new QuickstartResult();
            result.quickstart = quickstart;
            result.readiness = "not_ready";
            result.projectRoot = projectRoot.toString();
            result.preset = "unknown";
            result.profile = "unknown";
            result.module = "unknown";
            result.configStatus = "unknown";
            result.configPath = PathUtil.devharnessConfig(projectRoot).toString();
            result.installState = Files.isRegularFile(PathUtil.devharnessDirectory(projectRoot).resolve("install-state.json"))
                    ? "ok" : "missing";
            result.goalKey = "none";
            result.currentAction = "none";
            result.contextPath = PathUtil.goalContext(projectRoot).toString();
            result.nextCommand = nextCommand;
            result.adapterNextCommand = adapterNextCommand(projectRoot, result.installState);
            return result;
        }

        private static String adapterNextCommand(Path projectRoot, String installState) {
            if ("ok".equals(installState)) {
                return "";
            }
            return "scripts/devharness-control-panel.sh plan --project-root \"" + projectRoot
                    + "\" --target all --dry-run";
        }
    }
}
