#!/usr/bin/env sh
set -eu

PROJECT_ROOT="."
TARGET="all"
MODE="copy"
FORCE="false"
DRY_RUN="false"
REMOVE_LEGACY="true"
JAR_PATH=""

usage() {
  cat <<'USAGE'
install-agent-adapters.sh

Install DevHarnessKit agent adapters for Claude Code, OpenCode, and Comate.

Usage:
  ./scripts/install-agent-adapters.sh [options]

Options:
  --project-root <path>       Target project root. Default: .
  --target <target>           claude | opencode | comate | all. Default: all
  --mode <copy|link>          Copy or symlink skill directories. Default: copy
  --jar <path>                Optional dhk.jar to install into .agents/tools/devharness-kit/dhk.jar
  --force                     Overwrite existing adapter files/directories
  --dry-run                   Print actions without writing
  --remove-legacy             Remove legacy memory-first skills/rules. Default
  --no-remove-legacy          Do not remove legacy skills/rules
  -h, --help                  Show this help

Examples:
  ./scripts/install-agent-adapters.sh --project-root . --target all --force
  ./scripts/install-agent-adapters.sh --project-root /repo --target claude --mode link
  ./scripts/install-agent-adapters.sh --project-root . --target comate --dry-run
USAGE
}

log() {
  printf '%s\n' "$*"
}

fail() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 1
}

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

copy_or_link_dir() {
  src="$1"
  dest="$2"
  [ -d "$src" ] || fail "missing source skill directory: $src"

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
  if [ -d "$dir" ]; then
    run "find \"$dir\" -type f -name '*.sh' -exec chmod +x {} \\;"
  fi
}

remove_legacy() {
  if [ "$REMOVE_LEGACY" != "true" ]; then
    return
  fi
  legacy_skill="$PROJECT_ROOT/.agents/skills/devharness-java-development"
  legacy_rule1="$PROJECT_ROOT/.comate/rules/project-memory-bootstrap.mdr"
  legacy_rule2="$PROJECT_ROOT/.comate/rules/java-development-guard.mdr"

  [ -e "$legacy_skill" ] && run "rm -rf \"$legacy_skill\"" || true
  [ -e "$legacy_rule1" ] && run "rm -f \"$legacy_rule1\"" || true
  [ -e "$legacy_rule2" ] && run "rm -f \"$legacy_rule2\"" || true
}

install_jar() {
  if [ -z "$JAR_PATH" ]; then
    return
  fi
  [ -f "$JAR_PATH" ] || fail "jar not found: $JAR_PATH"
  dest_dir="$PROJECT_ROOT/.agents/tools/devharness-kit"
  run "mkdir -p \"$dest_dir\""
  run "cp \"$JAR_PATH\" \"$dest_dir/dhk.jar\""
}

install_claude() {
  goal_src="$PROJECT_ROOT/.agents/skills/devharness-goal-development"
  graph_src="$PROJECT_ROOT/.agents/skills/devharness-graph-aware-development"

  copy_or_link_dir "$goal_src" "$PROJECT_ROOT/.claude/skills/devharness-goal-development"
  copy_or_link_dir "$graph_src" "$PROJECT_ROOT/.claude/skills/devharness-graph-aware-development"

  claude_md='# DevHarnessKit

This repository uses DevHarnessKit.

For any code change:
1. Use `/devharness-goal-development`.
2. Start or resume a goal.
3. Run goal-next before every work step.
4. Read `.agents/memory/exports/GOAL_CONTEXT.md`.
5. Perform only the current_action.
6. Record each step with goal-step.
7. Complete only after goal verify returns ready_to_complete.

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
- Run graph index/export and graph impact only when GOAL_CONTEXT requires it.
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

while [ "$#" -gt 0 ]; do
  case "$1" in
    --project-root)
      shift; PROJECT_ROOT="${1:-}";;
    --target)
      shift; TARGET="${1:-}";;
    --mode)
      shift; MODE="${1:-}";;
    --jar)
      shift; JAR_PATH="${1:-}";;
    --force)
      FORCE="true";;
    --dry-run)
      DRY_RUN="true";;
    --remove-legacy)
      REMOVE_LEGACY="true";;
    --no-remove-legacy)
      REMOVE_LEGACY="false";;
    -h|--help)
      usage; exit 0;;
    *)
      fail "unknown argument: $1";;
  esac
  shift
done

[ -n "$PROJECT_ROOT" ] || fail "--project-root cannot be empty"
[ "$MODE" = "copy" ] || [ "$MODE" = "link" ] || fail "--mode must be copy or link"

PROJECT_ROOT=$(cd "$PROJECT_ROOT" && pwd)

[ -d "$PROJECT_ROOT/.agents/skills/devharness-goal-development" ] || \
  fail "missing .agents/skills/devharness-goal-development under $PROJECT_ROOT"

[ -d "$PROJECT_ROOT/.agents/skills/devharness-graph-aware-development" ] || \
  fail "missing .agents/skills/devharness-graph-aware-development under $PROJECT_ROOT"

log "DevHarnessKit agent adapter install"
log "project_root: $PROJECT_ROOT"
log "target: $TARGET"
log "mode: $MODE"
log "force: $FORCE"
log "dry_run: $DRY_RUN"
log "remove_legacy: $REMOVE_LEGACY"

remove_legacy
install_jar

case "$TARGET" in
  claude)
    install_claude;;
  opencode)
    install_opencode;;
  comate)
    install_comate;;
  all)
    install_claude
    install_opencode
    install_comate;;
  *)
    fail "--target must be claude, opencode, comate, or all";;
esac

chmod_scripts "$PROJECT_ROOT/.agents/skills"

log "install complete"
log "next:"
log "  .agents/skills/devharness-goal-development/scripts/goal-start.sh --profile auto --task \"<task>\" --module \"<module>\""
