package com.devharnesskit.dhk.integration;

import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.CommandRouter;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.util.Clock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MemoryQualityIntegrationTest {
    @TempDir
    Path tempDir;

    @Test
    void candidatesAcceptToDraftRejectAndBlockSensitiveContent() throws Exception {
        initProject("demo");

        Harness suggest = new Harness(tempDir);
        int suggestExit = new CommandRouter().run(new String[]{
                "memory", "suggest",
                "--project-root", "demo",
                "--title", "Reusable address validation rule",
                "--content", "Address validation requires province, city, district and detail.",
                "--tags", "address,validation",
                "--reason", "Reusable business rule",
                "--json"
        }, suggest.context());
        assertEquals(ExitCodes.SUCCESS, suggestExit);
        assertTrue(suggest.stdout().contains("\"candidate_id\": 1"));
        assertTrue(suggest.stdout().contains("\"status\": \"pending\""));

        Harness list = new Harness(tempDir);
        int listExit = new CommandRouter().run(new String[]{
                "memory", "candidates", "list", "--project-root", "demo", "--json"
        }, list.context());
        assertEquals(ExitCodes.SUCCESS, listExit);
        assertTrue(list.stdout().contains("\"command\": \"memory candidates list\""));
        assertTrue(list.stdout().contains("\"count\": 1"));

        Harness accept = new Harness(tempDir);
        int acceptExit = new CommandRouter().run(new String[]{
                "memory", "candidates", "accept", "--project-root", "demo", "--id", "1", "--json"
        }, accept.context());
        assertEquals(ExitCodes.SUCCESS, acceptExit);
        assertTrue(accept.stdout().contains("\"memory_id\": 1"));
        assertTrue(accept.stdout().contains("\"memory_status\": \"draft\""));
        assertEquals(1, countRows(tempDir.resolve("demo"), "memory_item"));

        Harness acceptAgain = new Harness(tempDir);
        int acceptAgainExit = new CommandRouter().run(new String[]{
                "memory", "candidates", "accept", "--project-root", "demo", "--id", "1"
        }, acceptAgain.context());
        assertEquals(ExitCodes.SUCCESS, acceptAgainExit);
        assertTrue(acceptAgain.stdout().contains("memory_id: 1"));
        assertEquals(1, countRows(tempDir.resolve("demo"), "memory_item"));

        Harness search = new Harness(tempDir);
        int searchExit = new CommandRouter().run(new String[]{
                "memory", "search", "--project-root", "demo", "--q", "province"
        }, search.context());
        assertEquals(ExitCodes.SUCCESS, searchExit);
        assertTrue(search.stdout().contains("status: draft"));

        Harness sensitive = new Harness(tempDir);
        int sensitiveExit = new CommandRouter().run(new String[]{
                "memory", "suggest",
                "--project-root", "demo",
                "--title", "Secret candidate",
                "--content", "password=abc"
        }, sensitive.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, sensitiveExit);
        assertTrue(sensitive.stderr().contains("Sensitive data rejected"));

        Harness second = new Harness(tempDir);
        int secondExit = new CommandRouter().run(new String[]{
                "memory", "suggest",
                "--project-root", "demo",
                "--title", "Rejected candidate",
                "--content", "This note is too temporary."
        }, second.context());
        assertEquals(ExitCodes.SUCCESS, secondExit);

        Harness reject = new Harness(tempDir);
        int rejectExit = new CommandRouter().run(new String[]{
                "memory", "candidates", "reject", "--project-root", "demo",
                "--id", "2", "--reason", "not reusable"
        }, reject.context());
        assertEquals(ExitCodes.SUCCESS, rejectExit);
        assertTrue(reject.stdout().contains("status: rejected"));
    }

    @Test
    void dedupeConflictsStaleRefreshSupersedeAndRankingExplain() throws Exception {
        initProject("demo");
        addMemory("demo", "Installer rule", "Installer uses a single integrated skill.", "installer");
        addMemory("demo", "Installer rule", "Installer uses a single integrated skill.", "installer");
        addMemory("demo", "Graph Lite status", "Graph Lite is alpha for correctness.", "graph");
        addMemory("demo", "Graph Lite status", "Graph Lite is stable for correctness.", "graph");
        confirm("demo", "1");
        confirm("demo", "2");

        Harness dedupe = new Harness(tempDir);
        int dedupeExit = new CommandRouter().run(new String[]{
                "memory", "dedupe", "--project-root", "demo", "--json"
        }, dedupe.context());
        assertEquals(ExitCodes.SUCCESS, dedupeExit);
        assertTrue(dedupe.stdout().contains("\"group_count\": 1"));
        assertTrue(dedupe.stdout().contains("\"ids\": \"1,2\"")
                || dedupe.stdout().contains("\"ids\": \"2,1\""));

        Harness conflicts = new Harness(tempDir);
        int conflictsExit = new CommandRouter().run(new String[]{
                "memory", "conflicts", "--project-root", "demo"
        }, conflicts.context());
        assertEquals(ExitCodes.SUCCESS, conflictsExit);
        assertTrue(conflicts.stdout().contains("memory conflicts"));
        assertTrue(conflicts.stdout().contains("Graph Lite status"));
        assertTrue(conflicts.stdout().contains("reason: semantic_opposition"));

        Harness expire = new Harness(tempDir);
        int expireExit = new CommandRouter().run(new String[]{
                "memory", "expire", "--project-root", "demo", "--id", "1", "--reason", "replaced by newer evidence"
        }, expire.context());
        assertEquals(ExitCodes.SUCCESS, expireExit);

        Harness stale = new Harness(tempDir);
        int staleExit = new CommandRouter().run(new String[]{
                "memory", "stale", "scan", "--project-root", "demo"
        }, stale.context());
        assertEquals(ExitCodes.SUCCESS, staleExit);
        assertTrue(stale.stdout().contains("memory_id: 1"));
        assertTrue(stale.stdout().contains("replaced by newer evidence"));

        Harness refresh = new Harness(tempDir);
        int refreshExit = new CommandRouter().run(new String[]{
                "memory", "refresh", "--project-root", "demo", "--id", "1", "--evidence", "verified in release notes"
        }, refresh.context());
        assertEquals(ExitCodes.SUCCESS, refreshExit);
        assertTrue(refresh.stdout().contains("stale: false"));
        assertTrue(refresh.stdout().contains("status: confirmed"));

        Harness search = new Harness(tempDir);
        int searchExit = new CommandRouter().run(new String[]{
                "memory", "search", "--project-root", "demo", "--q", "Installer", "--explain"
        }, search.context());
        assertEquals(ExitCodes.SUCCESS, searchExit);
        assertTrue(search.stdout().contains("verified:+1"));

        Harness supersede = new Harness(tempDir);
        int supersedeExit = new CommandRouter().run(new String[]{
                "memory", "supersede", "--project-root", "demo", "--old", "1", "--new", "2",
                "--reason", "duplicate replaced by newer id"
        }, supersede.context());
        assertEquals(ExitCodes.SUCCESS, supersedeExit);
        assertTrue(supersede.stdout().contains("status: deprecated"));

        Harness ranked = new Harness(tempDir);
        int rankedExit = new CommandRouter().run(new String[]{
                "memory", "search", "--project-root", "demo", "--q", "Installer", "--explain"
        }, ranked.context());
        assertEquals(ExitCodes.SUCCESS, rankedExit);
        assertTrue(ranked.stdout().indexOf("[2] Installer rule")
                < ranked.stdout().indexOf("[1] Installer rule"));
        assertTrue(ranked.stdout().contains("superseded:-12"));
    }

    @Test
    void refreshRestoresExpiredDraftAndConflictsReportFactVariants() throws Exception {
        initProject("demo");
        addMemory("demo", "Token expiry", "Token expires after 15 minutes.", "auth");
        addMemory("demo", "Token expiry", "Token expires after 60 minutes.", "auth");

        Harness conflicts = new Harness(tempDir);
        int conflictsExit = new CommandRouter().run(new String[]{
                "memory", "conflicts", "--project-root", "demo"
        }, conflicts.context());
        assertEquals(ExitCodes.SUCCESS, conflictsExit);
        assertTrue(conflicts.stdout().contains("Token expiry"));
        assertTrue(conflicts.stdout().contains("severity: low"));
        assertTrue(conflicts.stdout().contains("reason: fact_variant"));

        Harness expire = new Harness(tempDir);
        int expireExit = new CommandRouter().run(new String[]{
                "memory", "expire", "--project-root", "demo", "--id", "1", "--reason", "needs new evidence"
        }, expire.context());
        assertEquals(ExitCodes.SUCCESS, expireExit);

        Harness refresh = new Harness(tempDir);
        int refreshExit = new CommandRouter().run(new String[]{
                "memory", "refresh", "--project-root", "demo", "--id", "1", "--evidence", "rechecked by user"
        }, refresh.context());
        assertEquals(ExitCodes.SUCCESS, refreshExit);
        assertTrue(refresh.stdout().contains("status: draft"));
        assertTrue(refresh.stdout().contains("stale: false"));
    }

    @Test
    void staleScanReportsLifecycleReasonsAsJsonAndRejectsInvalidInput() throws Exception {
        initProject("demo");
        addMemory("demo", "Elapsed memory", "Old fact that should expire by date.", "lifecycle");
        addMemory("demo", "Invalid date memory", "Bad effective_to should be visible.", "lifecycle");
        addMemory("demo", "Deprecated memory", "Deprecated without an explicit stale reason.", "lifecycle");
        addMemory("demo", "Superseded memory", "Old version of a fact.", "lifecycle");
        addMemory("demo", "Replacement memory", "New version of a fact.", "lifecycle");
        updateMemory(tempDir.resolve("demo"), "effective_to = '2026-05-20T00:00:00Z'", 1);
        updateMemory(tempDir.resolve("demo"), "effective_to = 'not-a-date'", 2);
        updateMemory(tempDir.resolve("demo"), "status = 'deprecated'", 3);

        Harness supersede = new Harness(tempDir);
        int supersedeExit = new CommandRouter().run(new String[]{
                "memory", "supersede", "--project-root", "demo", "--old", "4", "--new", "5",
                "--reason", "replaced by a newer lifecycle fact"
        }, supersede.context());
        assertEquals(ExitCodes.SUCCESS, supersedeExit);

        Harness stale = new Harness(tempDir);
        int staleExit = new CommandRouter().run(new String[]{
                "memory", "stale", "scan", "--project-root", "demo", "--json"
        }, stale.context());
        assertEquals(ExitCodes.SUCCESS, staleExit);
        assertTrue(stale.stdout().contains("\"command\": \"memory stale scan\""));
        assertTrue(stale.stdout().contains("\"stale_count\": 4"));
        assertTrue(stale.stdout().contains("\"reason\": \"effective_to_elapsed\""));
        assertTrue(stale.stdout().contains("\"reason\": \"invalid_effective_to\""));
        assertTrue(stale.stdout().contains("\"reason\": \"deprecated\""));
        assertTrue(stale.stdout().contains("\"reason\": \"superseded_by=5\""));

        Harness badAction = new Harness(tempDir);
        int badActionExit = new CommandRouter().run(new String[]{
                "memory", "stale", "unexpected", "--project-root", "demo"
        }, badAction.context());
        assertEquals(ExitCodes.USAGE_ERROR, badActionExit);
        assertTrue(badAction.stderr().contains("Unknown memory stale command"));

        Harness badLimit = new Harness(tempDir);
        int badLimitExit = new CommandRouter().run(new String[]{
                "memory", "stale", "scan", "--project-root", "demo", "--limit", "0"
        }, badLimit.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, badLimitExit);
    }

    @Test
    void expireRejectsMissingInvalidUnknownAndSensitiveRequests() throws Exception {
        initProject("demo");
        addMemory("demo", "Expirable memory", "This memory can be expired.", "lifecycle");

        Harness missingReason = new Harness(tempDir);
        int missingReasonExit = new CommandRouter().run(new String[]{
                "memory", "expire", "--project-root", "demo", "--id", "1"
        }, missingReason.context());
        assertEquals(ExitCodes.USAGE_ERROR, missingReasonExit);
        assertTrue(missingReason.stderr().contains("MEMORY_EXPIRE_REASON_MISSING"));

        Harness invalidId = new Harness(tempDir);
        int invalidIdExit = new CommandRouter().run(new String[]{
                "memory", "expire", "--project-root", "demo", "--id", "abc", "--reason", "cleanup"
        }, invalidId.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, invalidIdExit);
        assertTrue(invalidId.stderr().contains("MEMORY_EXPIRE_INVALID_ID"));

        Harness unknownId = new Harness(tempDir);
        int unknownIdExit = new CommandRouter().run(new String[]{
                "memory", "expire", "--project-root", "demo", "--id", "99", "--reason", "cleanup"
        }, unknownId.context());
        assertEquals(ExitCodes.NOT_FOUND, unknownIdExit);
        assertTrue(unknownId.stderr().contains("MEMORY_ITEM_NOT_FOUND"));

        Harness sensitive = new Harness(tempDir);
        int sensitiveExit = new CommandRouter().run(new String[]{
                "memory", "expire", "--project-root", "demo", "--id", "1", "--reason", "password=abc"
        }, sensitive.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, sensitiveExit);
        assertTrue(sensitive.stderr().contains("Sensitive data rejected"));

        Harness json = new Harness(tempDir);
        int jsonExit = new CommandRouter().run(new String[]{
                "memory", "expire", "--project-root", "demo", "--id", "1", "--reason", "obsolete", "--json"
        }, json.context());
        assertEquals(ExitCodes.SUCCESS, jsonExit);
        assertTrue(json.stdout().contains("\"command\": \"memory expire\""));
        assertTrue(json.stdout().contains("\"status\": \"deprecated\""));
    }

    @Test
    void refreshRejectsUnsafeRequestsAndDoesNotReviveSupersededMemory() throws Exception {
        initProject("demo");
        addMemory("demo", "Archived memory", "Archived facts cannot be refreshed.", "lifecycle");
        addMemory("demo", "Superseded memory", "Old behavior.", "lifecycle");
        addMemory("demo", "Replacement memory", "New behavior.", "lifecycle");
        updateMemory(tempDir.resolve("demo"), "status = 'archived'", 1);

        Harness missingEvidence = new Harness(tempDir);
        int missingEvidenceExit = new CommandRouter().run(new String[]{
                "memory", "refresh", "--project-root", "demo", "--id", "1"
        }, missingEvidence.context());
        assertEquals(ExitCodes.USAGE_ERROR, missingEvidenceExit);
        assertTrue(missingEvidence.stderr().contains("MEMORY_REFRESH_EVIDENCE_MISSING"));

        Harness invalidId = new Harness(tempDir);
        int invalidIdExit = new CommandRouter().run(new String[]{
                "memory", "refresh", "--project-root", "demo", "--id", "abc", "--evidence", "checked"
        }, invalidId.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, invalidIdExit);
        assertTrue(invalidId.stderr().contains("MEMORY_REFRESH_INVALID_ID"));

        Harness sensitive = new Harness(tempDir);
        int sensitiveExit = new CommandRouter().run(new String[]{
                "memory", "refresh", "--project-root", "demo", "--id", "1", "--evidence", "token=abc123"
        }, sensitive.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, sensitiveExit);
        assertTrue(sensitive.stderr().contains("Sensitive data rejected"));

        Harness archived = new Harness(tempDir);
        int archivedExit = new CommandRouter().run(new String[]{
                "memory", "refresh", "--project-root", "demo", "--id", "1", "--evidence", "manual check"
        }, archived.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, archivedExit);
        assertTrue(archived.stderr().contains("archived memory cannot be refreshed"));

        Harness supersede = new Harness(tempDir);
        int supersedeExit = new CommandRouter().run(new String[]{
                "memory", "supersede", "--project-root", "demo", "--old", "2", "--new", "3",
                "--reason", "replacement verified"
        }, supersede.context());
        assertEquals(ExitCodes.SUCCESS, supersedeExit);

        Harness refresh = new Harness(tempDir);
        int refreshExit = new CommandRouter().run(new String[]{
                "memory", "refresh", "--project-root", "demo", "--id", "2", "--evidence", "old fact rechecked"
        }, refresh.context());
        assertEquals(ExitCodes.SUCCESS, refreshExit);
        assertTrue(refresh.stdout().contains("status: deprecated"));
        assertTrue(refresh.stdout().contains("stale: false"));
    }

    @Test
    void packCreateInspectImportAsDraftAndRejectUnsafeZipEntries() throws Exception {
        initProject("source");
        addMemory("source", "Confirmed team rule", "Team memory packs share confirmed facts.", "team,pack");
        confirm("source", "1");
        addMemory("source", "Draft local note", "Draft notes are not packed.", "draft");

        Path pack = tempDir.resolve("team-pack.zip");
        Harness create = new Harness(tempDir);
        int createExit = new CommandRouter().run(new String[]{
                "memory", "pack", "create", "--project-root", "source", "--out", pack.toString()
        }, create.context());
        assertEquals(ExitCodes.SUCCESS, createExit);
        assertTrue(Files.isRegularFile(pack));
        assertTrue(create.stdout().contains("item_count: 1"));

        Harness inspect = new Harness(tempDir);
        int inspectExit = new CommandRouter().run(new String[]{
                "memory", "pack", "inspect", "--path", pack.toString(), "--json"
        }, inspect.context());
        assertEquals(ExitCodes.SUCCESS, inspectExit);
        assertTrue(inspect.stdout().contains("\"item_count\": 1"));
        assertTrue(inspect.stdout().contains("\"checksum_ok\": \"true\""));

        initProject("imported");
        Harness importWithoutFlag = new Harness(tempDir);
        int importWithoutFlagExit = new CommandRouter().run(new String[]{
                "memory", "pack", "import", "--project-root", "imported", "--path", pack.toString()
        }, importWithoutFlag.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, importWithoutFlagExit);
        assertTrue(importWithoutFlag.stderr().contains("requires --as-draft"));

        Harness importPack = new Harness(tempDir);
        int importExit = new CommandRouter().run(new String[]{
                "memory", "pack", "import", "--project-root", "imported", "--path", pack.toString(), "--as-draft"
        }, importPack.context());
        assertEquals(ExitCodes.SUCCESS, importExit);
        assertTrue(importPack.stdout().contains("imported: 1"));
        assertTrue(importPack.stdout().contains("memory_status: draft"));

        Harness importAgain = new Harness(tempDir);
        int importAgainExit = new CommandRouter().run(new String[]{
                "memory", "pack", "import", "--project-root", "imported", "--path", pack.toString(), "--as-draft"
        }, importAgain.context());
        assertEquals(ExitCodes.SUCCESS, importAgainExit);
        assertTrue(importAgain.stdout().contains("imported: 0"));
        assertTrue(importAgain.stdout().contains("skipped_duplicates: 1"));

        Path unsafe = tempDir.resolve("unsafe-pack.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(unsafe))) {
            zip.putNextEntry(new ZipEntry("../manifest.json"));
            zip.write("{}".getBytes("UTF-8"));
            zip.closeEntry();
        }
        Harness unsafeInspect = new Harness(tempDir);
        int unsafeExit = new CommandRouter().run(new String[]{
                "memory", "pack", "inspect", "--path", unsafe.toString()
        }, unsafeInspect.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, unsafeExit);
        assertTrue(unsafeInspect.stderr().contains("unsafe zip entry"));
    }

    private void initProject(String root) {
        Harness init = new Harness(tempDir);
        int exitCode = new CommandRouter().run(new String[]{"memory", "init", "--project-root", root}, init.context());
        assertEquals(ExitCodes.SUCCESS, exitCode);
    }

    private void addMemory(String root, String title, String content, String tags) {
        Harness add = new Harness(tempDir);
        int exitCode = new CommandRouter().run(new String[]{
                "memory", "add",
                "--project-root", root,
                "--type", "project_fact",
                "--module", "global",
                "--title", title,
                "--content", content,
                "--tags", tags
        }, add.context());
        assertEquals(ExitCodes.SUCCESS, exitCode);
    }

    private void confirm(String root, String id) {
        Harness confirm = new Harness(tempDir);
        int exitCode = new CommandRouter().run(new String[]{
                "memory", "confirm", "--project-root", root, "--id", id
        }, confirm.context());
        assertEquals(ExitCodes.SUCCESS, exitCode);
    }

    private int countRows(Path root, String table) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:sqlite:" + root.resolve(".agents/memory/memory.db").toString());
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private void updateMemory(Path root, String assignment, long id) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:sqlite:" + root.resolve(".agents/memory/memory.db").toString());
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE memory_item SET " + assignment + " WHERE id = " + id);
        }
    }

    private static final class Harness {
        private final Path workingDirectory;
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();
        private final ByteArrayOutputStream err = new ByteArrayOutputStream();

        private Harness(Path workingDirectory) {
            this.workingDirectory = workingDirectory;
        }

        private CommandContext context() {
            return new CommandContext(
                    workingDirectory.toAbsolutePath().normalize(),
                    com.devharnesskit.dhk.testsupport.Utf8HarnessSupport.printStream(out),
                    com.devharnesskit.dhk.testsupport.Utf8HarnessSupport.printStream(err),
                    new FixedClock()
            );
        }

        private String stdout() {
            return com.devharnesskit.dhk.testsupport.Utf8HarnessSupport.text(out);
        }

        private String stderr() {
            return com.devharnesskit.dhk.testsupport.Utf8HarnessSupport.text(err);
        }
    }

    private static final class FixedClock implements Clock {
        public Instant now() {
            return Instant.parse("2026-05-21T00:00:00Z");
        }
    }
}
