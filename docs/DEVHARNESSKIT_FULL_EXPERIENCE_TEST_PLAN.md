# DevHarnessKit Full Experience Test Plan

This plan is for manually experiencing the local package at
`~/Desktop/DevHarnessKit-Package`. Run commands from a terminal on macOS unless
a Windows-only step says otherwise.

## 0. Test Setup

```bash
export PKG="$HOME/Desktop/DevHarnessKit-Package"
export JAR="$PKG/lib/dhk.jar"
export ROOT="$HOME/Desktop/DHK-Experience-Sandbox"
rm -rf "$ROOT"
mkdir -p "$ROOT"
```

Expected:

- `"$PKG"` exists and contains `lib/dhk.jar`, `install.sh`, `README.md`,
  `README.zh-CN.md`, `SECURITY.md`, `.agents/skills`, `.comate/rules`, and
  `scripts/`.
- `docs/`, `RELEASE.md`, `CONTRIBUTING.md`, and `CODE_OF_CONDUCT.md` are not
  required in the user package.

## 1. Package And CLI Smoke

```bash
java -jar "$JAR" version
java -jar "$JAR" help
java -jar "$JAR" completion bash | head
```

Expected:

- Version prints `DevHarness Kit 1.1.0`, `release_channel: stable`, and
  `schema_version: 16`.
- Help lists core commands: `memory`, `goal`, `bdd`, `graph`, `configure`,
  `workflow`, `spec`, `skill`, `artifact`, and `checkpoint`.
- Completion output is non-empty.

## 2. Installer And Agent Adapter Guidance

```bash
cd "$PKG"
scripts/install-agent-adapters.sh --help
scripts/devharness-control-panel.sh plan --project-root "$ROOT" --target all --dry-run
scripts/devharness-control-panel.sh configure \
  --project-root "$ROOT" \
  --target all \
  --preset springboot-manual-ide-test \
  --compile-mode manual \
  --test-mode manual \
  --graph advisory \
  --force
scripts/devharness-control-panel.sh status --project-root "$ROOT" --status-format markdown
scripts/devharness-control-panel.sh doctor --project-root "$ROOT" --target all
```

Expected:

- Installer help explains the guided flow and advanced compatibility mode.
- Control panel plan shows what will be installed without mutating files.
- Configure installs `.agents/skills`, `.comate/rules`, and local scripts.
- Status reports installed adapters as `ok`.
- Doctor can find the jar and wrappers.

Interactive installer experience:

```bash
scripts/install-agent-adapters.sh
```

Expected:

- When creating a Work Brief, the final guidance tells the user to open the
  generated Work Brief and tell the Agent: `按工作说明开始吧`.
- When not creating a Work Brief, the final guidance tells the user to give the
  Agent a normal task and mentions generated records under
  `.agents/devharness/briefs/`.

## 3. Configure, Doctor, Status, Readiness

```bash
java -jar "$JAR" configure show --project-root "$ROOT"
java -jar "$JAR" configure show --project-root "$ROOT" --json
java -jar "$JAR" configure doctor --project-root "$ROOT" --json
java -jar "$JAR" configure explain --key verification-policy
java -jar "$JAR" doctor --project-root "$ROOT"
java -jar "$JAR" doctor --project-root "$ROOT" --json
java -jar "$JAR" status --project-root "$ROOT" --json
java -jar "$JAR" readiness --project-root "$ROOT" --markdown --write "$ROOT/readiness.md"
```

Expected:

- Configuration shows manual compile/test policy and graph advisory mode.
- Doctor succeeds after memory/config initialization.
- Status/readiness include schema version, install state, and next command.
- `readiness.md` is created.

## 4. Work Brief, Agent Brief, And Interaction Requests

```bash
java -jar "$JAR" advise \
  --project-root "$ROOT" \
  --task "给订单新增备注字段" \
  --module order \
  --mode patch \
  --json
cat "$ROOT/.agents/devharness/briefs/WORK_BRIEF.md"
cat "$ROOT/.agents/devharness/briefs/AGENT_BRIEF.json"
```

Expected:

- Work Brief keeps stable anchors such as `## 建议模式` and
  `## 是否需要确认`.
- Work Brief includes machine-readable lines such as `recommendation: patch`
  plus user-friendly Chinese guidance.
- Agent Brief contains `harness_commands` and `agent_internal_only: true`.
- Work Brief does not tell users to copy low-level `dhk goal ...` commands.

Interaction request smoke:

```bash
java -jar "$JAR" brief show \
  --project-root "$ROOT" \
  --request "$(awk -F '\t' 'NR>1 && $10=="open" {print $1; exit}' "$ROOT/.agents/devharness/briefs/INTERACTION_REQUESTS.tsv")" \
  --json
```

Expected:

- Open requests are readable in JSON when present.
- If there is no open request, this step can be skipped.

## 5. Quickstart And Goal Core

```bash
GOAL_KEY="$(java -jar "$JAR" quickstart \
  --project-root "$ROOT" \
  --preset springboot-manual-ide-test \
  --task "体验 Goal 核心流程" \
  --module smoke \
  --mode patch \
  --resume-existing \
  | sed -n 's/^goal_key: //p' | head -n 1)"
echo "$GOAL_KEY"
java -jar "$JAR" goal status --project-root "$ROOT" --goal "$GOAL_KEY" --json
java -jar "$JAR" goal next --project-root "$ROOT" --goal "$GOAL_KEY"
java -jar "$JAR" goal evidence-template --project-root "$ROOT" --goal "$GOAL_KEY"
java -jar "$JAR" goal step \
  --project-root "$ROOT" \
  --goal "$GOAL_KEY" \
  --summary "Read package smoke files" \
  --changed-files "none" \
  --field goal_understanding="体验 Goal 核心流程" \
  --field assumptions="manual smoke"
java -jar "$JAR" goal export --project-root "$ROOT" --goal "$GOAL_KEY"
java -jar "$JAR" goal check --project-root "$ROOT" --goal "$GOAL_KEY" --all
java -jar "$JAR" goal evaluate --project-root "$ROOT" --goal "$GOAL_KEY" --json
java -jar "$JAR" goal verify --project-root "$ROOT" --goal "$GOAL_KEY"
java -jar "$JAR" goal audit --project-root "$ROOT" --goal "$GOAL_KEY"
java -jar "$JAR" goal recheck --project-root "$ROOT" --goal "$GOAL_KEY"
java -jar "$JAR" goal retrospective --project-root "$ROOT" --goal "$GOAL_KEY"
java -jar "$JAR" goal review-summary --project-root "$ROOT" --goal "$GOAL_KEY" --json
java -jar "$JAR" goal mr-summary --project-root "$ROOT" --goal "$GOAL_KEY" --write "$ROOT/goal-mr-summary.md"
```

Expected:

- Goal commands keep the agent on one current action.
- `GOAL_CONTEXT.md` is regenerated after goal state changes.
- Verification may report missing manual evidence until a verify step records
  `manual_evidence_status=passed`, compile/test scope, and evidence path.
- Retrospective commands produce Markdown, JSON, and written file output.

External reference smoke:

```bash
EXT_GOAL="$(java -jar "$JAR" goal start \
  --project-root "$ROOT" \
  --profile bugfix \
  --task "external ref smoke" \
  --module smoke \
  --external-ref "LOCAL-123" \
  --force-new | sed -n 's/^goal_key: //p' | head -n 1)"
java -jar "$JAR" goal status --project-root "$ROOT" --goal "$EXT_GOAL" --json | grep LOCAL-123
```

Expected:

- `external_ref` is visible in status/export outputs.

## 6. Memory Core

```bash
java -jar "$JAR" memory init --project-root "$ROOT"
MEM_ID="$(java -jar "$JAR" memory add \
  --project-root "$ROOT" \
  --type decision \
  --title "Manual smoke decision" \
  --content "Use local package for manual experience testing." \
  --module smoke \
  --tag smoke | sed -n 's/^memory_id: //p' | head -n 1)"
echo "$MEM_ID"
java -jar "$JAR" memory list --project-root "$ROOT" --json
java -jar "$JAR" memory search --project-root "$ROOT" --q "local package" --explain
java -jar "$JAR" memory confirm --project-root "$ROOT" --id "$MEM_ID"
java -jar "$JAR" memory export --project-root "$ROOT" --task "manual memory smoke"
java -jar "$JAR" memory checkpoint --project-root "$ROOT" --task "manual smoke" --summary "checkpoint smoke"
java -jar "$JAR" memory recover --project-root "$ROOT" --latest
java -jar "$JAR" memory backup --project-root "$ROOT" --out "$ROOT/memory-backup.sqlite"
```

Expected:

- Memory item can be added, found, confirmed, exported, checkpointed, recovered,
  and backed up.

## 7. BDD Acceptance Harness

```bash
java -jar "$JAR" bdd init --project-root "$ROOT"
java -jar "$JAR" bdd add \
  --project-root "$ROOT" \
  --feature checkout \
  --title "Checkout" \
  --scenario checkout-address \
  --scenario-title "Address validation"
java -jar "$JAR" bdd scenario create \
  --project-root "$ROOT" \
  --scenario checkout-address-line2 \
  --title "Optional address line" \
  --given "customer has a cart" \
  --when "customer edits address line 2" \
  --then "address is saved"
java -jar "$JAR" bdd list --project-root "$ROOT"
java -jar "$JAR" bdd scenario list --project-root "$ROOT" --feature checkout
java -jar "$JAR" bdd scenario show --project-root "$ROOT" --scenario checkout-address
java -jar "$JAR" bdd show --project-root "$ROOT" --feature checkout
java -jar "$JAR" bdd export --project-root "$ROOT" --feature checkout
java -jar "$JAR" bdd lint --project-root "$ROOT" --feature checkout
java -jar "$JAR" bdd evidence add \
  --project-root "$ROOT" \
  --scenario checkout-address \
  --summary "Manual evidence passed" \
  --evidence-path "$ROOT/readiness.md"
java -jar "$JAR" bdd verify --project-root "$ROOT" --scenario checkout-address
java -jar "$JAR" bdd coverage --project-root "$ROOT" --feature checkout
java -jar "$JAR" bdd bind-goal --project-root "$ROOT" --scenario checkout-address --goal "$GOAL_KEY"
java -jar "$JAR" bdd bind-graph --project-root "$ROOT" --scenario checkout-address --file src/main/java/Demo.java
java -jar "$JAR" bdd bind-test --project-root "$ROOT" --scenario checkout-address --class CheckoutTest --method validatesAddress
```

Expected:

- Feature/scenario records are created and exported.
- Lint and coverage report clear missing or covered evidence.
- Manual evidence can mark a scenario verifiable.
- Bind commands attach traceability without requiring source files to exist.

## 8. Graph Lite

Create a tiny source tree:

```bash
mkdir -p "$ROOT/src/main/java/com/example" "$ROOT/src/test/java/com/example"
cat > "$ROOT/src/main/java/com/example/UserService.java" <<'JAVA'
package com.example;
public class UserService {
    public boolean validEmail(String email) { return email != null && email.contains("@"); }
}
JAVA
cat > "$ROOT/src/test/java/com/example/UserServiceTest.java" <<'JAVA'
package com.example;
public class UserServiceTest {
    public void validEmail() { new UserService().validEmail("a@example.com"); }
}
JAVA
java -jar "$JAR" graph init --project-root "$ROOT"
java -jar "$JAR" graph doctor --project-root "$ROOT"
java -jar "$JAR" graph status --project-root "$ROOT" --json
java -jar "$JAR" graph index --project-root "$ROOT"
java -jar "$JAR" graph impact --project-root "$ROOT" --file src/main/java/com/example/UserService.java --depth 2
java -jar "$JAR" graph impact --project-root "$ROOT" --symbol UserService --depth 2 --json
java -jar "$JAR" graph export --project-root "$ROOT"
java -jar "$JAR" graph prune --project-root "$ROOT" --keep 1 --dry-run
```

Expected:

- Graph status shows snapshot metadata.
- Impact returns related files and recommended read files.
- Export writes graph context artifacts.

## 9. Workflow And Spec Traceability

```bash
java -jar "$JAR" workflow template seed --project-root "$ROOT"
java -jar "$JAR" workflow template list --project-root "$ROOT"
RUN_KEY="$(java -jar "$JAR" workflow start \
  --project-root "$ROOT" \
  --workflow api-change \
  --task "manual workflow smoke" \
  --module smoke | sed -n 's/^run_key: //p' | head -n 1)"
echo "$RUN_KEY"
java -jar "$JAR" workflow status --project-root "$ROOT" --run "$RUN_KEY"
java -jar "$JAR" workflow phase pass --project-root "$ROOT" --run "$RUN_KEY" --phase inspect_existing_code
java -jar "$JAR" workflow gate pass --project-root "$ROOT" --run "$RUN_KEY" --phase inspect_existing_code --gate context_ready
java -jar "$JAR" workflow export --project-root "$ROOT" --run "$RUN_KEY"
java -jar "$JAR" workflow artifact list --project-root "$ROOT" --run "$RUN_KEY"
java -jar "$JAR" workflow summary --project-root "$ROOT" --run "$RUN_KEY"

java -jar "$JAR" spec create --project-root "$ROOT" --change smoke-change --title "Smoke change"
java -jar "$JAR" spec document set --project-root "$ROOT" --change smoke-change --type proposal --content "Smoke proposal"
java -jar "$JAR" spec task add --project-root "$ROOT" --change smoke-change --task T001 --title "Run smoke"
java -jar "$JAR" spec acceptance add --project-root "$ROOT" --change smoke-change --acceptance A001 --description "Smoke passes"
java -jar "$JAR" spec bind-workflow --project-root "$ROOT" --change smoke-change --run "$RUN_KEY"
java -jar "$JAR" spec status --project-root "$ROOT" --change smoke-change
java -jar "$JAR" spec export --project-root "$ROOT" --change smoke-change
```

Expected:

- Workflow records phase/gate state and exports context.
- Spec records proposal, tasks, acceptance, and workflow binding.
- Avoid `spec archive` during normal smoke unless testing archive behavior.

## 10. Skill Contract And Governance

```bash
GOAL_SKILL="$PKG/.agents/skills/devharness-goal-development"
java -jar "$JAR" skill lint --project-root "$ROOT" --path "$GOAL_SKILL" --json
java -jar "$JAR" skill verify --project-root "$ROOT" --path "$GOAL_SKILL" --json
java -jar "$JAR" skill audit --project-root "$ROOT" --path "$GOAL_SKILL" --json
java -jar "$JAR" skill trust --project-root "$ROOT" --path "$GOAL_SKILL" --json
java -jar "$JAR" skill score --project-root "$ROOT" --goal "$GOAL_KEY" --json
java -jar "$JAR" skill report --project-root "$ROOT" --goal "$GOAL_KEY" --baseline-score 70
java -jar "$JAR" skill gate think-before-coding --project-root "$ROOT" --goal "$GOAL_KEY"
java -jar "$JAR" skill gate goal-driven --project-root "$ROOT" --goal "$GOAL_KEY"
java -jar "$JAR" skill gate simplicity --project-root "$ROOT" --goal "$GOAL_KEY"
java -jar "$JAR" skill gate surgical-change --project-root "$ROOT" --goal "$GOAL_KEY"
```

Expected:

- Built-in skill lint/verify/audit returns structured JSON.
- Gate commands explain pass/fail based on recorded goal evidence.

## 11. Artifact Passport And Checkpoints

```bash
CHK_ID="$(java -jar "$JAR" checkpoint request \
  --project-root "$ROOT" \
  --goal "$GOAL_KEY" \
  --type manual_review \
  --reason "Manual package smoke review" \
  --json | sed -n 's/.*"checkpoint_id": \([0-9][0-9]*\).*/\1/p' | head -n 1)"
echo "$CHK_ID"
java -jar "$JAR" checkpoint list --project-root "$ROOT" --goal "$GOAL_KEY"
java -jar "$JAR" checkpoint approve --project-root "$ROOT" --goal "$GOAL_KEY" --id "$CHK_ID" --approver tester --reason "approved"
java -jar "$JAR" artifact passport verify --path "$ROOT/.agents/memory/exports/ARTIFACT_PASSPORT.json"
```

Expected:

- Checkpoint request/list/approve works for a goal.
- Passport verify succeeds only after a passport artifact exists; if no
  completed goal has produced one yet, the command should fail clearly.

## 12. DB Inspection Beta

Use only a disposable or read-only database account.

```bash
java -jar "$JAR" db test --project-root "$ROOT" --host 127.0.0.1 --database devharness_smoke --user readonly --password-env DHK_DB_PASSWORD
java -jar "$JAR" db sql --project-root "$ROOT" --sql "select 1" --password-env DHK_DB_PASSWORD --format json
```

Expected:

- With a valid local DB, connection and guarded readonly SQL succeed.
- Without a DB, error output should be clear and actionable.
- Mutating SQL should be rejected by the safety guard.

## 13. Sensitive Policy And Local Policy Hooks

```bash
cat > "$ROOT/.agents/devharness/sensitive-policy.json" <<'JSON'
{
  "schema_version": "devharness-sensitive-policy/v1-alpha",
  "patterns": {
    "generic_secret": "reject"
  }
}
JSON
java -jar "$JAR" doctor --project-root "$ROOT" --json
```

Expected:

- Doctor reports the configured policy.
- Goal verify sensitive checks reject evidence or files containing known secret
  patterns.

## 14. Growth And Knowledge Candidate Flow

```bash
java -jar "$JAR" growth export --project-root "$ROOT" --json
java -jar "$JAR" growth review --project-root "$ROOT"
java -jar "$JAR" brief knowledge --project-root "$ROOT" --json
```

Expected:

- Growth and knowledge commands expose advisory material.
- Nothing becomes confirmed memory unless explicitly confirmed.

## 15. Windows Smoke To Run Separately

On Windows with Java 17+:

```bat
set PKG=%USERPROFILE%\Desktop\DevHarnessKit-Package
java -jar "%PKG%\lib\dhk.jar" version
"%PKG%\.agents\skills\devharness-goal-development\scripts\dhk.bat" version
"%PKG%\.agents\skills\devharness-goal-development\scripts\dhk.bat" help
"%PKG%\.agents\skills\devharness-goal-development\scripts\goal-next.bat" --goal WINDOWS-SMOKE-NOT-FOUND
```

Expected:

- `.bat` wrappers find `lib/dhk.jar` without a hardcoded version.
- Chinese output is readable in a UTF-8 terminal.

## 16. Pass/Fail Recording

For each section, record:

- environment: OS, Java version, terminal, package path
- commands run
- observed output summary
- pass/fail
- screenshots or logs when behavior is surprising
- whether the issue is product behavior, documentation, environment, or test
  plan ambiguity
