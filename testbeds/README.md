# DevHarnessKit Testbeds

This directory contains controlled evaluation projects and task definitions for
Graph-aware Goal Harness development.

The testbeds are not product code and are not included in release archives. They
exist so DevHarnessKit can compare Goal-only and Graph-aware behavior against
fixed tasks, ground truth, and scorecards.

## Layout

```text
testbeds/
  fixtures/              committed synthetic projects
  templates/             reusable task and scoring templates
  runs/                  local evaluation outputs, ignored by git
```

Use `fixtures/` for small, deterministic projects such as:

- `legacy-mybatis-order`
- `legacy-jsp-servlet-shop`
- `modern-java-api`

Use `templates/` when creating each task:

- `task.md`: task prompt given to the agent.
- `ground-truth.json`: expected files, impact points, validation, and risks.
- `scorecard.md`: human-readable DQI scoring sheet.
- `result.json`: machine-readable result shape for each run group.

Use `runs/` for local experiment outputs. Run outputs should not be committed
unless a future issue explicitly promotes a curated baseline report.

## Non-Goals

- Testbeds are not sample applications for end users.
- Testbeds should not depend on production credentials or private systems.
- Testbeds should stay small enough for deterministic CI and local review.
- Testbeds should not be packaged into the alpha release archive.

## Task Requirements

Every committed task should include:

1. A task description.
2. Initial project state or fixture reference.
3. Required changed files.
4. Forbidden changed files.
5. Expected impact points.
6. Required verification commands or manual evidence.
7. Risk notes and rollback requirements.
8. A scorecard for Goal-only and Graph-aware comparison.

The goal is not to make the synthetic projects large. The goal is to make the
impact surface measurable.
