package com.acme.legacy.shop.web;

import com.acme.legacy.shop.dao.JdbcShopOrderDao;
import com.acme.legacy.shop.dto.ShopOrderQuery;
import com.acme.legacy.shop.dto.ShopOrderView;
import com.acme.legacy.shop.service.ShopOrderService;
import com.acme.legacy.shop.util.PageResult;

import java.util.Map;

public class ShopOrderServlet extends LegacyBaseServlet {
    private final ShopOrderService orderService;

    public ShopOrderServlet() {
        this(new ShopOrderService(new JdbcShopOrderDao()));
    }

    public ShopOrderServlet(ShopOrderService orderService) {
        this.orderService = orderService;
    }

    public PageResult<ShopOrderView> search(Map<String, String[]> parameters) {
        ShopOrderQuery query = new ShopOrderQuery();
        query.setCustomerNo(first(parameters, "customerNo"));
        query.setStatus(first(parameters, "status"));
        query.setLegacyAction(first(parameters, "legacyAction"));
        query.setPageNo(intParam(parameters, "pageNo"));
        query.setPageSize(intParam(parameters, "pageSize"));
        return orderService.searchOrders(query);
    }
}
