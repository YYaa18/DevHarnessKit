package com.acme.legacy.order.service;

import com.acme.legacy.order.dto.OrderQuery;
import com.acme.legacy.order.dto.OrderView;
import com.acme.legacy.order.mapper.InMemoryOrderMapper;
import com.acme.legacy.order.util.PageResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OrderServiceTest {
    private final OrderService service = new OrderService(new InMemoryOrderMapper());

    @Test
    void filtersByStatus() {
        OrderQuery query = new OrderQuery();
        query.setStatus("PAID");
        query.setPageNo(1);
        query.setPageSize(10);

        PageResult<OrderView> result = service.searchOrders(query);

        assertEquals(1, result.getTotal());
        assertEquals("O-1001", result.getRows().get(0).getOrderId());
    }

    @Test
    void normalizesPageNoBelowOne() {
        OrderQuery query = new OrderQuery();
        query.setPageNo(0);
        query.setPageSize(2);

        PageResult<OrderView> result = service.searchOrders(query);

        assertEquals(1, result.getPageNo());
        assertEquals(2, result.getRows().size());
    }

    @Test
    void filtersPaidOnlyOrders() {
        OrderQuery query = new OrderQuery();
        query.setPaidOnly(Boolean.TRUE);
        query.setPageNo(1);
        query.setPageSize(10);

        PageResult<OrderView> result = service.searchOrders(query);

        assertTrue(result.getRows().size() > 0);
        assertEquals("PAID", result.getRows().get(0).getStatus());
    }
}
