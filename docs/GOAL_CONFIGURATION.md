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
