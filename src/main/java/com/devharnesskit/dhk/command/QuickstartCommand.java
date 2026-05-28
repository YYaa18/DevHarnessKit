package com.devharnesskit.dhk.command;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.cli.VersionInfo;
import com.devharnesskit.dhk.guidance.EnumGuidance;
import com.devharnesskit.dhk.model.brief.BriefRequest;
import com.devharnesskit.dhk.model.brief.BriefResult;
import com.devharnesskit.dhk.model.brief.ModeAdvice;
import com.devharnesskit.dhk.model.config.ConfigureInitResult;
import com.devharnesskit.dhk.model.config.DevHarnessConfig;
import com.devharnesskit.dhk.model.goal.GoalPlan;
import com.devharnesskit.dhk.model.goal.GoalProfile;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.model.spec.SpecChange;
import com.devharnesskit.dhk.service.brief.BriefService;
import com.devharnesskit.dhk.service.config.DevHarnessConfigService;
import com.devharnesskit.dhk.service.goal.GoalOrchestrator;
import com.devharnesskit.dhk.service.goal.GoalProfileService;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;

public final class QuickstartCommand implements Command {
    private final DevHarnessConfigService configService;
    private final GoalOrchestrator orchestrator;
    private final BriefService briefService;
    private final GoalProfileService profileService;

    public QuickstartCommand() {
        this(new DevHarnessConfigService(), new GoalOrchestrator(), new BriefService(), new GoalProfileService());
    }

    QuickstartCommand(DevHarnessConfigService configService, GoalOrchestrator orchestrator,
                      BriefService briefService, GoalProfileService profileService) {
        this.configService = configService;
        this.orchestrator = orchestrator;
        this.briefService = briefService;
        this.profileService = profileService;
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
        String profile = args.option("profile", defaultProfile(preset)).trim();
        String module = args.option("module", "global").trim();
        String target = args.option("target", "all").trim();
        String graph = args.option("graph", "").trim();
        String mode = args.option("mode", "recommend").trim();
        String condition = args.option("condition", "").trim();
        boolean force = args.hasFlag("force");
        boolean dryRun = args.hasFlag("dry-run");
        boolean resumeExisting = args.hasFlag("resume-existing");
        if (module.length() == 0) {
            module = "global";
        }
        if (target.length() == 0) {
            target = "all";
        }
        if (profile.length() == 0) {
            profile = "java-api-change";
        }
        if (!EnumGuidance.isConfigurePresetAllowed(preset)) {
            return EnumGuidance.printInvalid(context, args, "INVALID_CONFIGURE_PRESET",
                    "configure preset", preset, EnumGuidance.CONFIGURE_PRESETS,
                    new String[]{"auto -> springboot-manual-ide-test"},
                    "dhk quickstart --preset springboot-manual-ide-test --task \"<task>\"",
                    "docs/GOAL_CONFIGURATION.md");
        }
        if (!EnumGuidance.isRecommendationModeAllowed(mode)) {
            return EnumGuidance.printInvalid(context, args, "INVALID_RECOMMENDATION_MODE",
                    "recommendation mode", mode, EnumGuidance.RECOMMENDATION_MODES,
                    new String[]{"auto -> recommend", "full -> strict"},
                    "dhk quickstart --mode recommend --task \"<task>\"", "README.md#quickstart");
        }
        if (!EnumGuidance.isGraphModeAllowed(graph)) {
            return EnumGuidance.printInvalid(context, args, "INVALID_GRAPH_MODE", "graph mode", graph,
                    EnumGuidance.GRAPH_MODES, new String[]{"optional -> advisory", "disabled -> off"},
                    "dhk quickstart --graph advisory --task \"<task>\"", "docs/GOAL_CONFIGURATION.md");
        }
        if (!EnumGuidance.isVerificationModeAllowed(args.option("compile", ""))) {
            return EnumGuidance.printInvalid(context, args, "INVALID_VERIFICATION_MODE",
                    "verification compile mode", args.option("compile", ""), EnumGuidance.VERIFICATION_MODES,
                    new String[0], "dhk quickstart --compile manual --task \"<task>\"",
                    "docs/GOAL_CONFIGURATION.md");
        }
        if (!EnumGuidance.isVerificationModeAllowed(args.option("test", ""))) {
            return EnumGuidance.printInvalid(context, args, "INVALID_VERIFICATION_MODE",
                    "verification test mode", args.option("test", ""), EnumGuidance.VERIFICATION_MODES,
                    new String[0], "dhk quickstart --test manual --task \"<task>\"",
                    "docs/GOAL_CONFIGURATION.md");
        }

        try {
            BriefRequest briefRequest = new BriefRequest(projectRoot, task, module, target, mode, profile, preset, graph);
            if (dryRun) {
                BriefResult brief = briefService.prepare(briefRequest, false);
                ConfigureInitResult plan = configService.init(projectRoot, preset, force,
                        args.option("compile", ""), args.option("test", ""), graph, target, true);
                QuickstartResult result = QuickstartResult.dryRun(projectRoot, plan,
                        brief.agentBrief().profileKey(), task, module, brief.workBrief().recommendation(),
                        installStateStatus(projectRoot), PathUtil.goalContext(projectRoot).toString(),
                        brief);
                printResult(context, args, result);
                return ExitCodes.SUCCESS;
            }

            BriefResult initialBrief = briefService.prepare(briefRequest, true);
            if (!initialBrief.workBrief().safeToStart()) {
                QuickstartResult result = QuickstartResult.analysisOnly(projectRoot, initialBrief,
                        installStateStatus(projectRoot));
                printResult(context, args, result);
                return ExitCodes.SUCCESS;
            }

            ConfigureInitResult configResult = ensureConfig(projectRoot, preset, force,
                    args.option("compile", ""), args.option("test", ""), graph, target);
            String selectedProfile = initialBrief.agentBrief().profileKey();
            GoalRun existingGoal = resumeExisting
                    ? matchingOpenGoal(context, projectRoot, selectedProfile, task, module)
                    : null;
            QuickstartResult result;
            if (existingGoal != null) {
                GoalPlan plan = orchestrator.plan(projectRoot, existingGoal);
                BriefResult brief = briefService.prepare(briefRequest, existingGoal, plan, true);
                result = QuickstartResult.existing(projectRoot, configResult, existingGoal,
                        installStateStatus(projectRoot), PathUtil.goalContext(projectRoot).toString(), brief);
            } else {
                GoalProfile selected = profileService.find(projectRoot, selectedProfile);
                String goalMode = goalMode(initialBrief.workBrief().recommendation(),
                        selected == null ? "" : selected.defaultMode());
                GoalOrchestrator.GoalStartResult start = orchestrator.start(context, projectRoot, selectedProfile, task,
                        module, goalMode, condition);
                GoalPlan plan = orchestrator.plan(projectRoot, start.goal());
                BriefResult brief = briefService.prepare(new BriefRequest(projectRoot, task, module, target,
                        initialBrief.workBrief().recommendation(), selectedProfile, preset, graph),
                        start.goal(), plan, true);
                SpecChange spec = start.specChange();
                result = QuickstartResult.started(projectRoot, configResult, start.goal(), start.contextPath().toString(),
                        start.workflowRun().runKey(), spec == null ? "" : spec.changeKey(),
                        installStateStatus(projectRoot), brief);
                if (!resumeExisting) {
                    GoalRun similar = matchingOpenGoal(context, projectRoot, selectedProfile, task, module);
                    if (similar != null && !similar.goalKey().equals(start.goal().goalKey())) {
                        result.similarGoal = similar.goalKey();
                        result.resumeDecision = "create_new";
                        result.resumeReason = "resume_existing_not_requested";
                    }
                }
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

    private String goalMode(String recommendation, String defaultMode) {
        String normalized = recommendation == null ? "" : recommendation.trim();
        if ("patch".equals(normalized) || "standard".equals(normalized) || "strict".equals(normalized)
                || "recommend".equals(normalized) || normalized.length() == 0) {
            return defaultMode == null || defaultMode.trim().length() == 0 ? "api" : defaultMode.trim();
        }
        if ("analyze_only".equals(normalized) || "ask".equals(normalized)) {
            return defaultMode == null || defaultMode.trim().length() == 0 ? "api" : defaultMode.trim();
        }
        return normalized;
    }

    private String defaultProfile(String preset) {
        String normalized = preset == null ? "" : preset.trim().toLowerCase(java.util.Locale.ROOT);
        return "demo-no-build".equals(normalized) ? "java-api-patch" : "java-api-change";
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

    private GoalRun matchingOpenGoal(CommandContext context, Path projectRoot, String profile, String task,
                                     String module) {
        if (!Files.isRegularFile(PathUtil.memoryDb(projectRoot)) || !Files.isRegularFile(PathUtil.projectJson(projectRoot))) {
            return null;
        }
        try {
            return orchestrator.findOpenByIdentity(context, projectRoot, profile, task, module);
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
        builder.append("recommendation: ").append(result.recommendation).append('\n');
        builder.append("safe_to_start: ").append(result.safeToStart).append('\n');
        builder.append("confirmation_required: ").append(result.confirmationRequired).append('\n');
        builder.append("confirmation_reason: ").append(result.confirmationReason).append('\n');
        builder.append("requires_user_confirmation_reason: ").append(result.confirmationReason).append('\n');
        builder.append("work_brief_path: ").append(result.workBriefPath).append('\n');
        builder.append("agent_brief_path: ").append(result.agentBriefPath).append('\n');
        builder.append("goal_key: ").append(result.goalKey).append('\n');
        builder.append("workflow_run: ").append(result.workflowRun).append('\n');
        builder.append("spec_change: ").append(result.specChange).append('\n');
        builder.append("current_action: ").append(result.currentAction).append('\n');
        builder.append("context_path: ").append(result.contextPath).append('\n');
        builder.append("next_command: ").append(result.nextCommand).append('\n');
        if (result.resumeDecision.length() > 0) {
            builder.append("resume_decision: ").append(result.resumeDecision).append('\n');
            builder.append("resume_reason: ").append(result.resumeReason).append('\n');
            builder.append("similar_goal: ").append(result.similarGoal).append('\n');
        }
        if (result.adapterNextCommand.length() > 0) {
            builder.append("adapter_next_command: ").append(result.adapterNextCommand).append('\n');
        }
        return builder.toString();
    }

    private String renderJson(QuickstartResult result) {
        return JsonOutput.object(
                JsonOutput.stringField("command", "quickstart"),
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
                JsonOutput.stringField("recommendation", result.recommendation),
                JsonOutput.stringField("safe_to_start", result.safeToStart),
                JsonOutput.stringField("confirmation_required", result.confirmationRequired),
                JsonOutput.stringField("confirmation_reason", result.confirmationReason),
                JsonOutput.stringField("requires_user_confirmation_reason", result.confirmationReason),
                JsonOutput.stringField("work_brief_path", result.workBriefPath),
                JsonOutput.stringField("agent_brief_path", result.agentBriefPath),
                JsonOutput.stringField("goal_key", result.goalKey),
                JsonOutput.stringField("workflow_run", result.workflowRun),
                JsonOutput.stringField("spec_change", result.specChange),
                JsonOutput.stringField("current_action", result.currentAction),
                JsonOutput.stringField("context_path", result.contextPath),
                JsonOutput.stringField("next_command", result.nextCommand),
                JsonOutput.stringField("resume_decision", result.resumeDecision),
                JsonOutput.stringField("resume_reason", result.resumeReason),
                JsonOutput.stringField("similar_goal", result.similarGoal),
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
        private String recommendation = "";
        private String workBriefPath = "";
        private String agentBriefPath = "";
        private String safeToStart = "";
        private String confirmationRequired = "";
        private String confirmationReason = "";
        private String resumeDecision = "";
        private String resumeReason = "";
        private String similarGoal = "";

        private static QuickstartResult dryRun(Path projectRoot, ConfigureInitResult plan, String profile,
                                               String task, String module, String recommendation,
                                               String installState, String contextPath, BriefResult brief) {
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
                    + "\" --module " + module + " --mode " + recommendation;
            result.adapterNextCommand = adapterNextCommand(projectRoot, installState);
            applyBrief(result, brief);
            return result;
        }

        private static QuickstartResult started(Path projectRoot, ConfigureInitResult config,
                                                GoalRun goal, String contextPath, String workflowRun,
                                                String specChange, String installState, BriefResult brief) {
            QuickstartResult result = baseStarted(projectRoot, config, goal, contextPath, installState, brief);
            result.quickstart = "ready";
            result.workflowRun = workflowRun;
            result.specChange = specChange;
            return result;
        }

        private static QuickstartResult existing(Path projectRoot, ConfigureInitResult config,
                                                 GoalRun goal, String installState, String contextPath,
                                                 BriefResult brief) {
            QuickstartResult result = baseStarted(projectRoot, config, goal, contextPath, installState, brief);
            result.quickstart = "existing_goal";
            result.workflowRun = goal.workflowRunKey();
            result.specChange = goal.specChangeKey();
            return result;
        }

        private static QuickstartResult baseStarted(Path projectRoot, ConfigureInitResult config,
                                                    GoalRun goal, String contextPath, String installState,
                                                    BriefResult brief) {
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
            applyBrief(result, brief);
            return result;
        }

        private static QuickstartResult analysisOnly(Path projectRoot, BriefResult brief, String installState) {
            QuickstartResult result = new QuickstartResult();
            result.quickstart = ModeAdvice.ASK.equals(brief.workBrief().recommendation()) ? "needs_confirmation" : "analyze_only";
            result.readiness = "not_started";
            result.projectRoot = projectRoot.toString();
            result.preset = "not_applied";
            result.profile = brief.agentBrief().profileKey();
            result.module = "pending";
            result.configStatus = "not_applied";
            result.configPath = PathUtil.devharnessConfig(projectRoot).toString();
            result.installState = installState;
            result.goalKey = "none";
            result.currentAction = "none";
            result.contextPath = PathUtil.goalContext(projectRoot).toString();
            result.nextCommand = "用户确认 Work Brief 后再开始";
            result.adapterNextCommand = "";
            applyBrief(result, brief);
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

        private static void applyBrief(QuickstartResult result, BriefResult brief) {
            if (brief == null) {
                return;
            }
            result.recommendation = brief.workBrief().recommendation();
            result.workBriefPath = brief.workBriefPath().toString();
            result.agentBriefPath = brief.agentBriefPath().toString();
            result.safeToStart = Boolean.toString(brief.workBrief().safeToStart());
            result.confirmationRequired = Boolean.toString(brief.workBrief().confirmationRequired());
            result.confirmationReason = brief.workBrief().confirmationReason();
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
