CREATE TABLE schema_version (
  version INTEGER PRIMARY KEY,
  description TEXT NOT NULL,
  applied_at TEXT NOT NULL
);
INSERT INTO schema_version(version, description, applied_at)
VALUES
  (1, 'fixture v1', '2026-01-01T00:00:00Z'),
  (2, 'fixture v2 workflow state', '2026-01-01T00:00:00Z');

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

CREATE TABLE memory_item (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  project_key TEXT NOT NULL,
  module_name TEXT NOT NULL DEFAULT 'global',
  memory_type TEXT NOT NULL,
  scope TEXT NOT NULL DEFAULT 'project',
  title TEXT NOT NULL,
  content TEXT NOT NULL,
  tags TEXT NOT NULL DEFAULT '',
  status TEXT NOT NULL DEFAULT 'draft',
  confidence INTEGER NOT NULL DEFAULT 50,
  source_kind TEXT NOT NULL DEFAULT 'manual',
  confirmed_at TEXT NOT NULL DEFAULT '',
  confirmed_by TEXT NOT NULL DEFAULT '',
  source_files TEXT NOT NULL DEFAULT '',
  evidence TEXT NOT NULL DEFAULT '',
  effective_from TEXT NOT NULL DEFAULT '',
  effective_to TEXT NOT NULL DEFAULT '',
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  last_used_at TEXT,
  use_count INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE checkpoint (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  project_key TEXT NOT NULL,
  task_name TEXT NOT NULL,
  module_name TEXT NOT NULL DEFAULT 'global',
  summary TEXT NOT NULL,
  changed_files TEXT NOT NULL DEFAULT '',
  pending_items TEXT NOT NULL DEFAULT '',
  verify_status TEXT NOT NULL DEFAULT '',
  next_read_files TEXT NOT NULL DEFAULT '',
  created_at TEXT NOT NULL
);

CREATE TABLE workflow_template (
  workflow_key TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  description TEXT NOT NULL DEFAULT '',
  category TEXT NOT NULL DEFAULT 'development',
  target_mode TEXT NOT NULL DEFAULT 'auto',
  status TEXT NOT NULL DEFAULT 'active',
  version INTEGER NOT NULL DEFAULT 1,
  source_kind TEXT NOT NULL DEFAULT 'seed',
  source_ref TEXT NOT NULL DEFAULT '',
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);
INSERT INTO workflow_template(workflow_key, name, description, category, target_mode, status, version, source_kind, source_ref, created_at, updated_at)
VALUES ('api-change', 'API Change', 'Fixture workflow', 'development', 'api', 'active', 1, 'fixture', '', '2026-01-01T00:00:00Z', '2026-01-01T00:00:00Z');

CREATE TABLE workflow_phase_template (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  workflow_key TEXT NOT NULL,
  phase_key TEXT NOT NULL,
  phase_name TEXT NOT NULL,
  phase_order INTEGER NOT NULL,
  phase_type TEXT NOT NULL,
  required INTEGER NOT NULL DEFAULT 1,
  instruction TEXT NOT NULL DEFAULT '',
  expected_output TEXT NOT NULL DEFAULT '',
  default_gate_policy TEXT NOT NULL DEFAULT 'none',
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);
INSERT INTO workflow_phase_template(workflow_key, phase_key, phase_name, phase_order, phase_type, required, instruction, expected_output, default_gate_policy, created_at, updated_at)
VALUES ('api-change', 'export_context', 'Export Context', 1, 'context', 1, 'Export context.', 'Context exists.', 'hard', '2026-01-01T00:00:00Z', '2026-01-01T00:00:00Z');

CREATE TABLE workflow_gate_template (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  workflow_key TEXT NOT NULL,
  phase_key TEXT NOT NULL,
  gate_key TEXT NOT NULL,
  gate_name TEXT NOT NULL,
  gate_type TEXT NOT NULL,
  severity TEXT NOT NULL DEFAULT 'hard',
  rule_text TEXT NOT NULL,
  check_command TEXT NOT NULL DEFAULT '',
  expected_result TEXT NOT NULL DEFAULT '',
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);
INSERT INTO workflow_gate_template(workflow_key, phase_key, gate_key, gate_name, gate_type, severity, rule_text, check_command, expected_result, created_at, updated_at)
VALUES ('api-change', 'export_context', 'current_context_exists', 'Current Context Exists', 'file_exists', 'hard', 'CURRENT_CONTEXT.md exists.', '', 'Context exists.', '2026-01-01T00:00:00Z', '2026-01-01T00:00:00Z');

CREATE TABLE workflow_run (
  run_key TEXT PRIMARY KEY,
  project_key TEXT NOT NULL,
  workflow_key TEXT NOT NULL,
  task_name TEXT NOT NULL,
  task_summary TEXT NOT NULL DEFAULT '',
  module_name TEXT NOT NULL DEFAULT 'global',
  mode TEXT NOT NULL DEFAULT 'auto',
  status TEXT NOT NULL DEFAULT 'created',
  current_phase_key TEXT NOT NULL DEFAULT '',
  context_export_path TEXT NOT NULL DEFAULT '',
  recovery_export_path TEXT NOT NULL DEFAULT '',
  checkpoint_id INTEGER,
  started_at TEXT,
  completed_at TEXT,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);
INSERT INTO workflow_run(run_key, project_key, workflow_key, task_name, task_summary, module_name, mode, status, current_phase_key, context_export_path, recovery_export_path, checkpoint_id, started_at, completed_at, created_at, updated_at)
VALUES ('fixture-run', 'demo', 'api-change', 'Fixture workflow task', 'Existing workflow run', 'order', 'api', 'running', 'export_context', '', '', NULL, '2026-01-01T00:00:00Z', '', '2026-01-01T00:00:00Z', '2026-01-01T00:00:00Z');

CREATE TABLE workflow_phase_run (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  run_key TEXT NOT NULL,
  phase_key TEXT NOT NULL,
  phase_name TEXT NOT NULL,
  phase_order INTEGER NOT NULL,
  status TEXT NOT NULL DEFAULT 'pending',
  started_at TEXT,
  completed_at TEXT,
  input_summary TEXT NOT NULL DEFAULT '',
  output_summary TEXT NOT NULL DEFAULT '',
  evidence TEXT NOT NULL DEFAULT '',
  notes TEXT NOT NULL DEFAULT '',
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);
INSERT INTO workflow_phase_run(run_key, phase_key, phase_name, phase_order, status, started_at, completed_at, input_summary, output_summary, evidence, notes, created_at, updated_at)
VALUES ('fixture-run', 'export_context', 'Export Context', 1, 'running', '2026-01-01T00:00:00Z', '', '', 'Fixture output', '', '', '2026-01-01T00:00:00Z', '2026-01-01T00:00:00Z');

CREATE TABLE workflow_gate_run (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  run_key TEXT NOT NULL,
  phase_key TEXT NOT NULL,
  gate_key TEXT NOT NULL,
  gate_name TEXT NOT NULL,
  gate_type TEXT NOT NULL,
  severity TEXT NOT NULL DEFAULT 'hard',
  status TEXT NOT NULL DEFAULT 'pending',
  checked_at TEXT,
  result_summary TEXT NOT NULL DEFAULT '',
  failure_reason TEXT NOT NULL DEFAULT '',
  evidence TEXT NOT NULL DEFAULT '',
  command_output_path TEXT NOT NULL DEFAULT '',
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);
INSERT INTO workflow_gate_run(run_key, phase_key, gate_key, gate_name, gate_type, severity, status, checked_at, result_summary, failure_reason, evidence, command_output_path, created_at, updated_at)
VALUES ('fixture-run', 'export_context', 'current_context_exists', 'Current Context Exists', 'file_exists', 'hard', 'passed', '2026-01-01T00:00:00Z', 'Context found', '', '', '', '2026-01-01T00:00:00Z', '2026-01-01T00:00:00Z');

CREATE TABLE workflow_event (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  project_key TEXT NOT NULL,
  run_key TEXT NOT NULL,
  event_type TEXT NOT NULL,
  phase_key TEXT NOT NULL DEFAULT '',
  gate_key TEXT NOT NULL DEFAULT '',
  level TEXT NOT NULL DEFAULT 'info',
  message TEXT NOT NULL,
  data TEXT NOT NULL DEFAULT '',
  created_at TEXT NOT NULL
);
INSERT INTO workflow_event(project_key, run_key, event_type, phase_key, gate_key, level, message, data, created_at)
VALUES ('demo', 'fixture-run', 'run_started', '', '', 'info', 'Fixture workflow started', '', '2026-01-01T00:00:00Z');
