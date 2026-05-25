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
  (4, 'fixture v4', '2026-01-01T00:00:00Z');

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

CREATE TABLE spec_change (
  change_key TEXT PRIMARY KEY,
  project_key TEXT NOT NULL,
  title TEXT NOT NULL,
  summary TEXT NOT NULL DEFAULT '',
  module_name TEXT NOT NULL DEFAULT 'global',
  mode TEXT NOT NULL DEFAULT 'auto',
  status TEXT NOT NULL DEFAULT 'draft',
  priority TEXT NOT NULL DEFAULT 'normal',
  source_kind TEXT NOT NULL DEFAULT 'manual',
  created_by TEXT NOT NULL DEFAULT 'manual',
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  archived_at TEXT NOT NULL DEFAULT ''
);
INSERT INTO spec_change(change_key, project_key, title, summary, module_name, mode, status, priority, source_kind, created_by, created_at, updated_at, archived_at)
VALUES ('fixture-change', 'demo', 'Fixture change', 'Existing v4 spec state', 'order', 'api', 'draft', 'normal', 'manual', 'fixture', '2026-01-01T00:00:00Z', '2026-01-01T00:00:00Z', '');

CREATE TABLE spec_document (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  change_key TEXT NOT NULL,
  document_type TEXT NOT NULL,
  title TEXT NOT NULL,
  content TEXT NOT NULL,
  content_hash TEXT NOT NULL DEFAULT '',
  status TEXT NOT NULL DEFAULT 'draft',
  version INTEGER NOT NULL DEFAULT 1,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);
INSERT INTO spec_document(change_key, document_type, title, content, content_hash, status, version, created_at, updated_at)
VALUES ('fixture-change', 'design', 'Fixture design', 'Existing design body', 'hash-design', 'confirmed', 1, '2026-01-01T00:00:00Z', '2026-01-01T00:00:00Z');

CREATE TABLE spec_task (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  change_key TEXT NOT NULL,
  task_key TEXT NOT NULL,
  task_order INTEGER NOT NULL,
  title TEXT NOT NULL,
  description TEXT NOT NULL DEFAULT '',
  status TEXT NOT NULL DEFAULT 'pending',
  phase_key TEXT NOT NULL DEFAULT '',
  evidence TEXT NOT NULL DEFAULT '',
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  completed_at TEXT NOT NULL DEFAULT ''
);
INSERT INTO spec_task(change_key, task_key, task_order, title, description, status, phase_key, evidence, created_at, updated_at, completed_at)
VALUES ('fixture-change', 'T001', 1, 'Existing fixture task', 'Preserve task row', 'done', 'implement_minimal_change', 'done before upgrade', '2026-01-01T00:00:00Z', '2026-01-01T00:00:00Z', '2026-01-01T00:00:00Z');

CREATE TABLE spec_acceptance (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  change_key TEXT NOT NULL,
  acceptance_key TEXT NOT NULL,
  acceptance_order INTEGER NOT NULL,
  description TEXT NOT NULL,
  expected_result TEXT NOT NULL DEFAULT '',
  status TEXT NOT NULL DEFAULT 'pending',
  evidence TEXT NOT NULL DEFAULT '',
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  verified_at TEXT NOT NULL DEFAULT ''
);
INSERT INTO spec_acceptance(change_key, acceptance_key, acceptance_order, description, expected_result, status, evidence, created_at, updated_at, verified_at)
VALUES ('fixture-change', 'A001', 1, 'Existing fixture acceptance', 'Acceptance preserved', 'passed', 'passed before upgrade', '2026-01-01T00:00:00Z', '2026-01-01T00:00:00Z', '2026-01-01T00:00:00Z');

CREATE TABLE workflow_spec_binding (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  run_key TEXT NOT NULL,
  change_key TEXT NOT NULL,
  binding_type TEXT NOT NULL,
  created_at TEXT NOT NULL
);
INSERT INTO workflow_spec_binding(run_key, change_key, binding_type, created_at)
VALUES ('fixture-run', 'fixture-change', 'implements', '2026-01-01T00:00:00Z');

CREATE TABLE spec_event (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  project_key TEXT NOT NULL,
  change_key TEXT NOT NULL,
  event_type TEXT NOT NULL,
  level TEXT NOT NULL DEFAULT 'info',
  message TEXT NOT NULL,
  data TEXT NOT NULL DEFAULT '',
  created_at TEXT NOT NULL
);
INSERT INTO spec_event(project_key, change_key, event_type, level, message, data, created_at)
VALUES ('demo', 'fixture-change', 'change_created', 'info', 'Fixture spec created', '', '2026-01-01T00:00:00Z');
