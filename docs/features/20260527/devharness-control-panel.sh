#!/usr/bin/env sh
set -eu

PROJECT_ROOT="."
TARGET="all"
MODE="copy"
FORCE="false"
DRY_RUN="false"
REMOVE_LEGACY="true"
JAR_PATH=""
PRESET=""
COMPILE_MODE=""
TEST_MODE=""
GRAPH_MODE=""
ALLOW_STALE_POLICY=""
STATUS_FORMAT="text"

usage() {
  cat <<'USAGE'
devharness-control-panel.sh

Local control panel for DevHarnessKit adapter install, config bootstrap, status, doctor, repair, and uninstall.

Usage:
  ./scripts/devharness-control-panel.sh <command> [options]

Commands:
  configure     Generate DevHarness config/policy/graph config and adapters
  plan          Show planned adapter/config changes without applying
  install       Install adapters for Claude Code, OpenCode, and Comate
  status        Print local DevHarness readiness status
  doctor        Diagnose missing or drifted files
  repair        Reinstall missing adapters from source of truth
  uninstall     Remove generated adapters, optionally keeping source .agents files

Options:
  --project-root <path>          Target project root. Default: .
  --target <target>              claude | opencode | comate | all. Default: all
  --preset <preset>              springboot-manual-ide-test | springboot-auto-test | legacy-jsp-servlet | mybatis-monolith-manual-test
  --mode <copy|link>             Copy or symlink skills. Default: copy
  --jar <path>                   Optional dhk.jar to install into .agents/tools/devharness-kit/dhk.jar
  --force                        Overwrite existing files
  --dry-run                      Print actions only
  --remove-legacy                Remove legacy memory-first skill/rules. Default
  --no-remove-legacy             Keep legacy files
  --compile-mode <mode>          auto | manual | disabled
  --test-mode <mode>             auto | manual | disabled
  --graph <mode>                 required | optional | off
  --allow-stale-policy <policy>  approval | off
  --status-format <format>       text | json | markdown
  -h, --help                     Show this help

Examples:
  ./scripts/devharness-control-panel.sh configure --preset springboot-manual-ide-test --target all --force
  ./scripts/devharness-control-panel.sh plan --target claude --dry-run
  ./scripts/devharness-control-panel.sh install --target all --jar target/dhk-cli-0.4.4-beta.1-all.jar
  ./scripts/devharness-control-panel.sh status --status-format markdown
USAGE
}

fail() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }
log() { printf '%s\n' "$*"; }

run() {
  if [ "$DRY_RUN" = "true" ]; then
    printf '[dry-run] %s\n' "$*"
  else
    sh -c "$*"
  fi
}

write_file() {
  path="$1"
  content="$2"
  if [ -e "$path" ] && [ "$FORCE" != "true" ]; then
    fail "$path already exists. Use --force to overwrite."
  fi
  if [ "$DRY_RUN" = "true" ]; then
    printf '[dry-run] write %s\n' "$path"
  else
    mkdir -p "$(dirname "$path")"
    printf '%s\n' "$content" > "$path"
  fi
}

sha256_file() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print $1}'
  elif command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$1" | awk '{print $1}'
  else
    printf 'sha256-unavailable'
  fi
}

copy_or_link_dir() {
  src="$1"
  dest="$2"
  [ -d "$src" ] || fail "missing source directory: $src"

  if [ -e "$dest" ] || [ -L "$dest" ]; then
    if [ "$FORCE" != "true" ]; then
      fail "$dest already exists. Use --force to overwrite."
    fi
    run "rm -rf \"$dest\""
  fi

  run "mkdir -p \"$(dirname "$dest")\""
  if [ "$MODE" = "link" ]; then
    abs_src=$(cd "$src" && pwd)
    run "ln -s \"$abs_src\" \"$dest\""
  else
    run "cp -R \"$src\" \"$dest\""
  fi
}

chmod_scripts() {
  dir="$1"
  [ -d "$dir" ] && run "find \"$dir\" -type f -name '*.sh' -exec chmod +x {} \\;" || true
}

effective_modes_from_preset() {
  case "$PRESET" in
    springboot-manual-ide-test)
      [ -z "$COMPILE_MODE" ] && COMPILE_MODE="manual"
      [ -z "$TEST_MODE" ] && TEST_MODE="manual"
      [ -z "$GRAPH_MODE" ] && GRAPH_MODE="required"
      [ -z "$ALLOW_STALE_POLICY" ] && ALLOW_STALE_POLICY="approval"
      ;;
    springboot-auto-test)
      [ -z "$COMPILE_MODE" ] && COMPILE_MODE="auto"
      [ -z "$TEST_MODE" ] && TEST_MODE="auto"
      [ -z "$GRAPH_MODE" ] && GRAPH_MODE="required"
      [ -z "$ALLOW_STALE_POLICY" ] && ALLOW_STALE_POLICY="approval"
      ;;
    legacy-jsp-servlet|mybatis-monolith-manual-test)
      [ -z "$COMPILE_MODE" ] && COMPILE_MODE="manual"
      [ -z "$TEST_MODE" ] && TEST_MODE="manual"
      [ -z "$GRAPH_MODE" ] && GRAPH_MODE="required"
      [ -z "$ALLOW_STALE_POLICY" ] && ALLOW_STALE_POLICY="approval"
      ;;
    "")
      [ -z "$COMPILE_MODE" ] && COMPILE_MODE="manual"
      [ -z "$TEST_MODE" ] && TEST_MODE="manual"
      [ -z "$GRAPH_MODE" ] && GRAPH_MODE="required"
      [ -z "$ALLOW_STALE_POLICY" ] && ALLOW_STALE_POLICY="approval"
      ;;
    *)
      fail "unknown preset: $PRESET"
      ;;
  esac
}

validate_modes() {
  case "$COMPILE_MODE" in auto|manual|disabled) ;; *) fail "--compile-mode must be auto, manual, or disabled";; esac
  case "$TEST_MODE" in auto|manual|disabled) ;; *) fail "--test-mode must be auto, manual, or disabled";; esac
  case "$GRAPH_MODE" in required|optional|off) ;; *) fail "--graph must be required, optional, or off";; esac
  case "$ALLOW_STALE_POLICY" in approval|off) ;; *) fail "--allow-stale-policy must be approval or off";; esac
}

remove_legacy() {
  [ "$REMOVE_LEGACY" = "true" ] || return 0
  for path in \
    "$PROJECT_ROOT/.agents/skills/devharness-java-development" \
    "$PROJECT_ROOT/.comate/rules/project-memory-bootstrap.mdr" \
    "$PROJECT_ROOT/.comate/rules/java-development-guard.mdr"
  do
    [ -e "$path" ] && run "rm -rf \"$path\"" || true
  done
}

write_config() {
  effective_modes_from_preset
  validate_modes

  graph_required="false"
  graph_fresh="true"
  impact_required="true"
  if [ "$GRAPH_MODE" = "required" ]; then
    graph_required="true"
  elif [ "$GRAPH_MODE" = "off" ]; then
    graph_fresh="false"
    impact_required="false"
  fi

  allow_stale_requires="true"
  [ "$ALLOW_STALE_POLICY" = "off" ] && allow_stale_requires="false"

  config='{
  "schema_version": "devharness-config/v1-alpha",
  "project": {
    "preset": "'"${PRESET:-custom}"'",
    "project_type": "'"${PRESET:-custom}"'",
    "runtime": "local-or-company-environment"
  },
  "verification": {
    "compile": {
      "mode": "'"$COMPILE_MODE"'",
      "manual_trigger": "IDE build action",
      "required_evidence": "manual_evidence_status=passed,compile_scope,manual_evidence_path"
    },
    "test": {
      "mode": "'"$TEST_MODE"'",
      "manual_trigger": "IDE test button or CI evidence",
      "expected_duration": "project-specific",
      "required_evidence": "manual_evidence_status=passed,test_scope,manual_evidence_path"
    },
    "graph": {
      "required": '"$graph_required"',
      "fresh_snapshot_required": '"$graph_fresh"',
      "impact_map_required": '"$impact_required"',
      "allow_stale_requires_approval": '"$allow_stale_requires"'
    },
    "architecture": {
      "mode": "warn"
    },
    "rollback": {
      "required_when_auto_tests_unavailable": true
    }
  }
}'

  policy='{
  "mode": "guided",
  "db_sql_requires_explicit_request": true,
  "db_require_readonly_credentials": true,
  "context_export_require_sensitive_scan": true,
  "context_export_block_on_sensitive": true,
  "graph_allow_stale_requires_approval": '"$allow_stale_requires"',
  "protected_files": ".env,*.pem,*.key,src/main/resources/application-prod.*,src/main/resources/*prod*"
}'

  graph='{
  "schema_version": "devharness-graph-config/v1-alpha",
  "provider": "lite",
  "limits": {
    "max_file_bytes": 262144,
    "max_indexed_files": 2000,
    "max_impact_depth": 4,
    "max_export_nodes": 200
  }
}'

  write_file "$PROJECT_ROOT/.agents/devharness/config.json" "$config"
  write_file "$PROJECT_ROOT/.agents/devharness/policy.json" "$policy"
  write_file "$PROJECT_ROOT/.agents/graph/config.json" "$graph"
}

write_manifest() {
  manifest='{
  "schema_version": "devharness-agent-manifest/v1-alpha",
  "default_skill": "devharness-goal-development",
  "conditional_skills": [
    {
      "name": "devharness-graph-aware-development",
      "when": "GOAL_CONTEXT contains graph_required=true"
    }
  ],
  "targets": {
    "claude": { "enabled": true, "install_skills": true, "write_claude_md": true },
    "opencode": { "enabled": true, "write_agents_md": true },
    "comate": { "enabled": true, "write_rules": true }
  },
  "legacy": {
    "remove_memory_first_skill": true,
    "remove_memory_first_comate_rules": true
  }
}'
  write_file "$PROJECT_ROOT/.agents/devharness/agent-manifest.json" "$manifest"
}

install_jar() {
  [ -n "$JAR_PATH" ] || return 0
  [ -f "$JAR_PATH" ] || fail "jar not found: $JAR_PATH"
  run "mkdir -p \"$PROJECT_ROOT/.agents/tools/devharness-kit\""
  run "cp \"$JAR_PATH\" \"$PROJECT_ROOT/.agents/tools/devharness-kit/dhk.jar\""
}

install_claude() {
  copy_or_link_dir "$PROJECT_ROOT/.agents/skills/devharness-goal-development" \
    "$PROJECT_ROOT/.claude/skills/devharness-goal-development"
  copy_or_link_dir "$PROJECT_ROOT/.agents/skills/devharness-graph-aware-development" \
    "$PROJECT_ROOT/.claude/skills/devharness-graph-aware-development"

  claude_md='# DevHarnessKit

This repository uses DevHarnessKit.

For any code change:
1. Use `/devharness-goal-development`.
2. Start or resume a goal.
3. Run goal-next before every work step.
4. Read `.agents/memory/exports/GOAL_CONTEXT.md`.
5. Perform only the current_action.
6. Complete only after goal verify returns ready_to_complete.

If GOAL_CONTEXT shows graph_required=true, use `/devharness-graph-aware-development`.

Do not bypass goal with direct memory/workflow/spec/db commands.
Do not use graph impact --allow-stale unless explicit approval evidence is present.
'
  write_file "$PROJECT_ROOT/CLAUDE.md" "$claude_md"
  chmod_scripts "$PROJECT_ROOT/.claude/skills"
}

install_opencode() {
  agents_md='# DevHarnessKit Agent Instructions

This repository uses DevHarnessKit.

For any code change:
1. Do not start by editing code.
2. Use `.agents/skills/devharness-goal-development/scripts/goal-start.sh` or `goal-resume.sh`.
3. Run `goal-next.sh` before every work step.
4. Read `.agents/memory/exports/GOAL_CONTEXT.md`.
5. Perform only the current_action.
6. Run `goal-step.sh` after investigation, planning, editing, or verification.
7. Run `goal-verify.sh` before claiming completion.
8. Run `goal-complete.sh` only when ready_to_complete.

If GOAL_CONTEXT contains graph_required=true:
- Follow `.agents/skills/devharness-graph-aware-development/SKILL.md`.
- Do not use `graph impact --allow-stale` unless explicit approval evidence is present.

Do not call lower-level memory/workflow/spec/db commands unless GOAL_CONTEXT explicitly allows it.
'
  write_file "$PROJECT_ROOT/AGENTS.md" "$agents_md"
}

install_comate() {
  goal_rule='---
description: DevHarnessKit goal-first development protocol.
alwaysApply: true
---

This repository uses DevHarnessKit.

For any code change:
1. Do not start by editing code.
2. Run `.agents/skills/devharness-goal-development/scripts/goal-start.sh` or `goal-resume.sh`.
3. Run `goal-next.sh` before every work step.
4. Read `.agents/memory/exports/GOAL_CONTEXT.md`.
5. Perform only current_action.
6. Record every investigation, plan, edit, or verification with `goal-step.sh`.
7. Run `goal-verify.sh` before claiming completion.
8. Run `goal-complete.sh` only when ready_to_complete.

Do not bypass goal with direct memory/workflow/spec/db commands.
'
  graph_rule='---
description: DevHarnessKit graph-aware goal protocol.
alwaysApply: true
---

If GOAL_CONTEXT contains graph_required=true:
1. Run graph index/export when GOAL_CONTEXT requires graph_index_export.
2. Run graph impact when GOAL_CONTEXT requires graph_impact.
3. Read GRAPH_CONTEXT.md and IMPACT_MAP.md before editing.
4. Do not edit files outside the impact map unless impact is regenerated and risk is recorded.
5. Run graph impact again after code changes and record graph_reimpact evidence.
6. Do not use --allow-stale unless explicit approval evidence is provided.
7. Run goal verify before completion.
'
  write_file "$PROJECT_ROOT/.comate/rules/devharness-goal-protocol.mdr" "$goal_rule"
  write_file "$PROJECT_ROOT/.comate/rules/devharness-graph-aware-protocol.mdr" "$graph_rule"
}

install_adapters() {
  [ -d "$PROJECT_ROOT/.agents/skills/devharness-goal-development" ] || fail "missing goal skill source"
  [ -d "$PROJECT_ROOT/.agents/skills/devharness-graph-aware-development" ] || fail "missing graph-aware skill source"
  remove_legacy
  install_jar
  case "$TARGET" in
    claude) install_claude ;;
    opencode) install_opencode ;;
    comate) install_comate ;;
    all) install_claude; install_opencode; install_comate ;;
    *) fail "--target must be claude, opencode, comate, or all" ;;
  esac
  chmod_scripts "$PROJECT_ROOT/.agents/skills"
}

write_install_state() {
  [ "$DRY_RUN" = "true" ] && return 0
  mkdir -p "$PROJECT_ROOT/.agents/devharness"
  now=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
  state="$PROJECT_ROOT/.agents/devharness/install-state.json"
  cat > "$state" <<STATE
{
  "schema_version": "devharness-install-state/v1-alpha",
  "installed_at": "$now",
  "installed_by": "devharness-control-panel.sh",
  "target": "$TARGET",
  "mode": "$MODE",
  "preset": "${PRESET:-custom}",
  "compile_mode": "${COMPILE_MODE:-}",
  "test_mode": "${TEST_MODE:-}",
  "graph": "${GRAPH_MODE:-}",
  "remove_legacy": $REMOVE_LEGACY
}
STATE
}

status() {
  goal_skill="$PROJECT_ROOT/.agents/skills/devharness-goal-development/SKILL.md"
  graph_skill="$PROJECT_ROOT/.agents/skills/devharness-graph-aware-development/SKILL.md"
  config="$PROJECT_ROOT/.agents/devharness/config.json"
  policy="$PROJECT_ROOT/.agents/devharness/policy.json"
  state="$PROJECT_ROOT/.agents/devharness/install-state.json"
  claude="$PROJECT_ROOT/.claude/skills/devharness-goal-development/SKILL.md"
  agents="$PROJECT_ROOT/AGENTS.md"
  comate="$PROJECT_ROOT/.comate/rules/devharness-goal-protocol.mdr"

  ok_goal=$([ -f "$goal_skill" ] && echo ok || echo missing)
  ok_graph=$([ -f "$graph_skill" ] && echo ok || echo missing)
  ok_config=$([ -f "$config" ] && echo ok || echo missing)
  ok_policy=$([ -f "$policy" ] && echo ok || echo missing)
  ok_state=$([ -f "$state" ] && echo ok || echo missing)
  ok_claude=$([ -f "$claude" ] && echo ok || echo missing)
  ok_agents=$([ -f "$agents" ] && echo ok || echo missing)
  ok_comate=$([ -f "$comate" ] && echo ok || echo missing)

  case "$STATUS_FORMAT" in
    json)
      cat <<JSON
{
  "goal_skill": "$ok_goal",
  "graph_skill": "$ok_graph",
  "config": "$ok_config",
  "policy": "$ok_policy",
  "install_state": "$ok_state",
  "claude_adapter": "$ok_claude",
  "opencode_adapter": "$ok_agents",
  "comate_adapter": "$ok_comate"
}
JSON
      ;;
    markdown)
      cat <<MD
# DevHarnessKit Status

| Item | Status |
|---|---|
| goal_skill | $ok_goal |
| graph_skill | $ok_graph |
| config | $ok_config |
| policy | $ok_policy |
| install_state | $ok_state |
| claude_adapter | $ok_claude |
| opencode_adapter | $ok_agents |
| comate_adapter | $ok_comate |
MD
      ;;
    *)
      log "DevHarnessKit status"
      log "goal_skill: $ok_goal"
      log "graph_skill: $ok_graph"
      log "config: $ok_config"
      log "policy: $ok_policy"
      log "install_state: $ok_state"
      log "claude_adapter: $ok_claude"
      log "opencode_adapter: $ok_agents"
      log "comate_adapter: $ok_comate"
      ;;
  esac
}

doctor() {
  status
  [ -f "$PROJECT_ROOT/.agents/skills/devharness-goal-development/SKILL.md" ] || fail "goal skill missing"
  [ -f "$PROJECT_ROOT/.agents/skills/devharness-graph-aware-development/SKILL.md" ] || fail "graph-aware skill missing"
}

plan() {
  log "DevHarness plan"
  log "project_root: $PROJECT_ROOT"
  log "target: $TARGET"
  log "preset: ${PRESET:-custom}"
  log "mode: $MODE"
  log "compile_mode: ${COMPILE_MODE:-auto/default}"
  log "test_mode: ${TEST_MODE:-auto/default}"
  log "graph: ${GRAPH_MODE:-required/default}"
  log "will_write:"
  log "- .agents/devharness/config.json"
  log "- .agents/devharness/policy.json"
  log "- .agents/devharness/agent-manifest.json"
  [ "$TARGET" = "claude" ] || [ "$TARGET" = "all" ] && log "- .claude/skills/* and CLAUDE.md" || true
  [ "$TARGET" = "opencode" ] || [ "$TARGET" = "all" ] && log "- AGENTS.md" || true
  [ "$TARGET" = "comate" ] || [ "$TARGET" = "all" ] && log "- .comate/rules/*" || true
  [ "$REMOVE_LEGACY" = "true" ] && log "will_remove legacy memory-first skill/rules" || true
}

uninstall() {
  case "$TARGET" in
    claude)
      run "rm -rf \"$PROJECT_ROOT/.claude/skills/devharness-goal-development\" \"$PROJECT_ROOT/.claude/skills/devharness-graph-aware-development\" \"$PROJECT_ROOT/CLAUDE.md\""
      ;;
    opencode)
      run "rm -f \"$PROJECT_ROOT/AGENTS.md\""
      ;;
    comate)
      run "rm -f \"$PROJECT_ROOT/.comate/rules/devharness-goal-protocol.mdr\" \"$PROJECT_ROOT/.comate/rules/devharness-graph-aware-protocol.mdr\""
      ;;
    all)
      TARGET=claude uninstall
      TARGET=opencode uninstall
      TARGET=comate uninstall
      ;;
    *) fail "--target must be claude, opencode, comate, or all" ;;
  esac
}

COMMAND="${1:-}"
[ "$COMMAND" = "" ] && { usage; exit 1; }
shift || true

while [ "$#" -gt 0 ]; do
  case "$1" in
    --project-root) shift; PROJECT_ROOT="${1:-}" ;;
    --target) shift; TARGET="${1:-}" ;;
    --mode) shift; MODE="${1:-}" ;;
    --jar) shift; JAR_PATH="${1:-}" ;;
    --preset) shift; PRESET="${1:-}" ;;
    --compile-mode) shift; COMPILE_MODE="${1:-}" ;;
    --test-mode) shift; TEST_MODE="${1:-}" ;;
    --graph) shift; GRAPH_MODE="${1:-}" ;;
    --allow-stale-policy) shift; ALLOW_STALE_POLICY="${1:-}" ;;
    --status-format) shift; STATUS_FORMAT="${1:-}" ;;
    --force) FORCE="true" ;;
    --dry-run) DRY_RUN="true" ;;
    --remove-legacy) REMOVE_LEGACY="true" ;;
    --no-remove-legacy) REMOVE_LEGACY="false" ;;
    -h|--help) usage; exit 0 ;;
    *) fail "unknown argument: $1" ;;
  esac
  shift
done

[ -n "$PROJECT_ROOT" ] || fail "--project-root cannot be empty"
PROJECT_ROOT=$(cd "$PROJECT_ROOT" && pwd)
[ "$MODE" = "copy" ] || [ "$MODE" = "link" ] || fail "--mode must be copy or link"

case "$COMMAND" in
  configure)
    write_config
    write_manifest
    install_adapters
    write_install_state
    log "configure complete"
    ;;
  plan)
    effective_modes_from_preset
    plan
    ;;
  install)
    install_adapters
    write_install_state
    log "install complete"
    ;;
  status)
    status
    ;;
  doctor)
    doctor
    ;;
  repair)
    install_adapters
    write_install_state
    log "repair complete"
    ;;
  uninstall)
    uninstall
    log "uninstall complete"
    ;;
  *)
    usage
    fail "unknown command: $COMMAND"
    ;;
esac
