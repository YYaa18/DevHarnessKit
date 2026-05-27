package com.devharnesskit.dhk.command.artifact;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ArtifactPassportVerifyCommand implements Command {
    private static final String[] REQUIRED_SECTIONS = new String[]{
            "schema_version", "generated_at", "goal", "completion", "checks",
            "steps", "evidence", "graph", "bdd", "rollback", "artifacts"
    };

    public int run(CommandContext context, Args args) {
        try {
            Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
            Path passportPath = passportPath(args, projectRoot, context.workingDirectory());
            VerificationResult result = verify(passportPath);
            if (JsonOutput.enabled(args)) {
                printJson(context, passportPath, result);
            } else {
                printText(context, passportPath, result);
            }
            return result.passed() ? ExitCodes.SUCCESS : ExitCodes.VALIDATION_ERROR;
        } catch (Exception ex) {
            context.err().println("ERROR artifact passport verify failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private Path passportPath(Args args, Path projectRoot, Path workingDirectory) {
        String configured = args.option("path", "").trim();
        if (configured.length() == 0) {
            return PathUtil.artifactPassport(projectRoot);
        }
        return PathUtil.resolvePath(configured, workingDirectory);
    }

    private VerificationResult verify(Path passportPath) throws Exception {
        VerificationResult result = new VerificationResult();
        if (!Files.isRegularFile(passportPath)) {
            result.missing.add("passport file missing: " + passportPath);
            return result;
        }
        String text = new String(Files.readAllBytes(passportPath), "UTF-8");
        for (String section : REQUIRED_SECTIONS) {
            if (text.indexOf("\"" + section + "\"") < 0) {
                result.missing.add("required section missing: " + section);
            }
        }
        if (text.indexOf("\"schema_version\": \"artifact-passport/v1-alpha\"") < 0) {
            result.invalid.add("schema_version must be artifact-passport/v1-alpha");
        }
        verifyChecks(text, result);
        verifyGraph(text, result);
        verifyBdd(text, result);
        return result;
    }

    private void verifyChecks(String text, VerificationResult result) {
        List<String> checks = objectSnippetsWith(text, "check_key");
        if (checks.isEmpty()) {
            result.missing.add("checks array has no check entries");
            return;
        }
        for (String check : checks) {
            String key = stringValue(check, "check_key");
            String status = stringValue(check, "status");
            String fingerprint = stringValue(check, "check_fingerprint");
            String summary = stringValue(check, "summary");
            if (key.length() == 0) {
                result.invalid.add("check entry missing check_key");
            }
            if (!"passed".equals(status)) {
                result.invalid.add("check " + empty(key, "unknown") + " status is " + empty(status, "missing"));
            }
            if (fingerprint.length() == 0) {
                result.missing.add("check " + empty(key, "unknown") + " missing check_fingerprint");
            }
            if (summary.toLowerCase().indexOf("stale") >= 0) {
                result.stale.add("check " + empty(key, "unknown") + " summary indicates stale evidence");
            }
        }
    }

    private void verifyGraph(String text, VerificationResult result) {
        String graph = objectValue(text, "graph");
        if (graph.length() == 0) {
            return;
        }
        if (!booleanValue(graph, "enabled")) {
            return;
        }
        requireString(graph, "graph_snapshot", "graph", result);
        requireString(graph, "graph_context", "graph", result);
        requireString(graph, "graph_snapshot_hash", "graph", result);
    }

    private void verifyBdd(String text, VerificationResult result) {
        String bdd = objectValue(text, "bdd");
        if (bdd.length() == 0) {
            return;
        }
        if (!booleanValue(bdd, "enabled")) {
            return;
        }
        requireString(bdd, "bdd_evidence", "bdd", result);
        requireString(bdd, "bdd_coverage", "bdd", result);
    }

    private void requireString(String object, String field, String section, VerificationResult result) {
        if (stringValue(object, field).length() == 0) {
            result.missing.add(section + " evidence missing: " + field);
        }
    }

    private List<String> objectSnippetsWith(String text, String field) {
        List<String> result = new ArrayList<String>();
        Matcher matcher = Pattern.compile("\\{([^{}]*\"" + Pattern.quote(field) + "\"[^{}]*)\\}",
                Pattern.DOTALL).matcher(text == null ? "" : text);
        while (matcher.find()) {
            result.add(matcher.group(0));
        }
        return result;
    }

    private String objectValue(String text, String field) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\\{(.*?)\\}",
                Pattern.DOTALL).matcher(text == null ? "" : text);
        if (!matcher.find()) {
            return "";
        }
        return matcher.group(1);
    }

    private String stringValue(String text, String field) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(field)
                + "\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"", Pattern.DOTALL)
                .matcher(text == null ? "" : text);
        if (!matcher.find()) {
            return "";
        }
        return matcher.group(1).trim();
    }

    private boolean booleanValue(String text, String field) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*true",
                Pattern.DOTALL).matcher(text == null ? "" : text);
        return matcher.find();
    }

    private String empty(String value, String fallback) {
        return value == null || value.length() == 0 ? fallback : value;
    }

    private void printText(CommandContext context, Path passportPath, VerificationResult result) {
        context.out().println("artifact passport verify complete");
        context.out().println("status: " + (result.passed() ? "passed" : "failed"));
        context.out().println("path: " + passportPath);
        context.out().println("missing:");
        printList(context, result.missing);
        context.out().println("invalid:");
        printList(context, result.invalid);
        context.out().println("stale:");
        printList(context, result.stale);
    }

    private void printJson(CommandContext context, Path passportPath, VerificationResult result) {
        context.out().print(JsonOutput.object(
                JsonOutput.stringField("command", "artifact passport verify"),
                JsonOutput.stringField("status", result.passed() ? "passed" : "failed"),
                JsonOutput.stringField("path", passportPath.toString()),
                JsonOutput.rawField("missing", JsonOutput.stringArray(result.missingArray())),
                JsonOutput.rawField("invalid", JsonOutput.stringArray(result.invalidArray())),
                JsonOutput.rawField("stale", JsonOutput.stringArray(result.staleArray()))
        ));
    }

    private void printList(CommandContext context, List<String> values) {
        if (values.isEmpty()) {
            context.out().println("  - none");
            return;
        }
        for (String value : values) {
            context.out().println("  - " + value);
        }
    }

    private static final class VerificationResult {
        private final List<String> missing = new ArrayList<String>();
        private final List<String> invalid = new ArrayList<String>();
        private final List<String> stale = new ArrayList<String>();

        private boolean passed() {
            return missing.isEmpty() && invalid.isEmpty() && stale.isEmpty();
        }

        private String[] missingArray() {
            return missing.toArray(new String[missing.size()]);
        }

        private String[] invalidArray() {
            return invalid.toArray(new String[invalid.size()]);
        }

        private String[] staleArray() {
            return stale.toArray(new String[stale.size()]);
        }
    }
}
