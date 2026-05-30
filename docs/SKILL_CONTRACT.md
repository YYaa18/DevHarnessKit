# Skill Contract

Status: stable-candidate contract subset for 1.0.

Skill Contract turns a local skill from prompt text into a small, auditable
execution contract that `dhk skill` commands can lint, verify, trust, and audit.
For the 1.0 promotion track, the stable-candidate subset is the public shape of
`contract.json` plus the command semantics documented here. The full governance
runtime, scoring model, release enforcement, direct SQLite rows, and internal
`.agents/skills/` layout remain outside the stable contract until a release gate
explicitly promotes them.

## Local Layout

```text
.agents/skills/<skill-key>/
  SKILL.md
  contract.json
```

The stable-candidate `contract.json` loader accepts either JSON string arrays or
comma/newline-separated strings for command lists:

```json
{
  "schema_version": "skill-contract/v1",
  "skill_key": "devharness-goal-development",
  "version": "0.7.1",
  "task_type": "coding",
  "risk_level": "medium",
  "mode": "strict",
  "data_access_level": "context",
  "allowed_commands": [
    "dhk goal next",
    "dhk goal step",
    "dhk goal verify"
  ],
  "forbidden_commands": [
    "dhk workflow gate waive",
    "dhk spec archive",
    "dhk memory confirm",
    "dhk db sql"
  ],
  "declared_commands": [
    "dhk goal next",
    "dhk goal step",
    "dhk goal verify"
  ]
}
```

## Required Fields

| Field | Meaning |
| --- | --- |
| `schema_version` | Contract schema identifier. Current stable-candidate value: `skill-contract/v1`. Older `skill-contract/v1-alpha` files are accepted as legacy input during the 1.0 transition. |
| `skill_key` | Stable skill identifier and local directory name. |
| `version` | Skill contract version. |
| `task_type` | Primary task class such as `coding`, `review`, or `research`. |
| `data_access_level` | One of `none`, `metadata`, `context`, or `raw`. |
| `allowed_commands` | Command prefixes the skill may ask an agent to run. Required by lint. |
| `forbidden_commands` | Command prefixes the skill must not run or request. Required by lint. |

Stable-candidate optional fields:

| Field | Meaning |
| --- | --- |
| `risk_level` | Human review signal. Current default is `medium`; `high` triggers audit review. |
| `mode` | Governance mode label. Current default is `strict`. |
| `declared_commands` | Commands declared by skill scripts or protocol text. Lint checks they fit inside `allowed_commands` and avoid `forbidden_commands`. |

Non-contract fields may be added for internal governance experiments, but scripts
must ignore unknown fields unless a later stable contract lists them.

## MVP Contract Fixtures

The MVP contract is intentionally small and fixture-backed. The compatibility
fixtures live under `src/test/resources/fixtures/skill-contract/`:

```text
valid-contract.json
invalid-missing-fields.json
invalid-command-boundary.json
```

These fixtures are the minimum acceptance set for the contract loader and CLI:

```text
contract parsing        valid contract loads with command arrays
missing fields          missing task_type/data_access_level/command lists fail lint
command boundaries      declared commands cannot match forbidden_commands or fall outside allowed_commands
verify persistence      invalid contracts are not written to SQLite
trust/tamper hashing    trust pins source_hash, changed skill files require review_required
non-execution           trust and verify do not execute script-like files inside the skill directory
```

Adding fields to `contract.json` should preserve these fixtures or add a new
versioned fixture. Do not silently broaden command permissions through defaults.

## Persisted State

Schema v11 adds `skill_contract` to SQLite. It stores:

```text
skill_key
version
task_type
risk_level
mode
data_access_level
allowed_commands
forbidden_commands
contract_json
source_path
source_hash
trusted_source_hash
trust_status
trusted
created_at
updated_at
```

`source_hash` is a deterministic SHA-256 hash of the local skill directory.
`trusted_source_hash` records the exact source version approved through
`dhk skill trust`. When a later `dhk skill verify` sees that the current source
hash differs from the trusted hash, it marks `trust_status` as
`review_required` and sets `trusted` to `0`.

The table and column names above document current implementation behavior for
operators and migrations. They are not a public read or write API. Consumers
should use `dhk skill` commands and documented JSON/text output, not direct
SQLite access.

## CLI

```bash
dhk skill lint --skill <skill-key>
dhk skill lint --path .agents/skills/<skill-key>
dhk skill verify --skill <skill-key>
dhk skill verify --path .agents/skills/<skill-key>
dhk skill trust --skill <skill-key>
dhk skill trust --path .agents/skills/<skill-key>
dhk skill audit --skill <skill-key>
dhk skill audit --path .agents/skills/<skill-key>
dhk skill score --goal <goal-key>
dhk skill report --goal <goal-key> --baseline-score <score> --group F
dhk skill gate think-before-coding --goal <goal-key>
dhk skill gate goal-driven --goal <goal-key>
dhk skill gate simplicity --goal <goal-key>
dhk skill gate surgical-change --goal <goal-key>
```

`lint` checks the contract file and reports:

```text
missing    required fields or command lists
invalid    unsupported values such as an invalid data_access_level
forbidden  allowed commands that conflict with forbidden command prefixes
forbidden  declared commands outside allowed_commands or matching forbidden_commands
```

Stable-candidate `lint` exit codes:

```text
0  contract passed lint
2  command usage error, such as missing --skill/--path
3  contract validation failed
1  unexpected runtime error
```

`verify` runs the same lint contract and only persists the contract to
`skill_contract` when lint passes. Persistence is not trust: a first-time
verified skill is stored with `trust_status=unknown` and `trusted=0`. If a skill
was previously trusted and its source hash changes, verify stores
`trust_status=review_required`.

`trust` lints the skill and records the current source hash as trusted. Use it
only after reviewing the skill contract and local skill files.

Stable-candidate trust statuses:

| Status | Meaning |
| --- | --- |
| `unknown` | A valid contract was verified, but no trust pin exists. |
| `trusted` | `trusted_source_hash` matches the current `source_hash`. |
| `review_required` | A previously trusted skill changed and must be reviewed again. |
| `untrusted` | Reserved for explicit distrust workflows; current CLI does not emit it by default. |

`audit` scans local skill source for review risks before trust. It checks:

```text
contract          lint failures and high-risk declarations
script            script-like files that require manual review
dangerous_command forbidden/destructive commands such as rm -rf, dhk db sql, gate waive
script_execution  dynamic shell execution or curl/wget piped to shell
sensitive         sensitive-data pattern categories, without printing raw values
license           missing local LICENSE/NOTICE files
source            external or unreadable source paths
```

Any finding returns `decision=review_required`. Audit output is advisory and
does not persist trust; use `dhk skill trust` only after review.

For the built-in goal-development skill, the default release gate runs
`skill lint`, `skill verify`, and `skill audit`. Lint and verify must pass.
Audit is expected to return `review_required` because the bundled skill contains
wrapper scripts, but the release gate requires zero `critical` and zero `high`
audit findings.

`score` computes a deterministic skill quality score for a goal from local
goal steps and checks:

```text
gate_pass_rate          passed discipline gates / observed discipline gates
evidence_completeness   required evidence groups found in goal step evidence
rollback_quality        rollback_plan or rollback_quality evidence strength
score                   40% gates + 40% evidence + 20% rollback
```

Scores are local evaluation aids, not release approval by themselves.

`report` extends the score into a benchmark-style evaluation report. It includes
skill contract presence, trust status, the governed skill quality score, optional
baseline score, and DQI delta for F-group comparisons. The metric definitions
and fixture contract are documented in [SKILL_EVALUATION.md](SKILL_EVALUATION.md).

`skill gate` currently provides the first discipline gates:

```text
think-before-coding  requires inspect/plan ordering and goal_understanding evidence before implementation
goal-driven          checks recorded goal steps follow the profile action order and include evidence
simplicity           flags large change sets without scope evidence and abstraction files without justification
surgical-change      flags broad changes without impact/scope evidence and protected files without manual evidence
```

These gates write normal goal check rows using check keys
`think-before-coding`, `goal-driven`, `simplicity`, and `surgical-change`, so
they can be audited through goal check logs.

Projects can make these gates part of `goal verify` by adding them to
`.agents/devharness/goal-check-policy.json`:

```json
{
  "required_checks": "compile,test,sensitive,workflow,spec,think-before-coding,goal-driven,simplicity,surgical-change"
}
```

When configured as required checks, a failed discipline gate blocks
`goal complete` just like compile, test, sensitive, workflow, or spec.

## Governance Sequence

Skill Contract MVP is a governance foundation, not the full governance runtime.
The intended rollout order is:

```text
1. BDD quality gates define acceptance intent and evidence quality.
2. Skill Contract MVP records what a skill declares it may do.
3. Later governance layers enforce trust, discipline gates, artifact passports,
   human checkpoints, and release integrity.
```

For 1.0, `dhk skill lint`, `dhk skill verify`, `dhk skill audit`, and
`dhk skill trust` validate, review, persist, and explicitly pin local skill
source trust. They do not execute skill scripts, replace BDD evidence, create a
sandbox, or prove behavioral correctness.
