# Release Checklist

Current release channel: `beta / developer preview`.

## Before Tagging

1. Confirm README status table matches the code.
2. Confirm `THIRD_PARTY_NOTICES.md` matches runtime dependencies:

   ```bash
   mvn dependency:tree -Dscope=runtime
   ```

3. Run tests and package:

   ```bash
   mvn -B clean package
   java -jar target/dhk-cli-0.4.4-beta.1-all.jar version
   ```

   `mvn test` and `mvn package` generate the JaCoCo report under `target/site/jacoco/`.

4. Confirm release artifacts exist:

   ```text
   target/dhk-cli-0.4.4-beta.1-all.jar
   target/devharnesskit-0.4.4-beta.1.zip
   target/devharnesskit-0.4.4-beta.1.tar.gz
   ```

5. Confirm archive contents:

   ```bash
   unzip -l target/devharnesskit-0.4.4-beta.1.zip | grep 'lib/dhk.jar'
   unzip -l target/devharnesskit-0.4.4-beta.1.zip | grep 'LICENSE'
   unzip -l target/devharnesskit-0.4.4-beta.1.zip | grep 'THIRD_PARTY_NOTICES.md'
   unzip -l target/devharnesskit-0.4.4-beta.1.zip | grep '.agents/skills/devharness-goal-development/SKILL.md'
   unzip -l target/devharnesskit-0.4.4-beta.1.zip | grep '.agents/skills/devharness-graph-aware-development/SKILL.md'
   unzip -l target/devharnesskit-0.4.4-beta.1.zip | grep '.agents/skills/devharness-goal-development/scripts/goal-start.sh'
   unzip -l target/devharnesskit-0.4.4-beta.1.zip | grep '.agents/skills/devharness-goal-development/scripts/goal-start.bat'
   unzip -l target/devharnesskit-0.4.4-beta.1.zip | grep '.comate/rules/devharness-goal-protocol.mdr'
   unzip -l target/devharnesskit-0.4.4-beta.1.zip | grep '.comate/rules/devharness-graph-aware-protocol.mdr'
   unzip -l target/devharnesskit-0.4.4-beta.1.zip | grep 'scripts/devharness-control-panel.sh'
   unzip -l target/devharnesskit-0.4.4-beta.1.zip | grep 'scripts/install-agent-adapters.sh'
   ! unzip -l target/devharnesskit-0.4.4-beta.1.zip | grep 'devharness-java-development'
   tar -tzf target/devharnesskit-0.4.4-beta.1.tar.gz | grep 'lib/dhk.jar'
   ```

6. Confirm the archive jar and Unix wrapper can run:

   ```bash
   ARCHIVE_ROOT="$(mktemp -d)"
   unzip -q target/devharnesskit-0.4.4-beta.1.zip -d "$ARCHIVE_ROOT"
   java -jar "$ARCHIVE_ROOT/devharnesskit-0.4.4-beta.1/lib/dhk.jar" version
   "$ARCHIVE_ROOT/devharnesskit-0.4.4-beta.1/.agents/skills/devharness-goal-development/scripts/dhk.sh" version
   "$ARCHIVE_ROOT/devharnesskit-0.4.4-beta.1/scripts/devharness-control-panel.sh" plan --project-root "$ARCHIVE_ROOT/devharnesskit-0.4.4-beta.1" --target all --dry-run
   "$ARCHIVE_ROOT/devharnesskit-0.4.4-beta.1/scripts/install-agent-adapters.sh" --project-root "$ARCHIVE_ROOT/devharnesskit-0.4.4-beta.1" --target all --dry-run
   ```

   Windows `.bat` wrappers are verified by archive presence in this local checklist. Run them on Windows before a release promoted beyond alpha.

7. Run packaged CLI smoke:

   ```bash
   SMOKE_ROOT="$(mktemp -d)"
   JAR="target/dhk-cli-0.4.4-beta.1-all.jar"
   java -jar "$JAR" memory init --project-root "$SMOKE_ROOT"
   java -jar "$JAR" doctor --project-root "$SMOKE_ROOT" --json | grep '"command": "doctor"'
   java -jar "$JAR" memory export --project-root "$SMOKE_ROOT" --task "release smoke" --module global --json | grep '"command": "memory export"'
   java -jar "$JAR" configure init --project-root "$SMOKE_ROOT" --preset springboot-manual-ide-test --force
   java -jar "$JAR" configure show --project-root "$SMOKE_ROOT" --json | grep '"compile_mode": "manual"'
   java -jar "$JAR" configure doctor --project-root "$SMOKE_ROOT" --json | grep '"command": "configure doctor"'
   GOAL_KEY="$(java -jar "$JAR" goal start --project-root "$SMOKE_ROOT" --profile bugfix --task "release smoke goal" --module global | sed -n 's/^goal_key: //p' | head -n 1)"
   test -n "$GOAL_KEY"
   java -jar "$JAR" goal status --project-root "$SMOKE_ROOT" --goal "$GOAL_KEY" --json | grep '"command": "goal status"'
   ```

8. Confirm v0.4.5 agent adapter and verification-policy acceptance:

   ```bash
   test ! -d .agents/skills/devharness-java-development
   test -x scripts/install-agent-adapters.sh
   mvn -q -Dtest=GoalSkillPackagingTest test
   mvn -q -Dtest=ConfigureCommandIntegrationTest test
   grep -n "goal-first" README.md docs/features/DEVHARNESS_V0_4_5_ACCEPTANCE_CHECKLIST.md
   grep -n "manual evidence" README.md docs/GOAL_CONFIGURATION.md
   grep -n "devharness-config/v1-alpha" docs/GOAL_CONFIGURATION.md
   ```

9. Confirm v0.4.6 control panel acceptance:

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

10. Confirm alpha wording:

   ```bash
   grep -n "0.4.4-beta.1" README.md
   grep -n "developer preview" README.md RELEASE.md
   grep -n "not.*stable\\|Do not publish.*stable" README.md
   grep -n "stable-ish alpha" docs/COMPATIBILITY.md
   grep -n "downgrade" docs/COMPATIBILITY.md docs/MIGRATIONS.md
   ```

11. Confirm the release goal completion evidence:

   ```bash
   test -f .agents/memory/exports/GOAL_SUMMARY.md
   test -f .agents/memory/exports/ARTIFACT_PASSPORT.json
   grep -n "ARTIFACT_PASSPORT.json" .agents/memory/exports/GOAL_SUMMARY.md
   java -jar target/dhk-cli-0.4.4-beta.1-all.jar artifact passport verify \
     --path .agents/memory/exports/ARTIFACT_PASSPORT.json
   ```

   Artifact Passport is release audit metadata. It does not replace code review,
   human risk review, compile/test results, or security review.

12. Generate checksums:

   ```bash
   cd target
   sha256sum dhk-cli-0.4.4-beta.1-all.jar devharnesskit-0.4.4-beta.1.zip devharnesskit-0.4.4-beta.1.tar.gz > SHA256SUMS
   ```

   On macOS:

   ```bash
   cd target
   shasum -a 256 dhk-cli-0.4.4-beta.1-all.jar devharnesskit-0.4.4-beta.1.zip devharnesskit-0.4.4-beta.1.tar.gz > SHA256SUMS
   ```

## GitHub Release

The release workflow runs on `workflow_dispatch` and tags matching `v*`. Tag-triggered releases are created as drafts.

Release notes must state:

- DevHarness Kit is alpha/developer preview.
- The current CLI, JSON, export, and schema compatibility contract is in `docs/COMPATIBILITY.md`.
- Memory core is usable but schema compatibility is not stable.
- DB readonly is beta and requires read-only database credentials.
- Workflow/spec are experimental alpha surfaces.
- Downgrade migrations are unsupported unless release notes explicitly say otherwise.
- The shaded jar includes third-party dependencies listed in `THIRD_PARTY_NOTICES.md`.
