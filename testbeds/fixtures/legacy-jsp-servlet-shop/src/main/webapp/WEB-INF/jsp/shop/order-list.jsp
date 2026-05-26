<%@ page contentType="text/html; charset=UTF-8" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>
<html>
<head>
    <title>Order Search</title>
</head>
<body>
<form id="orderSearchForm" method="post" action="${pageContext.request.contextPath}/shop/orders/search">
    <input type="hidden" name="legacyAction" value="orderList"/>
    <label>Customer No</label>
    <input type="text" name="customerNo" value="${param.customerNo}"/>
    <label>Status</label>
    <select name="status">
        <option value="">All</option>
        <option value="CREATED">Created</option>
        <option value="PAID">Paid</option>
        <option value="CANCELLED">Cancelled</option>
    </select>
    <button type="submit">Search</button>
</form>
</body>
</html>
