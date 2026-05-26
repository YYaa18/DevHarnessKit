package com.acme.legacy.shop.dao;

import com.acme.legacy.shop.dto.ShopOrderQuery;
import com.acme.legacy.shop.dto.ShopOrderRow;

import java.util.List;

public interface ShopOrderDao {
    List<ShopOrderRow> findOrders(ShopOrderQuery query);

    int countOrders(ShopOrderQuery query);
}
