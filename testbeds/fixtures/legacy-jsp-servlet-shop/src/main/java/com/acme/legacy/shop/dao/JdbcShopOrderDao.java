package com.acme.legacy.shop.dao;

import com.acme.legacy.shop.dto.ShopOrderQuery;
import com.acme.legacy.shop.dto.ShopOrderRow;
import com.acme.legacy.shop.util.LegacyStrings;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class JdbcShopOrderDao implements ShopOrderDao {
    private final List<ShopOrderRow> rows;

    public JdbcShopOrderDao() {
        this(sampleRows());
    }

    public JdbcShopOrderDao(List<ShopOrderRow> rows) {
        this.rows = rows == null ? new ArrayList<ShopOrderRow>() : rows;
    }

    public List<ShopOrderRow> findOrders(ShopOrderQuery query) {
        List<ShopOrderRow> matched = new ArrayList<ShopOrderRow>();
        for (ShopOrderRow row : rows) {
            if (matches(row, query)) {
                matched.add(row);
            }
        }
        return matched;
    }

    public int countOrders(ShopOrderQuery query) {
        return findOrders(query).size();
    }

    private boolean matches(ShopOrderRow row, ShopOrderQuery query) {
        if (query == null) {
            return true;
        }
        String customerNo = LegacyStrings.trimToNull(query.getCustomerNo());
        if (customerNo != null && !row.getCustomerNo().contains(customerNo)) {
            return false;
        }
        String status = LegacyStrings.trimToNull(query.getStatus());
        return status == null || status.equals(row.getStatus());
    }

    private static List<ShopOrderRow> sampleRows() {
        List<ShopOrderRow> sample = new ArrayList<ShopOrderRow>();
        sample.add(row("SO-1001", "C001", "Alice", "PAID", "120.00"));
        sample.add(row("SO-1002", "C002", "Bob", "CREATED", "85.50"));
        sample.add(row("SO-1003", "C001", "Alice", "CANCELLED", "10.00"));
        return sample;
    }

    private static ShopOrderRow row(String orderNo, String customerNo, String customerName,
                                    String status, String amount) {
        ShopOrderRow row = new ShopOrderRow();
        row.setOrderNo(orderNo);
        row.setCustomerNo(customerNo);
        row.setCustomerName(customerName);
        row.setStatus(status);
        row.setTotalAmount(new BigDecimal(amount));
        return row;
    }
}
