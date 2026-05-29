#!/usr/bin/env sh
set -eu

# Developer validation helper. Requires the sqlite3 CLI to bulk-load
# sample memory rows; runtime DevHarness Kit commands do not require it.

JAR="${1:-}"
if [ -z "$JAR" ]; then
  JAR="$(ls target/dhk-cli-*-all.jar 2>/dev/null | head -n 1 || true)"
fi
if [ ! -f "$JAR" ]; then
  echo "Jar not found: $JAR" >&2
  echo "Run: mvn -DskipTests package" >&2
  exit 4
fi

if ! command -v sqlite3 >/dev/null 2>&1; then
  echo "sqlite3 is required for the perf sample loader." >&2
  exit 4
fi

if ! command -v perl >/dev/null 2>&1; then
  echo "perl is required for millisecond perf timing." >&2
  exit 4
fi

ROOT="${TMPDIR:-/tmp}/dhk-perf-smoke-$$"
mkdir -p "$ROOT"
cleanup() {
  rm -rf "$ROOT"
}
trap cleanup EXIT

MAX_HELP_MS="${DHK_PERF_MAX_HELP_MS:-1500}"
MAX_DOCTOR_MS="${DHK_PERF_MAX_DOCTOR_MS:-2000}"
MAX_MEMORY_SEARCH_MS="${DHK_PERF_MAX_MEMORY_SEARCH_MS:-1000}"
MAX_MEMORY_EXPORT_MS="${DHK_PERF_MAX_MEMORY_EXPORT_MS:-2000}"
JAR_WARN_BYTES="${DHK_PERF_JAR_WARN_BYTES:-26214400}"
JAR_MAX_BYTES="${DHK_PERF_JAR_MAX_BYTES:-36700160}"

now_ms() {
  perl -MTime::HiRes=time -e 'printf "%.0f\n", time() * 1000'
}

run_timed() {
  label="$1"
  max_ms="$2"
  shift
  shift
  start="$(now_ms)"
  "$@" >/dev/null
  end="$(now_ms)"
  elapsed_ms=$((end - start))
  echo "$label: ${elapsed_ms}ms (max ${max_ms}ms)"
  if [ "$elapsed_ms" -gt "$max_ms" ]; then
    echo "ERROR: $label exceeded performance budget: ${elapsed_ms}ms > ${max_ms}ms" >&2
    exit 1
  fi
}

jar_size="$(wc -c < "$JAR" | tr -d ' ')"
echo "jar size: ${jar_size} bytes"
if [ "$jar_size" -gt "$JAR_MAX_BYTES" ]; then
  echo "ERROR: jar size exceeds maximum budget: ${jar_size} bytes > ${JAR_MAX_BYTES} bytes" >&2
  exit 1
fi
if [ "$jar_size" -gt "$JAR_WARN_BYTES" ]; then
  echo "WARNING: jar size exceeds target budget: ${jar_size} bytes > ${JAR_WARN_BYTES} bytes" >&2
fi

run_timed "help" "$MAX_HELP_MS" java -jar "$JAR" help
run_timed "memory init" "$MAX_DOCTOR_MS" java -jar "$JAR" memory init --project-root "$ROOT"
run_timed "doctor" "$MAX_DOCTOR_MS" java -jar "$JAR" doctor --project-root "$ROOT"

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

run_timed "memory search 1000" "$MAX_MEMORY_SEARCH_MS" java -jar "$JAR" memory search --project-root "$ROOT" --q gateway-special --status confirmed
run_timed "memory export 1000" "$MAX_MEMORY_EXPORT_MS" java -jar "$JAR" memory export --project-root "$ROOT" --task "perf smoke" --keywords gateway-special

LINGERING_JAVA="$(pgrep -fl java 2>/dev/null \
  | grep -F "java -jar $JAR" \
  | grep -v -F "grep -F" || true)"
if [ -n "$LINGERING_JAVA" ]; then
  echo "Possible lingering dhk Java process detected." >&2
  printf '%s\n' "$LINGERING_JAVA" >&2
  exit 1
fi

ls -lh "$JAR"
