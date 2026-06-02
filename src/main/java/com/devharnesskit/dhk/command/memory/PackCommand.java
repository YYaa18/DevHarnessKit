package com.devharnesskit.dhk.command.memory;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.guidance.CommandErrorGuidance;
import com.devharnesskit.dhk.model.MemoryItem;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.repository.FtsRepository;
import com.devharnesskit.dhk.repository.MemoryRepository;
import com.devharnesskit.dhk.service.MemoryIdentity;
import com.devharnesskit.dhk.service.MemoryType;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.JsonUtil;
import com.devharnesskit.dhk.util.PathUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class PackCommand implements Command {
    private static final String SCHEMA_VERSION = "devharness-memory-pack/v1";
    private static final String MANIFEST = "manifest.json";
    private static final String ITEMS = "items.jsonl";

    private final DbConnectionFactory connectionFactory;
    private final ProjectService projectService;
    private final MemoryRepository memoryRepository;
    private final FtsRepository ftsRepository;

    public PackCommand() {
        this(new DbConnectionFactory(), new ProjectService(), new MemoryRepository(), new FtsRepository());
    }

    PackCommand(DbConnectionFactory connectionFactory, ProjectService projectService,
                MemoryRepository memoryRepository, FtsRepository ftsRepository) {
        this.connectionFactory = connectionFactory;
        this.projectService = projectService;
        this.memoryRepository = memoryRepository;
        this.ftsRepository = ftsRepository;
    }

    public int run(CommandContext context, Args args) {
        String action = args.positional(2);
        if ("create".equals(action)) {
            return create(context, args);
        }
        if ("inspect".equals(action)) {
            return inspect(context, args);
        }
        if ("import".equals(action)) {
            return importPack(context, args);
        }
        context.err().println("Unknown memory pack command: " + action);
        context.err().println("Run `dhk help` for usage.");
        return ExitCodes.USAGE_ERROR;
    }

    private int create(CommandContext context, Args args) {
        if (!args.hasOption("out")) {
            return CommandErrorGuidance.missing(context, args, "MEMORY_PACK_OUT_MISSING",
                    new String[]{"--out"}, "dhk memory pack create --out <zip>", "README.md#core-path");
        }
        int limit = MemoryCommandSupport.parseLimit(context, args, args.option("limit", "100"),
                "dhk memory pack create --out <zip> --limit 100");
        if (limit <= 0) {
            return ExitCodes.VALIDATION_ERROR;
        }
        String module = args.option("module", "").trim();
        Path out = PathUtil.resolvePath(args.option("out"), context.workingDirectory());
        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        try (Connection connection = connectionFactory.open(projectRoot)) {
            Project project = MemoryCommandSupport.requireInitializedProject(context, projectRoot, projectService, connection);
            if (project == null) {
                return ExitCodes.NOT_FOUND;
            }
            List<MemoryItem> items = memoryRepository.listPackableMemory(connection, project.projectKey(), module, limit);
            byte[] itemsBytes = renderItems(items).getBytes("UTF-8");
            String checksum = sha256(itemsBytes);
            Map<String, String> manifest = new LinkedHashMap<String, String>();
            manifest.put("schema_version", SCHEMA_VERSION);
            manifest.put("generated_at", context.clock().now().toString());
            manifest.put("project_key", project.projectKey());
            manifest.put("module", module);
            manifest.put("item_count", Integer.toString(items.size()));
            manifest.put("items_sha256", checksum);
            if (out.getParent() != null) {
                Files.createDirectories(out.getParent());
            }
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(out))) {
                writeEntry(zip, MANIFEST, JsonUtil.toObject(manifest).getBytes("UTF-8"));
                writeEntry(zip, ITEMS, itemsBytes);
            }
            if (JsonOutput.enabled(args)) {
                context.out().print(JsonOutput.object(
                        JsonOutput.stringField("command", "memory pack create"),
                        JsonOutput.stringField("pack_path", PathUtil.displayPath(out)),
                        JsonOutput.numberField("item_count", items.size()),
                        JsonOutput.stringField("items_sha256", checksum)
                ));
            } else {
                context.out().println("pack_path: " + PathUtil.displayPath(out));
                context.out().println("item_count: " + items.size());
                context.out().println("items_sha256: " + checksum);
            }
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR memory pack create failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private int inspect(CommandContext context, Args args) {
        PackData pack = readPack(context, args);
        if (pack == null) {
            return ExitCodes.VALIDATION_ERROR;
        }
        if (JsonOutput.enabled(args)) {
            context.out().print(JsonOutput.object(
                    JsonOutput.stringField("command", "memory pack inspect"),
                    JsonOutput.stringField("schema_version", pack.manifest.get("schema_version")),
                    JsonOutput.numberField("item_count", pack.items.size()),
                    JsonOutput.stringField("checksum_ok", Boolean.toString(pack.checksumOk))
            ));
        } else {
            context.out().println("memory pack inspect");
            context.out().println("schema_version: " + pack.manifest.get("schema_version"));
            context.out().println("item_count: " + pack.items.size());
            context.out().println("checksum_ok: " + pack.checksumOk);
        }
        return pack.valid ? ExitCodes.SUCCESS : ExitCodes.VALIDATION_ERROR;
    }

    private int importPack(CommandContext context, Args args) {
        if (!args.hasFlag("as-draft")) {
            context.err().println("memory pack import requires --as-draft; packs cannot auto-confirm memory");
            return ExitCodes.VALIDATION_ERROR;
        }
        PackData pack = readPack(context, args);
        if (pack == null || !pack.valid) {
            return ExitCodes.VALIDATION_ERROR;
        }
        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        try (Connection connection = connectionFactory.open(projectRoot)) {
            Project project = MemoryCommandSupport.requireInitializedProject(context, projectRoot, projectService, connection);
            if (project == null) {
                return ExitCodes.NOT_FOUND;
            }
            for (Map<String, String> item : pack.items) {
                if (!validPackItem(context, item)) {
                    return ExitCodes.VALIDATION_ERROR;
                }
                String sensitiveInput = item.get("title") + "\n" + item.get("content") + "\n"
                        + item.get("tags") + "\n" + item.get("source_ref") + "\n" + item.get("evidence");
                if (MemoryCommandSupport.rejectSensitive(context, sensitiveInput)) {
                    return ExitCodes.VALIDATION_ERROR;
                }
            }
            int imported = 0;
            int skipped = 0;
            String now = context.clock().now().toString();
            for (Map<String, String> item : pack.items) {
                String fingerprint = value(item, "fingerprint");
                if (fingerprint.length() == 0) {
                    fingerprint = MemoryIdentity.fingerprint(value(item, "title"), value(item, "content"));
                }
                if (memoryRepository.findByFingerprint(connection, project.projectKey(), fingerprint) != null) {
                    skipped++;
                    continue;
                }
                if (memoryRepository.findByTitleAndContent(connection, project.projectKey(),
                        value(item, "title"), value(item, "content")) != null) {
                    skipped++;
                    continue;
                }
                String module = value(item, "module");
                if (module.length() == 0) {
                    module = "global";
                }
                MemoryItem importedItem = new MemoryItem(0L, project.projectKey(), module,
                        value(item, "type"), "project", value(item, "title"), value(item, "content"),
                        value(item, "tags"), "draft", parseInt(value(item, "confidence"), 50),
                        "pack_import", "", "", value(item, "source_ref"), value(item, "evidence"),
                        "", "", now, now, "", 0, fingerprint,
                        MemoryIdentity.canonicalKey(module, value(item, "title")), 0L, "", "",
                        value(item, "source_ref"));
                long id = memoryRepository.insert(connection, importedItem);
                ftsRepository.sync(connection, memoryRepository.findById(connection, project.projectKey(), id));
                imported++;
            }
            if (JsonOutput.enabled(args)) {
                context.out().print(JsonOutput.object(
                        JsonOutput.stringField("command", "memory pack import"),
                        JsonOutput.numberField("imported", imported),
                        JsonOutput.numberField("skipped_duplicates", skipped),
                        JsonOutput.stringField("memory_status", "draft")
                ));
            } else {
                context.out().println("imported: " + imported);
                context.out().println("skipped_duplicates: " + skipped);
                context.out().println("memory_status: draft");
            }
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR memory pack import failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private PackData readPack(CommandContext context, Args args) {
        if (!args.hasOption("path")) {
            CommandErrorGuidance.missing(context, args, "MEMORY_PACK_PATH_MISSING",
                    new String[]{"--path"}, "dhk memory pack inspect --path <zip>", "README.md#core-path");
            return null;
        }
        Path path = PathUtil.resolvePath(args.option("path"), context.workingDirectory());
        try {
            Map<String, byte[]> entries = readZip(path);
            byte[] manifestBytes = entries.get(MANIFEST);
            byte[] itemsBytes = entries.get(ITEMS);
            if (manifestBytes == null || itemsBytes == null) {
                context.err().println("memory pack is missing manifest.json or items.jsonl");
                return new PackData(false, false, new LinkedHashMap<String, String>(), new ArrayList<Map<String, String>>());
            }
            Map<String, String> manifest = JsonUtil.parseObject(new String(manifestBytes, "UTF-8"));
            List<Map<String, String>> items = parseItems(new String(itemsBytes, "UTF-8"));
            String expected = manifest.get("items_sha256");
            boolean schemaOk = SCHEMA_VERSION.equals(manifest.get("schema_version"));
            boolean checksumOk = expected != null && expected.equals(sha256(itemsBytes));
            if (!schemaOk) {
                context.err().println("memory pack schema is not supported: " + manifest.get("schema_version"));
            }
            if (!checksumOk) {
                context.err().println("memory pack checksum mismatch");
            }
            return new PackData(schemaOk && checksumOk, checksumOk, manifest, items);
        } catch (Exception ex) {
            context.err().println("ERROR memory pack read failed: " + ex.getMessage());
            return null;
        }
    }

    private Map<String, byte[]> readZip(Path path) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<String, byte[]>();
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(path))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (unsafeEntryName(name)) {
                    throw new IOException("unsafe zip entry: " + name);
                }
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] chunk = new byte[4096];
                int read;
                while ((read = zip.read(chunk)) >= 0) {
                    buffer.write(chunk, 0, read);
                }
                entries.put(name, buffer.toByteArray());
            }
        }
        return entries;
    }

    private boolean unsafeEntryName(String name) {
        return name == null || name.length() == 0 || name.startsWith("/")
                || name.startsWith("\\") || name.indexOf("..") >= 0
                || name.indexOf('\\') >= 0;
    }

    private void writeEntry(ZipOutputStream zip, String name, byte[] content) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        zip.putNextEntry(entry);
        zip.write(content);
        zip.closeEntry();
    }

    private String renderItems(List<MemoryItem> items) {
        StringBuilder builder = new StringBuilder();
        for (MemoryItem item : items) {
            Map<String, String> fields = new LinkedHashMap<String, String>();
            fields.put("title", item.title());
            fields.put("content", item.content());
            fields.put("tags", item.tags());
            fields.put("module", item.moduleName());
            fields.put("type", item.memoryType());
            fields.put("confidence", Integer.toString(item.confidence()));
            fields.put("source_ref", item.sourceRef().length() == 0 ? item.sourceFiles() : item.sourceRef());
            fields.put("evidence", item.evidence());
            fields.put("fingerprint", item.fingerprint().length() == 0
                    ? MemoryIdentity.fingerprint(item.title(), item.content()) : item.fingerprint());
            builder.append(JsonUtil.toObject(fields).replace('\n', ' ').trim()).append('\n');
        }
        return builder.toString();
    }

    private List<Map<String, String>> parseItems(String text) {
        List<Map<String, String>> items = new ArrayList<Map<String, String>>();
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.length() == 0) {
                continue;
            }
            items.add(JsonUtil.parseObject(trimmed));
        }
        return items;
    }

    private String value(Map<String, String> map, String key) {
        String value = map.get(key);
        return value == null ? "" : value;
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private boolean validPackItem(CommandContext context, Map<String, String> item) {
        if (value(item, "title").length() == 0 || value(item, "content").length() == 0) {
            context.err().println("memory pack item requires non-empty title and content");
            return false;
        }
        if (!MemoryType.isAllowed(value(item, "type"))) {
            context.err().println("memory pack item has invalid memory type: " + value(item, "type"));
            return false;
        }
        int confidence = parseInt(value(item, "confidence"), -1);
        if (confidence < 0 || confidence > 100) {
            context.err().println("memory pack item confidence must be an integer from 0 to 100");
            return false;
        }
        return true;
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(hash[i] & 0xff);
                if (hex.length() == 1) {
                    builder.append('0');
                }
                builder.append(hex);
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to calculate checksum", ex);
        }
    }

    private static final class PackData {
        private final boolean valid;
        private final boolean checksumOk;
        private final Map<String, String> manifest;
        private final List<Map<String, String>> items;

        private PackData(boolean valid, boolean checksumOk, Map<String, String> manifest,
                         List<Map<String, String>> items) {
            this.valid = valid;
            this.checksumOk = checksumOk;
            this.manifest = manifest;
            this.items = items;
        }
    }
}
