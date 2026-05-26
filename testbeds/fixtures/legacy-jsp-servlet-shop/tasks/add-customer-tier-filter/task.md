# Task: add customer tier filter

Add a customer tier filter to the legacy order search page.

The change must:

- add a `customerTier` input to the JSP form;
- bind the value in the servlet/controller;
- carry the value through `ShopOrderQuery`;
- apply the filter in service/DAO behavior and SQL resource metadata;
- expose `customerTier` in row/view data where appropriate;
- add regression coverage.

Do not modify production-like configuration or shared base servlet code.
