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
  (9, 'fixture v9 graph state', '2026-01-01T00:00:00Z');

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
INSERT INTO goal_run(goal_key, project_key, profile_key, task_name, module_name, mode, status, current_action, created_at, updated_at)
VALUES ('fixture-goal', 'demo', 'java-api-change-with-graph', 'Fixture graph goal', 'graph', 'api', 'verifying', 'graph_reimpact', '2026-01-01T00:00:00Z', '2026-01-01T00:00:00Z');

CREATE TABLE code_graph_snapshot (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  project_key TEXT NOT NULL,
  snapshot_key TEXT NOT NULL UNIQUE,
  provider TEXT NOT NULL DEFAULT 'lite',
  config_hash TEXT NOT NULL DEFAULT '',
  workspace_fingerprint TEXT NOT NULL DEFAULT '',
  status TEXT NOT NULL DEFAULT 'created',
  file_count INTEGER NOT NULL DEFAULT 0,
  node_count INTEGER NOT NULL DEFAULT 0,
  edge_count INTEGER NOT NULL DEFAULT 0,
  skipped_file_count INTEGER NOT NULL DEFAULT 0,
  max_file_bytes INTEGER NOT NULL DEFAULT 1048576,
  max_indexed_files INTEGER NOT NULL DEFAULT 5000,
  summary TEXT NOT NULL DEFAULT '',
  created_at TEXT NOT NULL,
  completed_at TEXT NOT NULL DEFAULT ''
);
INSERT INTO code_graph_snapshot(id, project_key, snapshot_key, provider, config_hash, workspace_fingerprint, status, file_count, node_count, edge_count, skipped_file_count, summary, created_at, completed_at)
VALUES (1, 'demo', 'fixture-snapshot', 'lite', 'config-hash', 'workspace-hash', 'completed', 1, 1, 1, 0, 'Fixture graph snapshot', '2026-01-01T00:00:00Z', '2026-01-01T00:00:00Z');

CREATE TABLE code_graph_file (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  snapshot_id INTEGER NOT NULL,
  relative_path TEXT NOT NULL,
  file_kind TEXT NOT NULL DEFAULT '',
  language TEXT NOT NULL DEFAULT '',
  content_hash TEXT NOT NULL DEFAULT '',
  size_bytes INTEGER NOT NULL DEFAULT 0,
  indexed INTEGER NOT NULL DEFAULT 1,
  skip_reason TEXT NOT NULL DEFAULT '',
  protected_file INTEGER NOT NULL DEFAULT 0,
  created_at TEXT NOT NULL
);
INSERT INTO code_graph_file(id, snapshot_id, relative_path, file_kind, language, content_hash, size_bytes, indexed, skip_reason, protected_file, created_at)
VALUES (1, 1, 'src/main/java/demo/App.java', 'source', 'java', 'file-hash', 128, 1, '', 0, '2026-01-01T00:00:00Z');

CREATE TABLE code_graph_node (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  snapshot_id INTEGER NOT NULL,
  file_id INTEGER NOT NULL,
  node_key TEXT NOT NULL,
  node_kind TEXT NOT NULL,
  name TEXT NOT NULL,
  qualified_name TEXT NOT NULL DEFAULT '',
  start_line INTEGER NOT NULL DEFAULT 0,
  end_line INTEGER NOT NULL DEFAULT 0,
  signature TEXT NOT NULL DEFAULT '',
  metadata TEXT NOT NULL DEFAULT ''
);
INSERT INTO code_graph_node(id, snapshot_id, file_id, node_key, node_kind, name, qualified_name, start_line, end_line, signature, metadata)
VALUES (1, 1, 1, 'class:demo.App', 'class', 'App', 'demo.App', 1, 10, 'class App', '{}');

CREATE TABLE code_graph_edge (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  snapshot_id INTEGER NOT NULL,
  source_node_key TEXT NOT NULL,
  target_node_key TEXT NOT NULL,
  edge_kind TEXT NOT NULL,
  file_id INTEGER,
  confidence INTEGER NOT NULL DEFAULT 100,
  metadata TEXT NOT NULL DEFAULT ''
);
INSERT INTO code_graph_edge(id, snapshot_id, source_node_key, target_node_key, edge_kind, file_id, confidence, metadata)
VALUES (1, 1, 'class:demo.App', 'class:demo.App', 'self', 1, 100, '{}');

CREATE TABLE code_graph_query_cache (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  snapshot_id INTEGER NOT NULL,
  query_key TEXT NOT NULL,
  query_type TEXT NOT NULL,
  input_hash TEXT NOT NULL DEFAULT '',
  result_path TEXT NOT NULL DEFAULT '',
  result_summary TEXT NOT NULL DEFAULT '',
  created_at TEXT NOT NULL,
  expires_at TEXT NOT NULL DEFAULT ''
);
INSERT INTO code_graph_query_cache(snapshot_id, query_key, query_type, input_hash, result_path, result_summary, created_at, expires_at)
VALUES (1, 'file:App.java', 'file', 'input-hash', '.agents/graph/IMPACT_MAP.md', 'Fixture impact', '2026-01-01T00:00:00Z', '');

CREATE TABLE goal_graph_binding (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  goal_key TEXT NOT NULL,
  snapshot_id INTEGER NOT NULL,
  binding_type TEXT NOT NULL DEFAULT 'used',
  artifact_path TEXT NOT NULL DEFAULT '',
  impact_hash TEXT NOT NULL DEFAULT '',
  created_at TEXT NOT NULL
);
INSERT INTO goal_graph_binding(goal_key, snapshot_id, binding_type, artifact_path, impact_hash, created_at)
VALUES ('fixture-goal', 1, 'impact_map', '.agents/graph/IMPACT_MAP.md', 'impact-hash', '2026-01-01T00:00:00Z');
