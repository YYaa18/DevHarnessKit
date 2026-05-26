package com.acme.legacy.order.mapper;

import com.acme.legacy.order.dto.OrderQuery;
import com.acme.legacy.order.dto.OrderRow;

import java.util.List;

public interface OrderMapper {
    List<OrderRow> findOrders(OrderQuery query);

    int countOrders(OrderQuery query);
}
