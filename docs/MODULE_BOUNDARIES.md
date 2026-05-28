# Module Boundaries

DevHarness Kit is still distributed as one shaded CLI jar. The boundaries below
are logical ownership boundaries for stable-candidate work, not Maven modules.
They exist so future extraction can happen without changing user-facing commands,
agent adapters, or release archive layout too early.

## Dependency Direction

```text
dhk-cli
  -> dhk-governance, dhk-bdd, dhk-graph, dhk-db, dhk-core

dhk-governance -> dhk-core
dhk-bdd        -> dhk-core
dhk-graph      -> dhk-core
dhk-db         -> dhk-core
dhk-core       -> no feature module dependencies
```

Feature modules may publish small adapters back to goal checks, but the core
facade must stay stable. The current single-artifact implementation uses package
ownership and CI boundary scripts instead of Maven module enforcement.

## Package Ownership

| Logical module | Current package ownership |
| --- | --- |
| `dhk-cli` | `cli`, `command`, `Main`, wrapper scripts, release archive assembly. |
| `dhk-core` | `db`, `db.migration`, shared `model`, memory/repository roots, `service.goal`, `service.workflow`, `service.spec`, `service.config`, `service.checkpoint`, `export`, `util`, sensitive policy. |
| `dhk-db` | `command.projectdb`, `sql`, readonly DB execution and SQL rendering services. |
| `dhk-graph` | `command.graph`, `model.graph`, `repository.graph`, `service.graph`. |
| `dhk-bdd` | `command.bdd`, `model.bdd`, `repository.bdd`, `service.bdd`. |
| `dhk-governance` | `command.skill`, `command.artifact`, `command.checkpoint`, `model.policy`, `model.skill`, `repository.skill`, `service.policy`, `service.skill`, artifact passport and human checkpoint services. |

## Stabilized Split Points

- `GoalCheckService` is a facade. `DefaultGoalCheckRunnerFactory` owns default
  runner wiring, and each `GoalCheckRunner` owns one check family.
- `MigrationRunner` owns transaction control and FTS setup. `DefaultMigrationStepCatalog`
  owns the ordered step list, `MigrationBackupCoordinator` owns pre-upgrade
  backups, and `MigrationSchema` owns schema inspection helpers.
- `CliCommandCatalog` is descriptor-driven. Shell completion consumes the same
  command descriptors used by command metadata instead of maintaining an
  independent list.

## Boundary Rules

1. Command classes parse input, call services, and print stable text/JSON.
2. Repositories own SQL persistence and should not call command classes.
3. Services own validation, orchestration, and policy decisions.
4. Renderers own Markdown/JSON-ish export text and should not mutate state.
5. Feature modules may depend on core, but core should not import feature
   command packages.
6. Future Maven modules must keep `dhk-cli` as the compatibility artifact until
   installers, wrappers, notices, and release archives explicitly support a split.

## Known Boundary Debt

- Goal check runner implementations still physically live under `service.goal`.
  Before Maven modules, graph/BDD/governance-specific runners can move behind
  module-owned factories without changing check keys.
- Shared helpers such as sensitive policy and backup service are still in root
  `service`. A future split should decide whether they remain core or move into
  smaller internal shared packages.
- The command tree is intentionally monolithic while the release artifact is
  monolithic. Do not split Maven modules before adapter installation and release
  archive contracts support it.

## Experimental Artifact Boundary Matrix

This matrix records the 1.0-preparation decision for experimental surfaces. It
does not change the current beta packaging: DevHarness Kit still ships one
shaded CLI jar and one release archive. The stable promise is defined by
`STABLE_CONTRACT.md`; the rows below are included for dogfooding and adapter
compatibility, not as stable API commitments.

| Surface | Current 1.0-prep decision | Stable contract status | Future artifact path | Release packaging impact |
| --- | --- | --- | --- | --- |
| Graph Lite / Graph-aware Goal | Keep bundled in the default shaded CLI for beta and stable-candidate dogfooding. | Experimental; graph output is heuristic advisory context, not a correctness proof. | Candidate for `dhk-graph` only after graph checks and adapter paths can be isolated from goal core without changing check keys. | No current archive split. Future split requires README, release notes, wrappers, and `THIRD_PARTY_NOTICES.md` to describe core/full behavior. |
| BDD acceptance harness | Keep bundled while BDD-required profiles are still internal alpha. | Experimental; scenario evidence and BDD quality gates may change before 1.0. | Candidate for `dhk-bdd` after executable adapter contracts and evidence schema stabilize. | No current archive split. Future split must keep existing goal verify behavior compatible when BDD is absent. |
| Skill Contract / Evaluation / Trust | Keep bundled because agent adapters depend on these files and checks during dogfooding. | Experimental governance surface; not part of stable CLI core. | Candidate for `dhk-governance` once adapter install state, trust policy, and skill evaluation schemas are versioned. | Current adapters assume a single `dhk.jar`; split requires adapter installer and control panel updates. |
| Policy / Hook governance | Keep bundled as local enforcement scaffolding, but outside the stable contract. | Experimental; policy/hook points can still change. | Candidate for `dhk-governance` or a small shared policy module after hook events are fixed. | Future split must preserve default safe behavior when optional governance modules are not installed. |
| Routine / metrics / replay | Keep as planned alpha documentation and local report helpers; no public routine command is stable. | Experimental/planned alpha. | Candidate for a later reporting module, not a 1.0 core dependency. | No current release archive change. Do not add package-manager/install obligations until routine commands are public. |
| ECC Control Panel scripts | Keep bundled as scripts and adapter setup helpers for beta UX. | Experimental script surface; not stable CLI API. | Candidate for installer/control-panel packaging, not core. | Future split must preserve script paths or provide migration wrappers because users and adapters may call them directly. |

Default 1.0-prep answer: keep the shaded CLI bundled for compatibility, mark
these surfaces outside the stable contract, and delay independent artifacts
until licensing, startup, size, installer, and adapter costs justify the added
release surface.
