# Goal-only Baseline Samples

Tracked baseline samples for V0.4.4 Graph Baseline Evaluation.

These files are intentionally small manual scoring exercises. They are not a
large model benchmark and should not be treated as statistically significant.
They establish a concrete before-graph reference for:

- DQI scoring shape;
- impact recall and precision;
- evidence quality;
- safety and boundary discipline;
- where Graph Lite should improve Goal-only work.

Runtime-generated experiment output belongs in `testbeds/runs/`, which is
ignored by git except for its README.

## Samples

```text
legacy-mybatis-order/fix-null-page-bounds
modern-java-api/add-status-search-test-gap
```

Each sample contains:

- `result.json`: machine-readable result summary.
- `scorecard.md`: evaluator notes and category scores.
