#!/usr/bin/env sh
set -eu

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
cd "$ROOT"

hard_violations_file="$(mktemp)"
known_debt_file="$(mktemp)"
cleanup() {
  rm -f "$hard_violations_file" "$known_debt_file"
}
trap cleanup EXIT

find src/main/java/com/devharnesskit/dhk \
  -path '*/cli/*' -prune -o \
  -path '*/command/*' -prune -o \
  -name '*.java' -print \
  | xargs grep -n '^import com\.devharnesskit\.dhk\.command' \
  > "$hard_violations_file" 2>/dev/null || true

if [ -s "$hard_violations_file" ]; then
  echo "ERROR: non-command packages must not import CLI command packages" >&2
  cat "$hard_violations_file" >&2
  exit 1
fi

find src/main/java/com/devharnesskit/dhk/service/goal \
  -name '*.java' -print \
  | xargs grep -nE '^import com\.devharnesskit\.dhk\.(model|repository|service)\.(bdd|graph|skill|policy)' \
  > "$known_debt_file" 2>/dev/null || true

{
  echo "module boundary check"
  echo "hard_violations: 0"
  if [ -s "$known_debt_file" ]; then
    echo "known_goal_boundary_debt:"
    sed 's/^/  - /' "$known_debt_file"
  else
    echo "known_goal_boundary_debt: none"
  fi
  echo "result: passed"
}
