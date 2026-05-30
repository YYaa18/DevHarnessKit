#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)

detect_dist_root() {
  if [ -d "$SCRIPT_DIR/.agents/skills/devharness-goal-development" ] \
    && [ -f "$SCRIPT_DIR/scripts/devharness-control-panel.sh" ]; then
    printf '%s' "$SCRIPT_DIR"
    return 0
  fi
  parent=$(cd "$SCRIPT_DIR/.." && pwd)
  if [ -d "$parent/.agents/skills/devharness-goal-development" ]; then
    printf '%s' "$parent"
    return 0
  fi
  printf '%s' "$parent"
}

DIST_ROOT=$(detect_dist_root)
if [ -f "$SCRIPT_DIR/devharness-control-panel.sh" ]; then
  CONTROL_PANEL="$SCRIPT_DIR/devharness-control-panel.sh"
else
  CONTROL_PANEL="$DIST_ROOT/scripts/devharness-control-panel.sh"
fi

usage() {
  cat <<'USAGE'
install-agent-adapters.sh

DevHarnessKit 交互式安装向导。

不带参数运行会进入傻瓜式安装流程：
  scripts/install-agent-adapters.sh

向导会让你选择：
  - 目标项目绝对路径
  - Agent：文心快码、Claude Code、OpenCode 或全部
  - 使用场景：首次体验、公司 Java 项目、本机 Maven 项目、图分析建议、老旧 Java 项目
  - 是否创建第一份 Work Brief/Goal

高级/兼容用法仍然支持，并会转交给：
  scripts/devharness-control-panel.sh configure <args>

Examples:
  scripts/install-agent-adapters.sh
  scripts/install-agent-adapters.sh --project-root . --target comate --jar .agents/tools/devharness-kit/dhk.jar --force
USAGE
}

fail() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 1
}

# --- visual helpers (TTY-aware, ASCII-safe fallback) ---
if [ -t 1 ] && command -v tput >/dev/null 2>&1 && [ "$(tput colors 2>/dev/null || echo 0)" -ge 8 ]; then
  C_BOLD=$(tput bold 2>/dev/null || true)
  C_GREEN=$(tput setaf 2 2>/dev/null || true)
  C_CYAN=$(tput setaf 6 2>/dev/null || true)
  C_YELLOW=$(tput setaf 3 2>/dev/null || true)
  C_GRAY=$(tput setaf 8 2>/dev/null || true)
  C_RESET=$(tput sgr0 2>/dev/null || true)
else
  C_BOLD=''; C_GREEN=''; C_CYAN=''; C_YELLOW=''; C_GRAY=''; C_RESET=''
fi

hr()      { printf '%s──────────────────────────────────────────────%s\n' "$C_GRAY" "$C_RESET"; }
section() { printf '\n%s%s%s\n' "$C_BOLD$C_CYAN" "$1" "$C_RESET"; }
ok()      { printf '  %s✓%s %s\n' "$C_GREEN" "$C_RESET" "$1"; }
warn()    { printf '  %s⚠%s %s\n' "$C_YELLOW" "$C_RESET" "$1"; }
info()    { printf '  %s%s%s\n' "$C_GRAY" "$1" "$C_RESET"; }

ask() {
  prompt="$1"
  default="$2"
  if [ -n "$default" ]; then
    printf '%s [%s]: ' "$prompt" "$default" >&2
  else
    printf '%s: ' "$prompt" >&2
  fi
  answer=""
  read answer || answer=""
  if [ -z "$answer" ]; then
    printf '%s' "$default"
  else
    printf '%s' "$answer"
  fi
}

ask_yes_no() {
  prompt="$1"
  default="$2"
  while :; do
    answer=$(ask "$prompt" "$default")
    case "$(printf '%s' "$answer" | tr '[:upper:]' '[:lower:]')" in
      y|yes|是|true) printf 'yes'; return 0 ;;
      n|no|否|false) printf 'no'; return 0 ;;
      *) printf '请输入 yes 或 no。\n' >&2 ;;
    esac
  done
}

ask_choice() {
  prompt="$1"
  default="$2"
  allowed="$3"
  while :; do
    answer=$(ask "$prompt" "$default")
    case " $allowed " in
      *" $answer "*) printf '%s' "$answer"; return 0 ;;
      *) printf '可选值：%s\n' "$allowed" >&2 ;;
    esac
  done
}

default_project_root() {
  pwd
}

expand_user_path() {
  input="$1"
  case "$input" in
    "~") printf '%s' "$HOME" ;;
    "~/"*) printf '%s/%s' "$HOME" "${input#"~/"}" ;;
    "\$HOME") printf '%s' "$HOME" ;;
    "\$HOME/"*) printf '%s/%s' "$HOME" "${input#\$HOME/}" ;;
    "Desktop") printf '%s/Desktop' "$HOME" ;;
    "Desktop/"*) printf '%s/Desktop/%s' "$HOME" "${input#Desktop/}" ;;
    "桌面") printf '%s/Desktop' "$HOME" ;;
    "桌面/"*) printf '%s/Desktop/%s' "$HOME" "${input#桌面/}" ;;
    *) printf '%s' "$input" ;;
  esac
}

first_existing_jar() {
  root="$1"
  dist_root="$DIST_ROOT"
  for path in \
    "$dist_root/lib/dhk.jar" \
    "$dist_root/.agents/tools/devharness-kit/dhk.jar" \
    "$root/.agents/tools/devharness-kit/dhk.jar"
  do
    [ -f "$path" ] && { printf '%s' "$path"; return 0; }
  done
  found=$(find "$dist_root" "$root" \( -path '*/target/dhk-cli-*-all.jar' -o -path '*/lib/dhk.jar' \) -type f 2>/dev/null | sort | tail -1)
  printf '%s' "$found"
}

ensure_packaged_assets() {
  dist_root="$DIST_ROOT"
  [ -d "$dist_root/.agents/skills/devharness-goal-development" ] \
    || fail "missing packaged .agents/skills. Please run this installer from the DevHarnessKit release package, not from a lone script."
  [ -d "$dist_root/.agents/skills/devharness-graph-aware-development" ] \
    || fail "missing packaged graph skill. Please use the full DevHarnessKit release package."
}

copy_dir_from_dist() {
  src="$1"
  dest="$2"
  force="$3"
  [ -d "$src" ] || return 0
  if [ -d "$dest" ]; then
    src_abs=$(cd "$src" && pwd)
    dest_abs=$(cd "$dest" && pwd)
    [ "$src_abs" = "$dest_abs" ] && return 0
  fi
  if [ -e "$dest" ]; then
    if [ "$force" = "yes" ]; then
      rm -rf "$dest"
    else
      return 0
    fi
  fi
  mkdir -p "$(dirname "$dest")"
  cp -R "$src" "$dest"
}

install_packaged_assets_to_project() {
  project_root="$1"
  force="$2"
  dist_root="$DIST_ROOT"

  copy_dir_from_dist "$dist_root/.agents/skills" "$project_root/.agents/skills" "$force"
  copy_dir_from_dist "$dist_root/.agents/knowledge" "$project_root/.agents/knowledge" "$force"

  mkdir -p "$project_root/scripts"
  if [ "$force" = "yes" ] || [ ! -f "$project_root/scripts/devharness-control-panel.sh" ]; then
    cp "$dist_root/scripts/devharness-control-panel.sh" "$project_root/scripts/devharness-control-panel.sh"
  fi
  if [ "$force" = "yes" ] || [ ! -f "$project_root/scripts/install-agent-adapters.sh" ]; then
    cp "$dist_root/scripts/install-agent-adapters.sh" "$project_root/scripts/install-agent-adapters.sh"
  fi
  chmod +x "$project_root/scripts/devharness-control-panel.sh" "$project_root/scripts/install-agent-adapters.sh"
}

abs_file() {
  path=$(expand_user_path "$1")
  dir=$(dirname "$path")
  base=$(basename "$path")
  if [ -d "$dir" ]; then
    printf '%s/%s' "$(cd "$dir" && pwd)" "$base"
  else
    printf '%s' "$path"
  fi
}

detect_default_preset() {
  root="$1"
  if [ -f "$root/pom.xml" ]; then
    printf 'springboot-manual-ide-test'
  else
    printf 'demo-no-build'
  fi
}

choose_target() {
  section '第 2 步：选择要接入的 Agent'
  printf '  1) 文心快码 Comate（推荐，如果你正在用文心快码）\n'
  printf '  2) Claude Code\n'
  printf '  3) OpenCode\n'
  printf '  4) 全部安装\n'
  answer=$(ask_choice "请选择" "1" "1 2 3 4 comate claude opencode all")
  case "$answer" in
    1|comate) selected_target="comate"; selected_target_label="文心快码 Comate" ;;
    2|claude) selected_target="claude"; selected_target_label="Claude Code" ;;
    3|opencode) selected_target="opencode"; selected_target_label="OpenCode" ;;
    4|all) selected_target="all"; selected_target_label="全部 Agent" ;;
  esac
}

choose_scenario() {
  root="$1"
  default_choice="1"
  [ -f "$root/pom.xml" ] && default_choice="2"

  section '第 3 步：选择使用场景'
  printf '  1) 只是先体验一下，目标项目可能还是空的\n'
  printf '     不跑编译/测试，适合第一次试用。\n\n'
  printf '  2) 公司 Java 项目，编译/测试通常在 IDE、CI 或公司环境里做\n'
  printf '     DevHarnessKit 会要求人工验证证据，不会强行跑 Maven。\n\n'
  printf '  3) 本机就是标准 Maven 项目，可以自动跑 mvn compile/test\n'
  printf '     适合小型或本地依赖完整的项目。\n\n'
  printf '  4) 我只想先用图分析做影响面建议\n'
  printf '     图分析只做建议，不作为完成硬门槛。\n\n'
  printf '  5) 老旧 Java/JSP/MyBatis 项目\n'
  printf '     偏保守，默认人工验证并启用图分析。\n\n'

  answer=$(ask_choice "请选择场景" "$default_choice" "1 2 3 4 5 demo-no-build springboot-manual-ide-test springboot-auto-test graph-advisory legacy-jsp-servlet mybatis-monolith-manual-test")
  case "$answer" in
    1|demo-no-build)
      selected_preset="demo-no-build"
      selected_scenario_label="空项目/首次体验"
      control_panel_preset="springboot-manual-ide-test"
      compile_mode="disabled"
      test_mode="disabled"
      graph_mode="off"
      create_brief_default="yes"
      ;;
    2|springboot-manual-ide-test)
      selected_preset="springboot-manual-ide-test"
      selected_scenario_label="公司 Java 项目，IDE/CI 人工验证"
      control_panel_preset="springboot-manual-ide-test"
      compile_mode="manual"
      test_mode="manual"
      graph_mode="required"
      create_brief_default="no"
      ;;
    3|springboot-auto-test)
      selected_preset="springboot-auto-test"
      selected_scenario_label="标准 Maven 项目，本机自动验证"
      control_panel_preset="springboot-auto-test"
      compile_mode="auto"
      test_mode="auto"
      graph_mode="off"
      create_brief_default="no"
      ;;
    4|graph-advisory)
      selected_preset="graph-advisory"
      selected_scenario_label="图分析建议模式"
      control_panel_preset="springboot-auto-test"
      compile_mode="auto"
      test_mode="auto"
      graph_mode="advisory"
      create_brief_default="no"
      ;;
    5|legacy-jsp-servlet|mybatis-monolith-manual-test)
      selected_preset="mybatis-monolith-manual-test"
      selected_scenario_label="老旧 Java/JSP/MyBatis 项目"
      control_panel_preset="springboot-manual-ide-test"
      compile_mode="manual"
      test_mode="manual"
      graph_mode="required"
      create_brief_default="no"
      ;;
  esac
}

modes_for_preset() {
  preset="$1"
  case "$preset" in
    demo-no-build)
      printf 'springboot-manual-ide-test disabled disabled off'
      ;;
    springboot-auto-test)
      printf 'springboot-auto-test auto auto off'
      ;;
    graph-advisory)
      printf 'springboot-auto-test auto auto advisory'
      ;;
    legacy-jsp-servlet|mybatis-monolith-manual-test|springboot-manual-ide-test|manual-ide-test)
      printf 'springboot-manual-ide-test manual manual required'
      ;;
    *)
      printf 'springboot-manual-ide-test manual manual required'
      ;;
  esac
}

run_wizard() {
  [ -x "$CONTROL_PANEL" ] || fail "missing control panel: $CONTROL_PANEL"
  ensure_packaged_assets

  printf '\n'
  hr
  printf '%s  DevHarnessKit 安装向导%s\n' "$C_BOLD$C_CYAN" "$C_RESET"
  hr
  info '把 DevHarnessKit 安装到你的项目，并生成 Agent 规则。'
  info '当前目录是安装包，不是业务项目目录。'

  section '第 1 步：选择要安装到哪个项目'
  info '支持绝对路径，或以 ~ / Desktop 开头的快捷路径（如 ~/code/myapp）。'
  project_root=""
  while [ -z "$project_root" ]; do
    input=$(ask "项目路径" "")
    if [ -z "$input" ]; then
      printf '路径不能为空。\n' >&2
      continue
    fi
    input=$(expand_user_path "$input")
    case "$input" in
      /*) ;;
      *)
        printf '请输入绝对路径（以 / 开头），或用 ~ / Desktop 开头的快捷路径。\n' >&2
        continue
        ;;
    esac
    if [ "$input" = "/" ]; then
      printf '不能把系统根目录作为项目目录。\n' >&2
      continue
    fi
    if [ ! -d "$input" ]; then
      create_project=$(ask_yes_no "目录不存在，是否创建?" "yes")
      [ "$create_project" = "yes" ] || continue
      mkdir -p "$input"
    fi
    project_root=$(cd "$input" && pwd)
  done
  ok "项目目录：$project_root"

  choose_target
  target="$selected_target"
  choose_scenario "$project_root"
  preset="$selected_preset"

  jar_default=$(first_existing_jar "$project_root")
  if [ -n "$jar_default" ]; then
    jar_path="$jar_default"
    section '第 4 步：DevHarnessKit jar'
    ok "使用安装包内置 jar：$jar_path"
  else
    section '第 4 步：选择 DevHarnessKit jar'
    jar_path=$(ask "jar 路径" "")
    jar_path=$(expand_user_path "$jar_path")
    [ -f "$jar_path" ] || fail "jar not found: $jar_path"
  fi

  section '第 5 步：安装策略'
  if [ -d "$project_root/.agents/skills" ] || [ -d "$project_root/.agents/devharness" ]; then
    force=$(ask_yes_no "项目里已有 DevHarnessKit 文件，是否刷新覆盖?" "yes")
  else
    force="yes"
    ok "全新项目，将直接安装。"
  fi
  if [ "$force" = "yes" ]; then
    force_arg="--force"
  else
    force_arg=""
  fi

  create_brief=$(ask_yes_no "是否现在创建第一份 Work Brief/Goal?" "$create_brief_default")
  task=""
  module="demo"
  if [ "$create_brief" = "yes" ]; then
    task=$(ask "任务描述" "创建一个 README，说明这是文心快码接入 DevHarnessKit 的空项目体验")
    module=$(ask "模块名" "demo")
  fi

  printf '\n'
  hr
  printf '%s安装计划%s\n' "$C_BOLD" "$C_RESET"
  printf '  项目目录   : %s\n' "$project_root"
  printf '  Agent      : %s\n' "$selected_target_label"
  printf '  场景       : %s\n' "$selected_scenario_label"
  printf '  验证方式   : compile=%s  test=%s  graph=%s\n' "$compile_mode" "$test_mode" "$graph_mode"
  printf '  初始 Goal  : %s\n' "$create_brief"
  printf '  jar        : %s\n' "$jar_path"
  hr

  proceed=$(ask_yes_no "确认开始安装?" "yes")
  [ "$proceed" = "yes" ] || fail "用户取消安装"

  install_packaged_assets_to_project "$project_root" "$force"

  installed_jar="$project_root/.agents/tools/devharness-kit/dhk.jar"
  if [ "$(abs_file "$jar_path")" = "$(abs_file "$installed_jar")" ]; then
    configure_output=$("$CONTROL_PANEL" configure \
      --project-root "$project_root" \
      --target "$target" \
      --preset "$control_panel_preset" \
      --compile-mode "$compile_mode" \
      --test-mode "$test_mode" \
      --graph "$graph_mode" \
      $force_arg 2>&1) || { printf '%s\n' "$configure_output" >&2; exit 1; }
  else
    configure_output=$("$CONTROL_PANEL" configure \
      --project-root "$project_root" \
      --target "$target" \
      --preset "$control_panel_preset" \
      --compile-mode "$compile_mode" \
      --test-mode "$test_mode" \
      --graph "$graph_mode" \
      --jar "$jar_path" \
      $force_arg 2>&1) || { printf '%s\n' "$configure_output" >&2; exit 1; }
  fi

  [ -f "$installed_jar" ] || fail "installed jar missing: $installed_jar"
  java -jar "$installed_jar" configure init \
    --project-root "$project_root" \
    --preset "$preset" \
    --compile "$compile_mode" \
    --test "$test_mode" \
    --graph "$graph_mode" \
    --target "$target" \
    --force >/dev/null

  status_output=$("$CONTROL_PANEL" status --project-root "$project_root" --target "$target" --status-format text)
  section '安装完成'
  ok '配置已写入'
  case "$target" in
    comate)
      ok "文心快码规则：$(printf '%s\n' "$status_output" | sed -n 's/^comate_adapter: //p')"
      ;;
    claude)
      ok "Claude Code 适配：$(printf '%s\n' "$status_output" | sed -n 's/^claude_adapter: //p')"
      ;;
    opencode)
      ok "OpenCode 适配：$(printf '%s\n' "$status_output" | sed -n 's/^opencode_adapter: //p')"
      ;;
    all)
      ok 'Agent 适配：all'
      ;;
  esac
  ok "本地 jar：$installed_jar"

  # 装后验证：用安装好的 wrapper 跑一次 doctor，确认 agent 真实调用路径可用。
  # 只检查 banner 输出，不依赖退出码——刚装好、未 memory init 的项目 doctor 会返回 4，属正常。
  wrapper="$project_root/.agents/skills/devharness-goal-development/scripts/dhk.sh"
  if [ -x "$wrapper" ]; then
    verify_out=$("$wrapper" doctor 2>&1 || true)
    if printf '%s' "$verify_out" | grep -q 'DevHarness Kit doctor'; then
      ok '验证通过：wrapper + jar 可运行（dhk doctor）'
    else
      warn '验证未通过：wrapper 无法运行 dhk，请检查 jar 路径与脚本执行权限。'
    fi
  else
    warn "验证未通过：缺少可执行 wrapper（$wrapper）。"
  fi

  if [ "$create_brief" = "yes" ]; then
    quickstart_output=$(java -jar "$installed_jar" quickstart \
      --project-root "$project_root" \
      --preset "$preset" \
      --task "$task" \
      --module "$module" \
      --target "$target" \
      --graph "$graph_mode" \
      --mode recommend 2>&1) || { printf '%s\n' "$quickstart_output" >&2; exit 1; }
    work_brief_path=$(printf '%s\n' "$quickstart_output" | sed -n 's/^work_brief_path: //p')
    agent_brief_path=$(printf '%s\n' "$quickstart_output" | sed -n 's/^agent_brief_path: //p')
    goal_key=$(printf '%s\n' "$quickstart_output" | sed -n 's/^goal_key: //p')
    current_action=$(printf '%s\n' "$quickstart_output" | sed -n 's/^current_action: //p')
    section '已创建第一份工作说明'
    ok "Work Brief ：$work_brief_path"
    ok "Agent Brief：$agent_brief_path"
    ok "Goal       ：$goal_key"
    ok "当前动作   ：$current_action"
  fi

  section '下一步'
  info "1. 用 $selected_target_label 打开项目目录：$project_root"
  info '2. 对 Agent 说：读取项目规则、Work Brief 和 Agent Brief，按照 DevHarnessKit 流程完成当前任务。'
  printf '\n'
}

case "${1:-}" in
  -h|--help)
    usage
    exit 0
    ;;
  "")
    run_wizard
    ;;
  *)
    # Scripted package entrypoint performs a full local install. Advanced
    # adapter-only installs can call scripts/devharness-control-panel.sh install directly.
    project_root_arg="."
    force_arg="no"
    dry_run_arg="no"
    previous=""
    for arg in "$@"; do
      if [ "$previous" = "--project-root" ]; then
        project_root_arg="$arg"
        previous=""
        continue
      fi
      case "$arg" in
        --project-root=*)
          project_root_arg="${arg#--project-root=}"
          ;;
        --force)
          force_arg="yes"
          ;;
        --dry-run)
          dry_run_arg="yes"
          ;;
        --project-root)
          previous="--project-root"
          ;;
        *)
          previous=""
          ;;
      esac
    done
    project_root_arg=$(expand_user_path "$project_root_arg")
    mkdir -p "$project_root_arg"
    project_root_arg=$(cd "$project_root_arg" && pwd)
    ensure_packaged_assets
    if [ "$dry_run_arg" != "yes" ]; then
      install_packaged_assets_to_project "$project_root_arg" "$force_arg"
    fi
    exec "$CONTROL_PANEL" configure "$@"
    ;;
esac
