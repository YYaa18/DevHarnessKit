# DevHarnessKit Documentation Index

Use this page as the documentation entrypoint. README stays focused on the
shortest path to a working local setup; this index separates onboarding, core
contracts, experimental surfaces, release material, and historical design notes.

## Start Here

- [README](../README.md): project overview, build, quick start, and current
  release channel.
- [ARCHITECTURE](ARCHITECTURE.md): module boundaries and local-first design.
- [COMPATIBILITY](COMPATIBILITY.md): command, JSON, export, and schema contract
  by maturity level.
- [STABLE_CANDIDATE](STABLE_CANDIDATE.md): the narrow surface considered for
  stable-candidate work.
- [STABLE_CONTRACT](STABLE_CONTRACT.md): stable-beta CLI, JSON, export, and
  blocker-category contract for testing.
- [STABLE_EXPANSION_PLAN](STABLE_EXPANSION_PLAN.md): Linear-backed roadmap for
  promoting remaining beta/alpha surfaces toward stable contracts.
- [BRIEF_PROTOCOL](BRIEF_PROTOCOL.md): Work Brief, Agent Execution Brief, and
  GOAL_CONTEXT layering for agent-mounted usage.
- [PROFESSIONAL_KNOWLEDGE](PROFESSIONAL_KNOWLEDGE.md): advisory methodology
  and Java domain knowledge injected into goal and brief context.
- [SECURITY](../SECURITY.md): security model, limitations, and reporting.

## Daily Usage

- [CLI_INPUTS](CLI_INPUTS.md): file/stdin input, search explain, and backup
  command usage.
- [GOAL_CONFIGURATION](GOAL_CONFIGURATION.md): goal profiles, check policy,
  manual evidence, and graph-aware options.
- [BDD](BDD.md): stable BDD acceptance harness, evidence, and traceability
  bindings.
- [BDD_ACCEPTANCE_TEST_PLAN](BDD_ACCEPTANCE_TEST_PLAN.md): L0-L9 BDD acceptance
  checks from simple command smoke to goal-gated business validation.
- [GOAL_SYNC_STRICTNESS](GOAL_SYNC_STRICTNESS.md): how goal drives workflow/spec
  audit state.
- [DB_COMPATIBILITY](DB_COMPATIBILITY.md): MySQL 5.1 production floor, MySQL 8
  local usage, and Connector/J 5.1 constraints.
- [SENSITIVE_POLICY](SENSITIVE_POLICY.md): reject/redact/allow policy for
  financial PII and secret-like content.

## Experimental Surfaces

- [GRAPH_CONTEXT_LAYER](GRAPH_CONTEXT_LAYER.md): Graph Lite role in goal flows.
- [GRAPH_SCHEMA](GRAPH_SCHEMA.md): Graph Lite schema and snapshot contracts.
- [GRAPH_LIMITS](GRAPH_LIMITS.md): graph scan limits, skipped files, and
  truncation behavior.
- [GRAPH_LITE_PRECISION_TEST_PLAN](GRAPH_LITE_PRECISION_TEST_PLAN.md): staged
  precision, recall, false-positive, and ranking test plan for Graph Lite.
- [POLICY](POLICY.md): local project policy hooks and governance boundaries.
- [SKILL_CONTRACT](SKILL_CONTRACT.md): alpha skill contract metadata.
- [SKILL_EVALUATION](SKILL_EVALUATION.md): governed skill scoring and reports.
- [GOAL_METRICS_REPLAY](GOAL_METRICS_REPLAY.md): derived goal metrics and replay
  reports.
- [ROUTINE_LOCAL_CI_EXPORT](ROUTINE_LOCAL_CI_EXPORT.md): local/CI routine export
  plan.

## Release And Operations

- [CHANGELOG](../CHANGELOG.md): versioned user-facing changes.
- [RELEASE](../RELEASE.md): release checklist and local release gate.
- [ROADMAP](ROADMAP.md): stabilization priorities and non-goals.
- [MIGRATIONS](MIGRATIONS.md): SQLite schema compatibility and recovery policy.
- [EXPORT_CONTRACTS](EXPORT_CONTRACTS.md): generated Markdown export shapes.
- [JSON_OUTPUT](JSON_OUTPUT.md): JSON command output contracts.
- [THIRD_PARTY_NOTICES](../THIRD_PARTY_NOTICES.md): dependency license notices.
- [God Class Decomposition Baseline](decisions/god-class-decomposition-baseline.md):
  V0.8.4 service split contracts and size targets.
- [Secondary Goal Service Audit](decisions/goal-secondary-god-class-audit.md):
  remaining large goal/graph service boundaries.
- [Goal Orchestration Decomposition Baseline](decisions/goal-orchestration-decomposition-baseline.md):
  V0.8.5 goal coordinator and sync split contracts.
- [Graph Impact Service Audit](decisions/graph-impact-service-decomposition-audit.md):
  remaining graph impact service boundary proposal.

## Release Notes

- [v1.1.0](releases/v1.1.0.md)
- [v1.0.0](releases/v1.0.0.md)
- [v0.4.6-beta.1](releases/v0.4.6-beta.1.md)
- [v0.4.5-beta.1](releases/v0.4.5-beta.1.md)
- [v0.4.4-beta.1](releases/v0.4.4-beta.1.md)

## Historical Design Notes

These files are useful context, but they are not the current user contract:

- [PRD](PRD.md)
- [IMPLEMENTATION_PLAN](IMPLEMENTATION_PLAN.md)
- [DEVHARNESS_GOAL_CLI_ORCHESTRATION_PLAN](DEVHARNESS_GOAL_CLI_ORCHESTRATION_PLAN.md)
- [DEVHARNESS_SKILLS_REDESIGN_PLAN](DEVHARNESS_SKILLS_REDESIGN_PLAN.md)
- [DEVHARNESS_GRAPH_AWARE_GOAL_HARNESS_DETAILED_PLAN](DEVHARNESS_GRAPH_AWARE_GOAL_HARNESS_DETAILED_PLAN.md)
- [DEVHARNESS_GRAPH_AWARE_GOAL_HARNESS_EVALUATION_PLAN](DEVHARNESS_GRAPH_AWARE_GOAL_HARNESS_EVALUATION_PLAN.md)
- [features/](features/): dated implementation plans and acceptance checklists.
