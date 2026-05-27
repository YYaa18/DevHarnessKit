#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
# Deprecated compatibility wrapper. Prefer:
#   scripts/devharness-control-panel.sh install ...
exec "$SCRIPT_DIR/devharness-control-panel.sh" install "$@"
