# Release Checklist

Current release channel: `beta / developer preview`.

## Version Source

The release artifact version is Maven `project.version` from `pom.xml`.
Do not hardcode a concrete version in CI, release workflow, or local release
commands. Resolve it before packaging checks:

```bash
DHK_VERSION="$(mvn -q -DforceStdout help:evaluate -Dexpression=project.version)"
DHK_JAR="target/dhk-cli-${DHK_VERSION}-all.jar"
DHK_ZIP="target/devharnesskit-${DHK_VERSION}.zip"
DHK_TGZ="target/devharnesskit-${DHK_VERSION}.tar.gz"
```

`src/main/resources/com/devharnesskit/dhk/version.properties` is filtered from
the same Maven version so classpath test runs and packaged jars report the same
version source.

## Stable-Candidate Gate

Run the local gate before tagging:

```bash
scripts/release-gate.sh
```

For quick local checks after artifacts already exist:

```bash
scripts/release-gate.sh --skip-package --skip-smoke
```

The gate checks package output, version metadata, coverage threshold, version
smoke, archive contents, wrapper contracts, alpha/beta wording,
stable-candidate boundary docs, archive smoke, packaged CLI smoke, and
checksums.

## Before Tagging

1. Confirm README status table matches the code.
2. Confirm `THIRD_PARTY_NOTICES.md` matches runtime dependencies:

   ```bash
   mvn dependency:tree -Dscope=runtime
   ```

3. Run tests and package:

   ```bash
   mvn -B clean package
   java -jar "$DHK_JAR" version
   ```

   `mvn test` and `mvn package` generate the JaCoCo report under `target/site/jacoco/`.

4. Confirm release artifacts exist:

   ```bash
   test -f "$DHK_JAR"
   test -f "$DHK_ZIP"
   test -f "$DHK_TGZ"
   ```

5. Confirm version metadata and coverage gate:

   ```bash
   scripts/check-version-metadata.sh "$DHK_VERSION"
   scripts/check-coverage-threshold.sh 60
   ```

6. Confirm archive contents and wrapper contracts:

   ```bash
   unzip -l "$DHK_ZIP" | grep 'lib/dhk.jar'
   unzip -l "$DHK_ZIP" | grep 'LICENSE'
   unzip -l "$DHK_ZIP" | grep 'THIRD_PARTY_NOTICES.md'
   unzip -l "$DHK_ZIP" | grep '.agents/skills/devharness-goal-development/SKILL.md'
   unzip -l "$DHK_ZIP" | grep '.agents/skills/devharness-graph-aware-development/SKILL.md'
   unzip -l "$DHK_ZIP" | grep '.agents/skills/devharness-goal-development/scripts/dhk.sh'
   unzip -l "$DHK_ZIP" | grep '.agents/skills/devharness-goal-development/scripts/dhk.bat'
   unzip -l "$DHK_ZIP" | grep '.agents/skills/devharness-goal-development/scripts/goal-start.sh'
   unzip -l "$DHK_ZIP" | grep '.agents/skills/devharness-goal-development/scripts/goal-start.bat'
   unzip -l "$DHK_ZIP" | grep '.agents/skills/devharness-goal-development/scripts/goal-verify.sh'
   unzip -l "$DHK_ZIP" | grep '.agents/skills/devharness-goal-development/scripts/goal-verify.bat'
   unzip -l "$DHK_ZIP" | grep '.agents/skills/devharness-graph-aware-development/scripts/graph-impact.sh'
   unzip -l "$DHK_ZIP" | grep '.agents/skills/devharness-graph-aware-development/scripts/graph-impact.bat'
   unzip -l "$DHK_ZIP" | grep 'docs/INDEX.md'
   unzip -l "$DHK_ZIP" | grep 'scripts/check-version-metadata.sh'
   unzip -l "$DHK_ZIP" | grep 'scripts/check-coverage-threshold.sh'
   unzip -l "$DHK_ZIP" | grep '.comate/rules/devharness-goal-protocol.mdr'
   unzip -l "$DHK_ZIP" | grep '.comate/rules/devharness-graph-aware-protocol.mdr'
   unzip -l "$DHK_ZIP" | grep 'scripts/devharness-control-panel.sh'
   unzip -l "$DHK_ZIP" | grep 'scripts/install-agent-adapters.sh'
   ! unzip -l "$DHK_ZIP" | grep 'devharness-java-development'
   ! unzip -l "$DHK_ZIP" | grep -E 'project-memory-bootstrap.mdr|java-development-guard.mdr'
   tar -tzf "$DHK_TGZ" | grep 'lib/dhk.jar'
   ! grep -E 'target[\\/]+dhk-cli-[0-9][^ "$%]*-all\.jar' .agents/skills/devharness-goal-development/scripts/dhk.sh .agents/skills/devharness-goal-development/scripts/dhk.bat
   ```

7. Confirm the archive jar and Unix wrapper can run:

   ```bash
   ARCHIVE_ROOT="$(mktemp -d)"
   unzip -q "$DHK_ZIP" -d "$ARCHIVE_ROOT"
   DIST="$ARCHIVE_ROOT/devharnesskit-${DHK_VERSION}"
   java -jar "$DIST/lib/dhk.jar" version
   "$DIST/.agents/skills/devharness-goal-development/scripts/dhk.sh" version
   "$DIST/scripts/devharness-control-panel.sh" plan --project-root "$DIST" --target all --dry-run
   "$DIST/scripts/install-agent-adapters.sh" --project-root "$DIST" --target all --force --dry-run
   ```

   Windows `.bat` wrappers are verified by archive presence and dynamic jar
   fallback checks in this local gate. Run them on Windows before a release
   promoted beyond beta.

8. Run packaged CLI smoke:

   ```bash
   SMOKE_ROOT="$(mktemp -d)"
   java -jar "$DHK_JAR" memory init --project-root "$SMOKE_ROOT"
   java -jar "$DHK_JAR" doctor --project-root "$SMOKE_ROOT" --json | grep '"command": "doctor"'
   java -jar "$DHK_JAR" memory export --project-root "$SMOKE_ROOT" --task "release smoke" --module global --json | grep '"command": "memory export"'
   java -jar "$DHK_JAR" configure init --project-root "$SMOKE_ROOT" --preset springboot-manual-ide-test --force
   java -jar "$DHK_JAR" configure show --project-root "$SMOKE_ROOT" --json | grep '"compile_mode": "manual"'
   java -jar "$DHK_JAR" configure doctor --project-root "$SMOKE_ROOT" --json | grep '"command": "configure doctor"'
   GOAL_KEY="$(java -jar "$DHK_JAR" goal start --project-root "$SMOKE_ROOT" --profile bugfix --task "release smoke goal" --module global | sed -n 's/^goal_key: //p' | head -n 1)"
   test -n "$GOAL_KEY"
   java -jar "$DHK_JAR" goal status --project-root "$SMOKE_ROOT" --goal "$GOAL_KEY" --json | grep '"command": "goal status"'
   ```

9. Confirm agent adapter and verification-policy acceptance:

   ```bash
   test ! -d .agents/skills/devharness-java-development
   test -x scripts/install-agent-adapters.sh
   mvn -q -Dtest=GoalSkillPackagingTest test
   mvn -q -Dtest=ConfigureCommandIntegrationTest test
   grep -n "goal-first" README.md docs/features/DEVHARNESS_V0_4_5_ACCEPTANCE_CHECKLIST.md
   grep -n "manual evidence" README.md docs/GOAL_CONFIGURATION.md
   grep -n "devharness-config/v1-alpha" docs/GOAL_CONFIGURATION.md
   ```

10. Confirm control panel acceptance:

   ```bash
   CP_ROOT="$(mktemp -d)"
   mkdir -p "$CP_ROOT/.agents/skills"
   cp -R .agents/skills/devharness-goal-development "$CP_ROOT/.agents/skills/"
   cp -R .agents/skills/devharness-graph-aware-development "$CP_ROOT/.agents/skills/"
   scripts/devharness-control-panel.sh plan --project-root "$CP_ROOT" --target all --dry-run
   scripts/devharness-control-panel.sh configure --project-root "$CP_ROOT" --target all --preset springboot-manual-ide-test --compile-mode manual --test-mode manual --graph required --force
   scripts/devharness-control-panel.sh status --project-root "$CP_ROOT" --status-format json | grep '"install_state": "ok"'
   scripts/devharness-control-panel.sh status --project-root "$CP_ROOT" --status-format markdown | grep '| manifest | ok |'
   scripts/devharness-control-panel.sh doctor --project-root "$CP_ROOT" --target all
   scripts/devharness-control-panel.sh uninstall --project-root "$CP_ROOT" --target all --dry-run
   mvn -q -Dtest=GoalSkillPackagingTest test
   ```

11. Confirm beta and stable-candidate wording:

   ```bash
   grep -n "developer preview" README.md RELEASE.md
   grep -n "not production-ready" README.md
   grep -n "do not describe it" README.md
   grep -n "Beta core" docs/COMPATIBILITY.md
   grep -n "Stable candidate surface" docs/STABLE_CANDIDATE.md
   grep -n "Experimental surface" docs/STABLE_CANDIDATE.md
   grep -n "downgrade" docs/COMPATIBILITY.md docs/MIGRATIONS.md
   ```

12. Confirm the release goal completion evidence:

   ```bash
   test -f .agents/memory/exports/GOAL_SUMMARY.md
   test -f .agents/memory/exports/ARTIFACT_PASSPORT.json
   grep -n "ARTIFACT_PASSPORT.json" .agents/memory/exports/GOAL_SUMMARY.md
   java -jar "$DHK_JAR" artifact passport verify \
     --path .agents/memory/exports/ARTIFACT_PASSPORT.json
   ```

   Artifact Passport is release audit metadata. It does not replace code review,
   human risk review, compile/test results, or security review.

13. Generate checksums:

   ```bash
   cd target
   sha256sum "dhk-cli-${DHK_VERSION}-all.jar" "devharnesskit-${DHK_VERSION}.zip" "devharnesskit-${DHK_VERSION}.tar.gz" > SHA256SUMS
   ```

   On macOS:

   ```bash
   cd target
   shasum -a 256 "dhk-cli-${DHK_VERSION}-all.jar" "devharnesskit-${DHK_VERSION}.zip" "devharnesskit-${DHK_VERSION}.tar.gz" > SHA256SUMS
   ```

## GitHub Release

The release workflow runs on `workflow_dispatch` and tags matching `v*`.
Tag-triggered releases are created as drafts. The workflow resolves
`project.version` from Maven and uploads the matching jar, zip, tarball, and
`SHA256SUMS`.

Release notes must state:

- DevHarness Kit is beta/developer preview unless a later release explicitly
  promotes it.
- The current CLI, JSON, export, and schema compatibility contract is in
  `docs/COMPATIBILITY.md`.
- The stable-candidate boundary is in `docs/STABLE_CANDIDATE.md`.
- Memory core is usable but schema compatibility is not stable yet.
- DB readonly is beta and requires read-only database credentials.
- Graph, BDD, Skill Contract, Policy/Governance, Routine, and ECC Control Panel
  are experimental unless their release notes say otherwise.
- Downgrade migrations are unsupported unless release notes explicitly say
  otherwise.
- The shaded jar includes third-party dependencies listed in
  `THIRD_PARTY_NOTICES.md`.
