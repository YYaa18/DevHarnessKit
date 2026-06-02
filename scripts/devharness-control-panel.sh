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

Local control panel for DevHarnessKit configuration, agent adapters, status, doctor, repair, and uninstall.

Usage:
  ./scripts/devharness-control-panel.sh <command> [options]

Commands:
  configure     Generate config/policy/graph config/manifest, install adapters, and write install-state
  plan          Show planned adapter/config changes without applying
  install       Install adapters for Claude Code, OpenCode, and Comate
  status        Print local DevHarness readiness status
  doctor        Diagnose missing local DevHarness files and adapters
  repair        Reinstall adapters from source of truth
  uninstall     Remove generated adapters

Options:
  --project-root <path>          Target project root. Default: .
  --target <target>              claude | opencode | comate | all. Default: all
  --preset <preset>              initial-new-project | springboot-manual-ide-test | springboot-auto-test | legacy-jsp-servlet | mybatis-monolith-manual-test
  --mode <copy|link>             Copy or symlink skills. Default: copy
  --jar <path>                   Optional dhk.jar to install into .agents/tools/devharness-kit/dhk.jar
  --force                        Overwrite existing files
  --dry-run                      Print actions only
  --remove-legacy                Remove legacy memory-first skill/rules. Default
  --no-remove-legacy             Keep legacy files
  --compile-mode <mode>          auto | manual | disabled
  --test-mode <mode>             auto | manual | disabled
  --graph <mode>                 required | advisory | optional | off
  --allow-stale-policy <policy>  approval | off
  --status-format <format>       text | json | markdown
  -h, --help                     Show this help

Examples:
  ./scripts/devharness-control-panel.sh configure --preset springboot-manual-ide-test --target all --force
  ./scripts/devharness-control-panel.sh plan --target claude --dry-run
  ./scripts/devharness-control-panel.sh install --target all --jar target/dhk-cli-<version>-all.jar
  ./scripts/devharness-control-panel.sh status --status-format markdown
USAGE
}

fail() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 1
}

log() {
  printf '%s\n' "$*"
}

run() {
  if [ "$DRY_RUN" = "true" ]; then
    printf '[dry-run] %s\n' "$*"
  else
    sh -c "$*"
  fi
}

quote_json() {
  printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g'
}

sha256_file() {
  path="$1"
  if command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$path" | awk '{print $1}'
  elif command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$path" | awk '{print $1}'
  else
    cksum "$path" | awk '{print $1}'
  fi
}

managed_file_paths() {
  for path in \
    ".agents/devharness/config.json" \
    ".agents/devharness/policy.json" \
    ".agents/devharness/agent-manifest.json" \
    ".agents/graph/config.json"
  do
    [ -f "$PROJECT_ROOT/$path" ] && printf '%s\n' "$path"
  done
  case "$TARGET" in
    claude|all)
      for path in \
        ".claude/skills/devharness-goal-development/SKILL.md" \
        "CLAUDE.md"
      do
        [ -f "$PROJECT_ROOT/$path" ] && printf '%s\n' "$path"
      done
      ;;
  esac
  case "$TARGET" in
    opencode|all)
      [ -f "$PROJECT_ROOT/AGENTS.md" ] && printf '%s\n' "AGENTS.md"
      ;;
  esac
  case "$TARGET" in
    comate|all)
      for path in \
        ".comate/rules/devharness-goal-protocol.mdr"
      do
        [ -f "$PROJECT_ROOT/$path" ] && printf '%s\n' "$path"
      done
      ;;
  esac
  [ -f "$PROJECT_ROOT/.agents/tools/devharness-kit/dhk.jar" ] \
    && printf '%s\n' ".agents/tools/devharness-kit/dhk.jar"
}

extract_json_string() {
  printf '%s' "$1" | sed 's/.*"[^"]*": "\([^"]*\)".*/\1/'
}

write_text_file() {
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
    initial-new-project|demo-no-build)
      PRESET="initial-new-project"
      [ -z "$COMPILE_MODE" ] && COMPILE_MODE="disabled"
      [ -z "$TEST_MODE" ] && TEST_MODE="disabled"
      [ -z "$GRAPH_MODE" ] && GRAPH_MODE="off"
      [ -z "$ALLOW_STALE_POLICY" ] && ALLOW_STALE_POLICY="approval"
      ;;
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
    legacy-jsp-servlet|legacy-java-small-fix|mybatis-monolith-manual-test)
      [ -z "$COMPILE_MODE" ] && COMPILE_MODE="manual"
      [ -z "$TEST_MODE" ] && TEST_MODE="manual"
      [ -z "$GRAPH_MODE" ] && GRAPH_MODE="required"
      [ -z "$ALLOW_STALE_POLICY" ] && ALLOW_STALE_POLICY="approval"
      ;;
    "")
      PRESET="springboot-manual-ide-test"
      [ -z "$COMPILE_MODE" ] && COMPILE_MODE="manual"
      [ -z "$TEST_MODE" ] && TEST_MODE="manual"
      [ -z "$GRAPH_MODE" ] && GRAPH_MODE="required"
      [ -z "$ALLOW_STALE_POLICY" ] && ALLOW_STALE_POLICY="approval"
      ;;
    *)
      fail "unknown preset: $PRESET"
      ;;
  esac
  return 0
}

validate_modes() {
  case "$TARGET" in claude|opencode|comate|all) ;; *) fail "--target must be claude, opencode, comate, or all";; esac
  case "$MODE" in copy|link) ;; *) fail "--mode must be copy or link";; esac
  case "$COMPILE_MODE" in auto|manual|disabled) ;; *) fail "--compile-mode must be auto, manual, or disabled";; esac
  case "$TEST_MODE" in auto|manual|disabled) ;; *) fail "--test-mode must be auto, manual, or disabled";; esac
  case "$GRAPH_MODE" in required|advisory|optional|off) ;; *) fail "--graph must be required, advisory, optional, or off";; esac
  case "$ALLOW_STALE_POLICY" in approval|off) ;; *) fail "--allow-stale-policy must be approval or off";; esac
  case "$STATUS_FORMAT" in text|json|markdown) ;; *) fail "--status-format must be text, json, or markdown";; esac
}

project_type() {
  case "$PRESET" in
    initial-new-project) printf 'initial-new-project' ;;
    springboot-auto-test) printf 'springboot-api' ;;
    springboot-manual-ide-test) printf 'springboot-enterprise-large' ;;
    legacy-jsp-servlet|legacy-java-small-fix|mybatis-monolith-manual-test) printf '%s' "$PRESET" ;;
    *) printf 'custom' ;;
  esac
}

project_runtime() {
  case "$PRESET" in
    initial-new-project) printf 'local-new-project' ;;
    springboot-auto-test) printf 'local-cli' ;;
    *) printf 'local-or-company-environment' ;;
  esac
}

write_config() {
  effective_modes_from_preset
  validate_modes

  graph_required="false"
  graph_fresh="false"
  impact_required="false"
  graph_config_mode="$GRAPH_MODE"
  [ "$graph_config_mode" = "optional" ] && graph_config_mode="advisory"
  if [ "$GRAPH_MODE" = "required" ]; then
    graph_required="true"
    graph_fresh="true"
    impact_required="true"
  fi

  allow_stale_requires="true"
  [ "$ALLOW_STALE_POLICY" = "off" ] && allow_stale_requires="false"

  compile_trigger="IDE build action"
  test_trigger="IDE test button or CI evidence"
  test_duration="project-specific"
  compile_command="mvn -q -DskipTests compile"
  test_command="mvn -q test"
  rollback_required="true"
  initial_project_enabled="false"
  initial_project_warning=""
  if [ "$COMPILE_MODE" = "auto" ]; then
    compile_trigger="CLI compile command"
  fi
  if [ "$TEST_MODE" = "auto" ]; then
    test_trigger="CLI test command"
    test_duration="fast"
  fi
  if [ "$PRESET" = "springboot-manual-ide-test" ]; then
    test_duration="10m+ per test"
  fi
  if [ "$PRESET" = "initial-new-project" ]; then
    compile_trigger="not configured until the project has a build entry"
    test_trigger="not configured until the project has test coverage"
    compile_command=""
    test_command=""
    test_duration="not configured"
    rollback_required="false"
    initial_project_enabled="true"
    initial_project_warning="initial project mode does not prove production correctness until real compile/test evidence exists"
  fi

  config='{
  "schema_version": "devharness-config/v1-alpha",
  "preset": "'"$(quote_json "$PRESET")"'",
  "project.type": "'"$(project_type)"'",
  "project.module_style": "api",
  "project.runtime": "'"$(project_runtime)"'",
  "verification.compile.mode": "'"$COMPILE_MODE"'",
  "verification.compile.command": "'"$(quote_json "$compile_command")"'",
  "verification.compile.manual_trigger": "'"$(quote_json "$compile_trigger")"'",
  "verification.compile.required_evidence": "manual_evidence_status=passed,compile_scope,manual_evidence_path",
  "verification.test.mode": "'"$TEST_MODE"'",
  "verification.test.command": "'"$(quote_json "$test_command")"'",
  "verification.test.manual_trigger": "'"$(quote_json "$test_trigger")"'",
  "verification.test.expected_duration": "'"$(quote_json "$test_duration")"'",
  "verification.test.required_evidence": "manual_evidence_status=passed,test_scope,manual_evidence_path",
  "verification.graph.mode": "'"$graph_config_mode"'",
  "verification.graph.required": "'"$graph_required"'",
  "verification.graph.fresh_snapshot_required": "'"$graph_fresh"'",
  "verification.graph.impact_map_required": "'"$impact_required"'",
  "verification.graph.allow_stale_requires_approval": "'"$allow_stale_requires"'",
  "verification.initial_project.enabled": "'"$initial_project_enabled"'",
  "verification.initial_project.warning": "'"$(quote_json "$initial_project_warning")"'",
  "verification.architecture.mode": "warn",
  "verification.rollback.required_when_auto_tests_unavailable": "'"$rollback_required"'",
  "adapter.target": "'"$TARGET"'"
}'

  policy='{
  "schema_version": "devharness-policy/v1",
  "mode": "guided",
  "allowed_dhk_commands": "goal start,goal resume,goal next,goal step,goal verify,goal complete,graph status,graph index,graph export,graph impact,configure show,configure doctor",
  "forbidden_dhk_commands": "workflow gate waive,spec archive,memory confirm,db sql",
  "protected_files": ".env,*.pem,*.key,src/main/resources/application-prod.*,src/main/resources/*prod*",
  "db_sql_requires_explicit_request": "true",
  "db_require_readonly_credentials": "true",
  "context_export_require_sensitive_scan": "true",
  "context_export_block_on_sensitive": "true",
  "graph_allow_stale_requires_approval": "'"$allow_stale_requires"'",
  "skill_contract_required": "true",
  "skill_key": "devharness-goal-development"
}'

  graph='{
  "schema_version": "devharness-graph-config/v1-alpha",
  "provider": "lite",
  "cgc_command": "",
  "include": [
    "src/main/java/**/*.java",
    "src/test/java/**/*.java",
    "src/main/resources/**/*.xml",
    "src/main/webapp/**/*.jsp"
  ],
  "exclude": [
    "target/**",
    "build/**",
    ".git/**",
    ".agents/memory/**"
  ],
  "limits": {
    "max_file_bytes": 262144,
    "max_indexed_files": 2000,
    "max_impact_depth": 4,
    "max_export_nodes": 200
  },
  "safety": {
    "skip_protected_file_content": true,
    "skip_sensitive_file_content": true,
    "record_skipped_metadata": true
  }
}'

  write_text_file "$PROJECT_ROOT/.agents/devharness/config.json" "$config"
  write_text_file "$PROJECT_ROOT/.agents/devharness/policy.json" "$policy"
  write_text_file "$PROJECT_ROOT/.agents/graph/config.json" "$graph"
}

write_manifest() {
  effective_modes_from_preset
  validate_modes
  manifest='{
  "schema_version": "devharness-agent-manifest/v1-alpha",
  "default_skill": "devharness-goal-development",
  "graph": {
    "handling": "integrated_in_default_skill"
  },
  "targets": {
    "claude": { "enabled": true, "install_skills": true, "write_claude_md": true },
    "opencode": { "enabled": true, "write_agents_md": true },
    "comate": { "enabled": true, "write_rules": true }
  },
  "legacy": {
    "remove_memory_first_skill": '"$REMOVE_LEGACY"',
    "remove_memory_first_comate_rules": '"$REMOVE_LEGACY"'
  }
}'
  write_text_file "$PROJECT_ROOT/.agents/devharness/agent-manifest.json" "$manifest"
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

remove_obsolete_graph_artifacts() {
  for path in \
    "$PROJECT_ROOT/.agents/skills/devharness-graph-aware-development" \
    "$PROJECT_ROOT/.claude/skills/devharness-graph-aware-development" \
    "$PROJECT_ROOT/.comate/rules/devharness-graph-aware-protocol.mdr"
  do
    [ -e "$path" ] && run "rm -rf \"$path\"" || true
  done
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

  claude_md='# DevHarnessKit

This repository uses DevHarnessKit.

For any code change:
1. Use `/devharness-goal-development`.
2. For a new user request, start with quickstart using the latest user request as the task; if the previous Agent Brief is completed, do not reuse it.
3. Run goal-next before every work step.
4. Read `.agents/memory/exports/GOAL_CONTEXT.md`.
5. Perform only the current_action.
6. Record each step with goal-step.
7. Complete only after goal verify returns ready_to_complete.

If GOAL_CONTEXT shows graph_required=true, keep using `/devharness-goal-development`.
Treat graph-assist as internal evidence guidance inside the current main action.

On a non-trivial error, run `dhk memory search --q "<error signature>"` first to
reuse a known fix; after solving, run `dhk memory suggest` to queue a
troubleshooting candidate for the user to confirm. Do not run `dhk memory confirm`.

Do not bypass goal with direct workflow/spec/db commands. For memory, only search
and suggest are allowed; never confirm memory or write secrets into a suggestion.
Never retry a failing or hanging build in a loop. If compile/test cannot run or
fails for environmental reasons, record it once as `compile_result=blocked: <reason>`
plus risk/rollback evidence, then complete with manual evidence or report the
blocker to the user. Do not re-run the same failing command expecting a different result.
Do not use graph impact --allow-stale unless explicit approval evidence is present.
'
  write_text_file "$PROJECT_ROOT/CLAUDE.md" "$claude_md"
  chmod_scripts "$PROJECT_ROOT/.claude/skills"
}

install_opencode() {
  agents_md='# DevHarnessKit Agent Instructions

This repository uses DevHarnessKit.

For any code change:
1. Do not start by editing code.
2. If the previous Agent Brief is missing or has `current_action` = `completed`, run `.agents/skills/devharness-goal-development/scripts/quickstart.sh --task "<latest user request verbatim>" --mode recommend --resume-existing --module "<best module>"`.
3. Before editing, compare GOAL_CONTEXT task with the latest user request. If they do not match, create a new quickstart goal with the latest request.
4. Run `goal-next.sh` before every work step.
5. Read `.agents/memory/exports/GOAL_CONTEXT.md`.
6. Perform only the current_action.
7. Run `goal-step.sh` after investigation, planning, editing, or verification.
8. Run `goal-verify.sh` before claiming completion.
9. Run `goal-complete.sh` only when ready_to_complete.

If GOAL_CONTEXT contains graph_required=true:
- Keep using the main goal flow; graph is evidence inside the current action.
- If GOAL_CONTEXT has graph-assist, run the listed graph helper internally and record graph evidence in the current goal step.
- Do not use `graph impact --allow-stale` unless explicit approval evidence is present.

On a non-trivial error, run `dhk memory search --q "<error signature>"` first to reuse a known fix; after solving, run `dhk memory suggest` to queue a troubleshooting candidate for the user to confirm. Do not run `dhk memory confirm`.

Do not call lower-level workflow/spec/db commands unless GOAL_CONTEXT explicitly allows it. For memory, only `dhk memory search` and `dhk memory suggest` are allowed; never confirm memory or write secrets into a suggestion.

Never retry a failing or hanging build in a loop. If compile/test cannot run or fails for environmental reasons (missing deps, no local toolchain, manual policy), record it once as `compile_result=blocked: <reason>` plus risk/rollback evidence, then complete with manual evidence or report the blocker to the user. Do not re-run the same failing command expecting a different result.
'
  write_text_file "$PROJECT_ROOT/AGENTS.md" "$agents_md"
}

install_comate() {
  goal_rule='---
description: DevHarnessKit goal-first development protocol.
alwaysApply: true
---

This repository uses DevHarnessKit.

For any code change:
1. Do not start by editing code.
2. If `.agents/devharness/briefs/WORK_BRIEF.md` exists, summarize it for the user instead of listing raw Harness commands.
3. If Work Brief has `confirmation_required: true`, show the confirmation reason and checklist to the user, then wait for explicit user confirmation before answering the interaction or continuing work. Do not silently choose for the user.
4. If a business task asks for step-by-step delivery, pause after each step, list what was completed and what needs user confirmation, and wait before starting the next step.
5. If `.agents/devharness/briefs/AGENT_BRIEF.json` exists, read it before editing and use its `harness_commands` internally only.
6. Treat an Agent Brief with `current_action` = `completed` as historical. For a new user request, do not reuse that completed goal.
7. If there is no active goal for the current user request, first run `.agents/skills/devharness-goal-development/scripts/quickstart.sh --task "<latest user request verbatim>" --mode recommend --resume-existing --module "<best module>"`, then re-read Work Brief, Agent Brief, and GOAL_CONTEXT before editing.
8. Never copy the task from an old Work Brief or GOAL_CONTEXT when creating a new goal. The new goal task must match the latest user request.
9. If Agent Brief has a non-empty `goal_key` and `current_action` is not `completed`, do not start a new goal. Use the Agent Brief `goal_next`, `goal_step`, `goal_verify`, and `goal_complete` commands for that exact goal.
10. Do not assume a global `dhk` command exists. Execute the project-local scripts named in Agent Brief `script` fields, from the Agent Brief `cwd`.
11. Run `.agents/skills/devharness-goal-development/scripts/goal-next.sh` before every work step, using the Agent Brief goal when present.
12. Read `.agents/memory/exports/GOAL_CONTEXT.md`.
13. Before editing, compare GOAL_CONTEXT task with the latest user request. If they do not match, stop and create a new quickstart goal with the latest request.
14. Perform only current_action.
15. Even typo or documentation-only edits must have a lightweight patch goal; do not edit README, comments, or docs directly without a current goal.
16. If GOAL_CONTEXT contains graph_required=true or graph-assist, treat graph as evidence inside the current main action. Run graph helpers internally and record graph_snapshot, graph_context, impact_map, or graph_result evidence in the current goal step. Do not create a separate graph step. Do not use `graph impact --allow-stale` unless explicit approval evidence is present.
17. After any file edit, immediately run `.agents/skills/devharness-goal-development/scripts/goal-step.sh` with the required evidence from GOAL_CONTEXT.
   For verify/manual evidence, keep each `--field key=value` as its own CLI argument. Do not put `--field ...` inside `--evidence`. Use this command shape:
   `.agents/skills/devharness-goal-development/scripts/goal-step.sh --goal "<goal-key>" --summary "Record verification evidence" --field "compile_result=manual passed" --field "test_result=manual passed" --field "sensitive_result=passed" --field "manual_evidence_status=passed" --field "compile_scope=<module or changed classes>" --field "test_scope=<class or method>" --field "manual_evidence_path=<path>" --field "tester=<human or role>"`
18. If files changed but goal audit/next still shows step_count unchanged or "goal steps incomplete", self-repair by recording the missing goal step before replying.
19. If files were edited before a current goal existed, or under a goal whose task does not match the latest request, self-repair by creating a quickstart patch goal for the user request and recording the edit as a goal step before claiming completion.
20. Run `.agents/skills/devharness-goal-development/scripts/goal-verify.sh` before claiming completion.
21. Run `.agents/skills/devharness-goal-development/scripts/goal-complete.sh` only when ready_to_complete.

On a non-trivial error, run `dhk memory search --q "<error signature>"` first to reuse a known fix; after solving, run `dhk memory suggest` to queue a troubleshooting candidate for the user to confirm. Do not run `dhk memory confirm` or accept candidates yourself.

Never retry a failing or hanging build in a loop. If compile/test cannot run or fails for environmental reasons (missing deps, no local toolchain, manual policy), record it once as `compile_result=blocked: <reason>` plus risk/rollback evidence, then complete with manual evidence or report the friendly blocker to the user. Do not re-run the same failing command expecting a different result.

Do not bypass goal with direct workflow/spec/db commands. For memory, only search and suggest are allowed; never confirm memory or write secrets into a suggestion.
Do not show Agent Brief `harness_commands` to ordinary users unless they request debugging details.

Before replying that work is done, perform this Harness self-check:
- goal:
- current_action:
- goal_task_matches_user_request:
- edited_files:
- step_recorded_after_edit:
- verify:
- complete:

If `verify` is not ready or `complete` did not run, report the friendly blocker instead of saying the task is complete.
'
  write_text_file "$PROJECT_ROOT/.comate/rules/devharness-goal-protocol.mdr" "$goal_rule"
}

install_adapters() {
  validate_modes
  if [ "$DRY_RUN" = "true" ]; then
    run "install adapters target=$TARGET mode=$MODE"
    [ -n "$JAR_PATH" ] && run "install jar $JAR_PATH"
    return 0
  fi
  [ -d "$PROJECT_ROOT/.agents/skills/devharness-goal-development" ] || fail "missing .agents/skills/devharness-goal-development under $PROJECT_ROOT"
  remove_obsolete_graph_artifacts
  remove_legacy
  install_jar
  case "$TARGET" in
    claude) install_claude ;;
    opencode) install_opencode ;;
    comate) install_comate ;;
    all) install_claude; install_opencode; install_comate ;;
  esac
  chmod_scripts "$PROJECT_ROOT/.agents/skills"
}

write_install_state() {
  [ "$DRY_RUN" = "true" ] && return 0
  effective_modes_from_preset
  validate_modes
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
  "preset": "$PRESET",
  "compile_mode": "$COMPILE_MODE",
  "test_mode": "$TEST_MODE",
  "graph": "$GRAPH_MODE",
  "allow_stale_policy": "$ALLOW_STALE_POLICY",
  "remove_legacy": "$REMOVE_LEGACY",
  "managed_files": [
STATE
  first="true"
  managed_file_paths | while IFS= read -r rel; do
    [ -f "$PROJECT_ROOT/$rel" ] || continue
    hash=$(sha256_file "$PROJECT_ROOT/$rel")
    if [ "$first" = "true" ]; then
      first="false"
    else
      printf ',\n' >> "$state"
    fi
    printf '    {\n      "path": "%s",\n      "sha256": "%s"\n    }' "$(quote_json "$rel")" "$hash" >> "$state"
  done
  cat >> "$state" <<STATE
  ]
}
STATE
}

status_value() {
  [ -e "$1" ] && printf 'ok' || printf 'missing'
}

legacy_status() {
  if [ -e "$PROJECT_ROOT/.agents/skills/devharness-java-development" ] \
    || [ -e "$PROJECT_ROOT/.comate/rules/project-memory-bootstrap.mdr" ] \
    || [ -e "$PROJECT_ROOT/.comate/rules/java-development-guard.mdr" ]; then
    printf 'present'
  else
    printf 'removed'
  fi
}

obsolete_graph_status() {
  if [ -e "$PROJECT_ROOT/.agents/skills/devharness-graph-aware-development" ] \
    || [ -e "$PROJECT_ROOT/.claude/skills/devharness-graph-aware-development" ] \
    || [ -e "$PROJECT_ROOT/.comate/rules/devharness-graph-aware-protocol.mdr" ]; then
    printf 'present'
  else
    printf 'removed'
  fi
}

check_install_state_drift() {
  state="$PROJECT_ROOT/.agents/devharness/install-state.json"
  [ -f "$state" ] || return 0
  drift="false"
  current_path=""
  while IFS= read -r line; do
    case "$line" in
      *'"path": '*)
        current_path=$(extract_json_string "$line")
        ;;
      *'"sha256": '*)
        expected_hash=$(extract_json_string "$line")
        if [ -n "$current_path" ]; then
          file="$PROJECT_ROOT/$current_path"
          if [ ! -f "$file" ]; then
            printf 'doctor warning: managed file missing %s\n' "$current_path" >&2
            drift="true"
          else
            actual_hash=$(sha256_file "$file")
            if [ "$actual_hash" != "$expected_hash" ]; then
              printf 'doctor warning: managed file drift %s\n' "$current_path" >&2
              drift="true"
            fi
          fi
          current_path=""
        fi
        ;;
    esac
  done < "$state"
  [ "$drift" = "false" ]
}

status() {
  goal_skill=$(status_value "$PROJECT_ROOT/.agents/skills/devharness-goal-development/SKILL.md")
  graph_support="integrated"
  config=$(status_value "$PROJECT_ROOT/.agents/devharness/config.json")
  policy=$(status_value "$PROJECT_ROOT/.agents/devharness/policy.json")
  manifest=$(status_value "$PROJECT_ROOT/.agents/devharness/agent-manifest.json")
  graph_config=$(status_value "$PROJECT_ROOT/.agents/graph/config.json")
  install_state=$(status_value "$PROJECT_ROOT/.agents/devharness/install-state.json")
  claude_adapter=$(status_value "$PROJECT_ROOT/.claude/skills/devharness-goal-development/SKILL.md")
  claude_md=$(status_value "$PROJECT_ROOT/CLAUDE.md")
  opencode_adapter=$(status_value "$PROJECT_ROOT/AGENTS.md")
  comate_adapter=$(status_value "$PROJECT_ROOT/.comate/rules/devharness-goal-protocol.mdr")
  legacy=$(legacy_status)
  obsolete_graph=$(obsolete_graph_status)

  case "$STATUS_FORMAT" in
    json)
      cat <<JSON
{
  "goal_skill": "$goal_skill",
  "graph_support": "$graph_support",
  "config": "$config",
  "policy": "$policy",
  "manifest": "$manifest",
  "graph_config": "$graph_config",
  "install_state": "$install_state",
  "claude_adapter": "$claude_adapter",
  "claude_md": "$claude_md",
  "opencode_adapter": "$opencode_adapter",
  "comate_adapter": "$comate_adapter",
  "legacy_memory_first": "$legacy",
  "obsolete_graph_artifacts": "$obsolete_graph"
}
JSON
      ;;
    markdown)
      cat <<MD
# DevHarnessKit Status

| Item | Status |
|---|---|
| goal_skill | $goal_skill |
| graph_support | $graph_support |
| config | $config |
| policy | $policy |
| manifest | $manifest |
| graph_config | $graph_config |
| install_state | $install_state |
| claude_adapter | $claude_adapter |
| claude_md | $claude_md |
| opencode_adapter | $opencode_adapter |
| comate_adapter | $comate_adapter |
| legacy_memory_first | $legacy |
| obsolete_graph_artifacts | $obsolete_graph |
MD
      ;;
    *)
      log "DevHarnessKit status"
      log "goal_skill: $goal_skill"
      log "graph_support: $graph_support"
      log "config: $config"
      log "policy: $policy"
      log "manifest: $manifest"
      log "graph_config: $graph_config"
      log "install_state: $install_state"
      log "claude_adapter: $claude_adapter"
      log "claude_md: $claude_md"
      log "opencode_adapter: $opencode_adapter"
      log "comate_adapter: $comate_adapter"
      log "legacy_memory_first: $legacy"
      log "obsolete_graph_artifacts: $obsolete_graph"
      ;;
  esac
}

doctor() {
  status
  failed="false"
  for path in \
    "$PROJECT_ROOT/.agents/skills/devharness-goal-development/SKILL.md" \
    "$PROJECT_ROOT/.agents/devharness/config.json" \
    "$PROJECT_ROOT/.agents/devharness/policy.json" \
    "$PROJECT_ROOT/.agents/devharness/agent-manifest.json" \
    "$PROJECT_ROOT/.agents/devharness/install-state.json" \
    "$PROJECT_ROOT/.agents/graph/config.json"
  do
    if [ ! -e "$path" ]; then
      printf 'doctor warning: missing %s\n' "${path#$PROJECT_ROOT/}" >&2
      failed="true"
    fi
  done
  case "$TARGET" in
    claude|all)
      [ -e "$PROJECT_ROOT/.claude/skills/devharness-goal-development/SKILL.md" ] || { printf 'doctor warning: missing Claude goal adapter\n' >&2; failed="true"; }
      [ -e "$PROJECT_ROOT/CLAUDE.md" ] || { printf 'doctor warning: missing CLAUDE.md\n' >&2; failed="true"; }
      ;;
  esac
  case "$TARGET" in
    opencode|all)
      [ -e "$PROJECT_ROOT/AGENTS.md" ] || { printf 'doctor warning: missing AGENTS.md\n' >&2; failed="true"; }
      ;;
  esac
  case "$TARGET" in
    comate|all)
      [ -e "$PROJECT_ROOT/.comate/rules/devharness-goal-protocol.mdr" ] || { printf 'doctor warning: missing Comate goal rule\n' >&2; failed="true"; }
      ;;
  esac
  if [ "$(obsolete_graph_status)" = "present" ]; then
    printf 'doctor warning: obsolete graph-aware skill/rule artifacts are still present\n' >&2
    failed="true"
  fi
  check_install_state_drift || failed="true"
  if [ "$failed" = "true" ]; then
    printf 'doctor suggestion: run scripts/devharness-control-panel.sh repair --project-root "%s" --target %s --force\n' "$PROJECT_ROOT" "$TARGET" >&2
    exit 3
  fi
  log "doctor: ok"
}

plan() {
  effective_modes_from_preset
  validate_modes
  log "DevHarness plan"
  log "project_root: $PROJECT_ROOT"
  log "target: $TARGET"
  log "preset: $PRESET"
  log "mode: $MODE"
  log "compile_mode: $COMPILE_MODE"
  log "test_mode: $TEST_MODE"
  log "graph: $GRAPH_MODE"
  log "allow_stale_policy: $ALLOW_STALE_POLICY"
  log "will_write:"
  log "- .agents/devharness/config.json"
  log "- .agents/devharness/policy.json"
  log "- .agents/devharness/agent-manifest.json"
  log "- .agents/devharness/install-state.json"
  log "- .agents/graph/config.json"
  if [ "$MODE" = "link" ]; then
    log "will_link:"
  else
    log "will_copy:"
  fi
  case "$TARGET" in claude|all) log "- .claude/skills/devharness-goal-development"; log "- CLAUDE.md" ;; esac
  case "$TARGET" in opencode|all) log "- AGENTS.md" ;; esac
  case "$TARGET" in comate|all) log "- .comate/rules/devharness-goal-protocol.mdr" ;; esac
  log "will_chmod:"
  log "- .agents/skills/**/*.sh"
  log "- generated .claude/skills/**/*.sh"
  [ -n "$JAR_PATH" ] && { log "will_install_jar:"; log "- .agents/tools/devharness-kit/dhk.jar"; }
  log "will_remove:"
  log "- .agents/skills/devharness-graph-aware-development"
  log "- .claude/skills/devharness-graph-aware-development"
  log "- .comate/rules/devharness-graph-aware-protocol.mdr"
  [ "$REMOVE_LEGACY" = "true" ] && { log "- .agents/skills/devharness-java-development"; log "- .comate/rules/project-memory-bootstrap.mdr"; log "- .comate/rules/java-development-guard.mdr"; }
  log "warnings:"
  log "- plan is read-only and does not modify files"
  log "- Graph Lite remains heuristic advisory context"
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
      old_target="$TARGET"
      TARGET=claude uninstall
      TARGET=opencode uninstall
      TARGET=comate uninstall
      TARGET="$old_target"
      ;;
    *) fail "--target must be claude, opencode, comate, or all" ;;
  esac
  remove_obsolete_graph_artifacts
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
effective_modes_from_preset
validate_modes

case "$COMMAND" in
  configure)
    write_config
    write_manifest
    install_adapters
    write_install_state
    log "configure complete"
    ;;
  plan)
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
  refresh-state)
    write_install_state
    log "install-state refreshed"
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
