#!/usr/bin/env sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../../../.." && pwd)
JAR="$PROJECT_ROOT/.agents/tools/devharness-kit/dhk.jar"
if [ ! -f "$JAR" ]; then
  JAR="$PROJECT_ROOT/lib/dhk.jar"
fi
if [ ! -f "$JAR" ]; then
  JAR="$PROJECT_ROOT/target/dhk-cli-0.1.0-alpha-all.jar"
fi

if [ ! -f "$JAR" ]; then
  echo "DevHarness Kit jar not found. Run: mvn -DskipTests package" >&2
  exit 4
fi

if [ "${DHK_JAVA_OPTS:-}" = "" ]; then
  DHK_JAVA_OPTS="-Xms16m -Xmx128m -Dfile.encoding=UTF-8"
fi

HAS_PROJECT_ROOT=0
for arg in "$@"; do
  case "$arg" in
    --project-root|--project-root=*)
      HAS_PROJECT_ROOT=1
      ;;
  esac
done

if [ "$HAS_PROJECT_ROOT" = "1" ]; then
  exec java $DHK_JAVA_OPTS -jar "$JAR" "$@"
fi

exec java $DHK_JAVA_OPTS -jar "$JAR" "$@" --project-root "$PROJECT_ROOT"
