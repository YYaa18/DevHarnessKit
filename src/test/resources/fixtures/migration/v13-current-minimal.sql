CREATE TABLE schema_version (
  version INTEGER PRIMARY KEY,
  description TEXT NOT NULL,
  applied_at TEXT NOT NULL
);

INSERT INTO schema_version(version, description, applied_at) VALUES
  (1, 'MVP memory schema', '2026-05-21T00:00:00Z'),
  (2, 'V0.2 workflow persistence schema', '2026-05-21T00:00:00Z'),
  (3, 'V0.2-B workflow artifact binding schema', '2026-05-21T00:00:00Z'),
  (4, 'V0.3 spec persistence schema', '2026-05-21T00:00:00Z'),
  (5, 'V0.4 goal orchestration schema', '2026-05-21T00:00:00Z'),
  (6, 'V0.4 goal check freshness schema', '2026-05-21T00:00:00Z'),
  (7, 'V0.4 goal context export failure schema', '2026-05-21T00:00:00Z'),
  (8, 'V0.4 goal workflow/spec sync schema', '2026-05-21T00:00:00Z'),
  (9, 'V0.4 graph lite schema', '2026-05-21T00:00:00Z'),
  (10, 'V0.4 BDD acceptance schema', '2026-05-21T00:00:00Z'),
  (11, 'V0.4 skill contract schema', '2026-05-21T00:00:00Z'),
  (12, 'V0.4 human checkpoint schema', '2026-05-21T00:00:00Z'),
  (13, 'V0.4 skill trust hardening schema', '2026-05-21T00:00:00Z');
