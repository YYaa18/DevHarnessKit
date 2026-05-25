# Goal Configuration

Goal orchestration has built-in alpha profiles, but projects can override or add local profiles without changing the jar.

## Profile Overrides

Put profile files under:

```text
.agents/devharness/goal-profiles/
```

Example:

```json
{
  "workflow_key": "api-change",
  "requires_spec": "true",
  "default_mode": "api",
  "actions": "inspect_existing_code,create_change_plan,implement_minimal_change,verify"
}
```

Notes:

- Values are strings because DevHarness Kit intentionally uses a minimal JSON parser.
- `actions` is a comma-separated list.
- `workflow_key` must match a seeded workflow template.
- If the profile file is invalid, the built-in profile with the same key is used when available.
- If a custom-only profile is invalid, `goal start --profile <key>` will fail because there is no built-in fallback.
- A goal is not ready to complete until every configured action has a recorded `goal step`.
- `goal step` validates the current action's required evidence. Built-in action evidence keys should appear in `--evidence`, while `changed_files` may be satisfied by `--changed-files`.

Minimal custom profile:

```json
{
  "workflow_key": "api-change",
  "requires_spec": "false",
  "default_mode": "api",
  "actions": "inspect_existing_code,create_change_plan,implement_minimal_change,verify"
}
```

## Check Policy

Put the check policy at:

```text
.agents/devharness/goal-check-policy.json
```

Example:

```json
{
  "required_checks": "compile,test,sensitive,spec,workflow",
  "compile_command": "mvn -q -DskipTests compile",
  "test_command": "mvn -q test",
  "fail_pending_hard_gates": "false"
}
```

For a non-Maven project or a controlled internal flow, narrow the required checks:

```json
{
  "required_checks": "sensitive,workflow",
  "fail_pending_hard_gates": "true"
}
```

The command parser is intentionally simple and splits `compile_command` and `test_command` by whitespace. Use wrapper scripts when commands need quoting or shell features.

## Doctor Diagnostics

`dhk doctor` validates project-level goal configuration and prints `WARNING goal_config` diagnostics for:

- unknown fields in profile or policy JSON files;
- invalid or empty action lists;
- empty `required_checks`;
- unsupported check names;
- invalid boolean values;
- profile `workflow_key` values that do not match known workflow templates.

Warnings do not change fallback behavior. The runtime still uses built-in defaults when a configured profile or policy cannot be loaded and a built-in fallback exists. `dhk doctor --json` includes these warnings in `goal_config_warnings`.

## Financial-System Example

Financial projects often need email, phone, and ID-card values in legitimate docs and test data. Keep goal checks strict, and configure sensitive-data policy separately for redaction or allow rules:

```json
{
  "required_checks": "compile,test,sensitive,spec,workflow",
  "compile_command": "mvn -q -DskipTests compile",
  "test_command": "mvn -q test",
  "fail_pending_hard_gates": "true"
}
```

Recommended pairing:

- Use `sensitive-policy.json` to redact or allow common financial test identifiers.
- Keep secrets, tokens, JDBC URLs, Authorization headers, cookies, private keys, and raw production SQL results rejected.
- Run `dhk doctor` after editing goal config so invalid profile or policy files are visible before an agent starts a goal.
