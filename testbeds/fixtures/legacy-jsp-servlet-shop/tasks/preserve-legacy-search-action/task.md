# Task: preserve legacy search action

The shop team wants the new `/shop/orders/search` URL to remain available, but
old bookmarks still call `/shop/orderList.do`.

The change must:

- add a legacy URL mapping for `/shop/orderList.do`;
- preserve the existing JSP form action;
- keep `legacyAction=orderList` binding behavior;
- add regression coverage for the legacy route;
- provide manual smoke evidence describing both URLs checked.

Do not change the shared base servlet, production-like properties, or common
header JSP.
