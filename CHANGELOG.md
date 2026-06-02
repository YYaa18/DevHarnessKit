# Changelog

## 1.2.1

Stable patch release.

English:

- Published a fresh installable package from the latest main branch after the
  v1.2.0 tag.
- Fixed memory refresh lifecycle behavior so refreshed expired/deprecated
  memories can return to an active status when new evidence verifies them.
- Improved memory conflict detection by reporting same-key fact variants with
  different fingerprints in addition to explicit semantic oppositions.
- Hardened stale/expire/refresh integration coverage to lock the memory
  lifecycle behavior introduced in the 1.2 line.
- Included the latest BDD, Graph Lite, and agent-interaction correctness fixes
  already validated on main. SQLite schema remains v17.

中文：

- 基于 v1.2.0 之后的最新 main 分支重新发布可安装包。
- 修复 memory refresh 生命周期行为：已过期或 deprecated 的记忆在补充新证据后，
  可以恢复为有效状态。
- 增强 memory conflict 检测：除显式语义反义外，同一 canonical key 下不同
  fingerprint 的事实版本也会给出提示。
- 补充 stale/expire/refresh 集成测试，固化 1.2 版本引入的记忆生命周期行为。
- 纳入 main 分支已验证的 BDD、Graph Lite 和 Agent 交互确认修复。SQLite schema
  仍为 v17。

## 1.2.0

Stable memory quality release.

English:

- Added CLI-first memory candidate workflows: `memory suggest` and
  `memory candidates list|accept|reject`. Candidate acceptance creates draft
  memory only and never bypasses `memory confirm`.
- Added memory governance commands for duplicate detection, conflict detection,
  superseding, stale scans, expiration, and refresh evidence.
- Added explainable memory ranking with score components and stale/superseded
  penalties in `memory search --explain`.
- Added safe team memory packs with manifest checksums, zip-entry traversal
  rejection, inspect-only validation, and import-as-draft behavior.
- Added schema v17 for memory quality metadata and the memory candidate queue.
- Added boundary and unsafe-operation tests for sensitive content, duplicate
  candidate acceptance, stale lifecycle changes, checksum validation, and unsafe
  zip entries.

中文：

- 新增 CLI 优先的记忆候选流程：`memory suggest` 和
  `memory candidates list|accept|reject`。接受候选只会生成 draft memory，
  不会绕过 `memory confirm`。
- 新增记忆治理命令：重复检测、冲突检测、替代关系、过期扫描、显式过期和刷新证据。
- 增强 `memory search --explain`，输出排序加分项和 stale/superseded 降权原因。
- 新增安全的团队 memory pack：manifest checksum、zip 路径穿越拒绝、只读 inspect、
  默认以 draft 导入。
- 新增 schema v17，用于 memory quality metadata 和 memory candidate queue。
- 补充边界与不安全场景测试，覆盖敏感内容、重复接受候选、过期生命周期、
  checksum 校验和危险 zip entry。

## 1.1.1

Stable patch release.

- Published a fresh installable package from the latest main branch after the
  v1.1.0 tag.
- Added targeted `EvidenceValueParser` unit coverage for plain fields,
  embedded `--field key=value` fragments, quoted values, escaped values,
  duplicate keys, and empty input.
- Improved installer guidance and the Chinese end-to-end acceptance plan for
  colleague onboarding.
- Hardened Work Brief / Agent Brief pre-work confirmation behavior so agents
  wait for explicit confirmation before writing business files, and added the
  `pre-work-file-write` release check.
- Kept the v1.1 stable surface compatible; this release does not promote DB
  inspection beyond beta.

## 1.1.0

Stable compatibility release.

- Added Goal retrospective reporting with `dhk goal retrospective` and the
  compatibility aliases `goal review-summary` and `goal mr-summary`.
- Added Goal external references through `goal start --external-ref` and
  `--card-ref`, with `external_ref`, `created_at`, and `completed_at` surfaced
  in Goal context, summaries, and artifact passports.
- Added schema v16 for `goal_run.external_ref` and documented the migration.
- Hardened Goal evidence parsing so embedded `--field key=value` evidence can
  still be interpreted by generated artifact reports.
- Expanded the built-in goal-development skill package so agents can discover
  and proactively use retrospective/review-summary workflows.
- Added complete goal wrapper coverage for Unix and Windows skill packages, and
  packaging tests that keep GoalCommand, skill wrappers, and skill contract
  declarations aligned.

## 1.0.0

First stable release.

- Promoted the documented core surface to the 1.0 stable contract: core CLI,
  memory, goal, configure, status, readiness, advise/quickstart, release
  packaging, checksums, and documented generated-export anchors.
- Promoted BDD acceptance harness, Graph Lite stable-advisory output, Workflow
  and Spec audit/traceability subsets, and generated export anchors into the
  documented 1.0 contract.
- Kept DB inspection beta, and kept stable-candidate governance and
  installer/reporting subsets outside the stable surface until a future
  compatibility release promotes them.
- Updated release metadata, README status, security policy, compatibility docs,
  and release gates so stable releases no longer require beta/developer-preview
  wording.
- Hardened the installer wizard for user-facing 1.0 packages and removed fixed
  `0.4.6-beta.1` jar fallbacks from installer lookup.
- Added default release-gate coverage for Graph Lite precision, BDD acceptance,
  built-in skill governance, Workflow/Spec JSON smoke, Routine CI-safe exports,
  and artifact manifest/checksum generation.
- 1.0 artifacts are built from Maven `project.version` as
  `dhk-cli-1.0.0-all.jar`, `devharnesskit-1.0.0.zip`,
  `devharnesskit-1.0.0.tar.gz`, `ARTIFACT_MANIFEST.json`, and `SHA256SUMS`.

## 0.4.6-beta.1

Release-readiness, control-panel, and onboarding hardening beta.

- Maven `project.version`, README status, release notes, release archives, and
  `CHANGELOG.md` now agree on `0.4.6-beta.1`.
- Added release metadata gates so CI and the local release gate fail when the
  current Maven version has no changelog entry, README version marker, or
  matching `docs/releases/v<version>.md` note.
- Added a release coverage gate that requires the generated JaCoCo line
  coverage report to stay at or above 60% before beta release packaging.
- Added CI performance smoke coverage for packaged CLI cold-start commands,
  doctor, memory search/export, jar size budgets, and lingering-process checks
  on the Ubuntu Java 17 smoke lane.
- Added a class-size structural gate and split the largest God Classes:
  `MigrationRunner` now delegates versioned schema work to migration steps,
  `GoalCheckService` delegates checks to focused runners, and goal
  orchestration/sync responsibilities are split out of `GoalOrchestrator` and
  `GoalActionSyncService`.
- Added `docs/INDEX.md` as the documentation map for onboarding, core
  references, experimental surfaces, release notes, and historical design notes.
- Added `docs/STABLE_CONTRACT.md` to define the stable-beta CLI, JSON,
  Markdown export, and `goal verify` blocker-category contract.
- Added the agent-mounted Work Brief / Agent Brief entry layer: `dhk advise`
  generates user-facing work intent and machine-facing execution policy,
  `quickstart` reuses the same brief contract, and `goal step --auto` can
  collect objective diff evidence for light patch flows.
- `scripts/devharness-control-panel.sh` is the recommended setup/readiness
  entrypoint; `scripts/install-agent-adapters.sh` remains a deprecated
  compatibility wrapper for one beta minor line.
- Goal skill documentation now opens with the 5-step core path and keeps the
  stricter full protocol as the advanced/auditable path.
- Release archive smoke now verifies dynamic wrapper jar fallback for Unix and
  Windows wrappers and blocks legacy memory-first Comate rules from packaged
  archives.
- Release archives now use a user-facing whitelist: runtime jar, installer,
  adapter assets, README/license/security/changelog notices, and runtime
  control-panel scripts are packaged, while repository docs, release-process
  docs, CI gates, smoke scripts, and local development helper scripts are left
  out of the downloadable distribution.
- Windows Java 17 CI hardening now fixes UTF-8 CLI test harness output,
  resolves Maven process calls through `mvn.cmd`/`mvnw.cmd` on Windows, uses
  platform-specific `.sh`/`.cmd` CGC test substitutes, and normalizes displayed
  source paths to `/`.

## 0.4.5-beta.1

Goal UX and agent-adapter hardening beta.

- Added `dhk goal next --json`, `goal step --template`, structured
  `goal step --field key=value`, and `goal step --dry-run` to make the goal
  protocol easier for agents and humans to follow.
- Added `dhk goal audit`, `dhk goal recheck`, and
  `goal verify --level fast|standard|release`.
- Added project-level `configure init/show/doctor/explain` commands and the
  flat `devharness-config/v1-alpha` dotted-key config schema.
- Added manual compile/test evidence modes and verification-risk handling for
  environments where Maven checks must run in an IDE, company runtime, or CI.
- Added agent adapter packaging for Claude Code, OpenCode, and Comate, with
  goal-first skills/rules and cleanup for the old memory-first skill.
- Added packaging tests for adapter install, control-panel compatibility, and
  wrapper project-root behavior.

## 0.4.4-beta.1

Governance and release-hardening beta.

- Goal Orchestration is promoted to beta developer-preview status while remaining pre-1.0 and not stable.
- Graph Lite remains Alpha+ and heuristic; impact maps are advisory context, not correctness proofs.
- Graph-aware Goal now carries freshness, impact, architecture, protected-file, and legacy manual-evidence checks through `goal verify`.
- `dhk graph impact` rejects stale snapshots by default and now requires explicit approval evidence for `--allow-stale`.
- Added project policy field `graph_allow_stale_requires_approval` with default `true`.
- Graph-aware skill wrappers block weak-model self-service use of `--allow-stale` unless external approval sets `DHK_ALLOW_STALE_APPROVED=true`.
- Added `dhk graph prune --dry-run` to audit snapshots/files/nodes/edges/query-cache rows/goal bindings that would be deleted without modifying SQLite.
- Release packaging reads Maven `project.version` for jar and archive names.
- Full Maven test suite, package build, release archive smoke, graph stale override tests, graph prune dry-run tests, and checksum generation are required before tagging.

## 0.1.0-alpha

Initial developer-preview release.

- Memory core: init, add, confirm, search, export, checkpoint, recover.
- DB readonly: test, sql, and dry-run with fail-closed SQL guard.
- Workflow alpha: templates, runs, phases, gates, artifacts, bindings, summary, and export.
- Spec alpha: changes, documents, tasks, acceptance criteria, status, archive, workflow binding, and export.
- Goal orchestration alpha MVP: `goal start`, `goal resume`, `goal next`, `goal step`, `goal status`, `goal export`, `goal check`, `goal evaluate`, `goal verify`, `goal complete`, schema v5, `GOAL_CONTEXT.md`, and `GOAL_SUMMARY.md`.
- Goal evidence hardening: `goal step` validates required evidence for the current action and `goal evaluate` refuses completion until configured actions are recorded.
- Goal-first skill hardening: wrapper project-root behavior and protocol reference docs are covered by packaging tests.
- Goal config diagnostics: `dhk doctor` warns about invalid project-level goal profiles and check policies, including unknown fields, invalid actions, empty required checks, and missing workflow templates.
- Goal export contract tests: `GOAL_CONTEXT.md` and `GOAL_SUMMARY.md` required sections, next command, forbidden actions, checkpoint reference, and truncation behavior are covered by tests.
- Workflow/spec export contract docs and tests for required Markdown sections, audit-state boundaries, gates, tasks, acceptance items, and workflow bindings.
- Migration fixture coverage for v2 workflow state and richer v4 spec state, plus archive-state tests for blocked/failed versus skipped/waived spec closure.
- Compatibility contract docs for stable-ish alpha, beta, and alpha CLI/schema/JSON/export surfaces, including downgrade and deprecation policy.
- Module split and installer strategy decision record: keep one shaded alpha jar plus release archives in `0.1.x`, revisit memory-only/DB artifacts before promoting DB readonly or installers.
- Coverage threshold decision and SQL safety fuzz backlog: keep JaCoCo report-only in `0.1.x`, record the current baseline, and require targeted SQL guard fuzz/property tests before stable DB readonly claims.
- Sensitive-data guard for persisted and exported content paths.
- `dhk version`.
- File/stdin input for memory content, checkpoint summaries, SQL, and spec documents.
- `memory search --explain`.
- `--json` output for doctor, memory search/export, and goal status/check/evaluate/complete.
- DB readonly beta hardening: MySQL URL compatibility flags, DB risk notice, `db test --json`, `db sql --dry-run --json`, and `db sql --format json`.
- DB explain plan compatibility: `db sql --explain` works with MySQL 8 and Connector/J 5.1 by avoiding the JDBC read-only hint for validated `EXPLAIN` statements.
- `memory backup`.
- Automatic pre-migration memory backups with old-db fixture coverage.
- Section-budgeted `CURRENT_CONTEXT.md` with a truncation report.
- Project-level goal profile and check policy configuration.
- JaCoCo coverage report generation during `mvn test`.
- Project-level sensitive policy with `reject`, `redact`, and `allow` actions.
- Java/OS CI matrix.
- Release archives with third-party notices and checksums.
- Goal-first agent skill package with start/resume/next/step/check/evaluate/complete wrapper scripts.
- Goal check freshness metadata: schema v6 records `step_count_at_check` for each `goal check` result and exposes it in text/JSON output.
- Goal stale-check enforcement: `goal evaluate` and `goal complete` now reject required checks recorded before the latest goal step.
- Goal check status policy: built-in Java goal profiles now require passed compile/test/sensitive/spec checks, with project-level `accepted_<check>_statuses` overrides for non-Maven flows.
- Goal context export recovery: schema v7 adds resumable `context_exporting` and `context_export_failed` states for failed `GOAL_CONTEXT.md` exports.
- Goal workspace freshness: schema v8 records workspace, context, and check fingerprints so completion can reject checks that became stale after later workspace changes.
- Goal Sync Strictness alpha: profile action mappings can drive workflow phases, workflow gates, spec tasks, and strict phase order from accepted evidence or fresh checks.
- Goal business acceptance mappings: profiles can define managed acceptance items backed by fresh checks, test results, explicit evidence, or manual confirmation.
- Policy hook expansion: project policy can block goal step/check/export and DB SQL non-dry-run now requires explicit risk acknowledgement before credential validation.
- Goal verify/context protocol hardening: `goal verify` emits `freshness_status` and `completion_blockers`, and `GOAL_CONTEXT.md` includes allowed commands, evidence contract, freshness status, and completion blockers.
- Documentation: Goal Sync Strictness guide documents the goal-controlled workflow/spec model, automatic sync rules, manual confirmation boundaries, and experimental alpha limits.
