# Release Checklist

Current release channel: `alpha / developer preview`.

## Before Tagging

1. Confirm README status table matches the code.
2. Confirm `THIRD_PARTY_NOTICES.md` matches runtime dependencies:

   ```bash
   mvn dependency:tree -Dscope=runtime
   ```

3. Run tests and package:

   ```bash
   mvn clean package
   java -jar target/dhk-cli-0.1.0-alpha-all.jar version
   ```

   `mvn test` and `mvn package` generate the JaCoCo report under `target/site/jacoco/`.

4. Confirm release artifacts exist:

   ```text
   target/dhk-cli-0.1.0-alpha-all.jar
   target/devharnesskit-0.1.0-alpha.zip
   target/devharnesskit-0.1.0-alpha.tar.gz
   ```

5. Generate checksums:

   ```bash
   cd target
   sha256sum dhk-cli-0.1.0-alpha-all.jar devharnesskit-0.1.0-alpha.zip devharnesskit-0.1.0-alpha.tar.gz > SHA256SUMS
   ```

## GitHub Release

The release workflow runs on `workflow_dispatch` and tags matching `v*`. Tag-triggered releases are created as drafts.

Release notes must state:

- DevHarness Kit is alpha/developer preview.
- Memory core is usable but schema compatibility is not stable.
- DB readonly is beta and requires read-only database credentials.
- Workflow/spec are experimental alpha surfaces.
- The shaded jar includes third-party dependencies listed in `THIRD_PARTY_NOTICES.md`.
