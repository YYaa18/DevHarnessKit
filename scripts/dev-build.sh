#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JACOCO_VERSION="${DHK_JACOCO_VERSION:-}"

cd "$ROOT_DIR"

CMD=(mvn -q package)
if [[ -n "$JACOCO_VERSION" ]]; then
  CMD=(mvn -q "-Djacoco.version=$JACOCO_VERSION" package)
fi

if ! "${CMD[@]}"; then
  echo "dev-build failed." >&2
  echo "next_action: ensure Maven can resolve plugins, or retry with DHK_JACOCO_VERSION=<cached-version> ./scripts/dev-build.sh" >&2
  echo "example: DHK_JACOCO_VERSION=0.8.10 ./scripts/dev-build.sh" >&2
  exit 1
fi

echo "dev-build: passed"
