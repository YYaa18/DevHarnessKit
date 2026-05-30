#!/usr/bin/env sh
set -eu

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
cd "$ROOT"

MVN="${MVN:-mvn}"
RESULT_DIR="${DHK_GRAPH_PRECISION_RESULT_DIR:-testbeds/runs/graph-lite-precision/latest}"
RESULT_JSON="${DHK_GRAPH_PRECISION_RESULT_JSON:-$RESULT_DIR/result.json}"

log() {
  printf '[graph-lite-precision] %s\n' "$*"
}

fail() {
  printf '[graph-lite-precision] ERROR: %s\n' "$*" >&2
  exit 1
}

command -v "$MVN" >/dev/null 2>&1 || fail "$MVN is required"

log "running curated Graph Lite precision suite"
mkdir -p "$(dirname "$RESULT_JSON")"
"$MVN" -q -Dtest=GraphLitePrecisionSuiteTest \
  -Ddhk.graph.precision.result="$RESULT_JSON" test
[ -f "$RESULT_JSON" ] || fail "missing result JSON: $RESULT_JSON"
grep -F '"schema_version": "devharness-graph-lite-precision-result/v1"' "$RESULT_JSON" >/dev/null \
  || fail "result JSON schema marker missing"
grep -F '"cases": [' "$RESULT_JSON" >/dev/null || fail "result JSON cases missing"
log "result_json=$RESULT_JSON"
log "passed"
