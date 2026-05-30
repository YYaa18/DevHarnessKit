package com.devharnesskit.dhk.service.skill;

import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.model.skill.SkillContract;
import com.devharnesskit.dhk.repository.skill.SkillContractRepository;
import com.devharnesskit.dhk.util.Clock;
import com.devharnesskit.dhk.util.PathUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SkillContractServiceTest {
    @TempDir
    Path tempDir;

    private final SkillContractService service = new SkillContractService(
            new SkillContractRepository(), new FixedClock());

    @Test
    void loadsContractJsonWithCommandArrays() throws Exception {
        Path skillDir = tempDir.resolve(".agents/skills/devharness-strict");
        copyFixture("valid-contract.json", skillDir);

        SkillContract contract = service.load(skillDir);

        assertEquals("skill-contract/v1", contract.schemaVersion());
        assertEquals("devharness-strict", contract.skillKey());
        assertEquals("0.7.1", contract.version());
        assertEquals("coding", contract.taskType());
        assertEquals("context", contract.dataAccessLevel());
        assertEquals(3, contract.allowedCommands().size());
        assertTrue(contract.allowedCommands().contains("dhk goal next"));
        assertTrue(contract.forbiddenCommands().contains("dhk db sql"));
        assertEquals("2026-01-01T00:00:00Z", contract.createdAt());
        assertTrue(contract.sourcePath().endsWith(".agents/skills/devharness-strict/contract.json"));
    }

    @Test
    void importsContractIntoSkillContractTable() throws Exception {
        PathUtil.createMemoryDirectories(tempDir);
        Path skillDir = PathUtil.skillDirectory(tempDir, "devharness-local");
        Files.createDirectories(skillDir);
        Files.write(PathUtil.skillContract(tempDir, "devharness-local"),
                ("{\n"
                        + "  \"schema_version\": \"skill-contract/v1\",\n"
                        + "  \"skill_key\": \"devharness-local\",\n"
                        + "  \"version\": \"0.7.1\",\n"
                        + "  \"task_type\": \"coding\",\n"
                        + "  \"data_access_level\": \"raw\",\n"
                        + "  \"allowed_commands\": \"dhk goal next, dhk goal verify\",\n"
                        + "  \"forbidden_commands\": \"dhk db sql\"\n"
                        + "}\n").getBytes("UTF-8"));

        try (Connection connection = new DbConnectionFactory().open(tempDir)) {
            new MigrationRunner().migrate(connection, new FixedClock());
            SkillContract imported = service.importContract(connection, skillDir);
            SkillContract stored = new SkillContractRepository().findByKey(connection, imported.skillKey());

            assertEquals("devharness-local", stored.skillKey());
            assertEquals("raw", stored.dataAccessLevel());
            assertEquals(2, stored.allowedCommands().size());
            assertEquals("dhk db sql", stored.forbiddenCommands().get(0));
        }
    }

    @Test
    void rejectsMissingRequiredFieldsAndInvalidAccessLevel() throws Exception {
        Path skillDir = tempDir.resolve("bad-skill");
        copyFixture("invalid-missing-fields.json", skillDir);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                new org.junit.jupiter.api.function.Executable() {
                    public void execute() {
                        service.load(skillDir);
                    }
                });
        assertTrue(ex.getMessage().contains("Missing required skill contract field: task_type"));
    }

    @Test
    void rejectsInvalidAccessLevelFromContractJson() throws Exception {
        Path skillDir = tempDir.resolve("bad-access");
        Files.createDirectories(skillDir);
        Files.write(skillDir.resolve(PathUtil.CONTRACT_JSON),
                ("{\n"
                        + "  \"schema_version\": \"skill-contract/v1\",\n"
                        + "  \"skill_key\": \"bad-access\",\n"
                        + "  \"version\": \"0.1.0\",\n"
                        + "  \"task_type\": \"coding\",\n"
                        + "  \"data_access_level\": \"everything\"\n"
                        + "}\n").getBytes("UTF-8"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                new org.junit.jupiter.api.function.Executable() {
                    public void execute() {
                        service.load(skillDir);
                    }
                });
        assertTrue(ex.getMessage().contains("Invalid data_access_level"));
    }

    @Test
    void acceptsLegacyAlphaSchemaVersionDuringTransition() throws Exception {
        Path skillDir = tempDir.resolve("legacy-schema");
        Files.createDirectories(skillDir);
        Files.write(skillDir.resolve(PathUtil.CONTRACT_JSON),
                ("{\n"
                        + "  \"schema_version\": \"skill-contract/v1-alpha\",\n"
                        + "  \"skill_key\": \"legacy-schema\",\n"
                        + "  \"version\": \"0.7.1\",\n"
                        + "  \"task_type\": \"coding\",\n"
                        + "  \"data_access_level\": \"context\",\n"
                        + "  \"allowed_commands\": [\"dhk goal next\"],\n"
                        + "  \"forbidden_commands\": [\"dhk db sql\"]\n"
                        + "}\n").getBytes("UTF-8"));

        SkillContract contract = service.load(skillDir);

        assertEquals("skill-contract/v1", contract.schemaVersion());
        assertEquals("legacy-schema", contract.skillKey());
    }

    private void copyFixture(String fixtureName, Path skillDir) throws Exception {
        Files.createDirectories(skillDir);
        InputStream stream = getClass().getClassLoader()
                .getResourceAsStream("fixtures/skill-contract/" + fixtureName);
        if (stream == null) {
            throw new IllegalArgumentException("Missing fixture: " + fixtureName);
        }
        try {
            Files.copy(stream, skillDir.resolve(PathUtil.CONTRACT_JSON), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            stream.close();
        }
    }

    private static final class FixedClock implements Clock {
        public Instant now() {
            return Instant.parse("2026-01-01T00:00:00Z");
        }
    }
}
