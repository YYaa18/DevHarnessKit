# Skill Evaluation

Status: stable-candidate local report schema for governed skill runs.

Skill Evaluation compares governed skill runs against a simple local baseline.
For the 1.0 promotion track, the stable-candidate subset is the report field
contract documented below. It is not a statistical benchmark, release approval,
model evaluation, or replacement for tests, review, BDD evidence, goal checks,
or artifact passport verification.

## Commands

```bash
dhk skill score --goal <goal-key>
dhk skill report --goal <goal-key> --baseline-score <score> --group F
```

`skill score` reads local `goal_step` and `goal_check` rows. `skill report`
adds skill contract and trust metadata from `policy.json` and `skill_contract`.
Those storage details are internal; consumers should rely on CLI output fields,
not direct SQLite reads.

## Metrics

```text
gate_pass_rate
  Passed discipline gates divided by observed discipline gates.

evidence_completeness
  Percentage of required evidence groups present in goal step evidence:
  goal understanding, assumptions/pending, impacted files, risk points,
  verification plan, implementation summary, test result, compile result,
  sensitive result, rollback evidence.

rollback_quality
  100 when a meaningful rollback_plan or verified rollback_quality exists,
  partial when rollback evidence is present but weak, 0 when missing.

skill_quality_score
  40% gate_pass_rate + 40% evidence_completeness + 20% rollback_quality.

dqi_score
  For the alpha F-group report, this is the governed skill_quality_score.

dqi_delta
  dqi_score - baseline_score.
```

## Score Trajectory

The intended trajectory is:

```text
E group: BDD + Graph-aware Goal Harness
F group: E group + Skill Contract + Skill Gates + Skill Trust + Skill Score
```

A positive `dqi_delta` means the governed F-group run produced a higher local
quality score than the provided baseline. The number is only comparable within
the same fixture and scoring contract.

## Benchmark Fixture

The stable fixture lives at:

```text
src/test/resources/fixtures/skill-evaluation/f-group-benchmark.json
```

It fixes:

```text
group: F
baseline_score: 70
expected_skill_quality_score: 100
expected_dqi_delta: 30
```

Integration tests use this fixture to assert the report contract stays stable.

## Report Contract

Text and JSON reports include:

```text
goal_key
group
skill_key
skill_contract_present
skill_trusted
trust_status
skill_quality_score
dqi_score
baseline_score
dqi_delta
gate_pass_rate
evidence_completeness
rollback_quality
```

Stable-candidate consumers may rely on these field names, numeric score ranges
from `0` to `100`, and `dqi_delta = dqi_score - baseline_score`. They should
ignore unknown additive fields and must not compare scores across unrelated
fixtures, skill versions, or policy configurations.

## Outside The Contract

The following remain experimental:

- scoring weights beyond the documented local formula;
- DQI interpretation across real projects;
- benchmark group names beyond the fixture-backed `F` group;
- direct reads from `goal_step`, `goal_check`, `policy`, or `skill_contract`
  SQLite rows;
- using a score or positive delta as proof that code behavior is correct.
