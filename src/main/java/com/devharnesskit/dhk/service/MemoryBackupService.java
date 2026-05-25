package com.devharnesskit.dhk.service;

import com.devharnesskit.dhk.util.PathUtil;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class MemoryBackupService {
    public Path defaultBackupPath(Path projectRoot, String timestamp, String reason) {
        String safeTimestamp = timestamp.replace(':', '-').replace('.', '-');
        String suffix = reason == null || reason.length() == 0 ? "" : "-" + reason;
        return PathUtil.backupsDirectory(projectRoot).resolve("dhk-memory-backup-" + safeTimestamp + suffix + ".zip");
    }

    public void writeBackup(Path memoryDir, Path out) throws IOException {
        Files.createDirectories(out.getParent());
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
