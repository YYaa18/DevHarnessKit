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

grep -F "\`$VERSION\`" SECURITY.md >/dev/null || {
  echo "ERROR: SECURITY.md is missing supported version: $VERSION" >&2
  exit 1
}

grep -F 'dhk-cli-${DHK_VERSION}-all.jar' CONTRIBUTING.md >/dev/null || {
  echo "ERROR: CONTRIBUTING.md should use the Maven-derived DHK_VERSION jar name" >&2
  exit 1
}

grep -F 'dhk-cli-<version>-all.jar' THIRD_PARTY_NOTICES.md >/dev/null || {
  echo "ERROR: THIRD_PARTY_NOTICES.md should use a version placeholder for the shaded jar" >&2
  exit 1
}

if grep -E 'v0\.4\.4-beta\.1|dhk-cli-0\.1\.0-alpha-all\.jar' \
  SECURITY.md CONTRIBUTING.md THIRD_PARTY_NOTICES.md >/dev/null; then
  echo "ERROR: stale hard-coded version reference found in release governance docs" >&2
  exit 1
fi

echo "version metadata ok: $VERSION"
