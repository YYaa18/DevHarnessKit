# legacy-mybatis-order

Synthetic legacy Java/MyBatis testbed for DevHarnessKit Graph-aware Goal
Harness evaluation.

This fixture models a small order search flow with:

- a legacy-style controller;
- service logic with pagination normalization;
- a mapper interface;
- a MyBatis XML mapper;
- DTO/VO duplication common in old systems;
- weak but runnable JUnit tests;
- protected production-like configuration files;
- three scored tasks with ground truth.

## Verify

```bash
mvn -q -f testbeds/fixtures/legacy-mybatis-order/pom.xml test
```

## Tasks

```text
tasks/add-customer-level-field
tasks/tighten-paid-order-filter
tasks/fix-null-page-bounds
```

Each task includes `task.md`, `ground-truth.json`, and `scorecard.md`.

## Protected Files

The fixture includes `.agents/devharness/policy.json` and
`.agents/devharness/sensitive-policy.json` to exercise protected file and
sensitive policy behavior in later graph-aware tests.
