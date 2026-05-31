#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
INSTALLER="$SCRIPT_DIR/scripts/install-agent-adapters.sh"

case "${1:-}" in
  -h|--help)
    cat <<'USAGE'
DevHarnessKit 安装向导

请从 DevHarnessKit 发布包目录运行：
  ./install.sh

向导会依次询问：
  - 你的代码项目绝对路径
  - 要接入哪个 Agent：文心快码、Claude Code、OpenCode 或全部
  - 使用场景：初始新项目、公司 Java 项目、本机 Maven 项目、图分析建议、老旧 Java 项目
  - 是否现在创建第一份 Work Brief/Goal

高级用户仍可直接传参数，例如：
  ./install.sh --project-root /path/to/project --target comate --force
USAGE
    exit 0
    ;;
esac

if [ ! -x "$INSTALLER" ]; then
  printf 'ERROR: missing installer: %s\n' "$INSTALLER" >&2
  printf 'Please run this script from the DevHarnessKit release package root.\n' >&2
  exit 1
fi

exec "$INSTALLER" "$@"
