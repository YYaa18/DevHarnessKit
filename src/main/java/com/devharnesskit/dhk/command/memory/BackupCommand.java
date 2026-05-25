package com.devharnesskit.dhk.command.memory;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.util.PathUtil;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class BackupCommand implements Command {
    public int run(CommandContext context, Args args) {
        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        Path memoryDir = PathUtil.memoryDirectory(projectRoot);
        if (!Files.isDirectory(memoryDir)) {
            context.err().println("Memory is not initialized: " + memoryDir);
            return ExitCodes.NOT_FOUND;
        }
        Path out = args.hasOption("out")
                ? PathUtil.resolvePath(args.option("out"), context.workingDirectory())
                : defaultBackupPath(projectRoot, context.clock().now().toString());
        try {
            Files.createDirectories(out.getParent());
            writeBackup(memoryDir, out);
            context.out().println("backup_path: " + out);
            return ExitCodes.SUCCESS;
        } catch (IOException ex) {
            context.err().println("ERROR memory backup failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private Path defaultBackupPath(Path projectRoot, String timestamp) {
        String safeTimestamp = timestamp.replace(':', '-').replace('.', '-');
        return PathUtil.backupsDirectory(projectRoot).resolve("dhk-memory-backup-" + safeTimestamp + ".zip");
    }

    private void writeBackup(Path memoryDir, Path out) throws IOException {
        try (OutputStream fileOut = Files.newOutputStream(out);
             ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(fileOut))) {
            addFileIfExists(zipOut, memoryDir, memoryDir.resolve(PathUtil.PROJECT_JSON));
            addFileIfExists(zipOut, memoryDir, memoryDir.resolve(PathUtil.MEMORY_DB));
            addDirectoryFiles(zipOut, memoryDir, memoryDir.resolve(PathUtil.EXPORTS_DIRECTORY));
        }
    }

    private void addFileIfExists(ZipOutputStream zipOut, Path root, Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            return;
        }
        ZipEntry entry = new ZipEntry(root.relativize(file).toString().replace('\\', '/'));
        zipOut.putNextEntry(entry);
        Files.copy(file, zipOut);
        zipOut.closeEntry();
    }

    private void addDirectoryFiles(ZipOutputStream zipOut, Path root, Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path file : stream) {
                if (Files.isRegularFile(file)) {
                    addFileIfExists(zipOut, root, file);
                }
            }
        }
    }
}
