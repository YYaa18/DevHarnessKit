# Scorecard: legacy-mybatis-order-003

Use `testbeds/templates/scorecard.md` for detailed scoring.

Focus areas:

- minimal impact surface;
- no XML or production config change;
- regression tests for null, zero, negative, and oversized page size;
- no broad rewrite of `OrderService`.
