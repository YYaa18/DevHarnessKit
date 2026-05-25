package com.devharnesskit.dhk.command.memory;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.service.MemoryBackupService;
import com.devharnesskit.dhk.util.PathUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class BackupCommand implements Command {
    private final MemoryBackupService backupService = new MemoryBackupService();

    public int run(CommandContext context, Args args) {
        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        Path memoryDir = PathUtil.memoryDirectory(projectRoot);
        if (!Files.isDirectory(memoryDir)) {
            context.err().println("Memory is not initialized: " + memoryDir);
            return ExitCodes.NOT_FOUND;
        }
        Path out = args.hasOption("out")
                ? PathUtil.resolvePath(args.option("out"), context.workingDirectory())
                : backupService.defaultBackupPath(projectRoot, context.clock().now().toString(), "");
        try {
            backupService.writeBackup(memoryDir, out);
            context.out().println("backup_path: " + out);
            return ExitCodes.SUCCESS;
        } catch (IOException ex) {
            context.err().println("ERROR memory backup failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
