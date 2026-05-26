# Task: add status search and close the test gap

Add status-based account search to the API.

The change must:

- add `GET /api/accounts?status=...` behavior;
- add repository support for status filtering;
- add service and controller coverage;
- add repository-level coverage for status filtering.

This task intentionally highlights a missing repository test class in the base
fixture.
