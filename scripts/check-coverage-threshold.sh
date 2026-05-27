#!/usr/bin/env sh
set -eu

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
cd "$ROOT"

MIN_LINE_COVERAGE="${1:-60}"
CSV="target/site/jacoco/jacoco.csv"

[ -f "$CSV" ] || {
  echo "ERROR: missing JaCoCo CSV report: $CSV" >&2
  echo "Run mvn test before the release gate." >&2
  exit 1
}

LINE_COVERAGE="$(awk -F, '
  NR > 1 {
    missed += $8;
    covered += $9;
  }
  END {
    total = missed + covered;
    if (total <= 0) {
      print "0.00";
      exit 2;
    }
    printf "%.2f", (covered * 100.0) / total;
  }
' "$CSV")" || {
  echo "ERROR: could not compute JaCoCo line coverage from $CSV" >&2
  exit 1
}

awk -v coverage="$LINE_COVERAGE" -v minimum="$MIN_LINE_COVERAGE" '
  BEGIN {
    exit((coverage + 0.0) >= (minimum + 0.0) ? 0 : 1);
  }
' || {
  echo "ERROR: JaCoCo line coverage ${LINE_COVERAGE}% is below ${MIN_LINE_COVERAGE}%" >&2
  exit 1
}

echo "line coverage ok: ${LINE_COVERAGE}% >= ${MIN_LINE_COVERAGE}%"
