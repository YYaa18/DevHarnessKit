CREATE TABLE schema_version (
  version INTEGER PRIMARY KEY,
  description TEXT NOT NULL,
  applied_at TEXT NOT NULL
);
INSERT INTO schema_version(version, description, applied_at)
VALUES
  (1, 'fixture v1', '2026-01-01T00:00:00Z'),
  (2, 'fixture v2', '2026-01-01T00:00:00Z'),
  (3, 'fixture v3', '2026-01-01T00:00:00Z'),
  (4, 'fixture v4', '2026-01-01T00:00:00Z'),
  (5, 'fixture v5', '2026-01-01T00:00:00Z'),
  (6, 'fixture v6', '2026-01-01T00:00:00Z'),
  (7, 'fixture v7', '2026-01-01T00:00:00Z'),
  (8, 'fixture v8', '2026-01-01T00:00:00Z'),
  (9, 'fixture v9', '2026-01-01T00:00:00Z'),
  (10, 'fixture v10 bdd state', '2026-01-01T00:00:00Z');

CREATE TABLE project (
  project_key TEXT PRIMARY KEY,
  project_name TEXT NOT NULL,
  root_path TEXT NOT NULL,
  project_type TEXT NOT NULL DEFAULT 'unknown',
  language TEXT NOT NULL DEFAULT 'java',
  framework TEXT NOT NULL DEFAULT 'unknown',
  database_type TEXT NOT NULL DEFAULT 'unknown',
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);
INSERT INTO project(project_key, project_name, root_path, project_type, language, framework, database_type, created_at, updated_at)
VALUES ('demo', 'Demo', '/fixture/demo', 'service', 'java', 'spring', 'mysql', '2026-01-01T00:00:00Z', '2026-01-01T00:00:00Z');

CREATE TABLE bdd_feature (
  feature_key TEXT PRIMARY KEY,
  project_key TEXT NOT NULL,
  module_name TEXT NOT NULL DEFAULT 'global',
  title TEXT NOT NULL,
  description TEXT NOT NULL DEFAULT '',
  tags TEXT NOT NULL DEFAULT '',
  status TEXT NOT NULL DEFAULT 'draft',
  source_kind TEXT NOT NULL DEFAULT 'manual',
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);
INSERT INTO bdd_feature(feature_key, project_key, module_name, title, description, tags, status, source_kind, created_at, updated_at)
VALUES ('fixture-feature', 'demo', 'risk', 'Fixture feature', 'Existing BDD feature', 'bdd', 'active', 'fixture', '2026-01-01T00:00:00Z', '2026-01-01T00:00:00Z');

CREATE TABLE bdd_scenario (
  scenario_key TEXT PRIMARY KEY,
  feature_key TEXT NOT NULL,
  project_key TEXT NOT NULL,
  title TEXT NOT NULL,
  description TEXT NOT NULL DEFAULT '',
  scenario_type TEXT NOT NULL DEFAULT 'acceptance',
  priority TEXT NOT NULL DEFAULT 'normal',
  status TEXT NOT NULL DEFAULT 'draft',
  tags TEXT NOT NULL DEFAULT '',
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);
INSERT INTO bdd_scenario(scenario_key, feature_key, project_key, title, description, scenario_type, priority, status, tags, created_at, updated_at)
VALUES ('fixture-scenario', 'fixture-feature', 'demo', 'Fixture scenario', 'Existing scenario', 'acceptance', 'high', 'verified', 'regression', '2026-01-01T00:00:00Z', '2026-01-01T00:00:00Z');

CREATE TABLE bdd_step (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  scenario_key TEXT NOT NULL,
  step_order INTEGER NOT NULL,
  step_type TEXT NOT NULL,
  step_text TEXT NOT NULL,
  normalized_text TEXT NOT NULL DEFAULT '',
  created_at TEXT NOT NULL
);
INSERT INTO bdd_step(scenario_key, step_order, step_type, step_text, normalized_text, created_at)
VALUES ('fixture-scenario', 1, 'given', 'Given existing fixture state', 'existing fixture state', '2026-01-01T00:00:00Z');

CREATE TABLE bdd_binding (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  scenario_key TEXT NOT NULL,
  binding_type TEXT NOT NULL,
  binding_key TEXT NOT NULL,
  relation TEXT NOT NULL DEFAULT '',
  metadata TEXT NOT NULL DEFAULT '',
  created_at TEXT NOT NULL
);
INSERT INTO bdd_binding(scenario_key, binding_type, binding_key, relation, metadata, created_at)
VALUES ('fixture-scenario', 'goal', 'fixture-goal', 'covers', '{}', '2026-01-01T00:00:00Z');

CREATE TABLE bdd_evidence (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  scenario_key TEXT NOT NULL,
  goal_key TEXT NOT NULL DEFAULT '',
  evidence_type TEXT NOT NULL DEFAULT 'manual',
  status TEXT NOT NULL DEFAULT 'pending',
  evidence_path TEXT NOT NULL DEFAULT '',
  summary TEXT NOT NULL DEFAULT '',
  command TEXT NOT NULL DEFAULT '',
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);
INSERT INTO bdd_evidence(scenario_key, goal_key, evidence_type, status, evidence_path, summary, command, created_at, updated_at)
VALUES ('fixture-scenario', 'fixture-goal', 'manual', 'passed', '.agents/bdd/evidence/fixture.md', 'Fixture evidence', '', '2026-01-01T00:00:00Z', '2026-01-01T00:00:00Z');

CREATE TABLE bdd_quality_issue (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  project_key TEXT NOT NULL,
  feature_key TEXT NOT NULL DEFAULT '',
  scenario_key TEXT NOT NULL DEFAULT '',
  issue_type TEXT NOT NULL,
  severity TEXT NOT NULL DEFAULT 'warning',
  status TEXT NOT NULL DEFAULT 'open',
  message TEXT NOT NULL,
  created_at TEXT NOT NULL
);
INSERT INTO bdd_quality_issue(project_key, feature_key, scenario_key, issue_type, severity, status, message, created_at)
VALUES ('demo', 'fixture-feature', 'fixture-scenario', 'missing_when', 'warning', 'open', 'Fixture warning', '2026-01-01T00:00:00Z');
