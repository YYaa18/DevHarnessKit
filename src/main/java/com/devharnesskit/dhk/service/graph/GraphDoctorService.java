package com.devharnesskit.dhk.service.graph;

import com.devharnesskit.dhk.model.graph.GraphConfig;
import com.devharnesskit.dhk.model.graph.GraphDoctorResult;
import com.devharnesskit.dhk.util.PathUtil;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class GraphDoctorService {
    private static final int CGC_TIMEOUT_SECONDS = 3;
    private static final int MAX_OUTPUT_BYTES = 4096;

    private final GraphConfigService configService;

    public GraphDoctorService() {
        this(new GraphConfigService());
    }

    GraphDoctorService(GraphConfigService configService) {
        this.configService = configService;
    }

    public GraphDoctorResult diagnose(Path projectRoot) {
        GraphConfig config = configService.load(projectRoot);
        String provider = config.provider();
        boolean cgcRequired = "cgc".equalsIgnoreCase(provider);
        if (!cgcRequired) {
            return new GraphDoctorResult(PathUtil.graphConfig(projectRoot),
                    config.loadedFromFile() ? "file" : "default",
                    provider,
                    config.cgcCommand(),
                    false,
                    false,
                    "not_required",
                    "provider is lite; CGC adapter is disabled",
                    true);
        }

        CommandProbe probe = probe(config.cgcCommand());
        return new GraphDoctorResult(PathUtil.graphConfig(projectRoot),
                config.loadedFromFile() ? "file" : "default",
                provider,
                config.cgcCommand(),
                true,
                probe.available,
                probe.status,
                probe.detail,
                false);
    }

    private CommandProbe probe(String commandLine) {
        List<String> command = command(commandLine);
        if (command.isEmpty()) {
            return new CommandProbe(false, "unavailable", "cgc_command is empty");
        }
        command.add("--version");
        Process process = null;
        try {
            process = new ProcessBuilder(command).redirectErrorStream(true).start();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            InputStream input = process.getInputStream();
            byte[] buffer = new byte[1024];
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(CGC_TIMEOUT_SECONDS);
            while (System.nanoTime() < deadline) {
                drain(input, output, buffer);
                if (process.waitFor(50L, TimeUnit.MILLISECONDS)) {
                    drain(input, output, buffer);
                    int exit = process.exitValue();
                    String detail = firstLine(output);
                    if (exit == 0) {
                        return new CommandProbe(true, "available",
                                detail.length() == 0 ? "command returned exit 0" : detail);
                    }
                    return new CommandProbe(false, "unavailable", "command exited " + exit
                            + (detail.length() == 0 ? "" : ": " + detail));
                }
            }
            process.destroy();
            if (!process.waitFor(200L, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
            }
            return new CommandProbe(false, "timeout", "command timed out after " + CGC_TIMEOUT_SECONDS + "s");
        } catch (Exception ex) {
            if (process != null) {
                process.destroyForcibly();
            }
            return new CommandProbe(false, "unavailable", ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    private List<String> command(String commandLine) {
        List<String> parts = new ArrayList<String>();
        String text = commandLine == null ? "" : commandLine.trim();
        if (text.length() == 0) {
            return parts;
        }
        String[] split = text.split("\\s+");
        for (String part : split) {
            if (part.length() > 0) {
                parts.add(part);
            }
        }
        return parts;
    }

    private void drain(InputStream input, ByteArrayOutputStream output, byte[] buffer) throws Exception {
        while (input.available() > 0 && output.size() < MAX_OUTPUT_BYTES) {
            int read = input.read(buffer, 0, Math.min(buffer.length, MAX_OUTPUT_BYTES - output.size()));
            if (read < 0) {
                return;
            }
            output.write(buffer, 0, read);
        }
    }

    private String firstLine(ByteArrayOutputStream output) throws Exception {
        String text = new String(output.toByteArray(), "UTF-8").trim();
        int newline = text.indexOf('\n');
        if (newline >= 0) {
            text = text.substring(0, newline).trim();
        }
        return text;
    }

    private static final class CommandProbe {
        private final boolean available;
        private final String status;
        private final String detail;

        private CommandProbe(boolean available, String status, String detail) {
            this.available = available;
            this.status = status;
            this.detail = detail == null ? "" : detail;
        }
    }
}
