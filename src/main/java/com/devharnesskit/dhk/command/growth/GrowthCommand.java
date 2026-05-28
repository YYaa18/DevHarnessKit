package com.devharnesskit.dhk.command.growth;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.model.brief.GrowthLesson;
import com.devharnesskit.dhk.service.brief.BriefLifecycleService;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;
import java.util.List;

public final class GrowthCommand implements Command {
    private final BriefLifecycleService lifecycleService;

    public GrowthCommand() {
        this(new BriefLifecycleService());
    }

    GrowthCommand(BriefLifecycleService lifecycleService) {
        this.lifecycleService = lifecycleService;
    }

    public int run(CommandContext context, Args args) {
        String subCommand = args.subCommand();
        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        try {
            if ("review".equals(subCommand) || subCommand.length() == 0) {
                return review(context, projectRoot);
            }
            if ("confirm".equals(subCommand)) {
                return confirm(context, args, projectRoot);
            }
            if ("export".equals(subCommand)) {
                return export(context, projectRoot);
            }
            context.err().println("Unknown growth command: " + subCommand);
            context.err().println("Supported growth commands: review, confirm, export");
            return ExitCodes.USAGE_ERROR;
        } catch (IllegalArgumentException ex) {
            context.err().println(ex.getMessage());
            return ExitCodes.VALIDATION_ERROR;
        } catch (Exception ex) {
            context.err().println("ERROR growth command failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private int review(CommandContext context, Path projectRoot) {
        List<GrowthLesson> lessons = lifecycleService.loadGrowthLessons(projectRoot);
        context.out().println("growth_lessons: " + lessons.size());
        for (GrowthLesson lesson : lessons) {
            context.out().println("- " + lesson.lessonId() + " [" + lesson.status()
                    + "] advisory_only=" + lesson.advisoryOnly() + ": " + lesson.title());
        }
        return ExitCodes.SUCCESS;
    }

    private int confirm(CommandContext context, Args args, Path projectRoot) throws Exception {
        String lessonId = args.option("lesson", args.option("lesson-id", "")).trim();
        if (lessonId.length() == 0) {
            context.err().println("Missing required parameter: --lesson");
            return ExitCodes.USAGE_ERROR;
        }
        GrowthLesson lesson = lifecycleService.confirmGrowth(projectRoot, lessonId);
        context.out().println("lesson_id: " + lesson.lessonId());
        context.out().println("status: " + lesson.status());
        context.out().println("advisory_only: " + lesson.advisoryOnly());
        return ExitCodes.SUCCESS;
    }

    private int export(CommandContext context, Path projectRoot) throws Exception {
        Path path = lifecycleService.exportGrowth(projectRoot);
        context.out().println("growth_context_path: " + path);
        context.out().println("advisory_only: true");
        return ExitCodes.SUCCESS;
    }
}
