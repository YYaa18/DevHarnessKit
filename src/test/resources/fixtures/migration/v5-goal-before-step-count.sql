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
  (5, 'fixture v5', '2026-01-01T00:00:00Z');

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

CREATE TABLE goal_run (
  goal_key TEXT PRIMARY KEY,
  project_key TEXT NOT NULL,
  workflow_run_key TEXT NOT NULL DEFAULT '',
  spec_change_key TEXT NOT NULL DEFAULT '',
  profile_key TEXT NOT NULL,
  task_name TEXT NOT NULL,
  module_name TEXT NOT NULL DEFAULT 'global',
  mode TEXT NOT NULL DEFAULT 'auto',
  condition_text TEXT NOT NULL DEFAULT '',
  status TEXT NOT NULL DEFAULT 'created',
  current_action TEXT NOT NULL DEFAULT '',
  max_steps INTEGER NOT NULL DEFAULT 30,
  step_count INTEGER NOT NULL DEFAULT 0,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  completed_at TEXT NOT NULL DEFAULT ''
);
INSERT INTO goal_run(goal_key, project_key, workflow_run_key, spec_change_key, profile_key, task_name, module_name, mode, condition_text, status, current_action, max_steps, step_count, created_at, updated_at, completed_at)
VALUES ('fixture-goal', 'demo', '', '', 'java-api-change', 'Fixture goal', 'goal', 'debug', '', 'verifying', 'verify', 30, 2, '2026-01-01T00:00:00Z', '2026-01-01T00:00:00Z', '');

CREATE TABLE goal_check (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  goal_key TEXT NOT NULL,
  check_key TEXT NOT NULL,
  check_type TEXT NOT NULL,
  required INTEGER NOT NULL DEFAULT 1,
  command TEXT NOT NULL DEFAULT '',
  status TEXT NOT NULL DEFAULT 'pending',
  result_summary TEXT NOT NULL DEFAULT '',
  evidence_path TEXT NOT NULL DEFAULT '',
  checked_at TEXT NOT NULL DEFAULT '',
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  UNIQUE(goal_key, check_key),
  FOREIGN KEY (goal_key) REFERENCES goal_run(goal_key)
);
INSERT INTO goal_check(goal_key, check_key, check_type, required, command, status, result_summary, evidence_path, checked_at, created_at, updated_at)
VALUES ('fixture-goal', 'sensitive', 'sensitive', 1, '', 'passed', 'old v5 sensitive check', '', '2026-01-01T00:00:00Z', '2026-01-01T00:00:00Z', '2026-01-01T00:00:00Z');
