# Coverage Gate Report 2026-05-27

## Summary

`scripts/release-gate.sh` passed before and after the hardening changes on branch
`codex/ai-166-174-coverage-hardening`.

Coverage threshold remains unchanged:

```text
line coverage ok: 86.49% >= 60%
```

This means the 60% release gate is currently real, not a paper gate. This round
added targeted stable-core tests and release governance checks; there is still
no justification to lower or bypass the threshold.

## Command

```bash
./scripts/release-gate.sh
```

The raw local log was captured at:

```text
docs/reports/release-gate-coverage-baseline-20260527.log
docs/reports/release-gate-final-20260527.log
docs/reports/release-gate-final-rerun-20260527.log
```

That directory is intentionally ignored by git, so this report records the
durable summary.

## Environment

```text
OS: macOS 26.5 aarch64
Java: Zulu OpenJDK 21.0.11
Maven: 3.9.16
DevHarnessKit version: 0.4.6-beta.1
```

## Final Gate Result

```text
release-gate: passed
mvn clean package: passed
tests: 277 run, 0 failures, 0 errors, 1 skipped
module boundary gate: passed
archive smoke: passed
packaged CLI smoke: passed
checksum generation: passed
```

## Coverage Snapshot

| Metric | Result |
| --- | ---: |
| Line coverage | 86.49% |
| Instruction coverage | 85.71% |
| Branch coverage | 66.54% |
| Lines covered | 18,099 |
| Lines missed | 2,827 |

Baseline before targeted test additions was 86.25% line coverage. The final
result improves the line coverage by 0.24 percentage points while adding
contract coverage for stable-core behavior and release governance checks.

## Lowest Package Coverage

| Package | Line coverage | Covered / Total |
| --- | ---: | ---: |
| `com.devharnesskit.dhk.command.projectdb` | 38.69% | 65 / 168 |
| `com.devharnesskit.dhk.command.checkpoint` | 66.10% | 78 / 118 |
| `com.devharnesskit.dhk.command.graph` | 70.11% | 319 / 455 |
| `com.devharnesskit.dhk.command.bdd` | 72.28% | 751 / 1,039 |
| `com.devharnesskit.dhk.service.config` | 75.82% | 185 / 244 |
| `com.devharnesskit.dhk.command.memory` | 78.93% | 427 / 541 |
| `com.devharnesskit.dhk.command.goal` | 86.51% | 750 / 867 |
| `com.devharnesskit.dhk.service.goal` | 86.29% | 3,016 / 3,495 |

## Stable Core Gaps

| Area | Current signal | Follow-up |
| --- | --- | --- |
| Memory core | Command package moved to 78.93%. | Added integration tests for backup explicit output, uninitialized backup failure, invalid confirm ids, and invalid confidence. |
| Goal core | Command package moved to 86.51%; `service.goal` remains 86.29%. | Added `goal recheck --json` contract coverage and `WorkspaceFingerprintService` unit coverage for workspace/context/check fingerprints. |
| Config / quickstart / status | `service.config` moved to 75.82%; `command.configure` moved to 90.75%. | Added tests for overrides, invalid JSON diagnostics, JSON doctor output, active goal status, and nested markdown write behavior. |
| CI gate | Threshold value previously appeared in several gate call sites. | Added `scripts/release-thresholds.env` and sourced it from local release gate, CI, and release workflow. |
| Structural boundary | Architecture docs listed known GoalCheckService/MigrationRunner debt, but there was no executable boundary check. | Added `scripts/check-module-boundaries.sh` and included it in local, CI, and release gates. |

## Decision

AI-168 through AI-174 were treated as hardening work, not emergency threshold
repair:

1. Keep the 60% gate unchanged.
2. Add targeted tests in memory, goal, and config/status/quickstart.
3. Centralize the coverage threshold used by release gate and CI.
4. Add structural boundary reporting so experimental surface does not silently
   leak into stable core.
