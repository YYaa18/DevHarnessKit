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
useSSL=false
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
  --allow-public-key-retrieval \
  --jdbc-params "tinyInt1isBit=false"
```

Supported URL-building options:

```text
--server-timezone <zone>          appends serverTimezone=<zone>
--use-ssl <true|false>           overrides the default useSSL=false
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
