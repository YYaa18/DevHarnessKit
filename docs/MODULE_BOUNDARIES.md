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
