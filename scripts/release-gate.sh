#!/usr/bin/env sh
set -eu

RUN_PACKAGE="true"
RUN_SMOKE="true"
RUN_GRAPH_PRECISION="true"

usage() {
  cat <<'USAGE'
release-gate.sh

Stable-candidate release gate for DevHarnessKit.

Usage:
  ./scripts/release-gate.sh [--skip-package] [--skip-smoke] [--skip-graph-lite-precision] [--with-graph-lite-precision]

Options:
  --skip-package                Reuse existing target artifacts instead of running mvn clean package -P release-archive.
  --skip-smoke                  Skip archive and packaged CLI smoke checks.
  --skip-graph-lite-precision   Skip the default curated Graph Lite precision suite.
  --with-graph-lite-precision   Compatibility no-op; the precision suite runs by default.
  -h, --help                    Show this help.
USAGE
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --skip-package) RUN_PACKAGE="false" ;;
    --skip-smoke) RUN_SMOKE="false" ;;
    --skip-graph-lite-precision) RUN_GRAPH_PRECISION="false" ;;
    --with-graph-lite-precision) RUN_GRAPH_PRECISION="true" ;;
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

require_no_zip_entry() {
  if unzip -l "$ZIP" | grep -F "$1" >/dev/null; then
    fail "zip should not contain $1"
  fi
}

require_no_tgz_entry() {
  if tar -tzf "$TGZ" | grep -F "$1" >/dev/null; then
    fail "tar.gz should not contain $1"
  fi
}

require_doc_contains() {
  grep -F "$2" "$1" >/dev/null || fail "$1 missing stable contract text: $2"
}

require_output_contains() {
  printf '%s\n' "$1" | grep -F "$2" >/dev/null || fail "$3 missing expected JSON field: $2"
}

json_string_value() {
  printf '%s\n' "$1" | sed -n 's/.*"'$2'": "\([^"]*\)".*/\1/p' | head -n 1
}

run_skill_audit_json() {
  set +e
  output="$(java -jar "$JAR" skill audit --project-root "$1" --path "$2" --json)"
  status=$?
  set -e
  if [ "$status" -ne 0 ] && [ "$status" -ne 3 ]; then
    fail "skill audit failed unexpectedly with exit_code=$status"
  fi
  printf '%s\n' "$output"
}

check_no_hardcoded_wrapper_version() {
  if grep -E 'target[\\/]+dhk-cli-[0-9][^ "$%]*-all\.jar' \
      .agents/skills/devharness-goal-development/scripts/dhk.sh \
      .agents/skills/devharness-goal-development/scripts/dhk.bat >/dev/null; then
    fail "goal wrappers must use dhk-cli-*-all.jar fallback, not a fixed release version"
  fi
}

is_prerelease() {
  case "$VERSION" in
    *-*) return 0 ;;
    *) return 1 ;;
  esac
}

release_channel() {
  if is_prerelease; then
    printf 'prerelease'
  else
    printf 'stable'
  fi
}

write_artifact_manifest() {
  channel="$(release_channel)"
  cat > "$MANIFEST" <<JSON
{
  "schema_version": "devharness-release-artifacts/v1",
  "version": "$VERSION",
  "release_channel": "$channel",
  "version_source": "maven:project.version",
  "generated_by": "scripts/release-gate.sh",
  "checksum_algorithm": "sha256",
  "checksum_file": "target/SHA256SUMS",
  "artifacts": [
    { "path": "target/dhk-cli-$VERSION-all.jar", "kind": "shaded_cli_jar", "checksum_required": true },
    { "path": "target/devharnesskit-$VERSION.zip", "kind": "release_archive_zip", "checksum_required": true },
    { "path": "target/devharnesskit-$VERSION.tar.gz", "kind": "release_archive_targz", "checksum_required": true },
    { "path": "target/ARTIFACT_MANIFEST.json", "kind": "artifact_manifest", "checksum_required": true }
  ],
  "archive_contract": {
    "package_root": "devharnesskit-$VERSION",
    "entrypoints": [
      "install.sh",
      "scripts/install-agent-adapters.sh",
      "scripts/devharness-control-panel.sh",
      "lib/dhk.jar"
    ],
    "excluded_entries": [
      "docs/",
      "RELEASE.md",
      "CONTRIBUTING.md",
      "CODE_OF_CONDUCT.md"
    ]
  }
}
JSON
}

require_manifest_contains() {
  grep -F "$1" "$MANIFEST" >/dev/null || fail "artifact manifest missing: $1"
}

verify_artifact_manifest() {
  require_file "$MANIFEST"
  for text in \
    "\"schema_version\": \"devharness-release-artifacts/v1\"" \
    "\"version\": \"$VERSION\"" \
    "\"release_channel\": \"$(release_channel)\"" \
    "\"checksum_algorithm\": \"sha256\"" \
    "\"checksum_file\": \"target/SHA256SUMS\"" \
    "\"path\": \"target/dhk-cli-$VERSION-all.jar\"" \
    "\"path\": \"target/devharnesskit-$VERSION.zip\"" \
    "\"path\": \"target/devharnesskit-$VERSION.tar.gz\"" \
    "\"path\": \"target/ARTIFACT_MANIFEST.json\"" \
    "\"install.sh\"" \
    "\"scripts/install-agent-adapters.sh\"" \
    "\"scripts/devharness-control-panel.sh\"" \
    "\"lib/dhk.jar\"" \
    "\"docs/\""
  do
    require_manifest_contains "$text"
  done
}

VERSION="$(mvn -q -DforceStdout help:evaluate -Dexpression=project.version)"
[ -n "$VERSION" ] || fail "could not resolve Maven project.version"

JAR="target/dhk-cli-$VERSION-all.jar"
ZIP="target/devharnesskit-$VERSION.zip"
TGZ="target/devharnesskit-$VERSION.tar.gz"
MANIFEST="target/ARTIFACT_MANIFEST.json"

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
require_file "scripts/graph-lite-precision-suite.sh"
require_file "scripts/release-thresholds.env"
require_file ".agents/skills/devharness-goal-development/contract.json"

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

log "running Routine CI privacy smoke"
mvn -q -Dtest=RoutineReportServiceTest#ciSafeExportWritesOnlyAggregateArtifacts+ciSafeExportRejectsSensitiveAggregateLabels test >/dev/null

log "running BDD stable acceptance smoke"
mvn -q -Dtest=BddIntegrationTest#bddInitAddListShowAndJsonOutput+addressDeliveryValidationEndToEndBddAcceptancePassesGoalCheck test >/dev/null

if [ "$RUN_GRAPH_PRECISION" = "true" ]; then
  log "running Graph Lite precision suite"
  DHK_GRAPH_PRECISION_RESULT_DIR="${DHK_GRAPH_PRECISION_RESULT_DIR:-target/graph-lite-precision}" \
    scripts/graph-lite-precision-suite.sh
fi

log "checking built-in skill governance gate"
BUILTIN_GOAL_SKILL=".agents/skills/devharness-goal-development"
SKILL_GATE_ROOT="target/release-gate-skill-governance"
rm -rf "$SKILL_GATE_ROOT"
mkdir -p "$SKILL_GATE_ROOT"
SKILL_LINT_JSON="$(java -jar "$JAR" skill lint --path "$BUILTIN_GOAL_SKILL" --json)"
require_output_contains "$SKILL_LINT_JSON" '"command": "skill lint"' "skill lint"
require_output_contains "$SKILL_LINT_JSON" '"status": "passed"' "skill lint"
require_output_contains "$SKILL_LINT_JSON" '"missing": 0' "skill lint"
require_output_contains "$SKILL_LINT_JSON" '"invalid": 0' "skill lint"
require_output_contains "$SKILL_LINT_JSON" '"forbidden": 0' "skill lint"
require_output_contains "$SKILL_LINT_JSON" '"source_hash": "sha256:' "skill lint"
SKILL_VERIFY_JSON="$(java -jar "$JAR" skill verify --project-root "$SKILL_GATE_ROOT" --path "$BUILTIN_GOAL_SKILL" --json)"
require_output_contains "$SKILL_VERIFY_JSON" '"command": "skill verify"' "skill verify"
require_output_contains "$SKILL_VERIFY_JSON" '"status": "passed"' "skill verify"
require_output_contains "$SKILL_VERIFY_JSON" '"trust_status": "unknown"' "skill verify"
require_output_contains "$SKILL_VERIFY_JSON" '"trusted": false' "skill verify"
SKILL_AUDIT_JSON="$(run_skill_audit_json "$ROOT" "$BUILTIN_GOAL_SKILL")"
require_output_contains "$SKILL_AUDIT_JSON" '"command": "skill audit"' "skill audit"
require_output_contains "$SKILL_AUDIT_JSON" '"decision": "review_required"' "skill audit"
require_output_contains "$SKILL_AUDIT_JSON" '"critical": 0' "skill audit"
require_output_contains "$SKILL_AUDIT_JSON" '"high": 0' "skill audit"
require_output_contains "$SKILL_AUDIT_JSON" '"source_hash": "sha256:' "skill audit"

log "checking status wording"
if is_prerelease; then
  grep -n "developer preview" README.md RELEASE.md >/dev/null || fail "developer preview wording missing"
  grep -n "not production-ready" README.md >/dev/null \
    || fail "production-readiness warning missing from README"
  grep -n "do not describe it" README.md >/dev/null \
    || fail "not-stable warning missing from README"
else
  grep -n "Current stable release: \`$VERSION\`" README.md >/dev/null \
    || fail "stable release marker missing from README"
  grep -n "Current release channel: \`stable\`" RELEASE.md >/dev/null \
    || fail "stable release channel missing from RELEASE.md"
  grep -n "1.0 stable contract" docs/STABLE_CONTRACT.md >/dev/null \
    || fail "1.0 stable contract wording missing"
fi
grep -ni "Stable candidate surface" docs/STABLE_CANDIDATE.md >/dev/null \
  || fail "stable candidate contract missing"
grep -ni "Experimental surface" docs/STABLE_CANDIDATE.md >/dev/null \
  || fail "experimental surface contract missing"
grep -ni "Stable Surface" docs/STABLE_CONTRACT.md >/dev/null \
  || fail "stable surface contract missing"
grep -ni "Stable JSON Fields" docs/STABLE_CONTRACT.md >/dev/null \
  || fail "stable JSON contract missing"
grep -ni "Stable Markdown Export Anchors" docs/STABLE_CONTRACT.md >/dev/null \
  || fail "stable Markdown export contract missing"
grep -ni "Experimental Outside The Contract" docs/STABLE_CONTRACT.md >/dev/null \
  || fail "stable contract experimental boundary missing"

log "checking final 1.0 boundary wording"
for text in \
  "BDD acceptance harness" \
  "Graph Lite stable-advisory" \
  "DB inspection remains beta" \
  "Skill governance has a documented stable-candidate subset" \
  "ECC Control Panel has a stable-candidate user-facing script entrypoint subset"
do
  require_doc_contains docs/STABLE_CONTRACT.md "$text"
done
for text in \
  "Stable-candidate package script entrypoints" \
  "DB inspection JSON output is beta" \
  "Skill governance JSON output is stable-candidate"
do
  require_doc_contains docs/COMPATIBILITY.md "$text"
done
for text in \
  "BDD acceptance harness is stable" \
  "Graph Lite stable-advisory" \
  "Skill, Policy, Routine, and ECC Control Panel user-facing subsets are" \
  "stable-candidate where their compatibility docs and release gates name"
do
  require_doc_contains docs/ROADMAP.md "$text"
done
for text in \
  "BDD acceptance harness, Graph Lite stable-advisory output" \
  "stable-candidate governance and" \
  "installer/reporting subsets"
do
  require_doc_contains CHANGELOG.md "$text"
done

log "checking Workflow/Spec stable release gate contract"
for command in \
  "workflow audit subset: \`start\`, \`status\`, \`export\`, and \`summary\`" \
  "spec traceability subset: \`create\`, \`status\`, \`export\`, and \`bind-workflow\`" \
  "dhk workflow start --workflow <workflow> --task <task> --json" \
  "dhk workflow status --run <run-key> --json" \
  "dhk workflow export --run <run-key> --json" \
  "dhk workflow summary --run <run-key> --json" \
  "dhk spec create --change <key> --title <title> --json" \
  "dhk spec status --change <key> --json" \
  "dhk spec export --change <key> --json" \
  "dhk spec bind-workflow --change <key> --run <run-key> --json"
do
  case "$command" in
    workflow\ audit*|spec\ traceability*) require_doc_contains docs/STABLE_CONTRACT.md "$command" ;;
    *) require_doc_contains docs/JSON_OUTPUT.md "$command" ;;
  esac
done
for field in \
  "run_key" \
  "workflow" \
  "current_phase" \
  "phases" \
  "pending_hard_gates" \
  "workflow_context_path" \
  "change_key" \
  "tasks" \
  "acceptance" \
  "bound_workflows" \
  "workflow_spec_binding_id"
do
  require_doc_contains docs/STABLE_CONTRACT.md "$field"
  require_doc_contains docs/JSON_OUTPUT.md "$field"
done

log "checking wrapper version fallback"
check_no_hardcoded_wrapper_version

log "checking archive contents"
for entry in \
  "install.sh" \
  "lib/dhk.jar" \
  "README.md" \
  "LICENSE" \
  "THIRD_PARTY_NOTICES.md" \
  "SECURITY.md" \
  "CHANGELOG.md" \
  ".agents/skills/devharness-goal-development/SKILL.md" \
  ".agents/skills/devharness-goal-development/contract.json" \
  ".agents/skills/devharness-goal-development/references/knowledge-injection.md" \
  ".agents/skills/devharness-goal-development/references/graph-protocol.md" \
  ".agents/knowledge/packs/devharness-methodology-core/knowledge-pack.json" \
  ".agents/knowledge/packs/java-enterprise-core/knowledge-pack.json" \
  ".agents/skills/devharness-goal-development/scripts/dhk.sh" \
  ".agents/skills/devharness-goal-development/scripts/dhk.bat" \
  ".agents/skills/devharness-goal-development/scripts/goal-start.sh" \
  ".agents/skills/devharness-goal-development/scripts/goal-start.bat" \
  ".agents/skills/devharness-goal-development/scripts/goal-verify.sh" \
  ".agents/skills/devharness-goal-development/scripts/goal-verify.bat" \
  ".agents/skills/devharness-goal-development/scripts/graph-impact.sh" \
  ".agents/skills/devharness-goal-development/scripts/graph-impact.bat" \
  ".agents/skills/devharness-goal-development/scripts/graph-index-export.sh" \
  ".agents/skills/devharness-goal-development/scripts/graph-index-export.bat" \
  ".comate/rules/devharness-goal-protocol.mdr" \
  "scripts/devharness-control-panel.sh" \
  "scripts/install-agent-adapters.sh"
do
  require_zip_entry "$entry"
  require_tgz_entry "$entry"
done
for entry in \
  "docs/" \
  "RELEASE.md" \
  "CONTRIBUTING.md" \
  "CODE_OF_CONDUCT.md" \
  "scripts/check-version-metadata.sh" \
  "scripts/check-coverage-threshold.sh" \
  "scripts/check-module-boundaries.sh" \
  "scripts/check-class-size.sh" \
  "scripts/graph-lite-precision-suite.sh" \
  "scripts/release-thresholds.env" \
  "scripts/release-gate.sh" \
  "scripts/perf-smoke.sh" \
  "scripts/perf-smoke.bat" \
  "scripts/dev-build.sh" \
  "scripts/lark-codex-bridge.sh" \
  ".agents/skills/devharness-graph-aware-development" \
  ".comate/rules/devharness-graph-aware-protocol.mdr"
do
  require_no_zip_entry "$entry"
  require_no_tgz_entry "$entry"
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
if unzip -l "$ZIP" | grep -E 'devharness-graph-aware-development|devharness-graph-aware-protocol.mdr' >/dev/null; then
  fail "zip still contains obsolete graph-aware skill/rule"
fi
if tar -tzf "$TGZ" | grep -E 'devharness-graph-aware-development|devharness-graph-aware-protocol.mdr' >/dev/null; then
  fail "tar.gz still contains obsolete graph-aware skill/rule"
fi

log "writing artifact manifest"
write_artifact_manifest
verify_artifact_manifest

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
  grep -F '"schema_version": "skill-contract/v1"' "$DIST/.agents/skills/devharness-goal-development/contract.json" >/dev/null \
    || fail "archive goal skill contract schema_version drifted"
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

  log "running Workflow/Spec stable JSON smoke"
  java -jar "$JAR" workflow template seed --project-root "$SMOKE_ROOT" >/dev/null
  WORKFLOW_START_JSON="$(java -jar "$JAR" workflow start --project-root "$SMOKE_ROOT" \
    --workflow api-change --task "release workflow smoke" --module release --mode api --json)"
  require_output_contains "$WORKFLOW_START_JSON" '"command": "workflow start"' "workflow start"
  require_output_contains "$WORKFLOW_START_JSON" '"run_key":' "workflow start"
  require_output_contains "$WORKFLOW_START_JSON" '"phases": [' "workflow start"
  WORKFLOW_RUN="$(json_string_value "$WORKFLOW_START_JSON" run_key)"
  [ -n "$WORKFLOW_RUN" ] || fail "workflow start JSON missing run_key value"

  WORKFLOW_STATUS_JSON="$(java -jar "$JAR" workflow status --project-root "$SMOKE_ROOT" --run "$WORKFLOW_RUN" --json)"
  require_output_contains "$WORKFLOW_STATUS_JSON" '"command": "workflow status"' "workflow status"
  require_output_contains "$WORKFLOW_STATUS_JSON" '"pending_hard_gates": [' "workflow status"

  WORKFLOW_EXPORT_JSON="$(java -jar "$JAR" workflow export --project-root "$SMOKE_ROOT" --run "$WORKFLOW_RUN" --json)"
  require_output_contains "$WORKFLOW_EXPORT_JSON" '"command": "workflow export"' "workflow export"
  require_output_contains "$WORKFLOW_EXPORT_JSON" '"workflow_context_path":' "workflow export"

  WORKFLOW_SUMMARY_JSON="$(java -jar "$JAR" workflow summary --project-root "$SMOKE_ROOT" --run "$WORKFLOW_RUN" --json)"
  require_output_contains "$WORKFLOW_SUMMARY_JSON" '"command": "workflow summary"' "workflow summary"
  require_output_contains "$WORKFLOW_SUMMARY_JSON" '"pending_hard_gate_count":' "workflow summary"

  SPEC_CREATE_JSON="$(java -jar "$JAR" spec create --project-root "$SMOKE_ROOT" \
    --change release-spec-smoke --title "Release spec smoke" --module release --mode api --json)"
  require_output_contains "$SPEC_CREATE_JSON" '"command": "spec create"' "spec create"
  require_output_contains "$SPEC_CREATE_JSON" '"change_key": "release-spec-smoke"' "spec create"
  require_output_contains "$SPEC_CREATE_JSON" '"tasks": []' "spec create"

  SPEC_BIND_JSON="$(java -jar "$JAR" spec bind-workflow --project-root "$SMOKE_ROOT" \
    --change release-spec-smoke --run "$WORKFLOW_RUN" --type implements --json)"
  require_output_contains "$SPEC_BIND_JSON" '"command": "spec bind-workflow"' "spec bind-workflow"
  require_output_contains "$SPEC_BIND_JSON" '"workflow_spec_binding_id":' "spec bind-workflow"
  require_output_contains "$SPEC_BIND_JSON" '"run_key":' "spec bind-workflow"

  SPEC_STATUS_JSON="$(java -jar "$JAR" spec status --project-root "$SMOKE_ROOT" \
    --change release-spec-smoke --json)"
  require_output_contains "$SPEC_STATUS_JSON" '"command": "spec status"' "spec status"
  require_output_contains "$SPEC_STATUS_JSON" '"bound_workflows": [' "spec status"

  SPEC_EXPORT_JSON="$(java -jar "$JAR" spec export --project-root "$SMOKE_ROOT" \
    --change release-spec-smoke --json)"
  require_output_contains "$SPEC_EXPORT_JSON" '"command": "spec export"' "spec export"
  require_output_contains "$SPEC_EXPORT_JSON" '"export_path":' "spec export"
fi

log "generating checksums"
cd target
if command -v sha256sum >/dev/null 2>&1; then
  sha256sum "dhk-cli-$VERSION-all.jar" "devharnesskit-$VERSION.zip" \
    "devharnesskit-$VERSION.tar.gz" "ARTIFACT_MANIFEST.json" > SHA256SUMS
elif command -v shasum >/dev/null 2>&1; then
  shasum -a 256 "dhk-cli-$VERSION-all.jar" "devharnesskit-$VERSION.zip" \
    "devharnesskit-$VERSION.tar.gz" "ARTIFACT_MANIFEST.json" > SHA256SUMS
else
  fail "sha256sum or shasum is required"
fi

log "passed"
