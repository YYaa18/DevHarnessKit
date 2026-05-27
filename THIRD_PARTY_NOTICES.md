# Third-Party Notices

DevHarness Kit source code is licensed under the MIT License.

The shaded binary jar, `dhk-cli-<version>-all.jar`, bundles third-party runtime dependencies. Those dependencies remain under their own licenses.

This file is a dependency notice for users of the source repository and generated release artifacts. It is not legal advice.

## Runtime Dependencies Included in the Shaded Jar

| Component | Version | License | Usage |
| --- | --- | --- | --- |
| `org.xerial:sqlite-jdbc` | `3.53.1.0` | Apache License 2.0 | Local SQLite storage for project memory, workflow state, and spec state. |
| `mysql:mysql-connector-java` | `5.1.49` | GPL v2 with FOSS exception | Optional readonly MySQL inspection through JDBC. |

## Test-Only Dependencies

| Component | Version | License | Usage |
| --- | --- | --- | --- |
| `org.junit.jupiter:junit-jupiter` | `5.10.2` | Eclipse Public License 2.0 | Unit and integration tests only. Not included as a runtime dependency of the CLI. |

## Binary Distribution Note

The repository license is MIT, but the shaded jar is a combined binary distribution that includes the dependencies above. Release notes and GitHub release pages should state that:

- the DevHarness Kit project code is MIT licensed;
- the shaded jar includes SQLite JDBC under Apache 2.0;
- the shaded jar includes MySQL Connector/J 5.1.49 under GPL v2 with FOSS exception;
- users who do not need MySQL inspection should still review the dependency notice before redistributing the shaded jar.

Future releases may split the project into `dhk-core` and optional DB artifacts so memory-only users can avoid bundling MySQL Connector/J. The current decision is documented in [docs/decisions/0001-module-split-and-installer-strategy.md](docs/decisions/0001-module-split-and-installer-strategy.md): keep one shaded alpha jar for `0.1.x`, then revisit the split before DB readonly or installer distribution is promoted.
