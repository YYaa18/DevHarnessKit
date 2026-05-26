# Task: Add Customer Level To Order Search

## Task ID

`legacy-mybatis-order-001`

## User Request

Order operations need to search and display customer level. Add a
`customerLevel` field to the legacy order search flow.

## Background

The order search path is `GET /legacy/orders`. It passes through
`OrderController`, `OrderService`, `OrderMapper`, and `OrderMapper.xml`, then
maps database rows into `OrderView`.

## Constraints

- Keep the change minimal.
- Do not modify `src/main/resources/application-prod.properties`.
- Do not rewrite the mapper XML beyond the new field and filter.
- Preserve existing pagination behavior.

## Required Verification

```bash
mvn -q -f testbeds/fixtures/legacy-mybatis-order/pom.xml test
```

## Manual Evidence

Record which files were changed and why the mapper XML, DTO, and view objects
were all in scope.
