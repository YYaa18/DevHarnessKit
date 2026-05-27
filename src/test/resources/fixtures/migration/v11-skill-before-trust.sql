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
  (10, 'fixture v10', '2026-01-01T00:00:00Z'),
  (11, 'fixture v11 skill contract state', '2026-01-01T00:00:00Z');

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

CREATE TABLE skill_contract (
  skill_key TEXT PRIMARY KEY,
  version TEXT NOT NULL,
  task_type TEXT NOT NULL,
  risk_level TEXT NOT NULL DEFAULT 'medium',
  mode TEXT NOT NULL DEFAULT 'strict',
  data_access_level TEXT NOT NULL,
  allowed_commands TEXT NOT NULL DEFAULT '',
  forbidden_commands TEXT NOT NULL DEFAULT '',
  contract_json TEXT NOT NULL,
  source_path TEXT NOT NULL DEFAULT '',
  trusted INTEGER NOT NULL DEFAULT 0,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);
INSERT INTO skill_contract(skill_key, version, task_type, risk_level, mode, data_access_level, allowed_commands, forbidden_commands, contract_json, source_path, trusted, created_at, updated_at)
VALUES ('fixture-skill', '1.0.0', 'development', 'medium', 'strict', 'context', 'dhk goal next', 'dhk db sql', '{}', '.agents/skills/fixture-skill/contract.json', 1, '2026-01-01T00:00:00Z', '2026-01-01T00:00:00Z');
