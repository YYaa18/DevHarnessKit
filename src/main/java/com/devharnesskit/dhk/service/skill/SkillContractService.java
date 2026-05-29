package com.devharnesskit.dhk.service.skill;

import com.devharnesskit.dhk.model.skill.SkillContract;
import com.devharnesskit.dhk.repository.skill.SkillContractRepository;
import com.devharnesskit.dhk.util.Clock;
import com.devharnesskit.dhk.util.PathUtil;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SkillContractService {
    private static final String[] REQUIRED_FIELDS = new String[]{
            "skill_key", "version", "task_type", "data_access_level"
    };

    private final SkillContractRepository repository;
    private final Clock clock;

    public SkillContractService() {
        this(new SkillContractRepository(), new Clock() {
            public Instant now() {
                return Instant.now();
            }
        });
    }

    public SkillContractService(SkillContractRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public SkillContract load(Path skillDirectory) {
        Path contractPath = skillDirectory.resolve(PathUtil.CONTRACT_JSON);
        if (!Files.isRegularFile(contractPath)) {
            throw new IllegalArgumentException("Missing skill contract: " + contractPath);
        }
        try {
            String json = new String(Files.readAllBytes(contractPath), "UTF-8");
            return parse(json, contractPath, sourceHash(skillDirectory));
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to read skill contract: " + ex.getMessage(), ex);
        }
    }

    public SkillContract importContract(Connection connection, Path skillDirectory) throws SQLException {
        SkillContract contract = load(skillDirectory);
        SkillContract existing = repository.findByKey(connection, contract.skillKey());
        SkillContract resolved = resolveTrustState(contract, existing);
        repository.upsert(connection, resolved);
        return resolved;
    }

    public SkillContract trustContract(Connection connection, Path skillDirectory) throws SQLException {
        SkillContract contract = load(skillDirectory);
        SkillContract trusted = withTrustState(contract, contract.sourceHash(), "trusted", true);
        repository.upsert(connection, trusted);
        return trusted;
    }

    public SkillContract parse(String json, Path sourcePath) {
        return parse(json, sourcePath, textHash(json));
    }

    public SkillContract parse(String json, Path sourcePath, String sourceHash) {
        for (String field : REQUIRED_FIELDS) {
            require(field, stringValue(json, field));
        }
        String dataAccessLevel = stringValue(json, "data_access_level");
        if (!isAllowedDataAccessLevel(dataAccessLevel)) {
            throw new IllegalArgumentException("Invalid data_access_level: " + dataAccessLevel);
        }
        String now = clock.now().toString();
        return new SkillContract(
                stringValue(json, "skill_key"),
                stringValue(json, "version"),
                stringValue(json, "task_type"),
                value(stringValue(json, "risk_level"), "medium"),
                value(stringValue(json, "mode"), "strict"),
                dataAccessLevel,
                listValue(json, "allowed_commands"),
                listValue(json, "forbidden_commands"),
                json == null ? "" : json,
                PathUtil.displayPath(sourcePath),
                sourceHash,
                "",
                "unknown",
                false,
                now,
                now);
    }

    public String sourceHash(Path skillDirectory) {
        if (skillDirectory == null || !Files.isDirectory(skillDirectory)) {
            return "";
        }
        try {
            final List<Path> files = new ArrayList<Path>();
            Files.walkFileTree(skillDirectory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (attrs.isRegularFile()) {
                        files.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            Collections.sort(files, new Comparator<Path>() {
                public int compare(Path left, Path right) {
                    return relative(skillDirectory, left).compareTo(relative(skillDirectory, right));
                }
            });
            MessageDigest digest = sha256();
            for (Path file : files) {
                digest.update(relative(skillDirectory, file).getBytes("UTF-8"));
                digest.update((byte) 0);
                digest.update(Files.readAllBytes(file));
                digest.update((byte) 0);
            }
            return "sha256:" + hex(digest.digest());
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to hash skill source: " + ex.getMessage(), ex);
        }
    }

    private SkillContract resolveTrustState(SkillContract contract, SkillContract existing) {
        if (existing == null || existing.trustedSourceHash().length() == 0) {
            return withTrustState(contract, "", "unknown", false);
        }
        if (existing.trustedSourceHash().equals(contract.sourceHash())) {
            return withTrustState(contract, existing.trustedSourceHash(), "trusted", true);
        }
        return withTrustState(contract, existing.trustedSourceHash(), "review_required", false);
    }

    private SkillContract withTrustState(SkillContract contract, String trustedSourceHash,
                                         String trustStatus, boolean trusted) {
        return new SkillContract(
                contract.skillKey(),
                contract.version(),
                contract.taskType(),
                contract.riskLevel(),
                contract.mode(),
                contract.dataAccessLevel(),
                contract.allowedCommands(),
                contract.forbiddenCommands(),
                contract.contractJson(),
                contract.sourcePath(),
                contract.sourceHash(),
                trustedSourceHash,
                trustStatus,
                trusted,
                contract.createdAt(),
                clock.now().toString());
    }

    private String textHash(String value) {
        try {
            MessageDigest digest = sha256();
            digest.update((value == null ? "" : value).getBytes("UTF-8"));
            return "sha256:" + hex(digest.digest());
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to hash skill contract: " + ex.getMessage(), ex);
        }
    }

    private MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private String relative(Path root, Path file) {
        return root.relativize(file).toString().replace('\\', '/');
    }

    private String hex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            String part = Integer.toHexString(value & 0xff);
            if (part.length() == 1) {
                builder.append('0');
            }
            builder.append(part);
        }
        return builder.toString();
    }

    private void require(String field, String value) {
        if (value == null || value.trim().length() == 0) {
            throw new IllegalArgumentException("Missing required skill contract field: " + field);
        }
    }

    private boolean isAllowedDataAccessLevel(String value) {
        return "none".equals(value)
                || "metadata".equals(value)
                || "context".equals(value)
                || "raw".equals(value);
    }

    private String value(String raw, String defaultValue) {
        return raw == null || raw.length() == 0 ? defaultValue : raw;
    }

    private String stringValue(String json, String key) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"",
                Pattern.DOTALL).matcher(json == null ? "" : json);
        if (!matcher.find()) {
            return "";
        }
        return unescape(matcher.group(1)).trim();
    }

    private List<String> listValue(String json, String key) {
        String scalar = stringValue(json, key);
        if (scalar.length() > 0) {
            return splitList(scalar);
        }
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\\[(.*?)\\]",
                Pattern.DOTALL).matcher(json == null ? "" : json);
        List<String> result = new ArrayList<String>();
        if (!matcher.find()) {
            return result;
        }
        Matcher itemMatcher = Pattern.compile("\"((?:\\\\.|[^\"])*)\"").matcher(matcher.group(1));
        while (itemMatcher.find()) {
            String item = unescape(itemMatcher.group(1)).trim();
            if (item.length() > 0) {
                result.add(item);
            }
        }
        return result;
    }

    private List<String> splitList(String value) {
        List<String> result = new ArrayList<String>();
        String[] parts = value.split("[,\\n\\r]+");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.length() > 0) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private String unescape(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch != '\\' || i + 1 >= value.length()) {
                builder.append(ch);
                continue;
            }
            char escaped = value.charAt(++i);
            if (escaped == 'n') {
                builder.append('\n');
            } else if (escaped == 'r') {
                builder.append('\r');
            } else if (escaped == 't') {
                builder.append('\t');
            } else {
                builder.append(escaped);
            }
        }
        return builder.toString();
    }
}
