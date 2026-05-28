#!/usr/bin/env sh
set -eu

RUN_PACKAGE="true"
RUN_SMOKE="true"

usage() {
  cat <<'USAGE'
release-gate.sh

Stable-candidate release gate for DevHarnessKit.

Usage:
  ./scripts/release-gate.sh [--skip-package] [--skip-smoke]

Options:
  --skip-package  Reuse existing target artifacts instead of running mvn clean package -P release-archive.
  --skip-smoke    Skip archive and packaged CLI smoke checks.
  -h, --help      Show this help.
USAGE
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --skip-package) RUN_PACKAGE="false" ;;
    --skip-smoke) RUN_SMOKE="false" ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown option: $1" >&2; usage >&2; exit 2 ;;
  esac
  shift
done

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
cd "$ROOT"

. "$ROOT/scripts/release-thresholds.env"
: "${DHK_MIN_LINE_COVERAGE:=60}"

log() {
  printf '[release-gate] %s\n' "$*"
}

fail() {
  printf '[release-gate] ERROR: %s\n' "$*" >&2
  exit 1
}

require_file() {
  [ -f "$1" ] || fail "missing file: $1"
}

require_dir() {
  [ -d "$1" ] || fail "missing directory: $1"
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || fail "$1 is required"
}

require_zip_entry() {
  unzip -l "$ZIP" | grep -F "$1" >/dev/null || fail "zip missing $1"
}

require_tgz_entry() {
  tar -tzf "$TGZ" | grep -F "$1" >/dev/null || fail "tar.gz missing $1"
}

check_no_hardcoded_wrapper_version() {
  if grep -E 'target[\\/]+dhk-cli-[0-9][^ "$%]*-all\.jar' \
      .agents/skills/devharness-goal-development/scripts/dhk.sh \
      .agents/skills/devharness-goal-development/scripts/dhk.bat >/dev/null; then
    fail "goal wrappers must use dhk-cli-*-all.jar fallback, not a fixed release version"
  fi
}

VERSION="$(mvn -q -DforceStdout help:evaluate -Dexpression=project.version)"
[ -n "$VERSION" ] || fail "could not resolve Maven project.version"

JAR="target/dhk-cli-$VERSION-all.jar"
ZIP="target/devharnesskit-$VERSION.zip"
TGZ="target/devharnesskit-$VERSION.tar.gz"

log "version=$VERSION"

if [ "$RUN_PACKAGE" = "true" ]; then
  log "running mvn clean package -P release-archive"
  mvn -B clean package -P release-archive
fi

require_file "$JAR"
require_file "$ZIP"
require_file "$TGZ"
require_file "LICENSE"
require_file "THIRD_PARTY_NOTICES.md"
require_file "README.md"
require_file "docs/COMPATIBILITY.md"
require_file "docs/INDEX.md"
require_file "docs/STABLE_CONTRACT.md"
require_file "docs/STABLE_CANDIDATE.md"
require_file "scripts/check-version-metadata.sh"
require_file "scripts/check-coverage-threshold.sh"
require_file "scripts/check-module-boundaries.sh"
require_file "scripts/check-class-size.sh"
require_file "scripts/release-thresholds.env"

log "checking version smoke"
java -jar "$JAR" version | grep "$VERSION" >/dev/null || fail "version smoke did not print $VERSION"

log "checking version metadata"
scripts/check-version-metadata.sh "$VERSION" >/dev/null

log "checking coverage threshold"
scripts/check-coverage-threshold.sh "$DHK_MIN_LINE_COVERAGE" >/dev/null

log "checking module boundaries"
scripts/check-module-boundaries.sh >/dev/null

log "checking class size boundaries"
scripts/check-class-size.sh >/dev/null

log "checking status wording"
grep -n "developer preview" README.md RELEASE.md >/dev/null || fail "developer preview wording missing"
grep -n "not production-ready" README.md >/dev/null \
  || fail "production-readiness warning missing from README"
grep -n "do not describe it" README.md >/dev/null \
  || fail "not-stable warning missing from README"
grep -ni "Stable candidate surface" docs/STABLE_CANDIDATE.md >/dev/null \
  || fail "stable candidate contract missing"
grep -ni "Experimental surface" docs/STABLE_CANDIDATE.md >/dev/null \
  || fail "experimental surface contract missing"
grep -ni "Stable-Beta Surface" docs/STABLE_CONTRACT.md >/dev/null \
  || fail "stable-beta surface contract missing"
grep -ni "Stable JSON Fields" docs/STABLE_CONTRACT.md >/dev/null \
  || fail "stable JSON contract missing"
grep -ni "Stable Markdown Export Anchors" docs/STABLE_CONTRACT.md >/dev/null \
  || fail "stable Markdown export contract missing"
grep -ni "Experimental Outside The Contract" docs/STABLE_CONTRACT.md >/dev/null \
  || fail "stable contract experimental boundary missing"

log "checking wrapper version fallback"
check_no_hardcoded_wrapper_version

log "checking archive contents"
for entry in \
  "install.sh" \
  "lib/dhk.jar" \
  "LICENSE" \
  "THIRD_PARTY_NOTICES.md" \
  ".agents/skills/devharness-goal-development/SKILL.md" \
  ".agents/skills/devharness-goal-development/references/knowledge-injection.md" \
  ".agents/knowledge/packs/devharness-methodology-core/knowledge-pack.json" \
  ".agents/knowledge/packs/java-enterprise-core/knowledge-pack.json" \
  ".agents/skills/devharness-goal-development/scripts/dhk.sh" \
  ".agents/skills/devharness-goal-development/scripts/dhk.bat" \
  ".agents/skills/devharness-goal-development/scripts/goal-start.sh" \
  ".agents/skills/devharness-goal-development/scripts/goal-start.bat" \
  ".agents/skills/devharness-goal-development/scripts/goal-verify.sh" \
  ".agents/skills/devharness-goal-development/scripts/goal-verify.bat" \
  ".agents/skills/devharness-graph-aware-development/SKILL.md" \
  ".agents/skills/devharness-graph-aware-development/scripts/graph-impact.sh" \
  ".agents/skills/devharness-graph-aware-development/scripts/graph-impact.bat" \
  ".comate/rules/devharness-goal-protocol.mdr" \
  ".comate/rules/devharness-graph-aware-protocol.mdr" \
  "docs/INDEX.md" \
  "docs/STABLE_CONTRACT.md" \
  "scripts/check-version-metadata.sh" \
  "scripts/check-coverage-threshold.sh" \
  "scripts/check-module-boundaries.sh" \
  "scripts/check-class-size.sh" \
  "scripts/release-thresholds.env" \
  "scripts/devharness-control-panel.sh" \
  "scripts/install-agent-adapters.sh"
do
  require_zip_entry "$entry"
  require_tgz_entry "$entry"
done
if unzip -l "$ZIP" | grep 'devharness-java-development' >/dev/null; then
  fail "zip still contains legacy devharness-java-development skill"
fi
if unzip -l "$ZIP" | grep -E 'project-memory-bootstrap.mdr|java-development-guard.mdr' >/dev/null; then
  fail "zip still contains legacy Comate memory-first rules"
fi
if tar -tzf "$TGZ" | grep 'devharness-java-development' >/dev/null; then
  fail "tar.gz still contains legacy devharness-java-development skill"
fi
if tar -tzf "$TGZ" | grep -E 'project-memory-bootstrap.mdr|java-development-guard.mdr' >/dev/null; then
  fail "tar.gz still contains legacy Comate memory-first rules"
fi

if [ "$RUN_SMOKE" = "true" ]; then
  log "running archive smoke"
  ARCHIVE_ROOT="$(mktemp -d)"
  SMOKE_ROOT="$(mktemp -d)"
  cleanup() {
    rm -rf "$ARCHIVE_ROOT" "$SMOKE_ROOT"
  }
  trap cleanup EXIT
  unzip -q "$ZIP" -d "$ARCHIVE_ROOT"
  DIST="$ARCHIVE_ROOT/devharnesskit-$VERSION"
  require_dir "$DIST"
  java -jar "$DIST/lib/dhk.jar" version | grep "$VERSION" >/dev/null \
    || fail "archive jar version smoke failed"
  "$DIST/install.sh" --help | grep "DevHarnessKit 安装向导" >/dev/null \
    || fail "archive root installer help smoke failed"
  grep -F 'dhk-cli-*-all.jar' "$DIST/.agents/skills/devharness-goal-development/scripts/dhk.sh" >/dev/null \
    || fail "archive Unix goal wrapper missing dynamic target jar fallback"
  grep -F 'dhk-cli-*-all.jar' "$DIST/.agents/skills/devharness-goal-development/scripts/dhk.bat" >/dev/null \
    || fail "archive Windows goal wrapper missing dynamic target jar fallback"
  "$DIST/.agents/skills/devharness-goal-development/scripts/dhk.sh" version | grep "$VERSION" >/dev/null \
    || fail "archive goal wrapper version smoke failed"
  "$DIST/scripts/devharness-control-panel.sh" plan --project-root "$DIST" --target all --dry-run >/dev/null
  "$DIST/scripts/install-agent-adapters.sh" --project-root "$DIST" --target all --force --dry-run >/dev/null

  log "running packaged CLI smoke"
  java -jar "$JAR" memory init --project-root "$SMOKE_ROOT" >/dev/null
  java -jar "$JAR" doctor --project-root "$SMOKE_ROOT" --json | grep '"command": "doctor"' >/dev/null
  java -jar "$JAR" memory export --project-root "$SMOKE_ROOT" --task "release smoke" --module global --json \
    | grep '"command": "memory export"' >/dev/null
  java -jar "$JAR" configure init --project-root "$SMOKE_ROOT" --preset springboot-manual-ide-test --force >/dev/null
  java -jar "$JAR" configure show --project-root "$SMOKE_ROOT" --json | grep '"compile_mode": "manual"' >/dev/null
fi

log "generating checksums"
cd target
if command -v sha256sum >/dev/null 2>&1; then
  sha256sum "dhk-cli-$VERSION-all.jar" "devharnesskit-$VERSION.zip" "devharnesskit-$VERSION.tar.gz" > SHA256SUMS
elif command -v shasum >/dev/null 2>&1; then
  shasum -a 256 "dhk-cli-$VERSION-all.jar" "devharnesskit-$VERSION.zip" "devharnesskit-$VERSION.tar.gz" > SHA256SUMS
else
  fail "sha256sum or shasum is required"
fi

log "passed"
