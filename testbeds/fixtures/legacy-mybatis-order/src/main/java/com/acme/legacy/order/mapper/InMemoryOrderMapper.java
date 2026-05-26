package com.acme.legacy.order.mapper;

import com.acme.legacy.order.dto.OrderQuery;
import com.acme.legacy.order.dto.OrderRow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InMemoryOrderMapper implements OrderMapper {
    private final List<OrderRow> rows;

    public InMemoryOrderMapper() {
        this.rows = Arrays.asList(
                row("O-1001", "Alice", "PAID", "128.00", "2026-05-01 10:00:00"),
                row("O-1002", "Bob", "NEW", "89.50", "2026-05-02 11:20:00"),
                row("O-1003", "Alice", "CANCELLED", "64.00", "2026-05-03 09:30:00")
        );
    }

    public List<OrderRow> findOrders(OrderQuery query) {
        List<OrderRow> matched = filter(query);
        int offset = (query.getPageNo() - 1) * query.getPageSize();
        int end = Math.min(offset + query.getPageSize(), matched.size());
        if (offset >= matched.size()) {
            return new ArrayList<OrderRow>();
        }
        return new ArrayList<OrderRow>(matched.subList(offset, end));
    }

    public int countOrders(OrderQuery query) {
        return filter(query).size();
    }

    private List<OrderRow> filter(OrderQuery query) {
        List<OrderRow> matched = new ArrayList<OrderRow>();
        for (OrderRow row : rows) {
            if (query.getCustomerName() != null && !row.getCustomerName().contains(query.getCustomerName())) {
                continue;
            }
            if (query.getStatus() != null && !query.getStatus().equals(row.getStatus())) {
                continue;
            }
            if (Boolean.TRUE.equals(query.getPaidOnly()) && !"PAID".equals(row.getStatus())) {
                continue;
            }
            matched.add(row);
        }
        return matched;
    }

    private static OrderRow row(String orderId, String customerName, String status, String totalAmount,
                                String createdAt) {
        OrderRow row = new OrderRow();
        row.setOrderId(orderId);
        row.setCustomerName(customerName);
        row.setStatus(status);
        row.setTotalAmount(totalAmount);
        row.setCreatedAt(createdAt);
        return row;
    }
}
