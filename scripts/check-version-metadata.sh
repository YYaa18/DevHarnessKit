#!/usr/bin/env sh
set -eu

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
cd "$ROOT"

VERSION="${1:-${DHK_VERSION:-}}"
if [ -z "$VERSION" ]; then
  VERSION="$(mvn -q -DforceStdout help:evaluate -Dexpression=project.version)"
fi

[ -n "$VERSION" ] || {
  echo "ERROR: could not resolve Maven project.version" >&2
  exit 1
}

grep -F "## $VERSION" CHANGELOG.md >/dev/null || {
  echo "ERROR: CHANGELOG.md is missing heading: ## $VERSION" >&2
  exit 1
}

grep -F "Current beta release target: \`$VERSION\`" README.md >/dev/null || {
  echo "ERROR: README.md is missing current release target: $VERSION" >&2
  exit 1
}

[ -f "docs/releases/v$VERSION.md" ] || {
  echo "ERROR: missing release note docs/releases/v$VERSION.md" >&2
  exit 1
}

grep -F "v$VERSION" "docs/releases/v$VERSION.md" >/dev/null || {
  echo "ERROR: release note docs/releases/v$VERSION.md does not mention v$VERSION" >&2
  exit 1
}

echo "version metadata ok: $VERSION"
