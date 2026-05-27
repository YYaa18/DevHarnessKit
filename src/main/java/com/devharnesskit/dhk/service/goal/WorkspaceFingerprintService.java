package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.util.PathUtil;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class WorkspaceFingerprintService {
    private static final int COMMAND_TIMEOUT_SECONDS = 5;

    public String workspaceFingerprint(Path projectRoot) {
        if (projectRoot == null) {
            return "";
        }
        Path root = projectRoot.toAbsolutePath().normalize();
        String git = gitFingerprint(root);
        if (git.length() > 0) {
            return git;
        }
        return fallbackFingerprint(root);
    }

    public String contextFingerprint(Path projectRoot) {
        if (projectRoot == null) {
            return "";
        }
        List<Path> files = new ArrayList<Path>();
        files.add(PathUtil.currentContext(projectRoot));
        files.add(PathUtil.goalContext(projectRoot));
        files.add(PathUtil.workflowContext(projectRoot));
        files.add(PathUtil.specContext(projectRoot));
        StringBuilder builder = new StringBuilder();
        for (Path file : files) {
            if (!Files.isRegularFile(file)) {
                continue;
            }
            try {
                builder.append(projectRoot.toAbsolutePath().normalize().relativize(file.toAbsolutePath().normalize()))
                        .append('\n')
                        .append(new String(Files.readAllBytes(file), StandardCharsets.UTF_8))
                        .append('\n');
            } catch (Exception ignored) {
                builder.append(file.toString()).append(":unreadable\n");
            }
        }
        return "context:" + sha256(builder.toString());
    }

    public String checkFingerprint(String checkKey, String status, String summary, String evidencePath,
                                   String workspaceFingerprint, String contextFingerprint) {
        String text = value(checkKey) + "\n" + value(status) + "\n" + value(summary) + "\n"
                + value(evidencePath) + "\n" + value(workspaceFingerprint) + "\n"
                + value(contextFingerprint);
        return "check:" + sha256(text);
    }

    private String gitFingerprint(Path root) {
        CommandResult inside = run(root, new String[]{"git", "-C", root.toString(),
                "rev-parse", "--is-inside-work-tree"});
        if (!inside.success() || inside.output().trim().indexOf("true") < 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append(run(root, new String[]{"git", "-C", root.toString(),
                "status", "--porcelain=v1", "--untracked-files=all"}).output()).append('\n');
        builder.append(run(root, new String[]{"git", "-C", root.toString(),
                "diff", "--binary", "--no-ext-diff"}).output()).append('\n');
        builder.append(run(root, new String[]{"git", "-C", root.toString(),
                "diff", "--cached", "--binary", "--no-ext-diff"}).output()).append('\n');
        appendUntrackedContent(root, builder);
        return "git:" + sha256(builder.toString());
    }

    private void appendUntrackedContent(Path root, StringBuilder builder) {
        CommandResult result = run(root, new String[]{"git", "-C", root.toString(),
                "ls-files", "--others", "--exclude-standard", "-z"});
        if (!result.success() || result.output().length() == 0) {
            return;
        }
        String[] paths = result.output().split("\u0000");
        for (String path : paths) {
            if (path.length() == 0) {
                continue;
            }
            Path file = root.resolve(path).normalize();
            if (!Files.isRegularFile(file)) {
                continue;
            }
            try {
                builder.append("untracked:").append(path).append('\n');
                builder.append(sha256(Files.readAllBytes(file))).append('\n');
            } catch (Exception ignored) {
                builder.append("untracked:").append(path).append(":unreadable\n");
            }
        }
    }

    private String fallbackFingerprint(final Path root) {
        final List<Path> files = new ArrayList<Path>();
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    return shouldSkip(root, dir) ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
                }

                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (attrs.isRegularFile() && !shouldSkip(root, file)) {
                        files.add(file.toAbsolutePath().normalize());
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception ignored) {
            return "fallback:" + sha256("unreadable");
        }
        Collections.sort(files, new Comparator<Path>() {
            public int compare(Path left, Path right) {
                return root.relativize(left).toString().compareTo(root.relativize(right).toString());
            }
        });
        StringBuilder builder = new StringBuilder();
        for (Path file : files) {
            try {
                builder.append(root.relativize(file)).append('\n')
                        .append(Files.size(file)).append('\n')
                        .append(sha256(Files.readAllBytes(file))).append('\n');
            } catch (Exception ignored) {
                builder.append(root.relativize(file)).append(":unreadable\n");
            }
        }
        return "fallback:" + sha256(builder.toString());
    }

    private boolean shouldSkip(Path root, Path path) {
        Path relative;
        try {
            relative = root.relativize(path.toAbsolutePath().normalize());
        } catch (Exception ex) {
            return true;
        }
        if (relative.getNameCount() == 0) {
            return false;
        }
        String first = relative.getName(0).toString();
        if (".git".equals(first) || "target".equals(first)) {
            return true;
        }
        if (".agents".equals(first) && relative.getNameCount() > 1) {
            String second = relative.getName(1).toString();
            if ("memory".equals(second) || "tools".equals(second)) {
                return true;
            }
            if ("graph".equals(second) && relative.getNameCount() > 2) {
                String third = relative.getName(2).toString();
                return "exports".equals(third) || "snapshots".equals(third) || "cache".equals(third);
            }
            if ("bdd".equals(second) && relative.getNameCount() > 2) {
                String third = relative.getName(2).toString();
                return "exports".equals(third);
            }
        }
        return false;
    }

    private CommandResult run(Path root, String[] command) {
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.directory(root.toFile());
            builder.redirectErrorStream(true);
            Process process = builder.start();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            InputStream in = process.getInputStream();
            byte[] buffer = new byte[8192];
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(COMMAND_TIMEOUT_SECONDS);
            while (System.nanoTime() < deadline) {
                while (in.available() > 0) {
                    int read = in.read(buffer);
                    if (read < 0) {
                        break;
                    }
                    output.write(buffer, 0, read);
                }
                if (process.waitFor(50L, TimeUnit.MILLISECONDS)) {
                    while (in.available() > 0) {
                        int read = in.read(buffer);
                        if (read < 0) {
                            break;
                        }
                        output.write(buffer, 0, read);
                    }
                    return new CommandResult(process.exitValue(),
                            new String(output.toByteArray(), StandardCharsets.UTF_8));
                }
            }
            process.destroyForcibly();
            return new CommandResult(124, new String(output.toByteArray(), StandardCharsets.UTF_8));
        } catch (Exception ex) {
            return new CommandResult(1, "");
        }
    }

    private String sha256(String text) {
        return sha256(text.getBytes(StandardCharsets.UTF_8));
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder builder = new StringBuilder();
            for (byte b : hash) {
                builder.append(String.format("%02x", b & 0xff));
            }
            return builder.toString();
        } catch (Exception ex) {
            return "";
        }
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private static final class CommandResult {
        private final int exitCode;
        private final String output;

        private CommandResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output == null ? "" : output;
        }

        private boolean success() {
            return exitCode == 0;
        }

        private String output() {
            return output;
        }
    }
}
