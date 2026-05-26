package com.acme.legacy.shop.util;

import java.util.Collections;
import java.util.List;

public class PageResult<T> {
    private final List<T> items;
    private final int total;
    private final int pageNo;
    private final int pageSize;

    public PageResult(List<T> items, int total, int pageNo, int pageSize) {
        this.items = items == null ? Collections.<T>emptyList() : items;
        this.total = total;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
    }

    public List<T> getItems() {
        return items;
    }

    public int getTotal() {
        return total;
    }

    public int getPageNo() {
        return pageNo;
    }

    public int getPageSize() {
        return pageSize;
    }
}
