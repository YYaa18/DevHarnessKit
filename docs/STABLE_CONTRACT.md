# Stable Contract

DevHarness Kit `1.1.0` preserves the smallest documented stable surface first
promoted in 1.0 and adds stable-candidate reporting and skill-packaging
coverage. Anything not listed here is beta-only, experimental, or internal.
This remains the 1.0 stable contract carried forward under the 1.1 line.

## Stable Surface

The 1.0 stable surface is intentionally narrow:

- `dhk help`
- `dhk version`
- `dhk doctor`
- `dhk configure init/show/doctor/explain`
- `dhk status`
- `dhk readiness`
- `dhk advise`
- `dhk quickstart`
- memory core: `init`, `add`, `confirm`, `search`, `export`, `checkpoint`,
  `recover`, and `backup`
- goal core: `start`, `resume`, `next`, `step`, `status`, `export`, `verify`,
  `complete`, `audit`, and `recheck`
- workflow audit subset: `start`, `status`, `export`, and `summary`
- spec traceability subset: `create`, `status`, `export`, and `bind-workflow`
- Graph Lite stable-advisory subset: `status`, `index`, `impact`, and `export`
- BDD acceptance harness: `init`, `add`, `scenario create/list/show`,
  `list`, `show`, `export`, `lint`, `evidence add`, `evidence junit`,
  `evidence report`, `verify`, `coverage`, `bind-spec`, `bind-goal`,
  `bind-workflow`, `bind-graph`, and `bind-test`

For 1.0 and later, command names, required option names, exit code meanings,
and the minimum machine-readable fields below should change only under semantic
versioning and documented deprecation windows.

The current stable artifact is `1.1.0`.

## Exit Codes

The stable-beta exit code contract is:

- `0`: success
- `1`: runtime error
- `2`: usage error
- `3`: validation or safety rejection
- `4`: target not found

## Stable JSON Fields

Stable JSON consumers should ignore unknown fields. Additive fields are not
breaking changes.

The following fields are the minimum stable JSON fields:

- common command identity: `command`
- goal identity: `goal_key`
- completion decision: `decision`, `ready_to_complete`
- verification counts: `check_count`, `failed_count`, `missing_count`,
  `completion_blocker_count`, `stale_count`
- verification arrays: `checks`, `failed_checks`, `missing`,
  `completion_blockers`, `stale_checks`
- verification guidance: `blocker_summary`, `blocker_categories`,
  `blocker_details`, `user_guidance`, `next_action`, `next_command`
- context pointer: `context_path`
- BDD identity and evidence fields: `feature_key`, `scenario_key`, `status`,
  `steps`, `bindings`, `evidence_id`, `evidence_type`, `evidence_path`,
  `covered_count`, `missing_evidence`, `pending_evidence`, `failed_evidence`,
  and `scenarios`
- workflow identity and status fields: `run_key`, `workflow`, `task`,
  `module`, `mode`, `status`, `current_phase`, `phases`, and
  `pending_hard_gates`
- workflow export and summary fields: `workflow_context_path`,
  `exported_memory_count`, `artifact_count`, `checkpoint_count`,
  `bound_spec_count`, `pending_hard_gate_count`, and
  `blocking_hard_gate_count`
- spec identity and status fields: `change_key`, `title`, `status`, `module`,
  `mode`, `priority`, `tasks`, `acceptance`, and `bound_workflows`
- spec export and workflow binding fields: `export_path`,
  `workflow_spec_binding_id`, `run_key`, and `binding_type`
- Graph Lite stable-advisory status fields: `config_source`, `report_path`,
  `files_considered`, `indexed_files`, `skipped_files`, `max_file_bytes`,
  `max_indexed_files`, `max_impact_depth`, `max_export_nodes`,
  `graph_nodes`, `graph_edges`, `parse_errors`, `latest_snapshot_key`,
  `latest_snapshot_status`, `current_workspace_fingerprint`,
  `latest_snapshot_workspace_fingerprint`, `latest_snapshot_stale`,
  `latest_snapshot_nodes`, and `latest_snapshot_edges`
- Graph Lite stable-advisory impact fields: `found`, `query_type`, `query`,
  `depth`, `requested_depth`, `max_impact_depth`, `depth_limited`,
  `snapshot_key`, `snapshot_stale`, `allow_stale`,
  `allow_stale_evidence`, `current_workspace_fingerprint`,
  `snapshot_workspace_fingerprint`, `related_files`, `related_sql`,
  `related_tests`, `risk_nodes`, `recommended_read_files`, `impact_map`,
  and `scenario_impact_map`
- Graph Lite stable-advisory export fields: `snapshot_key`, `files`, `nodes`,
  `edges`, `context_path`, and `snapshot_path`

`goal verify` blocker categories are stable-beta values:

- `incomplete_goal_steps`
- `context_export_not_ready`
- `goal_status_blocked`
- `pending_check`
- `stale_check`
- `pending_workflow_gate`
- `missing_spec`
- `missing_evidence`
- `skipped_required_check`
- `failed_check`
- `goal_blocker`

## Stable Markdown Export Anchors

SQLite remains the source of truth. Markdown files are generated artifacts.
Stable-beta scripts may rely on these files existing after the matching command
succeeds, and may scan the listed anchors.

`CURRENT_CONTEXT.md`:

- project summary section
- confirmed memory section
- recent checkpoint section

`GOAL_CONTEXT.md`:

- goal identity
- current action
- allowed actions
- forbidden actions
- evidence contract
- freshness status
- next command

`GOAL_SUMMARY.md`:

- goal identity
- optional external reference
- created/completed timestamps
- final status
- steps summary
- checks summary
- artifacts summary

`ARTIFACT_PASSPORT.json`:

- `schema_version`
- `goal_key`
- `external_ref`
- `generated_at`
- `created_at`
- `completed_at`
- `artifacts`
- `checks`
- `manual_evidence_status`

`WORK_BRIEF.md`:

- task summary
- recommendation
- risk flags
- expected work
- user choices
- confirmation requirement

`BDD_CONTEXT.md`:

- boundary section
- features section
- scenarios section
- agent instructions section

`BDD_EVIDENCE.md`:

- summary section
- scenario evidence section

`BDD_COVERAGE.md`:

- coverage summary section
- scenario coverage section

`WORKFLOW_CONTEXT.md`:

- workflow run section
- current phase section
- phases section
- pending hard gates section
- agent instructions section

`SPEC_CONTEXT.md`:

- spec change section
- document type sections
- tasks section
- acceptance section
- bound workflows section
- agent instructions section

`GRAPH_CONTEXT.md`:

- boundary section
- graph-confidence section
- snapshot section
- limits section
- file-hashes section
- node-kinds section
- risk-nodes section
- truncation-report section
- agent-instructions section

`IMPACT_MAP.md`:

- summary section
- snapshot-freshness section
- graph-confidence section
- start-nodes section
- direct-callers section
- direct-callees section
- related-files section
- related-sql section
- related-tests section
- missing-related-tests section
- risk-nodes section
- recommended-read-files section
- candidate-suggestions section
- scoring-data section

`GRAPH_SNAPSHOT.json`:

- `schema_version`
- `generated_at`
- `snapshot_id`
- `snapshot_key`
- `provider`
- `status`
- `workspace_fingerprint`
- `config_hash`
- `git_commit`
- `git_dirty`
- `file_count`
- `node_count`
- `edge_count`
- `max_file_bytes`
- `max_indexed_files`
- `max_export_nodes`
- `file_hashes_truncated`
- `risk_nodes_truncated`
- `file_hashes`

`AGENT_BRIEF.json` is alpha machine-readable adapter protocol, not stable
public API. Its schema is versioned with `devharness-agent-brief/v1-alpha`.

## Beta But Included

DB inspection remains beta but is included in the developer toolkit. DevHarness
Kit does not force database read-only credentials because the intended target is
a trusted personal development environment. SQL guardrails and JDBC read-only
hints reduce accidental risk, but do not replace database permissions or local
developer responsibility.

## 1.0 Boundary Decisions

The first 1.0 stable tag keeps one shaded CLI jar and one release archive for
install simplicity. Experimental surfaces stay bundled for dogfooding and
adapter compatibility, but they remain outside the stable contract unless a
future release note explicitly promotes them.
Release artifacts are published with `target/ARTIFACT_MANIFEST.json` and
`target/SHA256SUMS`. The manifest schema is `devharness-release-artifacts/v1`;
it records the Maven version source, release channel, required jar/archive
paths, archive entrypoints, excluded internal entries, and the checksum file.

The artifact-boundary decision for Graph, BDD, Skill/Governance, Policy,
Routine, and ECC Control Panel is recorded in
[MODULE_BOUNDARIES.md](MODULE_BOUNDARIES.md). BDD is stable while still bundled
in the default artifact; splitting it into an optional artifact is deferred until
the split reduces user risk more than it increases install and adapter
complexity.

The BDD stable contract covers command shape, generated export anchors, evidence
status semantics, and goal-check integration. It does not make scenarios a proof
of implementation correctness. A scenario only supports completion when accepted
evidence, required compile/test checks, and any configured goal gates also pass.

The Workflow stable subset records audit state and renders context for agents.
It is not a workflow engine, scheduler, background runner, or correctness proof.

The Spec stable subset records change intent, task state, acceptance criteria,
and workflow traceability. It does not prove implementation correctness without
accepted evidence and required goal checks.

The Graph Lite stable-advisory subset gives agents bounded impact context,
snapshot freshness, recommended reads, related tests, and risk-node hints. Its
command names, required options, JSON fields, and generated export anchors are
stable for 1.0 consumers, but its results remain heuristic. Graph Lite is not a
complete parser, static-analysis oracle, security scanner, dependency solver, or
correctness proof. Completion still requires compile/test/manual evidence and
any configured goal, spec, workflow, or BDD checks.

Graph Lite SQLite tables, `.agents/graph/config.json`, provider adapter details,
precision thresholds, ranking algorithms, and raw node/edge scoring internals
remain internal or experimental unless a later contract explicitly promotes
them.

## Stable-Candidate Pending Gate

Skill governance has a documented stable-candidate subset, but it is not part
of the 1.0 stable surface. The default release gate now checks built-in skill
lint, verify, and audit output so the candidate subset cannot drift silently.
The candidate subset covers:

- `contract.json` schema identifier `skill-contract/v1`;
- required fields `skill_key`, `version`, `task_type`, `data_access_level`,
  `allowed_commands`, and `forbidden_commands`;
- optional fields `risk_level`, `mode`, and `declared_commands`;
- `dhk skill lint`, `verify`, `trust`, `audit`, `score`, and `report` command
  semantics and JSON fields documented in [SKILL_CONTRACT.md](SKILL_CONTRACT.md),
  [SKILL_EVALUATION.md](SKILL_EVALUATION.md), and
  [JSON_OUTPUT.md](JSON_OUTPUT.md).

The candidate subset does not make `.agents/skills/` layout details, direct
`skill_contract` SQLite rows, scoring weights beyond the documented local
formula, or full governance enforcement stable. Trust status and scores are
local governance signals, not sandboxing, permissions, or correctness proof.

Policy/Hook governance has a documented stable-candidate local schema and hook
behavior subset in [POLICY.md](POLICY.md). The candidate subset covers
`devharness-policy/v1` string-only policy files, doctor diagnostics for missing,
damaged, and conflicting policy settings, and validation-error hook behavior
with `next_command:` guidance for goal, DB SQL, Graph stale, and context export
blocks. It does not make policy files a sandbox, permission boundary, network
daemon, direct SQLite API, or guarantee that external tools cannot modify files
outside DevHarnessKit.

Routine reporting has a documented stable-candidate local report subset in
[GOAL_METRICS_REPLAY.md](GOAL_METRICS_REPLAY.md) and
[ROUTINE_LOCAL_CI_EXPORT.md](ROUTINE_LOCAL_CI_EXPORT.md). The candidate subset
covers `goal-metrics/v1`, `goal-replay/v1`, `routine-summary/v1`, deterministic
replay ordering, local Markdown/JSON/NDJSON export helpers, and CI-safe
aggregate export helper files. It does not make `dhk routine` a stable public
command, add a scheduler, add a dashboard, score models, upload telemetry, or
make SQLite tables a public API.

ECC Control Panel has a stable-candidate user-facing script entrypoint subset.
The candidate subset covers archive-root `install.sh`,
`scripts/install-agent-adapters.sh`, and `scripts/devharness-control-panel.sh`
commands `configure`, `plan`, `install`, `status`, `doctor`, `repair`, and
`uninstall`. It also covers documented option names, non-mutating `plan` and
`--dry-run` behavior, `status --status-format text|json|markdown`, and the
local `doctor`/`repair` flow. It does not make generated `.agents/`,
`.claude/`, or `.comate/` adapter layouts, generated rule contents,
`devharness-config/v1-alpha`, `devharness-agent-manifest/v1-alpha`,
`devharness-install-state/v1-alpha`, or private script helper structure stable
APIs.

## Experimental Outside The Contract

The following surfaces are included for dogfooding and feedback, but are not
part of the stable contract:

- Graph-aware Goal orchestration beyond the stable Graph Lite output contract
- Skill governance enforcement beyond the stable-candidate subset
- Professional Knowledge Layer and knowledge pack injection
- Policy/Hook governance beyond the documented stable-candidate local subset
- ECC Control Panel internals beyond the stable-candidate script entrypoint
  subset
- Routine CLI packaging, dashboards, schedulers, and cross-project replay
- direct SQLite table or column details

These surfaces may change in later releases with release notes.
