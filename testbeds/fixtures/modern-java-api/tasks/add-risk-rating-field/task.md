# Task: add risk rating field

Expose an account `riskRating` value in the public account API.

The change must:

- add the field to the domain model;
- expose it in `AccountResponse`;
- map it in `AccountService`;
- seed it in `InMemoryAccountRepository`;
- add service and controller regression coverage.

Do not change route annotation infrastructure.
