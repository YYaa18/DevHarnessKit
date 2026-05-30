# Database Compatibility

DevHarness Kit currently keeps `mysql:mysql-connector-java:5.1.49` on purpose.

The immediate compatibility target is:

```text
Production: MySQL Server 5.1
Local dev:  MySQL Server 8.x with a Connector/J 5.1-compatible readonly account
Driver:     mysql-connector-java 5.1.49
```

Do not upgrade the default driver unless the production database floor changes.

## Compatibility Strategy

DevHarness Kit should not guess compatibility from version numbers alone. Prefer:

- explicit `--jdbc-url` values for real environments;
- `dhk db test` capability probes before running SQL;
- lowest-common SQL features;
- read-only database credentials enforced by the database server.

## Compatibility Matrix

| Target | 1.0 status | Evidence type | Current evidence | Required constraints |
| --- | --- | --- | --- | --- |
| MySQL Server 5.1 production floor | Beta target | Real environment smoke evidence required | No bundled live MySQL 5.1 evidence is claimed by this repository unless a release artifact links a dated smoke log. Fixture/unit tests cover URL construction and probe formatting only. | Connector/J 5.1-compatible URL, lowest-common SQL, readonly database credentials, no CTE/window/JSON function assumptions. |
| MySQL Server 8 local target | Beta local target | Real environment smoke evidence required | Optional live smoke is available through `DHK_TEST_MYSQL_*`; when the variables are absent the test is skipped and does not prove MySQL 8 compatibility. | `mysql_native_password` account or explicit local-only compatibility flags, reviewed JDBC URL, readonly database credentials. |
| Connector/J 5.1.49 default driver | Included dependency | Fixture/unit coverage plus driver load checks | `pom.xml` pins `mysql:mysql-connector-java:5.1.49`; `dhk doctor` checks `com.mysql.jdbc.Driver`; unit tests cover generated JDBC URL flags. | Do not upgrade by default while MySQL 5.1 is the production floor. |
| `dhk db test` probes | Beta command | Fixture/unit and optional live smoke | SQLite fixture coverage proves probe output handles metadata and safe failures; live MySQL smoke must capture actual probe lines. | `probe_select_1: ok` is required before using `dhk db sql`; optional probes are diagnostic. |
| `dhk db sql` readonly execution | Beta command | Unit/integration dry-run plus optional live smoke | Dry-run and SQL safety tests do not connect to MySQL; live smoke is opt-in and must use readonly credentials. | SQL guard and JDBC read-only hints are guardrails, not permission boundaries. |

The matrix is intentionally evidence-bound. A fixture/unit test proves only the
local Java behavior named in the test. A real environment smoke proves only the
specific server, account, URL, driver, and probe output captured in that smoke
log.

## Automated And Manual Evidence Path

Automated fixture/unit coverage:

```bash
mvn -q -Dtest=MysqlConnectionServiceTest,DbCompatibilityProbeServiceTest,DbSqlDryRunIntegrationTest#dbTestRejectsNonMysqlJdbcUrlsBeforeConnecting test
```

This coverage is useful for CLI behavior, but it is not real MySQL 5.1 or MySQL
8 evidence.

Optional live MySQL smoke:

```bash
export DHK_TEST_MYSQL_URL="jdbc:mysql://127.0.0.1:3306/your_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&zeroDateTimeBehavior=convertToNull"
export DHK_TEST_MYSQL_USER="dhk_readonly"
export DHK_TEST_MYSQL_PASSWORD="<redacted>"
mvn -q -Dtest=DbSqlDryRunIntegrationTest#liveMysqlSmokeRunsWhenEnvironmentIsConfigured test
```

Manual smoke evidence can also be captured with:

```bash
dhk db test --json \
  --jdbc-url "<redacted-reviewed-jdbc-url>" \
  --user dhk_readonly \
  --password-env DHK_DB_PASSWORD
```

Store release evidence outside public docs unless it is redacted. A useful
smoke record includes:

- date, tester, target label, and whether it is MySQL 5.1 or MySQL 8;
- `database_product`, `database_version`, `driver_name`, and `driver_version`;
- `readonly_requested`, `readonly_effective`, and `probe_select_1`;
- `probe_explain_select_1`, `probe_show_tables`, and any compatibility hints;
- a clear note when a result is fixture/unit coverage rather than real MySQL.

Do not store passwords, full JDBC URLs, hostnames, tokens, or raw SQL result
sets in evidence files.

## Recommended MySQL 5.1 Production URL

Use a read-only account and keep the JDBC URL conservative:

```bash
dhk db test \
  --jdbc-url "jdbc:mysql://prod-host:3306/your_db?useUnicode=true&characterEncoding=utf8&useSSL=false&zeroDateTimeBehavior=convertToNull" \
  --user dhk_readonly \
  --password-env DHK_DB_PASSWORD
```

Notes:

- MySQL 5.1 does not support newer SQL features such as CTEs, window functions, or JSON functions.
- Keep DevHarness Kit SQL to `SELECT`, `SHOW`, `DESC`, `DESCRIBE`, and `EXPLAIN`.
- Do not connect with write-capable credentials.

## Recommended MySQL 8 Local Setup

Connector/J 5.1.49 can be used for local MySQL 8 only when the account and URL are compatible with the old driver.

Create a local readonly user with the legacy authentication plugin:

```sql
CREATE USER 'dhk_readonly'@'localhost'
IDENTIFIED WITH mysql_native_password BY 'your-password';

GRANT SELECT, SHOW VIEW ON your_db.* TO 'dhk_readonly'@'localhost';
```

Recommended local URL:

```bash
dhk db test \
  --jdbc-url "jdbc:mysql://127.0.0.1:3306/your_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&zeroDateTimeBehavior=convertToNull" \
  --user dhk_readonly \
  --password-env DHK_DB_PASSWORD
```

Only add `allowPublicKeyRetrieval=true` for local or disposable test databases, not production.

If you prefer `--host` and `--database` over a full JDBC URL, the CLI builds this conservative baseline:

```text
useUnicode=true
characterEncoding=utf8
zeroDateTimeBehavior=convertToNull
```

Optional compatibility flags:

```bash
dhk db test \
  --host 127.0.0.1 \
  --database your_db \
  --user dhk_readonly \
  --password-env DHK_DB_PASSWORD \
  --server-timezone Asia/Shanghai \
  --use-ssl false \
  --allow-public-key-retrieval \
  --jdbc-params "tinyInt1isBit=false"
```

Supported URL-building options:

```text
--server-timezone <zone>          appends serverTimezone=<zone>
--use-ssl <true|false>           appends useSSL=<value>
--character-encoding <encoding>  overrides the default characterEncoding=utf8
--zero-date-time-behavior <mode> overrides the default zeroDateTimeBehavior=convertToNull
--allow-public-key-retrieval     appends allowPublicKeyRetrieval=true
--jdbc-params <query-string>     appends additional raw JDBC query parameters
```

For production, prefer an explicit `--jdbc-url` that has been reviewed by your DBA or platform team.

## `dhk db test` Output

`dhk db test` prints connection and capability diagnostics:

```text
db_test: ok
database_product: MySQL
database_version: ...
driver_name: MySQL Connector Java
driver_version: mysql-connector-java-5.1.49 (...)
readonly_requested: true
readonly_effective: ...
probe_select_1: ok
probe_explain_select_1: ok
probe_show_tables: ok
server_version_query: ...
server_time_zone: ...
server_sql_mode: ...
server_version_comment: ...
```

Treat failed optional probes as compatibility information, not always as fatal errors. `probe_select_1` should be `ok` before using `dhk db sql`.

`dhk db test --json` emits the same probe lines as a `probes` array for scripts.

## Explain Plans

`dhk db sql --explain --sql "SELECT ..."` validates the input as a readonly `SELECT`, executes `EXPLAIN SELECT ...`, and returns the execution plan.

Connector/J 5.1 can reject `EXPLAIN SELECT ...` on MySQL 8 when JDBC `Connection.setReadOnly(true)` is enabled, even though execution plans are a normal development workflow. For compatibility, DevHarness Kit does not request the JDBC read-only hint for `EXPLAIN` statements. The SQL safety guard still only allows `EXPLAIN` around readonly statements, and users must still use database credentials that are read-only at the database server.

## Common Failures

### MySQL 8 `caching_sha2_password`

If connection fails with `caching_sha2_password`, the account is using the MySQL 8 default authentication plugin. Create the DevHarness Kit readonly account with `mysql_native_password`.

### Time Zone Errors

If connection fails with a server time zone error, add an explicit `serverTimezone` to `--jdbc-url`, for example:

```text
serverTimezone=Asia/Shanghai
```

### Zero Date Values

For older schemas that contain `0000-00-00` date values, include:

```text
zeroDateTimeBehavior=convertToNull
```

## Compatibility Rule

When production is MySQL 5.1, design DevHarness Kit DB features for MySQL 5.1 first. MySQL 8 local development should adapt through account setup and JDBC URL parameters, not through a default driver upgrade.
