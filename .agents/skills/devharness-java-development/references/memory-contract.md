# Memory Contract

- `memory add` always writes draft memory.
- Draft memory is searchable for review, but it is not exported by default.
- `memory confirm` is the only MVP path that makes memory exportable.
- `memory export` includes confirmed memory with confidence at least 70 and filters sensitive content again.
- Raw database credentials, JDBC URLs, tokens, Authorization headers, and raw SQL result sets must not be stored in memory.
