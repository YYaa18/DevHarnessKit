package com.acme.legacy.order.service;

import com.acme.legacy.order.dto.OrderQuery;
import com.acme.legacy.order.dto.OrderRow;
import com.acme.legacy.order.dto.OrderView;
import com.acme.legacy.order.mapper.OrderMapper;
import com.acme.legacy.order.util.LegacyPageBounds;
import com.acme.legacy.order.util.PageResult;

import java.util.ArrayList;
import java.util.List;

public class OrderService {
    private final OrderMapper orderMapper;

    public OrderService(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    public PageResult<OrderView> searchOrders(OrderQuery query) {
        OrderQuery safeQuery = query == null ? new OrderQuery() : query;
        int pageNo = LegacyPageBounds.normalizePageNo(safeQuery.getPageNo());
        int pageSize = LegacyPageBounds.normalizePageSize(safeQuery.getPageSize());

        safeQuery.setPageNo(pageNo);
        safeQuery.setPageSize(pageSize);

        List<OrderRow> rows = orderMapper.findOrders(safeQuery);
        int total = orderMapper.countOrders(safeQuery);
        List<OrderView> views = new ArrayList<OrderView>();
        for (OrderRow row : rows) {
            views.add(toView(row));
        }
        return new PageResult<OrderView>(views, total, pageNo, pageSize);
    }

    private OrderView toView(OrderRow row) {
        OrderView view = new OrderView();
        view.setOrderId(row.getOrderId());
        view.setCustomerName(row.getCustomerName());
        view.setStatus(row.getStatus());
        view.setTotalAmount(row.getTotalAmount());
        view.setCreatedAt(row.getCreatedAt());
        return view;
    }
}
