#!/usr/bin/env sh
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
for arg in "$@"; do
  if [ "$arg" = "--allow-stale" ] && [ "${DHK_ALLOW_STALE_APPROVED:-}" != "true" ]; then
    echo "graph-impact.sh blocks --allow-stale unless DHK_ALLOW_STALE_APPROVED=true is set by a human/policy approval." >&2
    exit 3
  fi
done
exec "$SCRIPT_DIR/dhk.sh" graph impact "$@"
