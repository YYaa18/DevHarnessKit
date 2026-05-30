# Stable Expansion Plan

Status: active roadmap for promoting remaining DevHarness Kit surfaces toward a
broader 1.0 stable contract.

This plan tracks the Linear roadmap rooted at `AI-319`. It does not make every
bundled command stable at once. Each module must first define an external
contract, tests, release gate coverage, and migration or rollback notes. Internal
SQLite tables, generated adapter layouts, and implementation classes remain
internal unless a later contract explicitly names them.

## Linear Map

| Milestone | Linear Feature | Purpose |
| --- | --- | --- |
| M1 | `AI-320` | Workflow and Spec stable subset. |
| M2 | `AI-321` | Graph Lite stable-advisory contract. |
| M3 | `AI-322` | Skill governance stable subset. |
| M4 | `AI-323` | Policy and Routine stable reporting. |
| M5 | `AI-324` | DB SQL beta to stable-readonly. |
| M6 | `AI-325` | Packaging, ECC control, and final contract sweep. |

## Promotion Rules

A surface can move into the stable contract only when all of these are true:

- stable command names and required option names are documented;
- stable JSON fields or Markdown anchors are documented;
- exit code and failure semantics are covered by tests;
- release gate coverage exists and runs by default;
- migration, rollback, or compatibility guidance is documented;
- the docs keep non-goals explicit, especially where a feature is advisory
  rather than proof of correctness.

## M1 Workflow And Spec Stable Subset

Workflow and Spec are the first promotion target because Goal and BDD already
depend on them for audit state and traceability.

Stable Workflow commands:

- `dhk workflow start`
- `dhk workflow status`
- `dhk workflow export`
- `dhk workflow summary`

Stable Spec commands:

- `dhk spec create`
- `dhk spec status`
- `dhk spec export`
- `dhk spec bind-workflow`

Stable exports:

- `WORKFLOW_CONTEXT.md`
- `SPEC_CONTEXT.md`

Boundary:

- Workflow records process audit state. It is not a workflow execution engine.
- Spec records change intent, tasks, acceptance criteria, and workflow bindings.
  It does not prove implementation correctness.
- Direct workflow/spec SQLite rows are not public API.

## Later Milestones

M2 Graph Lite is promoted as a stable-advisory surface: the command/export
contract is documented, the curated precision suite writes structured result
JSON, and the precision suite is part of the default release gate. It must
never be described as complete static analysis or a correctness proof.

M3 Skill governance stabilizes the minimum `contract.json` schema, adds trust
hash regression coverage, and makes built-in skill governance checks part of
the default release gate. Direct `.agents/skills/` layout details and
`skill_contract` SQLite rows remain non-contract.

M4 Policy and Routine stabilize local policy hooks and deterministic reporting.
M4.1 promotes `devharness-policy/v1` as a stable-candidate string-only schema,
doctor warns on missing, damaged, or conflicting policy configuration, and hook
blocks include actionable `next_command:` guidance. Policy must not be
described as a sandbox or operating-system permission boundary. Routine must
not become a scheduler or dashboard in this track.

M5 DB SQL can become stable-readonly only after SQL safety fuzz/property tests
and real MySQL 5.1/8 compatibility evidence are documented.

M6 stabilizes user-facing installer, manifest, wrapper, and control-panel
entrypoints. `.agents/` and `.comate/` internal layouts remain non-contract.
