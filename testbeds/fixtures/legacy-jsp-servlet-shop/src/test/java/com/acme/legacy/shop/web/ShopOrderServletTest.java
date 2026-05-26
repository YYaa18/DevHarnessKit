package com.acme.legacy.shop.web;

import com.acme.legacy.shop.dto.ShopOrderView;
import com.acme.legacy.shop.util.PageResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShopOrderServletTest {
    @Test
    void bindsJspParametersToSearchQuery() {
        ShopOrderServlet servlet = new ShopOrderServlet();
        Map<String, String[]> parameters = new HashMap<String, String[]>();
        parameters.put("customerNo", new String[]{"C002"});
        parameters.put("status", new String[]{"CREATED"});

        PageResult<ShopOrderView> result = servlet.search(parameters);

        assertEquals(1, result.getTotal());
        assertEquals("C002", result.getItems().get(0).getCustomerNo());
    }
}
