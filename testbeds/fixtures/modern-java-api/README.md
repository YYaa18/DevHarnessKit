# modern-java-api

Synthetic modern Java API testbed for DevHarnessKit Graph-aware Goal Harness
evaluation.

This fixture models a small account API with:

- controller-style route annotations;
- service and repository layers;
- DTO/domain separation;
- clear architecture boundary rules;
- weak but runnable JUnit tests;
- scored tasks for public API impact, boundary violations, and test gaps.

The project intentionally uses tiny local annotations instead of a web framework
so it stays fast, offline, and Java 8 friendly.

## Verify

```bash
mvn -q -f testbeds/fixtures/modern-java-api/pom.xml test
```

## Tasks

```text
tasks/add-risk-rating-field
tasks/add-freeze-endpoint-boundary
tasks/add-status-search-test-gap
```

Each task includes `task.md`, `ground-truth.json`, and `scorecard.md`.

## Architecture Rules

```text
controller -> service -> repository -> domain
controller must not depend on repository
repository must not depend on controller or service
```
