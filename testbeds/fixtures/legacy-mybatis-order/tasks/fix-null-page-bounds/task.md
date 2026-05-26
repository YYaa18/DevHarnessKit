# Task: Fix Null And Invalid Page Bounds

## Task ID

`legacy-mybatis-order-003`

## User Request

The legacy order search should handle null, zero, negative, and oversized page
sizes safely. Normalize invalid page sizes to the default and cap oversized
values at the legacy maximum.

## Background

`LegacyPageBounds` currently handles null page size but does not guard every
invalid value. This is a small bugfix task with a narrow impact surface.

## Constraints

- Do not change SQL or mapper XML.
- Do not modify production configuration.
- Add focused tests for null, zero, negative, and oversized page sizes.

## Required Verification

```bash
mvn -q -f testbeds/fixtures/legacy-mybatis-order/pom.xml test
```

## Manual Evidence

Record the before/after normalization rules and confirm mapper XML was not
changed.
