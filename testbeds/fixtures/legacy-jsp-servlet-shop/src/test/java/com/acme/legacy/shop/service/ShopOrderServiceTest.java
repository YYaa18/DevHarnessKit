package com.acme.legacy.shop.service;

import com.acme.legacy.shop.dao.JdbcShopOrderDao;
import com.acme.legacy.shop.dto.ShopOrderQuery;
import com.acme.legacy.shop.dto.ShopOrderView;
import com.acme.legacy.shop.util.PageResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShopOrderServiceTest {
    @Test
    void filtersOrdersByCustomerNo() {
        ShopOrderService service = new ShopOrderService(new JdbcShopOrderDao());
        ShopOrderQuery query = new ShopOrderQuery();
        query.setCustomerNo("C001");

        PageResult<ShopOrderView> result = service.searchOrders(query);

        assertEquals(2, result.getTotal());
        assertEquals("C001", result.getItems().get(0).getCustomerNo());
    }

    @Test
    void mapsStatusTextForPaidOrders() {
        ShopOrderService service = new ShopOrderService(new JdbcShopOrderDao());
        ShopOrderQuery query = new ShopOrderQuery();
        query.setStatus("PAID");

        PageResult<ShopOrderView> result = service.searchOrders(query);

        assertEquals(1, result.getTotal());
        assertEquals("Paid", result.getItems().get(0).getStatusText());
    }
}
