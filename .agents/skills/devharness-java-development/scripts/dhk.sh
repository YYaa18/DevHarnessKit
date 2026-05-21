#!/usr/bin/env sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../../../.." && pwd)
JAR="$PROJECT_ROOT/.agents/tools/devharness-kit/dhk.jar"
if [ ! -f "$JAR" ]; then
  JAR="$PROJECT_ROOT/target/dhk-cli-0.1.0-all.jar"
fi

if [ ! -f "$JAR" ]; then
  echo "DevHarness Kit jar not found. Run: mvn -DskipTests package" >&2
  exit 4
fi

if [ "${DHK_JAVA_OPTS:-}" = "" ]; then
  DHK_JAVA_OPTS="-Xms16m -Xmx128m -Dfile.encoding=UTF-8"
fi

exec java $DHK_JAVA_OPTS -jar "$JAR" "$@"
