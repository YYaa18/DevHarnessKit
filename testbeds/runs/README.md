# Evaluation Runs

This directory is for local evaluation outputs and is ignored by git except for
this README.

Suggested run layout:

```text
testbeds/runs/
  legacy-001/
    A-raw-agent/
      diff.patch
      result.json
      logs/
    C-goal-harness/
      diff.patch
      result.json
      logs/
    D-graph-aware-goal/
      diff.patch
      result.json
      logs/
    ground-truth.json
    scorecard.md
```

Commit only curated reports when a Linear issue explicitly asks for them.
