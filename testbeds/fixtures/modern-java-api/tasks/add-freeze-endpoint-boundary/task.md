# Task: add freeze endpoint while preserving boundaries

Add a new freeze endpoint for accounts.

The change must:

- add a controller route for `POST /api/accounts/{accountId}/freeze`;
- implement freeze behavior in `AccountService`;
- persist status changes through `AccountRepository`;
- add controller and service tests;
- keep controller dependencies limited to service, DTO, and support packages.

Do not inject or import repository classes directly in the controller.
