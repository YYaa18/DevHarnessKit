#!/usr/bin/env sh
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DHK="$SCRIPT_DIR/../../devharness-goal-development/scripts/dhk.sh"
"$DHK" graph index "$@" && exec "$DHK" graph export "$@"
