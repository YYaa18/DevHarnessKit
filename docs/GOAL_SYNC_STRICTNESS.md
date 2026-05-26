# Goal Sync Strictness

Goal Sync Strictness is an experimental alpha contract for making `dhk goal`
the main development harness entry point while keeping workflow and spec state
auditable.

It is not a stable workflow engine. The current model is:

```text
goal        main controller and completion decision
workflow    auditable phase, gate, artifact, and checkpoint state
spec        auditable task and acceptance state
checks      fresh verification evidence for completion
exports     generated Markdown context, not source of truth
```

SQLite remains the source of truth. `GOAL_CONTEXT.md`, `GOAL_SUMMARY.md`,
`WORKFLOW_CONTEXT.md`, and `SPEC_CONTEXT.md` are regenerated views.

## Controller Model

For goal-driven work, use this flow:

```bash
dhk goal start --profile <profile> --task "<task>"
dhk goal next --goal <goal-key>
dhk goal step --goal <goal-key> --summary "<summary>" --evidence "<evidence>"
dhk goal verify --goal <goal-key>
dhk goal complete --goal <goal-key>
```

`goal complete` is allowed only after `goal verify` reports
`ready_to_complete`. Completion creates the final checkpoint and summary, binds
them to workflow artifacts, and closes the completion gate when the workflow is
otherwise complete.

## Automatic Sync

Profile action mappings define which workflow/spec state can be synchronized by
the goal layer:

- `phase_pass_mode=step` passes a mapped workflow phase after accepted goal
  evidence for that action;
- `phase_pass_mode=check` passes a mapped phase only after the mapping's
  required checks are fresh and accepted;
- `gate_pass_mode=step` passes mapped gates after accepted evidence;
- `gate_pass_mode=check` passes mapped gates only after fresh accepted checks;
- mapped spec tasks are created and marked done after the mapped action step;
- business acceptance mappings create managed spec acceptance items and can
  auto-pass only from `checks`, `test`, or explicit `evidence` sources.

Strict profiles also enforce workflow phase order. A later mapped phase is not
passed while earlier workflow phases remain incomplete.

## Manual Boundaries

Some state intentionally remains manual:

- `source=manual` business acceptance items are never auto-passed;
- workflow gate waivers are not performed by goal commands;
- spec archive is not performed by goal commands;
- memory confirmation remains outside the goal protocol;
- DB SQL is never run automatically by goal commands.

These boundaries keep the harness auditable. A goal may guide the next command,
but it should not silently waive process controls.

## Completion Gates

Strict goal completion requires:

- every configured action has a recorded `goal step`;
- required checks are accepted by the profile or check policy;
- checks are fresh for the latest goal steps and workspace fingerprint;
- required specs are non-empty when the profile requires tasks/acceptance;
- spec tasks and acceptance items are closed;
- workflow hard gates are closed unless the policy explicitly relaxes them;
- policy hooks allow completion.

If any condition fails, `goal verify` returns `not_ready`, lists completion
blockers, and prints the next command to run.

## Known Alpha Limits

Goal Sync Strictness is still alpha:

- profile mappings use a flat string JSON format;
- workflow/spec synchronization is limited to deterministic mappings;
- business acceptance mapping is intentionally conservative;
- generated Markdown section order may still expand before 1.0;
- schema tables and columns are not public API before a stable release.

See [GOAL_CONFIGURATION.md](GOAL_CONFIGURATION.md) for profile fields and
[POLICY.md](POLICY.md) for enforced hook points.
