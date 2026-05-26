# Task: Tighten Paid Order Filter

## Task ID

`legacy-mybatis-order-002`

## User Request

The `paidOnly` search option should return only completed paid orders and must
not include cancelled orders or newly created unpaid orders.

## Background

The legacy search has both Java test data and MyBatis XML SQL. Keep them
consistent so Graph-aware impact analysis can catch both sides.

## Constraints

- Keep the public route unchanged.
- Do not modify production configuration.
- Update tests for the stricter paid-only behavior.

## Required Verification

```bash
mvn -q -f testbeds/fixtures/legacy-mybatis-order/pom.xml test
```

## Manual Evidence

Record the SQL condition that changed and the matching in-memory mapper
condition used by tests.
