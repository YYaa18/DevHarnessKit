# Task Template

## Task ID

`<project>-<number>`

## Project

`<fixture-name>`

## User Request

Describe the change exactly as the agent should receive it.

## Background

Explain only the context the agent is allowed to know at task start. Do not
include the ground truth.

## Constraints

- Keep the change minimal.
- Do not modify protected files.
- Do not reformat unrelated files.
- Preserve existing behavior unless the task explicitly changes it.

## Required Verification

```bash
<compile-or-test-command>
```

## Manual Evidence

Describe any smoke test, screenshot, SQL explain, or business verification that
must be recorded when automated tests are insufficient.

## Completion Bar

The task is complete only when the implementation, validation evidence, and
ground-truth scorecard can be reviewed without chat history.
