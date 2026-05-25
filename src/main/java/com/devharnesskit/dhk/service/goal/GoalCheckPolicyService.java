package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.util.JsonUtil;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class GoalCheckPolicyService {
    public GoalCheckPolicy load(Path projectRoot) {
        if (projectRoot == null) {
            return GoalCheckPolicy.defaults();
        }
        Path policyPath = PathUtil.goalCheckPolicy(projectRoot);
        if (!Files.isRegularFile(policyPath)) {
            return GoalCheckPolicy.defaults();
        }
        try {
            Map<String, String> raw = JsonUtil.parseObject(new String(Files.readAllBytes(policyPath), "UTF-8"));
            String[] requiredChecks = splitList(raw.get("required_checks"));
            String[] compileCommand = splitCommand(raw.get("compile_command"));
            String[] testCommand = splitCommand(raw.get("test_command"));
            boolean failPendingHardGates = parseBoolean(raw.get("fail_pending_hard_gates"), false);
            return new GoalCheckPolicy(requiredChecks, compileCommand, testCommand, failPendingHardGates);
        } catch (Exception ex) {
            return GoalCheckPolicy.defaults();
        }
    }

    private String[] splitList(String value) {
        if (value == null || value.trim().length() == 0) {
            return new String[0];
        }
        String[] parts = value.split(",");
        List<String> cleaned = new ArrayList<String>();
        for (String part : parts) {
            String text = part.trim();
            if (text.length() > 0) {
                cleaned.add(text);
            }
        }
        return cleaned.toArray(new String[cleaned.size()]);
    }

    private String[] splitCommand(String value) {
        if (value == null || value.trim().length() == 0) {
            return new String[0];
        }
        String[] parts = value.trim().split("\\s+");
        List<String> cleaned = new ArrayList<String>();
        for (String part : parts) {
            if (part.length() > 0) {
                cleaned.add(part);
            }
        }
        return cleaned.toArray(new String[cleaned.size()]);
    }

    private boolean parseBoolean(String value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("true".equals(normalized) || "yes".equals(normalized) || "1".equals(normalized)) {
            return true;
        }
        if ("false".equals(normalized) || "no".equals(normalized) || "0".equals(normalized)) {
            return false;
        }
        return defaultValue;
    }
}
