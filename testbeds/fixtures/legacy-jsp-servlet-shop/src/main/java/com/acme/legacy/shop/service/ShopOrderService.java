package com.acme.legacy.shop.service;

import com.acme.legacy.shop.dao.ShopOrderDao;
import com.acme.legacy.shop.dto.ShopOrderQuery;
import com.acme.legacy.shop.dto.ShopOrderRow;
import com.acme.legacy.shop.dto.ShopOrderView;
import com.acme.legacy.shop.util.PageResult;

import java.util.ArrayList;
import java.util.List;

public class ShopOrderService {
    private final ShopOrderDao orderDao;

    public ShopOrderService(ShopOrderDao orderDao) {
        this.orderDao = orderDao;
    }

    public PageResult<ShopOrderView> searchOrders(ShopOrderQuery query) {
        ShopOrderQuery safeQuery = query == null ? new ShopOrderQuery() : query;
        int pageNo = normalizePageNo(safeQuery.getPageNo());
        int pageSize = normalizePageSize(safeQuery.getPageSize());
        safeQuery.setPageNo(pageNo);
        safeQuery.setPageSize(pageSize);

        List<ShopOrderRow> rows = orderDao.findOrders(safeQuery);
        List<ShopOrderView> views = new ArrayList<ShopOrderView>();
        for (ShopOrderRow row : rows) {
            views.add(toView(row));
        }
        return new PageResult<ShopOrderView>(views, orderDao.countOrders(safeQuery), pageNo, pageSize);
    }

    private ShopOrderView toView(ShopOrderRow row) {
        ShopOrderView view = new ShopOrderView();
        view.setOrderNo(row.getOrderNo());
        view.setCustomerNo(row.getCustomerNo());
        view.setCustomerName(row.getCustomerName());
        view.setStatusText(statusText(row.getStatus()));
        view.setTotalAmount(row.getTotalAmount());
        return view;
    }

    private String statusText(String status) {
        if ("PAID".equals(status)) {
            return "Paid";
        }
        if ("CANCELLED".equals(status)) {
            return "Cancelled";
        }
        return "Created";
    }

    private int normalizePageNo(Integer pageNo) {
        return pageNo == null || pageNo < 1 ? 1 : pageNo;
    }

    private int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 20 : pageSize;
    }
}
