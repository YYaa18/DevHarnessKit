#!/usr/bin/env sh
set -eu

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
cd "$ROOT"

failures_file="$(mktemp)"
debt_file="$(mktemp)"
cleanup() {
  rm -f "$failures_file" "$debt_file"
}
trap cleanup EXIT

check_file() {
  file="$1"
  max_lines="$2"
  [ -f "$file" ] || {
    echo "missing file: $file" >> "$failures_file"
    return
  }
  lines="$(wc -l < "$file" | tr -d ' ')"
  if [ "$lines" -gt "$max_lines" ]; then
    echo "$file lines=$lines max=$max_lines" >> "$failures_file"
  fi
}

check_glob() {
  pattern="$1"
  max_lines="$2"
  found="false"
  for file in $pattern; do
    [ -e "$file" ] || continue
    found="true"
    check_file "$file" "$max_lines"
  done
  [ "$found" = "true" ] || echo "missing files for pattern: $pattern" >> "$failures_file"
}

check_file "src/main/java/com/devharnesskit/dhk/service/goal/GoalCheckService.java" 500
check_file "src/main/java/com/devharnesskit/dhk/db/MigrationRunner.java" 450
check_file "src/main/java/com/devharnesskit/dhk/service/goal/GoalOrchestrator.java" 650
check_file "src/main/java/com/devharnesskit/dhk/service/goal/GoalActionSyncService.java" 550
check_file "src/main/java/com/devharnesskit/dhk/service/goal/GoalCheckSupport.java" 450
check_glob "src/main/java/com/devharnesskit/dhk/service/goal/*GoalCheckRunner.java" 250
check_glob "src/main/java/com/devharnesskit/dhk/db/migration/V*Migration.java" 250

find src/main/java/com/devharnesskit/dhk -type f -name '*.java' -print \
  | while IFS= read -r file; do
      lines="$(wc -l < "$file" | tr -d ' ')"
      case "$file" in
        src/main/java/com/devharnesskit/dhk/service/goal/GoalCheckService.java|\
        src/main/java/com/devharnesskit/dhk/db/MigrationRunner.java|\
        src/main/java/com/devharnesskit/dhk/service/goal/GoalOrchestrator.java|\
        src/main/java/com/devharnesskit/dhk/service/goal/GoalActionSyncService.java)
          ;;
        *)
          if [ "$lines" -gt 650 ]; then
            echo "$file lines=$lines" >> "$debt_file"
          fi
          ;;
      esac
    done

if [ -s "$failures_file" ]; then
  echo "ERROR: class size gate failed" >&2
  cat "$failures_file" >&2
  exit 1
fi

echo "class size check"
echo "hard_violations: 0"
if [ -s "$debt_file" ]; then
  echo "known_large_class_debt:"
  sed 's/^/  - /' "$debt_file"
else
  echo "known_large_class_debt: none"
fi
echo "tracked_targets:"
echo "  - GoalCheckService.java <= 500"
echo "  - MigrationRunner.java <= 450"
echo "  - GoalOrchestrator.java <= 650"
echo "  - GoalActionSyncService.java <= 550"
echo "  - *GoalCheckRunner.java <= 250"
echo "  - V*Migration.java <= 250"
echo "result: passed"
