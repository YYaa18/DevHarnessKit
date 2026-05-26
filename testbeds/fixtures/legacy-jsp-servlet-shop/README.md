# legacy-jsp-servlet-shop

Synthetic legacy JSP/Servlet shop testbed for DevHarnessKit Graph-aware Goal
Harness evaluation.

This fixture models an old order search page with:

- a JSP form and include;
- `web.xml` URL mappings;
- a legacy servlet/controller class;
- service and DAO layers;
- SQL stored in a resource file;
- DTO/VO duplication common in older web systems;
- weak but runnable JUnit tests;
- protected production-like configuration and base classes;
- scored tasks with ground truth.

## Verify

```bash
mvn -q -f testbeds/fixtures/legacy-jsp-servlet-shop/pom.xml test
```

## Tasks

```text
tasks/add-customer-tier-filter
tasks/preserve-legacy-search-action
```

Each task includes `task.md`, `ground-truth.json`, and `scorecard.md`.

## Protected Files

The fixture includes `.agents/devharness/policy.json` and
`.agents/devharness/sensitive-policy.json` to exercise protected file,
manual-evidence, and sensitive-policy behavior in later graph-aware tests.
