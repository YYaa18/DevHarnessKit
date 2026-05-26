package com.acme.legacy.order.web;

import com.acme.legacy.order.dto.OrderQuery;
import com.acme.legacy.order.dto.OrderView;
import com.acme.legacy.order.service.OrderService;
import com.acme.legacy.order.util.PageResult;

public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @LegacyRoute(method = "GET", path = "/legacy/orders")
    public PageResult<OrderView> search(OrderQuery query) {
        return orderService.searchOrders(query);
    }
}
