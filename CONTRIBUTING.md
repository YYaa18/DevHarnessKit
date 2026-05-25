# Contributing

Thanks for helping make DevHarness Kit more dependable.

## Development Setup

Requirements:

- Java 8+ runtime for compatibility testing.
- Maven for build and tests.

Run:

```bash
mvn clean test
mvn -DskipTests package
java -jar target/dhk-cli-0.1.0-alpha-all.jar version
```

## Pull Request Expectations

- Keep changes scoped to the requested behavior.
- Add or update tests for command behavior, persistence changes, and safety checks.
- Do not weaken sensitive-data or SQL safety behavior without a security note.
- Update README or docs when command behavior, schema behavior, or release packaging changes.
- Avoid introducing daemons, HTTP listeners, ORMs, dependency injection frameworks, or LLM API calls into the core CLI.

## Schema Changes

Schema changes must update:

- `MigrationRunner`;
- migration tests or old-db fixture tests;
- [docs/MIGRATIONS.md](docs/MIGRATIONS.md);
- README schema status when user-visible.

## Command Output

The CLI is still alpha, but output should remain predictable. If output changes in a way scripts may observe, call it out in the PR.

## Local Smoke

After packaging, run:

```bash
scripts/perf-smoke.sh
```

This smoke requires the `sqlite3` CLI. The runtime CLI itself does not.
