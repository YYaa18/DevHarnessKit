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
   unzip -l target/devharnesskit-0.4.4-beta.1.zip | grep '.agents/skills/devharness-goal-development/scripts/goal-start.sh'
   unzip -l target/devharnesskit-0.4.4-beta.1.zip | grep '.agents/skills/devharness-goal-development/scripts/goal-start.bat'
   unzip -l target/devharnesskit-0.4.4-beta.1.zip | grep '.comate/rules/java-development-guard.mdr'
   tar -tzf target/devharnesskit-0.4.4-beta.1.tar.gz | grep 'lib/dhk.jar'
   ```

6. Confirm the archive jar and Unix wrapper can run:

   ```bash
   ARCHIVE_ROOT="$(mktemp -d)"
   unzip -q target/devharnesskit-0.4.4-beta.1.zip -d "$ARCHIVE_ROOT"
   java -jar "$ARCHIVE_ROOT/devharnesskit-0.4.4-beta.1/lib/dhk.jar" version
   "$ARCHIVE_ROOT/devharnesskit-0.4.4-beta.1/.agents/skills/devharness-goal-development/scripts/dhk.sh" version
   ```

   Windows `.bat` wrappers are verified by archive presence in this local checklist. Run them on Windows before a release promoted beyond alpha.

7. Run packaged CLI smoke:

   ```bash
   SMOKE_ROOT="$(mktemp -d)"
   JAR="target/dhk-cli-0.4.4-beta.1-all.jar"
   java -jar "$JAR" memory init --project-root "$SMOKE_ROOT"
   java -jar "$JAR" doctor --project-root "$SMOKE_ROOT" --json | grep '"command": "doctor"'
   java -jar "$JAR" memory export --project-root "$SMOKE_ROOT" --task "release smoke" --module global --json | grep '"command": "memory export"'
   GOAL_KEY="$(java -jar "$JAR" goal start --project-root "$SMOKE_ROOT" --profile bugfix --task "release smoke goal" --module global | sed -n 's/^goal_key: //p' | head -n 1)"
   test -n "$GOAL_KEY"
   java -jar "$JAR" goal status --project-root "$SMOKE_ROOT" --goal "$GOAL_KEY" --json | grep '"command": "goal status"'
   ```

8. Confirm alpha wording:

   ```bash
   grep -n "0.4.4-beta.1" README.md
   grep -n "developer preview" README.md RELEASE.md
   grep -n "not.*stable\\|Do not publish.*stable" README.md
   grep -n "stable-ish alpha" docs/COMPATIBILITY.md
   grep -n "downgrade" docs/COMPATIBILITY.md docs/MIGRATIONS.md
   ```

9. Generate checksums:

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
