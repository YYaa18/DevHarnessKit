# Decision 0001: Module Split and Installer Strategy

Date: 2026-05-25

Status: Accepted for `0.1.x`; revisit before `0.2.x`.

## Context

DevHarness Kit currently ships one CLI artifact:

```text
dhk-cli-0.1.0-alpha-all.jar
```

The shaded jar includes:

- DevHarness Kit source under MIT;
- `org.xerial:sqlite-jdbc` for local SQLite storage;
- `mysql:mysql-connector-java:5.1.49` for optional readonly MySQL inspection.

Keeping Connector/J 5.1.49 is intentional while the production database floor is
MySQL Server 5.1. Local MySQL 8 development is handled through compatible
account setup and JDBC URL parameters, not through a default driver upgrade.

The project also generates release archives:

```text
target/devharnesskit-0.1.0-alpha.zip
target/devharnesskit-0.1.0-alpha.tar.gz
```

These archives include the shaded jar, scripts, agent skill/rule packaging,
license, notices, and documentation.

## Decision

Do not split the Maven project or introduce installers in `0.1.x`.

The `0.1.x` release path remains:

```text
single CLI jar + zip/tar.gz release archives + explicit third-party notices
```

The project should keep documenting the future split path, but defer the actual
work until DB readonly behavior, release governance, and compatibility contracts
are more stable.

## Considered Options

### Option A: Keep One Shaded CLI Jar For Alpha

Pros:

- simplest path for local users and agent wrapper scripts;
- least disruption while goal/workflow/spec are still alpha;
- preserves the current MySQL 5.1 compatibility target;
- avoids Maven multi-module churn before public alpha release quality is proven.

Cons:

- memory-only users still receive MySQL Connector/J in the shaded binary;
- binary redistribution requires clear third-party dependency notices;
- optional DB licensing boundaries remain documentation-based rather than
  artifact-based.

### Option B: Split Into `dhk-core`, `dhk-db`, and `dhk-full` Now

Pros:

- clearer dependency and licensing boundaries;
- memory-only users could avoid the MySQL driver;
- future package managers could expose a smaller default artifact.

Cons:

- changes build, release, wrapper, test, CI, and documentation surfaces at once;
- risks delaying alpha readiness for a distribution concern;
- may force premature API boundaries while goal/workflow/spec contracts are still
  moving;
- adds more artifacts to verify on Java 8, Java 17, macOS, Windows, and Linux.

### Option C: Start Homebrew, Scoop, SDKMAN, Or Global Installers Now

Pros:

- easier installation for some users;
- creates a familiar CLI distribution story.

Cons:

- expands support obligations before the CLI contract is stable;
- makes rollback and migration guidance more important before it is proven;
- can hide the shaded dependency notice if packaging is not carefully designed.

## Consequences

For `0.1.x`:

- release archives are the only supported distribution packaging beyond source;
- no Homebrew, Scoop, SDKMAN, or global installer support is claimed;
- users should run the jar or archive scripts directly;
- release notes must continue to mention third-party dependencies in the shaded
  jar;
- DB readonly remains beta and requires database-level read-only credentials.

For `0.2.x` planning:

- evaluate a memory-only artifact after DB readonly command contracts settle;
- keep Connector/J 5.1 as default while MySQL Server 5.1 remains the production
  floor;
- identify whether users actually need a DB-free binary before adding build
  complexity;
- prefer a simple archive-based install script before ecosystem package managers.

For `1.0` planning:

- split optional modules if licensing, redistribution, package size, or user
  demand makes the boundary worth the extra release surface;
- define artifact names and compatibility promises before publishing stable
  installers;
- ensure every published artifact includes `LICENSE`, `THIRD_PARTY_NOTICES.md`,
  checksums, and release notes.

## Revisit Triggers

Revisit this decision when any of these are true:

- DB readonly graduates from beta toward stable;
- users ask for a memory-only binary;
- binary redistribution needs stricter dependency separation;
- package size or startup behavior becomes a real issue;
- MySQL Server 5.1 is no longer the production compatibility floor;
- the project is preparing a non-alpha public installer.
