#!/usr/bin/env sh
set -eu

# Developer validation helper. Requires the sqlite3 CLI to bulk-load
# sample memory rows; runtime DevHarness Kit commands do not require it.

JAR="${1:-target/dhk-cli-0.1.0-all.jar}"
if [ ! -f "$JAR" ]; then
  echo "Jar not found: $JAR" >&2
  echo "Run: mvn -DskipTests package" >&2
  exit 4
fi

if ! command -v sqlite3 >/dev/null 2>&1; then
  echo "sqlite3 is required for the perf sample loader." >&2
  exit 4
fi

ROOT="${TMPDIR:-/tmp}/dhk-perf-smoke-$$"
mkdir -p "$ROOT"
cleanup() {
  rm -rf "$ROOT"
}
trap cleanup EXIT

run_timed() {
  label="$1"
  shift
  start="$(date +%s)"
  "$@" >/dev/null
  end="$(date +%s)"
  echo "$label: $((end - start))s"
}

run_timed "help" java -jar "$JAR" help
run_timed "memory init" java -jar "$JAR" memory init --project-root "$ROOT"

PROJECT_KEY="$(sed -n 's/.*"project_key"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "$ROOT/.agents/memory/project.json")"
NOW="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
DB="$ROOT/.agents/memory/memory.db"

sqlite3 "$DB" <<SQL
BEGIN;
WITH RECURSIVE seq(x) AS (
  SELECT 1
  UNION ALL
  SELECT x + 1 FROM seq WHERE x < 1000
)
INSERT INTO memory_item(project_key, module_name, memory_type, scope, title, content, tags,
  status, confidence, source_kind, confirmed_at, confirmed_by, source_files, evidence,
  effective_from, effective_to, created_at, updated_at, last_used_at, use_count)
SELECT '$PROJECT_KEY', 'global', 'project_fact', 'project',
  'Perf sample ' || x,
  'Perf content sample ' || x,
  CASE WHEN x = 1 THEN 'gateway-special,perf' ELSE 'perf' END,
  'confirmed', 90, 'manual', '$NOW', 'perf', '', '', '', '', '$NOW', '$NOW', NULL, 0
FROM seq;
COMMIT;
SQL

run_timed "memory search 1000" java -jar "$JAR" memory search --project-root "$ROOT" --q gateway-special --status confirmed
run_timed "memory export 1000" java -jar "$JAR" memory export --project-root "$ROOT" --task "perf smoke" --keywords gateway-special

if pgrep -fl "dhk-cli|devharnesskit|dhk.jar|java -jar $JAR" >/dev/null 2>&1; then
  echo "Possible lingering dhk Java process detected." >&2
  exit 1
fi

ls -lh "$JAR"
